# Soak Test Report

## Goal

Produce the strongest capacity evidence possible in the current environment, and clearly state whether it is strong enough for production approval or whether it still requires a waiver.

## Execution Environment

- Date: `2026-04-08`
- Requested mode: `production-grade soak`
- Achieved mode: `local-compose-substitute`
- Host: single local Docker Desktop stack
- Tool used: `node backend/tests/release/soak_substitute.mjs`
- Duration: `120s`

## Why A Substitute Was Used

The current environment does not provide:

- a real staging or pre-production cluster context
- a real staging cluster
- representative ingress/load balancer behavior
- production-like concurrency topology
- stable multi-node infrastructure for long-duration soak
- guaranteed `k6` runtime on the local Windows host
- artifact-tagged candidate deployment identity

Because of that, this run is a substitute evidence rehearsal, not a production-grade soak.

## Executed Actions

- `DRAMAFLOW_SOAK_DURATION_SECONDS=120 node backend/tests/release/soak_substitute.mjs`

## Results

```json
{
  "durationSeconds": 120,
  "feedHomeCount": 33272,
  "playbackCreateCount": 33272,
  "adminDramasCount": 33272,
  "feedHomeP95Ms": 0.99,
  "playbackCreateP95Ms": 2.28,
  "adminDramasP95Ms": 1.16,
  "failureCount": 0
}
```

## Interpretation

- No request failures occurred during the substitute run.
- Observed local P95 values were comfortably below the current local thresholds.
- The result is useful as a regression signal: the current stack did not immediately collapse under sustained local request loops.

## Why This Still Does Not Unlock Production

1. The test ran on one local machine, not a staging or production-like environment.
2. It did not cover long-duration resource creep, background queue pressure, or network variance.
3. It did not validate horizontally scaled behavior or alert fatigue across a real watch window.

## Conclusion

- Substitute soak evidence: `pass`
- Production soak evidence: `not achieved`
- Waiver status: `not granted`
- Production release impact: `still blocked`

## Follow-up Actions

1. Run a true soak in staging or a production-like environment for at least one longer watch window.
2. Include Prometheus snapshots for latency, error rate, CPU, memory, DB saturation, and Redis saturation.
3. Attach operator sign-off to the release evidence pack.
4. If soak still cannot be run, request a formal written waiver instead of silently carrying the gap.
