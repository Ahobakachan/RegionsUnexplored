"""Verify separate real worlds rather than swapping seeds on a live biome source."""
from pathlib import Path
import json
import sys

reports = [json.loads(path.read_text(encoding="utf-8")) for path in Path(sys.argv[1]).rglob("compat-report.json")]
if len(reports) != 3 or any(report.get("success") is not True for report in reports):
    raise SystemExit("Expected three successful world-generation reports")
generations = [report["generation"][0] for report in reports]
if {row["seed"] for row in generations} != {0, 12345, 8675309}:
    raise SystemExit("Wrong world seeds tested")
if len({row["layout_hash"] for row in generations}) != 3:
    raise SystemExit("Different seeds produced identical biome layouts")
for row in sorted(generations, key=lambda row: row["seed"]):
    print(f"Seed {row['seed']}: {row['samples']} samples, {row['cave_fraction']:.2%} cave coverage, "
          f"nearest sampled cave {row['nearest_cave_blocks']:.0f} blocks from origin")
