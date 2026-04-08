import { loadTicketMap, saveTicketMap, writeNamedEvidence } from "./_issue_ops_helpers.mjs";

const assignee = process.env.DRAMAFLOW_DEFAULT_ASSIGNEE || "lronegg6600-code";
const ticketMap = loadTicketMap();
const generatedAt = new Date().toISOString();

const items = ticketMap.items.map((item) => {
  if (item.issue_number) {
    item.issue_lifecycle_status = "assigned";
  }
  item.assignee_login = assignee;
  return {
    blocker_id: item.blocker_id,
    issue_number: item.issue_number,
    issue_url: item.issue_url,
    assignee_login: assignee,
    assignment_state: item.issue_number ? "assigned" : "not_launched"
  };
});

saveTicketMap(ticketMap);
writeNamedEvidence("blocker-owner-assignment.json", { generatedAt, assignee, items });

console.log("[assign_blocker_owners] pass");
