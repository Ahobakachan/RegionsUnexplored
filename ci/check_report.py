"""Make startup failures or failing assertions fail Actions even after a clean server shutdown."""
import json
from pathlib import Path
import sys

report = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(json.dumps(report, indent=2))
if report.get("success") is not True:
    raise SystemExit("Integration checks failed")
