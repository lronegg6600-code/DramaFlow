import { readFileSync, writeFileSync, existsSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-version-check.json");
const sourceFile = path.join(root, "backend", "services", "entitlement-service", "internal", "service", "service.go");
const probeFile = path.join(root, "release-evidence", "entitlement-runtime-contract-probe.json");

const source = readFileSync(sourceFile, "utf8");
const probe = existsSync(probeFile) ? JSON.parse(readFileSync(probeFile, "utf8")) : null;

const payload = {
  generatedAt: new Date().toISOString(),
  sourceContainsNormalization: source.includes("normalizeEntitlements(items)") && source.includes("make([]domain.Entitlement, 0)"),
  runtimeProbePassed: probe?.passed ?? false,
  runtimeEntitlementsShape: probe?.entitlementsShape ?? "unknown",
  converged: Boolean(
    source.includes("normalizeEntitlements(items)") &&
      source.includes("make([]domain.Entitlement, 0)") &&
      probe?.passed,
  ),
  interpretation:
    probe?.passed
      ? "source and runtime both reflect empty-list semantics"
      : "source fix is present but runtime probe still disagrees",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
