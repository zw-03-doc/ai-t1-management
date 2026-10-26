"""Add the T1 virtual host without recreating existing services."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request


def run(args, **kwargs):
    return subprocess.run(args, check=True, capture_output=True, **kwargs)


def main():
    if Path("/var/lib/cloud/data/instance-id").read_text().strip() != "i-7xvbo7bswbug7bn1m8gt":
        raise SystemExit("Wrong target instance")
    release = Path(__file__).resolve().parent
    if not str(release).startswith("/opt/t1-management/releases/"):
        raise SystemExit("Must run from a T1 release directory")
    with urllib.request.urlopen("http://172.19.0.1:18868/healthz", timeout=10) as response:
        if response.status != 200:
            raise SystemExit("T1 gateway is not healthy")
    run(["openssl", "x509", "-in", "/etc/sunmoonlake-ops/tls/t1/fullchain.pem", "-noout", "-checkhost", "t1.wedodiary.top"])
    container = "sunmoonlake-ops-web-1"
    source = Path("/etc/wedo-studio/nginx.conf")
    current = "/etc/nginx/conf.d/studio.conf"
    include = b"include /etc/nginx/tls/t1/gateway.conf;"
    original_source = source.read_bytes()
    original_live = run(["docker", "exec", container, "cat", current]).stdout
    backup = Path("/opt/t1-management/gateway-backups") / time.strftime("%Y%m%dT%H%M%S")
    backup.mkdir(parents=True, mode=0o700)
    (backup / "studio-template.conf").write_bytes(original_source)
    (backup / "studio-live.conf").write_bytes(original_live)
    destination = Path("/etc/sunmoonlake-ops/tls/t1/gateway.conf")
    if destination.exists():
        shutil.copy2(destination, backup / "previous-t1.conf")
    shutil.copy2(release / "gateway.conf", destination)
    destination.chmod(0o644)
    addition = b"\n# T1 independent virtual host; preserve on subsequent OPS deployments.\n" + include + b"\n"
    try:
        if include not in original_source:
            # Keep the inode because this file is already bind-mounted in Docker.
            source.write_bytes(original_source + addition)
        if include not in original_live:
            run(["docker", "exec", "-i", container, "sh", "-c", "cat > " + current], input=original_live + addition)
        run(["docker", "exec", container, "nginx", "-t"])
        run(["docker", "exec", container, "nginx", "-s", "reload"])
    except Exception:
        source.write_bytes(original_source)
        run(["docker", "exec", "-i", container, "sh", "-c", "cat > " + current], input=original_live)
        if (backup / "previous-t1.conf").exists():
            shutil.copy2(backup / "previous-t1.conf", destination)
        run(["docker", "exec", container, "nginx", "-t"])
        run(["docker", "exec", container, "nginx", "-s", "reload"])
        raise
    result = {"release": release.name, "backup": str(backup), "gateway_sha256": hashlib.sha256(destination.read_bytes()).hexdigest(), "checked_at": time.strftime("%Y-%m-%dT%H:%M:%S%z")}
    for host in ["t1.wedodiary.top", "ops.wedodiary.top", "studio.wedodiary.top"]:
        response = run(["curl", "--max-time", "20", "--resolve", f"{host}:443:127.0.0.1", "--silent", "--show-error", "-o", "/dev/null", "-w", "%{http_code}", f"https://{host}/"])
        result[host] = int(response.stdout.decode())
    old = json.loads(Path("/opt/t1-management/before-t1.json").read_text())
    live = json.loads(run(["docker", "inspect", "sunmoonlake-ops-web-1", "sunmoonlake-ops-api-1", "sunmoonlake-ops-postgres-1"]).stdout)
    result["original_container_ids_unchanged"] = {x["Id"] for x in old["containers"]} == {x["Id"] for x in live}
    (backup / "receipt.json").write_text(json.dumps(result, indent=2))
    print(json.dumps(result, indent=2))
    if not result["original_container_ids_unchanged"] or result["t1.wedodiary.top"] != 200 or result["ops.wedodiary.top"] != 200 or result["studio.wedodiary.top"] not in [200, 302]:
        raise SystemExit("Gateway verification did not pass; inspect receipt")


if __name__ == "__main__":
    main()
