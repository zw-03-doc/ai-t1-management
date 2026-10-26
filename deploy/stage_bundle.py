"""Create a self-contained release bundle; real secrets remain in deploy/local/."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import tarfile
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", required=True)
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--admin", required=True, type=Path)
    parser.add_argument("--h5", required=True, type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[a-zA-Z0-9-]+", args.release):
        raise SystemExit("Invalid release name")
    for path in [args.jar, args.admin / "index.html", args.h5 / "index.html"]:
        if not path.is_file():
            raise SystemExit(f"Missing build artifact: {path}")
    deploy = Path(__file__).resolve().parent
    repo = deploy.parent
    target = deploy / "local" / args.release
    target.mkdir(parents=True, exist_ok=False)
    for name in ["compose.yml", "application-production.yml", "web.conf", "gateway.conf", "initialize_server.py", "activate_gateway.py"]:
        shutil.copy2(deploy / name, target / name)
    shutil.copy2(args.jar, target / "app.jar")
    shutil.copytree(args.admin, target / "admin")
    shutil.copytree(args.h5, target / "h5")
    sql_root = repo / "backend/T1-RuoYi-Vue-Plus/script/sql"
    sql_files = [sql_root / "01-框架基础" / name for name in ["ry_vue_5.X.sql", "ry_job.sql", "ry_workflow.sql"]]
    sql_files += [sql_root / "02-项目业务基线-V1.0.1" / name for name in ["01_建表.sql", "02_字典初始化.sql", "03_菜单权限.sql", "05_业务编号补齐.sql"]]
    # V1.2.0 replaces V1.1.3 without deleting history or blocking cancellation/rebooking.
    upgrades = [p for p in (sql_root / "03-业务版本升级").glob("*.sql") if not p.name.startswith("V1.1.3_")]
    sql_files += sorted(upgrades, key=lambda p: tuple(int(n) for n in re.match(r"V([\d.]+)", p.name)[1].split(".")))
    sql_target = target / "sql"
    sql_target.mkdir()
    sql_sources = []
    for index, source in enumerate(sql_files, 1):
        content = source.read_text(encoding="utf-8-sig")
        # Framework examples are not live storage configuration.
        content = re.sub(r"(?im)^insert into sys_oss_config[^\n]*\n?", "", content)
        name = f"{index:03}.sql"
        (sql_target / name).write_text(content, encoding="utf-8")
        sql_sources.append({"file": name, "source": source.relative_to(repo).as_posix()})
    manifest = {
        "release": args.release,
        "sql_sources": sql_sources,
        "sha256": {p.relative_to(target).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in target.rglob("*") if p.is_file()},
    }
    (target / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    archive = target.with_suffix(".tar.gz")
    with tarfile.open(archive, "w:gz") as bundle:
        bundle.add(target, arcname=args.release)
    print(archive)


if __name__ == "__main__":
    main()
