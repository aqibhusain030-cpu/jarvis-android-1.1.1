"""Persistent Memory Engine: ChromaDB Vector Store + notes.json + reminders.json + SqliteSaver."""
from __future__ import annotations

import hashlib
import json
import math
import re
import sqlite3
import threading
import uuid
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from friday_ultra.config import STATE, friday_tool, get_data_dir

_MEMORY_LOCK = threading.RLock()

try:
    import chromadb  # type: ignore

    CHROMA_AVAILABLE = True
except ImportError:
    chromadb = None  # type: ignore
    CHROMA_AVAILABLE = False

_CHROMA_CLIENTS: dict[str, Any] = {}


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _notes_file() -> Path:
    return get_data_dir() / "notes.json"


def _reminders_file() -> Path:
    return get_data_dir() / "reminders.json"


def _fallback_vectors_file() -> Path:
    return get_data_dir() / "chroma_db" / "fallback_vectors.json"


def _sqlite_db_path() -> Path:
    return get_data_dir() / "checkpoints.sqlite"


def _atomic_write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(path.suffix + ".tmp")
    tmp_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
    tmp_path.replace(path)


def _read_json(path: Path, default: Any) -> Any:
    if not path.is_file():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return default


# ====================== CHROMADB + SEMANTIC VECTOR FALLBACK ======================
def _get_chroma_collection() -> Any | None:
    if not CHROMA_AVAILABLE or chromadb is None:
        return None
    chroma_dir = str(get_data_dir() / "chroma_db")
    with _MEMORY_LOCK:
        if chroma_dir not in _CHROMA_CLIENTS:
            try:
                client = chromadb.PersistentClient(path=chroma_dir)
                _CHROMA_CLIENTS[chroma_dir] = client.get_or_create_collection(
                    name="friday_long_term_memory",
                    metadata={"hnsw:space": "cosine"},
                )
            except Exception:
                _CHROMA_CLIENTS[chroma_dir] = None
        return _CHROMA_CLIENTS[chroma_dir]


def _tokenize(text: str) -> Counter[str]:
    tokens = re.findall(r"[a-z0-9_]{2,}", text.lower())
    return Counter(tokens)


def _cosine_counter(a: Counter[str], b: Counter[str]) -> float:
    if not a or not b:
        return 0.0
    dot = sum(a[k] * b[k] for k in (a.keys() & b.keys()))
    if dot == 0:
        return 0.0
    norm_a = math.sqrt(sum(v * v for v in a.values()))
    norm_b = math.sqrt(sum(v * v for v in b.values()))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return dot / (norm_a * norm_b)


def upsert_vector_memory(doc_id: str, document: str, metadata: dict[str, Any] | None = None) -> None:
    """Persist a document into ChromaDB and maintain the local fallback vector store."""
    meta = dict(metadata or {})
    meta.setdefault("timestamp", _utc_now_iso())

    collection = _get_chroma_collection()
    if collection is not None:
        try:
            clean_meta = {
                k: (v if isinstance(v, (str, int, float, bool)) else str(v))
                for k, v in meta.items()
            }
            collection.upsert(ids=[doc_id], documents=[document], metadatas=[clean_meta])
        except Exception:
            pass

    with _MEMORY_LOCK:
        store: dict[str, Any] = _read_json(_fallback_vectors_file(), {})
        store[doc_id] = {"document": document, "metadata": meta}
        _atomic_write_json(_fallback_vectors_file(), store)


def delete_vector_memory(doc_id: str) -> None:
    """Remove a document from ChromaDB and the fallback vector store."""
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            collection.delete(ids=[doc_id])
        except Exception:
            pass

    with _MEMORY_LOCK:
        store: dict[str, Any] = _read_json(_fallback_vectors_file(), {})
        if doc_id in store:
            del store[doc_id]
            _atomic_write_json(_fallback_vectors_file(), store)


def query_vector_memory(query: str, top_k: int = 5) -> list[dict[str, Any]]:
    """Query ChromaDB semantically, falling back to cosine token retrieval when needed."""
    k = max(1, min(int(top_k), 25))
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            total = collection.count()
            if total > 0:
                res = collection.query(query_texts=[query], n_results=min(k, total))
                docs = (res.get("documents") or [[]])[0]
                metas = (res.get("metadatas") or [[]])[0]
                dists = (res.get("distances") or [[]])[0]
                results: list[dict[str, Any]] = []
                for idx, doc in enumerate(docs):
                    dist = dists[idx] if idx < len(dists) and dists[idx] is not None else 0.0
                    score = round(max(0.0, 1.0 - float(dist)), 4)
                    results.append(
                        {
                            "document": doc,
                            "metadata": metas[idx] if idx < len(metas) else {},
                            "score": score,
                        }
                    )
                if results:
                    return results
        except Exception:
            pass

    with _MEMORY_LOCK:
        store: dict[str, Any] = _read_json(_fallback_vectors_file(), {})
    if not store:
        return []

    q_vec = _tokenize(query)
    scored: list[dict[str, Any]] = []
    q_lower = query.strip().lower()
    for item in store.values():
        doc = str(item.get("document", ""))
        sim = _cosine_counter(q_vec, _tokenize(doc))
        if q_lower and q_lower in doc.lower():
            sim = max(sim, 0.85)
        if sim > 0.0:
            scored.append(
                {
                    "document": doc,
                    "metadata": item.get("metadata", {}),
                    "score": round(sim, 4),
                }
            )
    scored.sort(key=lambda r: r["score"], reverse=True)
    return scored[:k]


def vector_memory_count() -> int:
    """Return total indexed long-term vector memories."""
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            return int(collection.count())
        except Exception:
            pass
    store: dict[str, Any] = _read_json(_fallback_vectors_file(), {})
    return len(store)


def archive_interaction(user_msg: str, assistant_msg: str, thread_id: str = "main") -> None:
    """Automatically index completed conversational turns into long-term memory & SQLite."""
    snippet = f"User ({STATE.user_name}): {user_msg.strip()}\nFRIDAY: {assistant_msg.strip()}"
    doc_id = f"turn-{thread_id}-{uuid.uuid4().hex[:12]}"
    upsert_vector_memory(
        doc_id=doc_id,
        document=snippet[:4000],
        metadata={"type": "conversation", "thread_id": thread_id, "timestamp": _utc_now_iso()},
    )
    save_sqlite_turn(thread_id=thread_id, role="user", content=user_msg)
    save_sqlite_turn(thread_id=thread_id, role="assistant", content=assistant_msg)


# ====================== STRUCTURED NOTES & REMINDERS JSON ======================
def load_notes() -> dict[str, dict[str, Any]]:
    with _MEMORY_LOCK:
        data = _read_json(_notes_file(), {})
        return data if isinstance(data, dict) else {}


def save_notes(notes: dict[str, dict[str, Any]]) -> None:
    with _MEMORY_LOCK:
        _atomic_write_json(_notes_file(), notes)


def load_reminders() -> list[dict[str, Any]]:
    with _MEMORY_LOCK:
        data = _read_json(_reminders_file(), [])
        return data if isinstance(data, list) else []


def save_reminders(reminders: list[dict[str, Any]]) -> None:
    with _MEMORY_LOCK:
        _atomic_write_json(_reminders_file(), reminders)


# ====================== SQLITE SAVER & CONVERSATION LOG ======================
def _init_sqlite_history_table(conn: sqlite3.Connection) -> None:
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS friday_conversation_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            thread_id TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at TEXT NOT NULL
        )
        """
    )
    conn.commit()


def save_sqlite_turn(thread_id: str, role: str, content: str) -> None:
    db_path = _sqlite_db_path()
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with _MEMORY_LOCK:
        conn = sqlite3.connect(str(db_path))
        try:
            _init_sqlite_history_table(conn)
            conn.execute(
                "INSERT INTO friday_conversation_history (thread_id, role, content, created_at) VALUES (?, ?, ?, ?)",
                (thread_id, role, content, _utc_now_iso()),
            )
            conn.commit()
        finally:
            conn.close()


def load_sqlite_turns(thread_id: str, limit: int = 20) -> list[dict[str, str]]:
    db_path = _sqlite_db_path()
    if not db_path.is_file():
        return []
    with _MEMORY_LOCK:
        conn = sqlite3.connect(str(db_path))
        try:
            _init_sqlite_history_table(conn)
            rows = conn.execute(
                """
                SELECT role, content, created_at
                FROM friday_conversation_history
                WHERE thread_id = ?
                ORDER BY id DESC
                LIMIT ?
                """,
                (thread_id, limit),
            ).fetchall()
        finally:
            conn.close()
    rows.reverse()
    return [{"role": r[0], "content": r[1], "created_at": r[2]} for r in rows]


def clear_sqlite_thread(thread_id: str) -> None:
    db_path = _sqlite_db_path()
    if not db_path.is_file():
        return
    with _MEMORY_LOCK:
        conn = sqlite3.connect(str(db_path))
        try:
            _init_sqlite_history_table(conn)
            conn.execute("DELETE FROM friday_conversation_history WHERE thread_id = ?", (thread_id,))
            conn.commit()
        finally:
            conn.close()


def get_checkpointer() -> Any:
    """Create a persistent LangGraph SqliteSaver backed by checkpoints.sqlite."""
    db_path = _sqlite_db_path()
    db_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        from langgraph.checkpoint.sqlite import SqliteSaver  # type: ignore

        conn = sqlite3.connect(str(db_path), check_same_thread=False)
        _init_sqlite_history_table(conn)
        return SqliteSaver(conn)
    except ImportError:
        pass

    try:
        from langgraph.checkpoint.memory import MemorySaver  # type: ignore

        return MemorySaver()
    except ImportError:
        return None


# ====================== MEMORY @tool SKILLS ======================
@friday_tool
def remember(key: str, value: str = "", category: str = "general") -> str:
    """Persist an important fact, preference, or project detail into notes.json and ChromaDB long-term memory.

    Args:
        key: Identifier or short title for the memory (or full note if value is omitted).
        value: Detailed fact or value to store.
        category: Optional organizational tag (e.g. 'preference', 'project', 'security').
    """
    clean_key = key.strip()
    clean_val = value.strip()
    if not clean_val:
        clean_val = clean_key
        digest = hashlib.sha1(clean_val.encode("utf-8")).hexdigest()[:8]
        clean_key = f"note_{digest}"

    now = _utc_now_iso()
    with _MEMORY_LOCK:
        notes = load_notes()
        created = notes.get(clean_key, {}).get("created_at", now)
        notes[clean_key] = {
            "value": clean_val,
            "category": category.strip() or "general",
            "created_at": created,
            "updated_at": now,
        }
        save_notes(notes)

    doc_id = f"note::{clean_key}"
    upsert_vector_memory(
        doc_id=doc_id,
        document=f"[{category}] {clean_key}: {clean_val}",
        metadata={"type": "note", "key": clean_key, "category": category, "timestamp": now},
    )
    return f"Archived '{clean_key}' under [{category}] in structured notes and ChromaDB long-term memory, {STATE.user_name}."


@friday_tool
def recall(query: str = "") -> str:
    """Retrieve stored notes from notes.json matching a key, keyword, or category (leave empty to list all)."""
    notes = load_notes()
    if not notes:
        return f"No structured notes archived yet, {STATE.user_name}."

    q = query.strip().lower()
    matched: list[str] = []
    for k, entry in sorted(notes.items()):
        val = str(entry.get("value", ""))
        cat = str(entry.get("category", "general"))
        updated = str(entry.get("updated_at", ""))[:19]
        if not q or q == "all" or q in k.lower() or q in val.lower() or q in cat.lower():
            matched.append(f"- [{cat}] {k}: {val} (updated {updated})")

    if not matched:
        return f"No structured notes matched '{query}', {STATE.user_name}. Try search_long_term_memory for semantic search."
    return "Stored Notes:\n" + "\n".join(matched)


@friday_tool
def forget(key: str) -> str:
    """Delete a specific note by key from notes.json and ChromaDB long-term memory."""
    clean_key = key.strip()
    with _MEMORY_LOCK:
        notes = load_notes()
        if clean_key not in notes:
            return f"Key '{clean_key}' was not found in structured notes, {STATE.user_name}."
        del notes[clean_key]
        save_notes(notes)

    delete_vector_memory(f"note::{clean_key}")
    return f"Removed '{clean_key}' from structured notes and long-term vector memory, {STATE.user_name}."


@friday_tool
def search_long_term_memory(query: str, top_k: int = 5) -> str:
    """Perform semantic vector search across ChromaDB long-term memory (notes and past conversations)."""
    if not query.strip():
        return "Please provide a non-empty semantic search query."
    hits = query_vector_memory(query=query, top_k=top_k)
    if not hits:
        return f"No relevant long-term vector memories found for '{query}', {STATE.user_name}."

    lines = [f"Long-Term Memory Hits for '{query}':"]
    for idx, hit in enumerate(hits, start=1):
        meta = hit.get("metadata", {}) or {}
        mtype = meta.get("type", "memory")
        ts = str(meta.get("timestamp", ""))[:19]
        lines.append(f"{idx}. (score={hit['score']}, type={mtype}, at={ts}) {hit['document']}")
    return "\n".join(lines)


MEMORY_TOOLS = [remember, recall, forget, search_long_term_memory]
