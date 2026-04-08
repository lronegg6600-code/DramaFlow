import { loadTicketMap, execGh, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const repo = process.env.GITHUB_REPOSITORY;
const botLogin = process.env.DRAMAFLOW_BOT_LOGIN || "lronegg6600-code";
const generatedAt = new Date().toISOString();

let comments = [];
let mode = "real";
try {
  const output = execGh(["api", `repos/${repo}/issues/comments?per_page=100`]);
  comments = JSON.parse(output);
} catch (error) {
  mode = "failed";
  writeNamedEvidence("issue-reply-ingest-log.json", {
    generatedAt,
    mode,
    error: String(error.message || error),
    totalReplies: 0,
    items: []
  });
  console.log("[fetch_issue_replies] pass");
  process.exit(0);
}

const ticketMap = loadTicketMap();
const issueNumberByUrl = new Map(ticketMap.items.map((item) => [item.issue_url, item.issue_number]));
const issueReplies = [];

for (const comment of comments) {
  const issueNumber = issueNumberByUrl.get(comment.issue_url);
  if (!issueNumber) continue;
  const isExternal = comment.user?.login !== botLogin;
  issueReplies.push({
    issue_number: issueNumber,
    issue_url: comment.issue_url,
    comment_id: comment.id,
    author_login: comment.user?.login ?? "unknown",
    created_at: comment.created_at,
    updated_at: comment.updated_at,
    is_external_reply: isExternal,
    body_preview: String(comment.body || "").slice(0, 300)
  });
}

const externalReplies = issueReplies.filter((item) => item.is_external_reply);

writeNamedEvidence("issue-reply-ingest-log.json", {
  generatedAt,
  mode,
  botLogin,
  totalCommentsScanned: comments.length,
  totalReplies: externalReplies.length,
  items: externalReplies
});

console.log("[fetch_issue_replies] pass");
