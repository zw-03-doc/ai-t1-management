"""Initialize only a fresh T1 deployment. Does not change the shared gateway."""
from __future__ import annotations

import json
import argparse
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time


def run(args: list[str], **kwargs):
    return subprocess.run(args, check=True, **kwargs)


def private_file(path: Path, value: str) -> None:
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as stream:
        stream.write(value)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--infrastructure-only", action="store_true")
    args = parser.parse_args()
    release = Path(__file__).resolve().parent
    if not str(release).startswith("/opt/t1-management/releases/"):
        raise SystemExit("Must run from a T1 release directory")
    config = Path("/etc/t1-management")
    marker = Path("/var/lib/t1-management/.initialized")
    if marker.exists():
        raise SystemExit("Database already initialized; use a reviewed migration, not bootstrap")
    for name in ["mysql", "redis", "media", "logs", "temp"]:
        path = Path("/var/lib/t1-management") / name
        path.mkdir(mode=0o700, parents=True, exist_ok=True)
        if name != "mysql":
            os.chown(path, 10001, 10001)
    config.mkdir(mode=0o700, exist_ok=True)
    (config / "secrets").mkdir(mode=0o700, exist_ok=True)
    if not (config / "production.env").exists():
        db_password, root_password, redis_password = [secrets.token_urlsafe(36) for _ in range(3)]
        private_file(config / "secrets/database_password", db_password)
        private_file(config / "secrets/database_root_password", root_password)
        private_file(config / "production.env", "\n".join([
            f"T1_DATABASE_PASSWORD={db_password}", f"T1_REDIS_PASSWORD={redis_password}",
            f"T1_JWT_SECRET_KEY={secrets.token_urlsafe(64)}", "WECHAT_MINI_APPID=wx556bef49cde7a986", "WECHAT_MINI_SECRET=", ""
        ]))
        redis_config = config / "redis.conf"
        private_file(redis_config, f"bind 0.0.0.0\nprotected-mode yes\nrequirepass {redis_password}\ndir /data\nappendonly yes\nmaxmemory 64mb\nmaxmemory-policy noeviction\n")
        os.chown(redis_config, 10001, 10001)
        redis_config.chmod(0o400)
    admin_sql = config / "bootstrap-admin.sql"
    if not admin_sql.is_file():
        raise SystemExit("Missing separately transferred administrator bootstrap SQL")
    compose = ["docker", "compose", "-f", str(release / "compose.yml")]
    run(compose + ["up", "-d", "mysql", "redis"])
    for _ in range(60):
        result = subprocess.run(["docker", "inspect", "t1-management-mysql-1", "--format", "{{.State.Health.Status}}"], capture_output=True, text=True)
        if result.stdout.strip() == "healthy":
            break
        time.sleep(2)
    else:
        raise SystemExit("T1 MySQL health check did not pass")
    if args.infrastructure_only:
        print("T1_INFRASTRUCTURE_READY; database schema has not been initialized")
        return
    query = ["docker", "exec", "-i", "t1-management-mysql-1", "sh", "-c", 'MYSQL_PWD="$(cat /run/secrets/database_root_password)" exec mysql -uroot --default-character-set=utf8mb4 t1_fitness']
    count = run(query, input=b"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='t1_fitness';", capture_output=True)
    if count.stdout.decode().strip().splitlines()[-1] != "0":
        raise SystemExit("T1 database is not empty; refusing to rerun destructive baseline SQL")
    for path in sorted((release / "sql").glob("*.sql")):
        result = subprocess.run(query, input=path.read_bytes(), capture_output=True)
        if result.returncode:
            print(f"SQL_FAILED {path.name}", file=sys.stderr)
            # Bootstrap SQL contains only schema/seed data, never real secrets.
            print(result.stderr.decode(errors="replace"), file=sys.stderr)
            raise SystemExit(result.returncode)
        print(f"SQL_OK {path.name}")
    run(query, input=admin_sql.read_bytes(), capture_output=True)
    admin_sql.unlink()
    private_file(marker, json.dumps({"release": release.name, "initialized_at": time.strftime("%Y-%m-%dT%H:%M:%S%z")}))
    print("T1_DATABASE_INITIALIZED; application activation is a separate step")


if __name__ == "__main__":
    main()
