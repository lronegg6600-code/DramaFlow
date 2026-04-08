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

const category = "deploy_tooling_input_package";
const dir = path.join(intakeRoot(), "received", "deploy-tooling");
const files = listCandidateFiles(dir);
const sourceFile = files[0] ?? null;
const data = sourceFile ? parseLooseConfig(sourceFile) : {};

const items = [
  {
    blocker_id: "DT-01",
    blocker_title: "helm 可用",
    current_status: sourceFile ? boolStatus(data.helm_available) : "not_received",
    field: "helm_available",
    notes: sourceFile ? "需要平台确认 helm 已安装可执行" : "未收到 deploy tooling 输入包"
  },
  {
    blocker_id: "DT-02",
    blocker_title: "helm version",
    current_status: sourceFile ? statusForValue(data.helm_version) : "not_received",
    field: "helm_version",
    notes: sourceFile ? "建议 v3.15+，至少要给出实际版本" : "未收到 deploy tooling 输入包"
  },
  {
    blocker_id: "DT-03",
    blocker_title: "values overlay file",
    current_status: sourceFile ? statusForValue(data.values_overlay_file) : "not_received",
    field: "values_overlay_file",
    notes: sourceFile ? "需要 staging overlay 实际路径" : "未收到 deploy tooling 输入包"
  },
  {
    blocker_id: "DT-04",
    blocker_title: "release name",
    current_status: sourceFile ? statusForValue(data.release_name) : "not_received",
    field: "release_name",
    notes: sourceFile ? "需要固定 release 名称" : "未收到 deploy tooling 输入包"
  },
  {
    blocker_id: "DT-05",
    blocker_title: "chart path",
    current_status: sourceFile ? statusForValue(data.chart_path) : "not_received",
    field: "chart_path",
    notes: sourceFile ? "需要 chart 或 deploy chart 路径" : "未收到 deploy tooling 输入包"
  }
];

const payload = validationPayload(category, sourceFile, items);
writeValidationFiles(category, payload);
printValidationResult(category, payload);
