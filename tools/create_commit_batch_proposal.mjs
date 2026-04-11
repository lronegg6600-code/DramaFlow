import { execFile } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);
const ROOT = "Z:/Projects/DramaFlow";

const BATCHES = [
  {
    id: "B1",
    title: "Android and entitlement runtime fix",
    include: [
      /^android\/core\/network\//,
      /^backend\/services\/entitlement-service\//,
      /^backend\/tests\/integration\/entitlement_empty_list_contract\.mjs$/,
      /^docs\/entitlement-contract-fix-report\.md$/,
      /^docs\/phase28-contract-fix-report\.md$/,
      /^release-evidence\/entitlement-/,
      /^release-evidence\/backend-entitlement-empty-list-test\.json$/,
      /^release-evidence\/mobile-go-no-go\.json$/,
      /^release-evidence\/mobile-backend-readiness\.json$/,
    ],
  },
  {
    id: "B2",
    title: "Android local integration and burn-in execution pack",
    include: [
      /^android\/integration\//,
      /^android\/app\/src\/debug\//,
      /^android\/core\/network\/src\/test\//,
      /^backend\/tests\/integration\/mobile_/,
      /^backend\/tests\/integration\/billing_restore_no_crash_flow\.mjs$/,
      /^backend\/tests\/integration\/_mobile_integration_helpers\.mjs$/,
      /^backend\/deployments\/scripts\/(run_android_|capture_android_|collect_android_|diagnose_android_|verify_android_|install_debug_apk_to_target|launch_app_on_target|export_android_|summarize_android_|repair_avd_|inspect_android_sdk_assets|inspect_avd_definitions|prepare_emulator_boot_command|run_final_device_)/,
      /^docs\/android-/,
      /^docs\/mobile-local-/,
      /^docs\/mobile-integration-/,
      /^docs\/phase1[6-9]-/,
      /^docs\/phase2[5-9]-/,
      /^docs\/phase3[0-3]-/,
      /^release-evidence\/android-/,
      /^release-evidence\/mobile-/,
      /^release-evidence\/local-/,
    ],
  },
  {
    id: "B3",
    title: "Workspace recovery and source rehydration tooling",
    include: [
      /^tools\//,
      /^backend\/deployments\/scripts\/(audit_workspace_layout|find_source_tree_candidates|find_git_root_candidates|verify_android_source_tree|verify_backend_source_tree|relink_workspace_if_sources_found|bootstrap_recovered_workspace|rerun_mobile_readiness_if_recovered|export_workspace_recovery_report|rehydrate_repo_workspace|relink_recovered_source_trees|verify_rehydrated_)/,
      /^docs\/workspace-recovery-/,
      /^docs\/source-of-truth-candidates\.md$/,
      /^docs\/source-restore-go-no-go\.md$/,
      /^docs\/source-recovery-/,
      /^docs\/external-source-recovery-request\.md$/,
      /^docs\/missing-source-inventory\.md$/,
      /^docs\/minimal-repo-restore-requirements\.md$/,
      /^docs\/repo-restore-handoff\.md$/,
      /^docs\/recovered-workspace-integration-report\.md$/,
      /^docs\/rehydrated-workspace-readiness\.md$/,
      /^docs\/repo-rehydration-report\.md$/,
      /^release-evidence\/(workspace-|git-root-candidates|android-source-candidates|backend-source-candidates|recovered-|rehydrated-|missing-source-|repo-restore-requirements|external-source-recovery-request|source-recovery-)/,
    ],
  },
  {
    id: "B4",
    title: "Staging input and external URL unblock operations",
    include: [
      /^backend\/deployments\/scripts\/(accept_|reject_|validate_|ingest_|fetch_.*(staging|external|source_recovery)|dispatch_.*(staging|external|source_recovery)|process_.*(staging|external|source_recovery)|summarize_.*(staging|external|source_recovery)|build_.*staging|extract_internal_service_map|close_invalid_candidate_cycle|close_source_recovery_blockers|reopen_source_recovery_blockers|mark_source_recovery_|rerun_mobile_.*external|accept_verified_real_external_urls)/,
      /^docs\/staging-/,
      /^docs\/source-recovery-(acceptance|burndown|escalation|intake|reminder|reply|status-transition|validation)-report\.md$/,
      /^docs\/phase2[0-4]-/,
      /^platform-intake\/(examples|received|escalation)\//,
      /^platform-intake\/README\.md$/,
      /^release-evidence\/staging-/,
      /^release-evidence\/source-recovery-/,
      /^release-evidence\/staging-return-handoff\.json$/,
      /^release-evidence\/staging-url-layer-separation\.json$/,
    ],
  },
  {
    id: "B5",
    title: "Project tracking state synchronization",
    include: [
      /^docs\/unblock-status-board\.md$/,
      /^release-evidence\/blocker-/,
      /^release-evidence\/unblock-status-board\.json$/,
      /^release-evidence\/github-project-/,
      /^backend\/deployments\/scripts\/(summarize_local_line_closure|summarize_local_integration_resume|summarize_mobile_integration_resume|summarize_local_integration_resume)\.sh$/,
      /^docs\/local-line-closure-report\.md$/,
    ],
  },
];

const EXCLUDE = [/^android\/local\.properties$/, /^\.env\./];

function parseStatusLine(line) {
  const status = line.slice(0, 2);
  const file = line.slice(3).trim();
  return { status, file };
}

function matchAny(file, patterns) {
  return patterns.some((regex) => regex.test(file));
}

async function getStatusFiles() {
  const { stdout } = await execFileAsync("git", ["-C", ROOT, "status", "--porcelain"]);
  return stdout
    .split(/\r?\n/)
    .filter(Boolean)
    .map(parseStatusLine);
}

function assignBatch(file) {
  if (matchAny(file, EXCLUDE)) {
    return "EXCLUDE";
  }
  for (const batch of BATCHES) {
    if (matchAny(file, batch.include)) {
      return batch.id;
    }
  }
  return "UNMAPPED";
}

function buildMarkdown(payload) {
  const lines = [];
  lines.push("# Commit Batch Plan");
  lines.push("");
  lines.push(`Generated: ${payload.generatedAt}`);
  lines.push(`Total changed files: ${payload.totalFiles}`);
  lines.push("");
  lines.push("## Excluded");
  lines.push("");
  lines.push("These files are intentionally excluded from commit by default:");
  lines.push("- `android/local.properties`");
  lines.push("- `.env.*`");
  lines.push("");
  for (const batch of payload.batches) {
    lines.push(`## ${batch.id} - ${batch.title}`);
    lines.push("");
    lines.push(`Files: ${batch.files.length}`);
    lines.push("");
    lines.push("Suggested commands:");
    lines.push("```powershell");
    lines.push(`git -C "Z:\\Projects\\DramaFlow" add ${batch.files.length > 0 ? batch.files.map((f) => `"${f}"`).join(" ") : "<no-files>"}`);
    lines.push(`git -C "Z:\\Projects\\DramaFlow" commit -m "${batch.commitMessage}"`);
    lines.push("```");
    lines.push("");
  }
  lines.push("## Unmapped");
  lines.push("");
  lines.push(`Files: ${payload.unmapped.length}`);
  lines.push("");
  payload.unmapped.slice(0, 50).forEach((file) => lines.push(`- \`${file}\``));
  lines.push("");
  return `${lines.join("\n")}\n`;
}

async function main() {
  const files = await getStatusFiles();
  const assigned = files.map(({ status, file }) => ({ status, file, batch: assignBatch(file) }));

  const batchPayloads = BATCHES.map((batch) => {
    const batchFiles = assigned.filter((item) => item.batch === batch.id).map((item) => item.file);
    return {
      id: batch.id,
      title: batch.title,
      commitMessage: `chore(${batch.id.toLowerCase()}): ${batch.title.toLowerCase()}`,
      files: batchFiles,
    };
  });

  const payload = {
    generatedAt: new Date().toISOString(),
    totalFiles: assigned.length,
    excluded: assigned.filter((item) => item.batch === "EXCLUDE").map((item) => item.file),
    unmapped: assigned.filter((item) => item.batch === "UNMAPPED").map((item) => item.file),
    batches: batchPayloads,
  };

  await mkdir(path.join(ROOT, "release-evidence"), { recursive: true });
  await writeFile(path.join(ROOT, "release-evidence", "commit-batch-proposal.json"), `${JSON.stringify(payload, null, 2)}\n`, "utf8");
  await writeFile(path.join(ROOT, "docs", "commit-batch-plan.md"), buildMarkdown(payload), "utf8");
  console.log(JSON.stringify(payload, null, 2));
}

await main();
