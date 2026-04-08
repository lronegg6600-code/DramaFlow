from __future__ import annotations

import json
import os
import sys
import urllib.request
import urllib.error

RUN = os.getenv("DRAMAFLOW_RUN_INTEGRATION") == "1"
BASE = os.getenv("DRAMAFLOW_BASE_URL", "http://localhost")

if not RUN:
    print("[integration] skipped. set DRAMAFLOW_RUN_INTEGRATION=1 to execute.")
    sys.exit(0)


def request(url: str, method: str = "GET", payload: dict | None = None, headers: dict | None = None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode("utf-8"))


def main():
    request(f"{BASE}:8088/health/live")
    request(f"{BASE}:8083/health/live")
    request(f"{BASE}:8085/health/live")

    _, login_payload = request(
        f"{BASE}:8088/v1/admin/auth/login",
        method="POST",
        payload={"email": "admin@dramaflow.local", "password": "change-me-now"},
        headers={"Content-Type": "application/json"},
    )
    if "data" not in login_payload:
        raise RuntimeError("admin login failed")

    request(f"{BASE}:8083/v1/feed/home")
    print("[integration] smoke checks passed")


if __name__ == "__main__":
    try:
        main()
    except (urllib.error.HTTPError, urllib.error.URLError, RuntimeError) as exc:
        print(f"[integration] failed: {exc}")
        sys.exit(1)
