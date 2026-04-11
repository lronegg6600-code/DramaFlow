# Failure Triage Guide

- `Android bug`: request built incorrectly, DTO mapping wrong, client state does not refresh.
- `Backend bug`: endpoint fails, wrong payload, wrong error code, broken entitlement/playback logic.
- `Contract mismatch`: Android request/response expectations differ from backend contract.
- `Environment/config bug`: missing base URL, token injection, staging credentials, or missing source checkout.
- `Data/setup bug`: no seed data, invalid test products, missing purchase token path.
