from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
INDEX = ROOT / "api" / "openapi" / "index.yaml"

REQUIRED_SPECS = {
    "admin-service": ["/v1/admin/auth/login", "resource.not_found"],
    "billing-service": ["/v1/billing/google-play/purchases:sync", "request.invalid"],
    "entitlement-service": ["/v1/entitlements/grants", "auth.forbidden"],
    "playback-service": ["/v1/playback/sessions", "playback.entitlement_required"],
}

index_text = INDEX.read_text(encoding="utf-8")
for service in REQUIRED_SPECS:
    spec_path = ROOT / "services" / service / "openapi" / "openapi.yaml"
    if service not in index_text:
        print(f"[contract] missing service in index: {service}")
        sys.exit(1)
    if not spec_path.exists():
        print(f"[contract] missing spec file: {spec_path}")
        sys.exit(1)

    spec_text = spec_path.read_text(encoding="utf-8")
    if "openapi:" not in spec_text or "paths:" not in spec_text:
        print(f"[contract] invalid spec header: {service}")
        sys.exit(1)
    for needle in REQUIRED_SPECS[service]:
        if needle not in spec_text:
          print(f"[contract] missing contract marker '{needle}' in {service}")
          sys.exit(1)

print("[contract] openapi checks passed")
