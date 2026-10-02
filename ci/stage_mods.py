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
    seed = sys.argv[3] if len(sys.argv) > 3 else "0"
    run = destination.parent / f"run-{mode}-{seed}"
    run.mkdir(parents=True, exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (run / "server.properties").write_text(
        f"level-seed={seed}\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=0\n"
        "view-distance=2\nsimulation-distance=2\nmax-tick-time=0\n", encoding="utf-8")
else:
    raise SystemExit(f"Unknown staging mode: {mode}")
