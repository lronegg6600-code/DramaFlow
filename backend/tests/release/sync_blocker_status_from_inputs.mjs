import fs from "node:fs";
import path from "node:path";

import {
  ensureDir,
  evidenceDir,
  rootDir,
  validationDir,
  writeJson
} from "./_intake_validation_helpers.mjs";

const releaseEvidenceDir = evidenceDir();
const docsDir = path.join(rootDir(), "docs");

function loadValidation(name) {
  const file = path.join(validationDir(), `${name}.json`);
  if (!fs.existsSync(file)) {
    return { items: [] };
  }
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

const validations = {
  repo: loadValidation("repo_identity_input_package"),
  artifact: loadValidation("artifact_identity_input_package"),
  cluster: loadValidation("cluster_access_input_package"),
  github: loadValidation("github_environment_input_package"),
  secrets: loadValidation("secret_inventory_input_package"),
  tooling: loadValidation("deploy_tooling_input_package")
};

const itemIndex = new Map();
for (const payload of Object.values(validations)) {
  for (const item of payload.items ?? []) {
    itemIndex.set(item.blocker_id, item);
  }
}

const generatedAt = new Date().toISOString();

const rows = [
  ["RI-01", "Repository identity", "真实 .git checkout", "not_started", "P0", "yes", "yes", "repo_root", "yaml", "repo owner", "repo admin", "engineering release owner", "T+0", "same_day", "", "目录包含 .git 且指向真实 release checkout", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> repo admin -> engineering manager", "必须是真实 checkout，不接受 zip 包"],
  ["RI-02", "Repository identity", "commit SHA", "not_started", "P0", "yes", "yes", "commit_sha", "yaml", "repo owner", "repo admin", "engineering release owner", "T+0", "same_day", "RI-01", "提供可追溯 SHA", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> repo admin", "应与 workflow / artifact 对齐"],
  ["RI-03", "Repository identity", "branch", "not_started", "P1", "yes", "yes", "branch", "yaml", "repo owner", "repo admin", "engineering release owner", "T+0", "same_day", "RI-01", "提供 release 分支名", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> repo admin", "建议与 release note 一起给出"],
  ["RI-04", "Repository identity", "tag 或 RC version", "not_started", "P0", "yes", "yes", "tag_or_version", "yaml", "release manager", "repo admin", "engineering release owner", "T+0", "same_day", "RI-02", "存在可复用的 tag/version", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> engineering director", "是 production candidate 的身份锚点"],
  ["RI-05", "Repository identity", "changed files summary", "not_started", "P2", "no", "yes", "changed_files_summary", "yaml", "release manager", "repo admin", "engineering release owner", "T+1", "next_business_day", "RI-02", "能看出本次 RC 影响范围", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> repo admin", "可以是 compare link 摘要"],
  ["RI-06", "Repository identity", "release notes 链接", "not_started", "P2", "no", "yes", "release_notes_url", "yaml", "release manager", "repo admin", "engineering release owner", "T+1", "next_business_day", "RI-05", "提供 release notes 或 PR 列表链接", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager", "供审批人快速理解变更"],
  ["RI-07", "Repository identity", "仓库管理员确认", "not_started", "P1", "yes", "yes", "repo_admin_confirmed", "yaml", "repo admin", "release manager", "engineering release owner", "T+0", "same_day", "RI-01", "repo admin 明确确认 checkout 可发版", "node backend/tests/release/validate_repo_input_package.mjs", "release-evidence/repo_identity_input_package.json", "Release manager -> repo admin -> org admin", "yes/no 字段即可"],
  ["AI-01", "Artifact identity", "docker image tag", "not_started", "P0", "yes", "yes", "image_tag", "yaml", "platform", "registry admin", "engineering release owner", "T+0", "same_day", "RI-02", "提供 RC 镜像 tag", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Platform lead -> release manager", "例如 dramaflow/backend:rc-2026.04.08.1"],
  ["AI-02", "Artifact identity", "docker image digest", "not_started", "P0", "yes", "yes", "image_digest", "yaml", "platform", "registry admin", "engineering release owner", "T+0", "same_day", "AI-01", "提供不可变 digest", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Platform lead -> release manager", "没有 digest 不允许 production"],
  ["AI-03", "Artifact identity", "admin web build artifact id", "not_started", "P1", "yes", "yes", "admin_build_artifact_id", "yaml", "admin owner", "platform", "engineering release owner", "T+0", "same_day", "RI-02", "可回查 admin web build 产物", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Admin owner -> release manager", "可用 workflow artifact 名称或 ID"],
  ["AI-04", "Artifact identity", "workflow run id", "not_started", "P1", "yes", "yes", "workflow_run_id", "yaml", "repo admin", "platform", "engineering release owner", "T+0", "same_day", "RI-02", "提供 GitHub Actions run id", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Repo admin -> release manager", "用于回查 evidence artifact"],
  ["AI-05", "Artifact identity", "workflow sha", "not_started", "P1", "yes", "yes", "workflow_sha", "yaml", "repo admin", "platform", "engineering release owner", "T+0", "same_day", "AI-04", "workflow 绑定真实 commit SHA", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Repo admin -> release manager", "需与 RI-02 一致"],
  ["AI-06", "Artifact identity", "registry identity", "not_started", "P1", "yes", "yes", "registry", "yaml", "registry admin", "platform", "engineering release owner", "T+0", "same_day", "", "明确镜像所在 registry", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Platform lead -> registry admin", "例如 ghcr.io / harbor"],
  ["AI-07", "Artifact identity", "artifact provenance 链接", "not_started", "P1", "no", "yes", "provenance_url", "yaml", "platform", "repo admin", "engineering release owner", "T+1", "next_business_day", "AI-02", "提供 provenance / SBOM 链接", "node backend/tests/release/validate_artifact_input_package.mjs", "release-evidence/artifact_identity_input_package.json", "Platform lead -> security", "建议放 workflow summary 链接"],
  ["CA-01", "Cluster access", "kubeconfig 文件路径", "not_started", "P0", "yes", "yes", "kubeconfig_path", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "", "提供 staging kubeconfig 安全路径", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> platform lead -> engineering manager", "不要求把 kubeconfig 提交到仓库"],
  ["CA-02", "Cluster access", "current-context", "not_started", "P0", "yes", "yes", "current_context", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-01", "kubectl 能切到 staging context", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> platform lead", "与 kubeconfig 一起提供"],
  ["CA-03", "Cluster access", "staging context", "not_started", "P0", "yes", "yes", "staging_context", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-01", "给出 staging context 名称", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> platform lead", "命名建议 dramaflow-staging"],
  ["CA-04", "Cluster access", "namespace", "not_started", "P0", "yes", "yes", "namespace", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-03", "给出 staging namespace", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead", "例如 dramaflow-staging"],
  ["CA-05", "Cluster access", "RBAC role", "not_started", "P0", "yes", "yes", "rbac_role", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-01", "提供 deploy service account/role binding", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> security", "至少能 rollout status / undo"],
  ["CA-06", "Cluster access", "get deployments 权限", "not_started", "P0", "yes", "yes", "can_get_deployments", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-05", "kubectl auth can-i get deployments 返回 yes", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> security", "直接决定能否读状态"],
  ["CA-07", "Cluster access", "patch deployments 权限", "not_started", "P0", "yes", "yes", "can_patch_deployments", "yaml", "ops", "platform", "engineering release owner", "T+0", "same_day", "CA-05", "kubectl auth can-i patch deployments 返回 yes", "node backend/tests/release/validate_cluster_input_package.mjs", "release-evidence/cluster_access_input_package.json", "Ops lead -> security", "直接决定能否 rollout/rollback"],
  ["GH-01", "GitHub release control", "staging environment", "not_started", "P0", "yes", "yes", "staging_environment", "yaml", "repo admin", "release manager", "engineering release owner", "T+0", "same_day", "", "GitHub environment 已创建", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin -> org admin", "workflow 需能引用此环境"],
  ["GH-02", "GitHub release control", "production environment", "not_started", "P1", "no", "yes", "production_environment", "yaml", "repo admin", "release manager", "engineering release owner", "T+1", "next_business_day", "", "GitHub production environment 已创建", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin -> org admin", "Phase 12 非 staging 必需，但 production 必需"],
  ["GH-03", "GitHub release control", "required reviewers", "not_started", "P0", "yes", "yes", "required_reviewers", "yaml", "repo admin", "release manager", "engineering release owner", "T+0", "same_day", "GH-01", "至少包含 release manager 与 ops reviewer", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin", "审批链未配置则不能真实 rehearsal"],
  ["GH-04", "GitHub release control", "prevent self-review", "not_started", "P1", "yes", "yes", "prevent_self_review", "yaml", "repo admin", "release manager", "engineering release owner", "T+0", "same_day", "GH-01", "发布审批不可自审", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin", "降低错误放行风险"],
  ["GH-05", "GitHub release control", "branch/tag restrictions", "not_started", "P1", "yes", "yes", "deployment_policy", "yaml", "repo admin", "release manager", "engineering release owner", "T+0", "same_day", "GH-01", "只允许 release 分支/tag 触发部署", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin", "避免误发非候选版本"],
  ["GH-06", "GitHub release control", "environment secrets/vars", "not_started", "P1", "yes", "yes", "environment_secrets_manifest", "yaml", "repo admin", "platform", "engineering release owner", "T+0", "same_day", "GH-01", "确认 environment secrets/vars 已绑定", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Repo admin -> platform", "可提供截图或清单"],
  ["GH-07", "GitHub release control", "artifact/evidence upload policy", "not_started", "P2", "no", "yes", "evidence_upload_enabled", "yaml", "repo admin", "release manager", "engineering release owner", "T+1", "next_business_day", "GH-01", "workflow 需保留 evidence artifact", "node backend/tests/release/validate_github_env_input_package.mjs", "release-evidence/github_environment_input_package.json", "Release manager -> repo admin", "便于签字留痕"],
  ["SC-01", "Secrets / config", "DRAMAFLOW_POSTGRES_DSN", "not_started", "P0", "yes", "yes", "postgres_dsn_secret_ref", "yaml", "platform security", "ops", "engineering release owner", "T+0", "same_day", "", "提供 secret ref 或 vault path", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> platform lead -> engineering manager", "不要提交明文"],
  ["SC-02", "Secrets / config", "DRAMAFLOW_REDIS_ADDR", "not_started", "P0", "yes", "yes", "redis_addr_secret_ref", "yaml", "platform security", "ops", "engineering release owner", "T+0", "same_day", "", "提供 secret ref 或 config 注入位置", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> platform lead", "不要提交明文"],
  ["SC-03", "Secrets / config", "DRAMAFLOW_JWT_SECRET", "not_started", "P0", "yes", "yes", "jwt_secret_ref", "yaml", "platform security", "ops", "engineering release owner", "T+0", "same_day", "", "提供 JWT secret 注入位", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> platform lead", "不要提交明文"],
  ["SC-04", "Secrets / config", "ADMIN_SESSION_SECRET", "not_started", "P0", "yes", "yes", "admin_session_secret_ref", "yaml", "platform security", "ops", "engineering release owner", "T+0", "same_day", "", "提供 admin session secret 注入位", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> platform lead", "不要提交明文"],
  ["SC-05", "Secrets / config", "ADMIN_BOOTSTRAP_PASSWORD", "not_started", "P1", "yes", "yes", "admin_bootstrap_password_ref", "yaml", "release manager", "security", "engineering release owner", "T+0", "same_day", "", "提供 bootstrap admin 密码托管引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Release manager -> security", "不要提交明文"],
  ["SC-06", "Secrets / config", "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "not_started", "P0", "yes", "yes", "google_play_service_account_secret_ref", "yaml", "platform security", "billing owner", "engineering release owner", "T+0", "same_day", "", "提供 Google Play service account secret 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> billing owner -> platform lead", "直接影响 billing 真相链路"],
  ["SC-07", "Secrets / config", "BILLING_RTDN_PUSH_SECRET", "not_started", "P0", "yes", "yes", "billing_rtdn_push_secret_ref", "yaml", "platform security", "billing owner", "engineering release owner", "T+0", "same_day", "", "提供 RTDN push secret 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> billing owner", "直接影响 RTDN 链路"],
  ["SC-08", "Secrets / config", "DRAMAFLOW_REGISTRY", "not_started", "P0", "yes", "yes", "registry_host", "yaml", "registry admin", "platform", "engineering release owner", "T+0", "same_day", "AI-06", "提供 registry host", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Platform lead -> registry admin", "与 artifact identity 对齐"],
  ["SC-09", "Secrets / config", "DRAMAFLOW_REGISTRY_USERNAME", "not_started", "P0", "yes", "yes", "registry_username_secret_ref", "yaml", "registry admin", "platform", "engineering release owner", "T+0", "same_day", "SC-08", "提供 registry 用户名 secret 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Platform lead -> registry admin", "不要提交明文"],
  ["SC-10", "Secrets / config", "DRAMAFLOW_REGISTRY_PASSWORD", "not_started", "P0", "yes", "yes", "registry_password_secret_ref", "yaml", "registry admin", "platform", "engineering release owner", "T+0", "same_day", "SC-08", "提供 registry 密码 secret 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Platform lead -> registry admin", "不要提交明文"],
  ["SC-11", "Secrets / config", "DRAMAFLOW_CLOUD_SERVICE_ACCOUNT", "not_started", "P0", "yes", "yes", "cloud_service_account_secret_ref", "yaml", "platform security", "ops", "engineering release owner", "T+0", "same_day", "", "提供 cloud deploy service account 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Security -> platform lead", "不要提交明文"],
  ["SC-12", "Secrets / config", "DRAMAFLOW_DEPLOY_TOKEN", "not_started", "P0", "yes", "yes", "deploy_token_secret_ref", "yaml", "repo admin", "platform", "engineering release owner", "T+0", "same_day", "", "提供 deploy token secret 引用", "node backend/tests/release/validate_secret_input_package.mjs", "release-evidence/secret_inventory_input_package.json", "Repo admin -> platform lead", "用于 workflow/environment deploy 权限"],
  ["DT-01", "Deployment tooling", "helm 可用与 overlay 配置", "not_started", "P0", "yes", "yes", "helm_available / helm_version / values_overlay_file / release_name / chart_path", "yaml", "platform", "ops", "engineering release owner", "T+0", "same_day", "CA-01", "helm 可执行且 values overlay 指向 staging 文件", "node backend/tests/release/validate_deploy_tooling_input_package.mjs", "release-evidence/deploy_tooling_input_package.json", "Ops lead -> platform lead", "这项 verified 后才允许真正 deploy"]
];

const normalizedRows = rows.map((row) => {
  const item = itemIndex.get(row[0]);
  return {
    blocker_id: row[0],
    blocker_category: row[1],
    blocker_title: row[2],
    current_status: item?.current_status ?? "not_received",
    severity: row[4],
    blocks_staging: row[5],
    blocks_production: row[6],
    required_input: row[7],
    input_format: row[8],
    provider_role: row[9],
    primary_owner: row[10],
    fallback_owner: row[11],
    engineering_receiver: row[12],
    due_date_placeholder: row[13],
    sla_level: row[13],
    dependency: row[14],
    acceptance_criteria: row[15],
    verification_command: row[16],
    evidence_output: row[17],
    escalation_path: row[18],
    notes: `${row[19]} | validator_notes=${item?.notes ?? "等待对应输入包"}`
  };
});

const counts = normalizedRows.reduce(
  (acc, row) => {
    acc[row.current_status] += 1;
    return acc;
  },
  {
    not_received: 0,
    received_but_invalid: 0,
    received_and_verified: 0
  }
);

const stagingRows = normalizedRows.filter((row) => row.blocks_staging === "yes");
const stagingCounts = stagingRows.reduce(
  (acc, row) => {
    acc[row.current_status] += 1;
    return acc;
  },
  {
    not_received: 0,
    received_but_invalid: 0,
    received_and_verified: 0
  }
);

const board = {
  generatedAt,
  readyForPlatformHandoffExecution: true,
  readyForRealStagingExecution: stagingCounts.not_received === 0 && stagingCounts.received_but_invalid === 0,
  blockersClosed: counts.received_and_verified,
  blockersRemaining: counts.not_received + counts.received_but_invalid,
  blockerCounts: {
    total: normalizedRows.length,
    ...counts
  },
  stagingGateCounts: {
    total: stagingRows.length,
    ...stagingCounts
  },
  nextAction: counts.not_received > 0
    ? "向各 owner 发送 request bundle，并要求将输入投递到 platform-intake/received"
    : counts.received_but_invalid > 0
      ? "逐项退回 invalid 输入，等待补齐后重跑 validate_received_inputs"
      : "执行 run_real_staging_readiness.sh 并触发真实 staging rehearsal",
  lastUpdate: generatedAt,
  blockers: normalizedRows
};

ensureDir(releaseEvidenceDir);
ensureDir(docsDir);
writeJson(path.join(releaseEvidenceDir, "platform-blocker-owner-matrix.json"), {
  generatedAt,
  items: normalizedRows
});
writeJson(path.join(releaseEvidenceDir, "unblock-status-board.json"), board);

const tableHeader = "| blocker_id | category | status | owner | due | dependency |\n| --- | --- | --- | --- | --- | --- |\n";
const tableBody = normalizedRows
  .map((row) => `| ${row.blocker_id} | ${row.blocker_category} | ${row.current_status} | ${row.primary_owner} | ${row.due_date_placeholder} | ${row.dependency || "-"} |`)
  .join("\n");

fs.writeFileSync(
  path.join(docsDir, "platform-blocker-owner-matrix.md"),
  `# Platform Blocker Owner Matrix\n\nGenerated at: ${generatedAt}\n\n## Summary\n\n- Total blockers: ${normalizedRows.length}\n- not_received: ${counts.not_received}\n- received_but_invalid: ${counts.received_but_invalid}\n- received_and_verified: ${counts.received_and_verified}\n\n## Matrix\n\n${tableHeader}${tableBody}\n`,
  "utf8"
);

fs.writeFileSync(
  path.join(docsDir, "unblock-status-board.md"),
  `# Unblock Status Board\n\nGenerated at: ${generatedAt}\n\n- Ready for platform handoff execution: yes\n- Ready for real staging execution: ${board.readyForRealStagingExecution ? "yes" : "no"}\n- Blockers total: ${normalizedRows.length}\n- not_received: ${counts.not_received}\n- received_but_invalid: ${counts.received_but_invalid}\n- received_and_verified: ${counts.received_and_verified}\n- Next action: ${board.nextAction}\n\n## Owner Snapshot\n\n${tableHeader}${tableBody}\n`,
  "utf8"
);

if (!board.readyForRealStagingExecution) {
  console.error("[sync_blocker_status_from_inputs] blocker");
  console.error(`- not_received: ${counts.not_received}`);
  console.error(`- received_but_invalid: ${counts.received_but_invalid}`);
  process.exit(1);
}

console.log("[sync_blocker_status_from_inputs] pass");
