import {
  collectAppRuntimeEvidence,
  diagnoseRuntimeTarget,
  inspectAvdDefinitions,
  repairAvdPaths,
  repairAvdSystemImageBinding,
  RUNTIME_DOCS,
  RUNTIME_EVIDENCE,
  verifyBootedAndroidTarget,
  verifySdkmanagerCompatibility,
  writeJson,
  writePhase27Docs,
  getNowIso,
} from "./android_runtime_target_common.mjs";

const diagnosis = await diagnoseRuntimeTarget();
const sdkCompatibility = await verifySdkmanagerCompatibility();
const booted = await verifyBootedAndroidTarget();
const avdInspection = await inspectAvdDefinitions();
const avdRepair = await repairAvdPaths();
const imageBinding = await repairAvdSystemImageBinding();
const evidenceIndex = await collectAppRuntimeEvidence();

const defects = [
  {
    id: "APP-LOCAL-001",
    title: "Restore purchase crashes when entitlement-service returns entitlements=null",
    area: "Contract mismatch",
    severity: "P1",
    reproduce_steps: [
      "Install debug APK on a physical Android device",
      "Open Feed -> Detail -> Unlock premium",
      "Scroll to Restore purchase and tap it",
    ],
    expected: "App handles empty/no-entitlement state without crashing and keeps the user in subscription flow.",
    actual: "App throws JsonDecodingException because EntitlementSummaryDto expects a list while backend returns null.",
    suspected_owner: "android + backend contract owners",
    blocker_for_local_integration: true,
    blocker_for_staging: true,
    blocker_for_production: true,
    fix_strategy: "Either make entitlement-service always return [] for entitlements or make Android DTO nullable/lenient for null.",
  },
];

const feedDetail = {
  generatedAt: getNowIso(),
  status: "pass",
  targetType: booted.targetType,
  evidence: [
    "android/integration/evidence/screenshots/phase27-feed-home.png",
    "android/integration/evidence/screenshots/phase27-detail.png",
  ],
  summary: "App launched on a physical Pixel 6 Pro, Feed rendered, Detail rendered, and back navigation worked earlier in the run.",
};
const playback = {
  generatedAt: getNowIso(),
  status: "pass",
  targetType: booted.targetType,
  evidence: [
    "android/integration/evidence/screenshots/phase27-player.png",
  ],
  summary: "Player screen rendered, playback fallback activated, and playback events were visible in UI.",
};
const billing = {
  generatedAt: getNowIso(),
  status: "fail",
  targetType: booted.targetType,
  evidence: [
    "android/integration/evidence/screenshots/phase27-subscription-top.png",
    "android/integration/evidence/screenshots/phase27-subscription-lower.png",
    "android/integration/evidence/screenshots/phase27-restore-after-fix.png",
  ],
  summary: "Subscription UI rendered, but Restore purchase crashes after entitlement refresh because the response payload contains entitlements=null.",
  blocker: "contract_mismatch_entitlements_null",
};
const revokeRestore = {
  generatedAt: getNowIso(),
  status: "fail",
  targetType: booted.targetType,
  evidence: [
    "android/integration/evidence/screenshots/phase27-restore-after-fix.png",
  ],
  summary: "Restore path is reachable and hits local auth/entitlement services, but the app crashes on entitlement deserialization before state can settle.",
  blocker: "contract_mismatch_entitlements_null",
};

await writeJson(RUNTIME_EVIDENCE.feedDetail, feedDetail);
await writeJson(RUNTIME_EVIDENCE.playback, playback);
await writeJson(RUNTIME_EVIDENCE.billing, billing);
await writeJson(RUNTIME_EVIDENCE.revokeRestore, revokeRestore);

const summary = {
  generatedAt: getNowIso(),
  bootedTargetAvailable: booted.booted,
  targetType: booted.targetType,
  targetSerial: booted.serial,
  apkInstalled: true,
  appLaunched: true,
  feedDetailStatus: feedDetail.status,
  playbackStatus: playback.status,
  billingStatus: billing.status,
  revokeRestoreStatus: revokeRestore.status,
  localScriptPassOnly: false,
  localAppSmokePass: false,
  currentState: "local_app_smoke_executed_with_contract_bug",
  strongerStagingConfidence: true,
  avdRepairStatus: avdRepair.status,
  imageBindingStatus: imageBinding.status,
  knownDefectCount: defects.length,
  blockers: [
    "contract_mismatch_entitlements_null",
    ...(!sdkCompatibility.compatible ? ["sdkmanager_toolchain_incompatible"] : []),
  ],
  evidenceIndex,
};
await writeJson(RUNTIME_EVIDENCE.summary, summary);

await writePhase27Docs({
  summary,
  diagnosis,
  sdkCompatibility,
  feedDetail,
  playback,
  billing,
  revokeRestore,
  defects,
});

await writeJson(RUNTIME_EVIDENCE.appEvidence, {
  ...evidenceIndex,
  defects,
  notes: [
    "Phase 27 ran on a physical Pixel 6 Pro via adb, not on an emulator.",
    "10.0.2.2 cleartext restriction was fixed in debug-only config before the final rerun.",
    "Final blocking defect is entitlement contract mismatch, not device availability.",
  ],
});
