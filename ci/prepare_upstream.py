"""Adapt only the local Citadel jar path in the pinned Alex's Caves checkout."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
build = root / "build.gradle"
text = build.read_text(encoding="utf-8")
old = "implementation files('../Citadel/build/libs/citadel-1.21.1-2.7.0.jar')"
new = "implementation files('../Citadel/build/libs/citadel-1.21.1-2.7.1.jar')"
if text.count(old) != 1:
    raise SystemExit("Pinned upstream dependency changed; inspect before updating")
build.write_text(text.replace(old, new), encoding="utf-8")
