import path from "node:path";

import {
  boolStatus,
  intakeRoot,
  listCandidateFiles,
  parseLooseConfig,
  statusForValue,
  validationPayload,
  writeValidationFiles,
  printValidationResult
} from "./_intake_validation_helpers.mjs";

const category = "cluster_access_input_package";
const dir = path.join(intakeRoot(), "received", "cluster-access");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const items = [
  {
    blocker_id: "CA-01",
    blocker_title: "kubeconfig 文件路径",
    current_status: sourceFile ? statusForValue(data.kubeconfig_path) : "not_received",
    field: "kubeconfig_path",
    notes: sourceFile ? "需要真实 kubeconfig 路径或安全分发方式" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-02",
    blocker_title: "current-context",
    current_status: sourceFile ? statusForValue(data.current_context) : "not_received",
    field: "current_context",
    notes: sourceFile ? "需要 staging 当前上下文" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-03",
    blocker_title: "staging context",
    current_status: sourceFile ? statusForValue(data.staging_context) : "not_received",
    field: "staging_context",
    notes: sourceFile ? "需要 staging 上下文名" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-04",
    blocker_title: "namespace",
    current_status: sourceFile ? statusForValue(data.namespace) : "not_received",
    field: "namespace",
    notes: sourceFile ? "需要 staging namespace" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-05",
    blocker_title: "RBAC role",
    current_status: sourceFile ? statusForValue(data.rbac_role) : "not_received",
    field: "rbac_role",
    notes: sourceFile ? "需要 deploy 用 service account / role" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-06",
    blocker_title: "get deployments 权限",
    current_status: sourceFile ? boolStatus(data.can_get_deployments) : "not_received",
    field: "can_get_deployments",
    notes: sourceFile ? "需要读取 deployment 权限" : "未收到 cluster access 输入包"
  },
  {
    blocker_id: "CA-07",
    blocker_title: "patch deployments 权限",
    current_status: sourceFile ? boolStatus(data.can_patch_deployments) : "not_received",
    field: "can_patch_deployments",
    notes: sourceFile ? "需要 rollout/rollback patch 权限" : "未收到 cluster access 输入包"
  }
];

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
