from pathlib import Path
import hashlib
from datetime import datetime, timezone

ROOT = Path('.')
OUT = ROOT/'RELEASE_MANIFEST.md'
SKIP = {OUT.resolve()}
rows=[]
for p in sorted(ROOT.rglob('*')):
    if not p.is_file() or p.resolve() == OUT.resolve():
        continue
    if '.git' in p.parts:
        continue
    h=hashlib.sha256(p.read_bytes()).hexdigest()
    rows.append((p.as_posix(), p.stat().st_size, h))
text=['# JARVIS Release Manifest','',f'Generated UTC: {datetime.now(timezone.utc).isoformat()}','','| Path | Bytes | SHA-256 |','|---|---:|---|']
text += [f'| `{p}` | {n} | `{h}` |' for p,n,h in rows]
text += ['', f'Total files: {len(rows)}']
OUT.write_text('\n'.join(text)+'\n',encoding='utf-8')
print(f'Wrote {OUT} with {len(rows)} file hashes')
