"""Adjust only the upload limit in the already-installed T1 virtual host."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile


def run(*args):
    return subprocess.run(args, check=True, capture_output=True).stdout


def change_limit(config, limit):
    old = b"client_max_body_size 35m;"
    new = b"client_max_body_size 55m;"
    if config.count(b"client_max_body_size") != 1 or config.count(old) + config.count(new) != 1:
        raise ValueError("Unexpected T1 request limit; inspect its current configuration")
    return config.replace(old, new) if limit == 55 else config.replace(new, old)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--limit-mib", choices=(35, 55), required=True, type=int)
    args = parser.parse_args()
    release = Path(__file__).resolve().parent
    if release.parent != Path("/opt/t1-management/releases"):
        raise SystemExit("Expected a staged T1 release")
    if Path("/var/lib/cloud/data/instance-id").read_text().strip() != "i-7xvbo7bswbug7bn1m8gt":
        raise SystemExit("Wrong target instance")
    container = "sunmoonlake-ops-web-1"
    mounted = "/etc/nginx/tls/t1/gateway.conf"
    destination = Path("/etc/sunmoonlake-ops/tls/t1/gateway.conf")
    original = destination.read_bytes()
    if run("docker", "exec", container, "cat", mounted) != original:
        raise SystemExit("T1 host configuration differs from the gateway mount")
    if f"# configuration file {mounted}:".encode() not in run("docker", "exec", container, "nginx", "-T"):
        raise SystemExit("T1 configuration is not included by the current nginx")
    updated = change_limit(original, args.limit_mib)
    expected = change_limit((release / "gateway.conf").read_bytes(), args.limit_mib)
    if expected.splitlines() != updated.splitlines():
        raise SystemExit("T1 gateway differs from the release beyond the upload limit")
    backups = Path("/opt/t1-management/gateway-backups")
    backups.mkdir(exist_ok=True, mode=0o700)
    backup = Path(tempfile.mkdtemp(prefix="t1-upload-", dir=backups)) / "previous-t1.conf"
    shutil.copy2(destination, backup)
    try:
        if destination.read_bytes() != original:
            raise SystemExit("T1 configuration changed while preparing the update")
        if original != updated:
            destination.write_bytes(updated)  # Retain the inode of the existing volume mount.
        run("docker", "exec", container, "nginx", "-t")
        run("docker", "exec", container, "nginx", "-s", "reload")
    except Exception:
        destination.write_bytes(original)
        run("docker", "exec", container, "nginx", "-t")
        run("docker", "exec", container, "nginx", "-s", "reload")
        raise
    print(json.dumps({"limitMiB": args.limit_mib, "backup": str(backup), "changed": original != updated}))


if __name__ == "__main__":
    main()
