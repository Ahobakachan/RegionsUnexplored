"""Stage production jars; the checks mod is never part of the downloadable kit."""
from pathlib import Path
import shutil
import sys

mode = sys.argv[1]
destination = Path(sys.argv[2])
destination.mkdir(parents=True, exist_ok=True)
if mode == "build":
    roots = [Path("build/libs"), Path("_upstream/Citadel/build/libs"), Path("_upstream/AlexsCaves/build/libs")]
    for root in roots:
        jars = [p for p in root.glob("*.jar") if not any(s in p.name for s in ("sources", "javadoc", "dev", "data"))]
        if root == Path("build/libs"):
            jars = [p for p in jars if "neoforge" in p.name]
        if len(jars) != 1:
            raise SystemExit(f"Expected exactly one production jar in {root}: {jars}")
        shutil.copy2(jars[0], destination / jars[0].name)
elif mode in ("with-caves", "ru-only"):
    for jar in Path("artifacts/mods").glob("*.jar"):
        if mode == "with-caves" or "regions-unexplored" in jar.name:
            shutil.copy2(jar, destination / jar.name)
    run = destination.parent / "run"
    run.mkdir(parents=True, exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (run / "server.properties").write_text("level-seed=0\nonline-mode=false\nview-distance=2\nsimulation-distance=2\n", encoding="utf-8")
else:
    raise SystemExit(f"Unknown staging mode: {mode}")
