# Android x Backend Risk Register

- `R-MOBILE-001`: No Android source checkout in current workspace.
  Impact: Real integration cannot run.
  Mitigation: Restore Android source tree before mobile smoke.

- `R-MOBILE-002`: No backend source checkout in current workspace.
  Impact: Contract smoke and mobile support scripts cannot target real services from repo context.
  Mitigation: Restore backend tree and staging scripts.

- `R-MOBILE-003`: No git metadata in current workspace.
  Impact: Candidate commit and reproducibility cannot be proven.
  Mitigation: Re-run from a git-backed checkout.
