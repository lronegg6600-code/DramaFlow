import path from "node:path";
import {
  EVIDENCE_DIR,
  PLATFORM_OWNER_LOGIN,
  PLATFORM_ISSUE_NUMBER,
  PLATFORM_REPO,
  fetchPlatformIssueComments,
  getNowIso,
  writeJson,
} from "./real_external_url_common.mjs";

const result = await fetchPlatformIssueComments();
const externalReplies = (result.comments || [])
  .filter((comment) => comment?.author?.login && comment.author.login !== PLATFORM_OWNER_LOGIN)
  .map((comment) => ({
    author: comment.author.login,
    body: comment.body,
    createdAt: comment.createdAt,
    url: comment.url,
  }));

const payload = {
  generatedAt: getNowIso(),
  issueNumber: PLATFORM_ISSUE_NUMBER,
  repo: PLATFORM_REPO,
  fetchSucceeded: result.ok,
  reason: result.ok ? null : result.reason,
  totalComments: result.comments?.length ?? 0,
  platformReplyCount: externalReplies.length,
  replies: externalReplies,
};

await writeJson(path.join(EVIDENCE_DIR, "staging-real-external-url-reply-log.json"), payload);
console.log(JSON.stringify(payload, null, 2));
process.exit(result.ok ? 0 : 1);
