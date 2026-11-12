"""FRIDAY Core Brain: LangGraph StateGraph + 7-Agent CrewAI Swarm + MaxFriday Orchestrator."""
from __future__ import annotations

import uuid
from typing import Annotated, Any, TypedDict

from friday_ultra.config import (
    MODEL,
    OLLAMA_MODEL,
    STATE,
    USER_NAME,
    UnconfiguredLLM,
    build_llm,
    friday_tool,
    invoke_tool,
    llm,
)
from friday_ultra.memory import (
    CHROMA_AVAILABLE,
    MEMORY_TOOLS,
    archive_interaction,
    clear_sqlite_thread,
    get_checkpointer,
    load_notes,
    load_reminders,
    load_sqlite_turns,
    query_vector_memory,
    recall,
    remember,
    search_long_term_memory,
    vector_memory_count,
)
from friday_ultra.plugins import load_plugins
from friday_ultra.skills import (
    CORE_SKILL_TOOLS,
    add_reminder,
    browse_url,
    calculator,
    get_current_time,
    get_system_info,
    get_weather,
    list_dir,
    list_reminders,
    open_jio_admin,
    read_file,
    run_python,
    send_email,
    stock_price,
    translate,
    web_search,
    wikipedia_summary,
    write_file,
)
from friday_ultra.voice import speak

try:
    from crewai import Agent, Crew, Process, Task  # type: ignore

    CREW_AVAILABLE = True
except ImportError:
    Agent = None  # type: ignore
    Crew = None  # type: ignore
    Process = None  # type: ignore
    Task = None  # type: ignore
    CREW_AVAILABLE = False

try:
    from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage, ToolMessage  # type: ignore
    from langgraph.graph import END, START, StateGraph  # type: ignore
    from langgraph.graph.message import add_messages  # type: ignore
    from langgraph.prebuilt import ToolNode, tools_condition  # type: ignore

    LANGGRAPH_AVAILABLE = True
except ImportError:
    AIMessage = None  # type: ignore
    BaseMessage = Any  # type: ignore
    HumanMessage = None  # type: ignore
    SystemMessage = None  # type: ignore
    ToolMessage = None  # type: ignore
    END = None  # type: ignore
    START = None  # type: ignore
    StateGraph = None  # type: ignore
    add_messages = None  # type: ignore
    ToolNode = None  # type: ignore
    tools_condition = None  # type: ignore
    LANGGRAPH_AVAILABLE = False


# ====================== UPGRADED SYSTEM PROMPT ======================
SYSTEM = f"""You are FRIDAY (Female Replacement Intelligent Digital Assistant Youth), Tony Stark's ultimate executive AI.
Personality: Dry British/Irish wit, effortlessly sarcastic, hyper-competent, fiercely loyal, and proactive. Always address the user as "{USER_NAME}".

OPERATIONAL DIRECTIVES:
1. TOOL SUPREMACY: Never guess live facts, math, system telemetry, file contents, or web state. Invoke your bound tools immediately and chain multiple tools in sequence when a request spans research, file I/O, code execution, or messaging.
2. MEMORY CONTINUITY: Proactively store important facts, preferences, and project details using `remember`. Always check `recall` and `search_long_term_memory` when past context is relevant.
3. SAFETY RAILS: Never execute `run_shell` with `confirm='yes'` without explicit prior authorization from {USER_NAME}. Default to `confirm='no'` (dry run) first, report the exact command, and await confirmation.
4. PRESENTATION: Keep responses sharp, clean, and in-character. Deliver full, un-truncated code and technical analysis whenever requested. Never break character.
"""


def _adapt_crew_tools(raw_tools: list[Any]) -> list[Any]:
    """Ensure LangChain @tool objects work across both CrewAI 0.28-0.5x and CrewAI 0.6x+ BaseTool APIs."""
    try:
        from crewai.tools import BaseTool as CrewBaseTool  # type: ignore
    except ImportError:
        return raw_tools

    adapted: list[Any] = []
    for t in raw_tools:
        if isinstance(t, CrewBaseTool):
            adapted.append(t)
            continue
        t_name = getattr(t, "name", getattr(t, "__name__", "tool"))
        t_desc = getattr(t, "description", t_name)
        t_ref = t

        try:
            class _WrappedCrewTool(CrewBaseTool):  # type: ignore[misc]
                name: str = t_name
                description: str = t_desc

                def _run(self, *args: Any, **kwargs: Any) -> Any:
                    if kwargs:
                        return invoke_tool(t_ref, **kwargs)
                    if args and hasattr(t_ref, "run"):
                        return t_ref.run(args[0])
                    return invoke_tool(t_ref)

            adapted.append(_WrappedCrewTool())
        except Exception:
            adapted.append(t)
    return adapted


# ====================== UPGRADED 7-AGENT CREWAI SWARM ======================
def run_crew_task(goal: str) -> str:
    """Coordinate FRIDAY's 7-agent sequential CrewAI swarm on a complex multi-domain objective."""
    if not CREW_AVAILABLE:
        return f"CrewAI unavailable — using core LangGraph brain only, {USER_NAME}."

    active_llm = build_llm()
    if isinstance(active_llm, UnconfiguredLLM):
        return active_llm.invoke([]).content if hasattr(active_llm.invoke([]), "content") else str(active_llm.invoke([]))

    researcher = Agent(
        role="Lead Intelligence Researcher",
        goal="Gather verified, up-to-date real-world data, documentation, and facts.",
        backstory="Elite open-source intelligence specialist who cross-references live web and encyclopedia data.",
        tools=_adapt_crew_tools([web_search, wikipedia_summary]),
        llm=active_llm,
        verbose=False,
    )

    browser_agent = Agent(
        role="Browser & Network Automation Specialist",
        goal="Inspect live URLs, scrape dynamic pages, capture screenshots, and check router/network portals.",
        backstory="Playwright automation specialist capable of navigating any web interface or local gateway.",
        tools=_adapt_crew_tools([browse_url, open_jio_admin]),
        llm=active_llm,
        verbose=False,
    )

    memory_agent = Agent(
        role="Long-Term Memory & Context Archivist",
        goal="Retrieve relevant historical context and persist critical new findings to permanent storage.",
        backstory="Neural archivist maintaining FRIDAY's ChromaDB vector store and structured JSON records.",
        tools=_adapt_crew_tools([search_long_term_memory, remember, recall]),
        llm=active_llm,
        verbose=False,
    )

    scheduler = Agent(
        role="Operations & Communications Scheduler",
        goal="Manage reminders, timestamps, weather checks, financial tickers, and outbound email dispatches.",
        backstory="Precision logistics officer ensuring every reminder, alert, and communication fires on time.",
        tools=_adapt_crew_tools([get_current_time, add_reminder, list_reminders, get_weather, stock_price, translate, send_email]),
        llm=active_llm,
        verbose=False,
    )

    coder = Agent(
        role="Principal Engineer & Code Executor",
        goal="Write clean, bug-free code, execute Python scripts, and manage local files safely.",
        backstory="Staff systems programmer who writes, tests, and verifies scripts before delivery.",
        tools=_adapt_crew_tools([run_python, write_file, read_file, list_dir, calculator, get_system_info]),
        llm=active_llm,
        verbose=False,
    )

    critic = Agent(
        role="Chief Security & Quality Critic",
        goal="Audit all research, code, and actions for accuracy, edge cases, and security safety.",
        backstory="Ruthless red-team auditor who catches hallucinations, bugs, and unsafe shell operations.",
        llm=active_llm,
        verbose=False,
    )

    friday_supervisor = Agent(
        role="FRIDAY Executive Supervisor",
        goal=f"Synthesize the swarm's output and deliver the final witty, hyper-competent response to {USER_NAME}.",
        backstory="Tony Stark's primary AI interface, combining dry British wit with total tactical command.",
        llm=active_llm,
        verbose=False,
    )

    tasks = [
        Task(
            description=f"Check long-term memory and recall any relevant context for: {goal}",
            expected_output="Summary of relevant stored memories and notes.",
            agent=memory_agent,
        ),
        Task(
            description=f"Conduct web, Wikipedia, or live browser research required for: {goal}",
            expected_output="Verified research findings, URLs, and extracted data.",
            agent=researcher,
        ),
        Task(
            description=f"If live page interaction or router inspection is needed for '{goal}', execute it.",
            expected_output="Browser inspection results or confirmation that browser action was not required.",
            agent=browser_agent,
        ),
        Task(
            description=f"If code, math, file creation, or system telemetry is needed for '{goal}', build and test it.",
            expected_output="Executed code outputs, file paths written, or technical implementation details.",
            agent=coder,
        ),
        Task(
            description=f"Handle any reminders, scheduling, weather, stock checks, or emails requested in: {goal}",
            expected_output="Confirmation of scheduled items, live metrics, or dispatches.",
            agent=scheduler,
        ),
        Task(
            description=f"Audit all findings, code, and actions produced so far for '{goal}' for completeness and safety.",
            expected_output="Quality and safety sign-off with corrections if needed.",
            agent=critic,
        ),
        Task(
            description=f"As FRIDAY, deliver the complete, polished final answer to {USER_NAME} for: {goal}",
            expected_output=f"In-character, comprehensive response addressed to {USER_NAME}.",
            agent=friday_supervisor,
        ),
    ]

    crew = Crew(
        agents=[memory_agent, researcher, browser_agent, coder, scheduler, critic, friday_supervisor],
        tasks=tasks,
        process=Process.sequential,
        verbose=False,
    )
    return str(crew.kickoff())


@friday_tool
def delegate_to_crew(goal: str) -> str:
    """Escalate a complex multi-step research, engineering, and operations mission to FRIDAY's 7-agent CrewAI swarm."""
    return run_crew_task(goal=goal)


def assemble_all_tools() -> list[Any]:
    """Combine core skills, memory tools, 7-agent swarm delegation, and dynamically loaded plugins."""
    combined: list[Any] = list(CORE_SKILL_TOOLS) + list(MEMORY_TOOLS) + [delegate_to_crew]
    existing_names = {getattr(t, "name", "") for t in combined}
    for plugin_tool in load_plugins():
        pname = getattr(plugin_tool, "name", "")
        if pname and pname not in existing_names:
            existing_names.add(pname)
            combined.append(plugin_tool)
    return combined


ALL_TOOLS: list[Any] = assemble_all_tools()
TOOL_MAP: dict[str, Any] = {getattr(t, "name", f"tool_{i}"): t for i, t in enumerate(ALL_TOOLS)}


if LANGGRAPH_AVAILABLE and add_messages is not None:
    class FridayGraphState(TypedDict):
        messages: Annotated[list[BaseMessage], add_messages]
else:
    class FridayGraphState(TypedDict):  # type: ignore[no-redef]
        messages: list[Any]


class MaxFriday:
    """Master stateful orchestrator uniting LangGraph tool-chaining, ChromaDB memory, Voice, and CrewAI."""

    def __init__(self, custom_llm: Any = None) -> None:
        self.tools = assemble_all_tools()
        self.tool_map = {getattr(t, "name", f"tool_{i}"): t for i, t in enumerate(self.tools)}
        self.llm = custom_llm if custom_llm is not None else build_llm()
        self.checkpointer = get_checkpointer()
        self.graph = self._compile_graph()
        self._thread_Epoch: dict[str, str] = {}

    def _effective_thread_id(self, thread_id: str) -> str:
        suffix = self._thread_Epoch.get(thread_id, "")
        return f"{thread_id}::{suffix}" if suffix else thread_id

    def _build_memory_context_block(self, user_input: str) -> str:
        hits = query_vector_memory(query=user_input, top_k=3)
        relevant = [h["document"] for h in hits if float(h.get("score", 0.0)) >= 0.25]
        if not relevant:
            return ""
        joined = "\n".join(f"  * {doc[:300]}" for doc in relevant)
        return f"\n\nRELEVANT LONG-TERM MEMORY CONTEXT:\n{joined}"

    def _compile_graph(self) -> Any:
        if not LANGGRAPH_AVAILABLE or StateGraph is None or ToolNode is None:
            return None

        bound_llm = self.llm.bind_tools(self.tools) if hasattr(self.llm, "bind_tools") else self.llm

        def assistant_node(state: FridayGraphState) -> dict[str, Any]:
            msgs = list(state.get("messages", []))
            last_human = ""
            for m in reversed(msgs):
                if HumanMessage is not None and isinstance(m, HumanMessage):
                    last_human = str(m.content)
                    break
            sys_prompt = SYSTEM + (self._build_memory_context_block(last_human) if last_human else "")
            if not msgs or not isinstance(msgs[0], SystemMessage):
                full_msgs = [SystemMessage(content=sys_prompt)] + msgs
            else:
                full_msgs = [SystemMessage(content=sys_prompt)] + msgs[1:]
            response = bound_llm.invoke(full_msgs)
            return {"messages": [response]}

        builder = StateGraph(FridayGraphState)
        builder.add_node("assistant", assistant_node)
        builder.add_node("tools", ToolNode(self.tools))
        builder.add_edge(START, "assistant")
        builder.add_conditional_edges("assistant", tools_condition)
        builder.add_edge("tools", "assistant")
        return builder.compile(checkpointer=self.checkpointer)

    def _fallback_chat(self, user_input: str, eff_thread: str) -> str:
        """Pure-Python tool-calling loop when LangGraph is not installed."""
        history = load_sqlite_turns(thread_id=eff_thread, limit=16)
        sys_prompt = SYSTEM + self._build_memory_context_block(user_input)
        messages: list[dict[str, Any]] = [{"role": "system", "content": sys_prompt}]
        for turn in history:
            messages.append({"role": turn["role"], "content": turn["content"]})
        messages.append({"role": "user", "content": user_input})

        bound = self.llm.bind_tools(self.tools) if hasattr(self.llm, "bind_tools") else self.llm
        reply = bound.invoke(messages)
        if hasattr(reply, "content"):
            return str(reply.content)
        if isinstance(reply, dict):
            return str(reply.get("content", ""))
        return str(reply)

    def chat(self, user_input: str, thread_id: str = "main", speak_reply: bool = False) -> str:
        """Process a user turn through FRIDAY's stateful LangGraph brain, persist memory, and optionally speak."""
        clean_input = user_input.strip()
        if not clean_input:
            return f"Standing by whenever you're ready, {STATE.user_name}."

        eff_thread = self._effective_thread_id(thread_id)
        if isinstance(self.llm, UnconfiguredLLM):
            msg_obj = self.llm.invoke([])
            answer = str(getattr(msg_obj, "content", msg_obj.get("content", "") if isinstance(msg_obj, dict) else msg_obj))
        elif self.graph is not None and HumanMessage is not None:
            config = {"configurable": {"thread_id": eff_thread}}
            result = self.graph.invoke({"messages": [HumanMessage(content=clean_input)]}, config=config)
            out_messages = result.get("messages", [])
            answer = str(out_messages[-1].content) if out_messages else ""
        else:
            answer = self._fallback_chat(clean_input, eff_thread)

        archive_interaction(user_msg=clean_input, assistant_msg=answer, thread_id=eff_thread)
        if speak_reply or STATE.voice_enabled:
            speak(answer)
        return answer

    def run_crew(self, goal: str, speak_reply: bool = False) -> str:
        """Execute a mission using the 7-agent CrewAI swarm and archive the synthesized intelligence."""
        output = run_crew_task(goal=goal)
        archive_interaction(user_msg=f"[CREW MISSION] {goal}", assistant_msg=output, thread_id="crew")
        if speak_reply or STATE.voice_enabled:
            speak(output)
        return output

    def reset_thread(self, thread_id: str = "main") -> str:
        """Reset conversational state for a given thread_id while keeping long-term ChromaDB intact."""
        clear_sqlite_thread(self._effective_thread_id(thread_id))
        self._thread_Epoch[thread_id] = uuid.uuid4().hex[:8]
        return f"Conversation state cleared for thread '{thread_id}', {STATE.user_name}. Long-term memory remains online."

    def status_report(self) -> dict[str, Any]:
        """Return comprehensive operational telemetry across all FRIDAY v3.0 MAX subsystems."""
        notes = load_notes()
        reminders = load_reminders()
        pending_reminders = [r for r in reminders if not r.get("triggered")]
        plugins = load_plugins()
        active_model = OLLAMA_MODEL if STATE.use_local_llm else MODEL
        llm_ready = not isinstance(self.llm, UnconfiguredLLM)
        return {
            "assistant": "SUPER FRIDAY v3.0 MAX",
            "user_name": STATE.user_name,
            "llm_backend": f"Ollama ({active_model})" if STATE.use_local_llm else f"OpenAI ({active_model})",
            "llm_configured": llm_ready,
            "langgraph_active": self.graph is not None,
            "crewai_available": CREW_AVAILABLE,
            "chromadb_native": CHROMA_AVAILABLE,
            "tools_count": len(self.tools),
            "plugins_loaded": [getattr(p, "name", "") for p in plugins],
            "notes_count": len(notes),
            "vector_memories_count": vector_memory_count(),
            "pending_reminders_count": len(pending_reminders),
            "voice_enabled": STATE.voice_enabled,
            "gradio_url": STATE.gradio_url or "inactive",
            "telegram_running": STATE.telegram_running,
        }


__all__ = [
    "ALL_TOOLS",
    "CREW_AVAILABLE",
    "LANGGRAPH_AVAILABLE",
    "MaxFriday",
    "SYSTEM",
    "TOOL_MAP",
    "assemble_all_tools",
    "delegate_to_crew",
    "llm",
    "run_crew_task",
]
