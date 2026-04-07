import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

function list(items) {
  if (!Array.isArray(items)) {
    return `- ${String(items ?? "")}`;
  }
  return items.map((item) => `- ${item}`).join("\n");
}

function renderObjectList(items) {
  return items.map((item) => `- **${item.objection}:** ${item.response}`).join("\n");
}

function renderLead(lead, index) {
  return [
    `### ${index + 1}. ${lead.companyName} (${lead.segment})`,
    `- **Fit score:** ${lead.fitScore}/100`,
    `- **Fit tier:** ${lead.fitTier}`,
    `- **Confidence:** ${lead.confidence}`,
    `- **Company size:** ${lead.companySize}`,
    `- **Location:** ${lead.location}`,
    `- **Budget signal:** ${lead.budgetSignal}`,
    `- **Website:** ${lead.website}`,
    `- **Primary channel:** ${lead.contact.primaryChannel}`,
    `- **Contact email:** ${lead.contact.email || "N/A"}`,
    `- **Contact phone:** ${lead.contact.phone || "N/A"}`,
    `- **Source:** ${lead.source}`,
    `- **Verified from:** ${lead.sourceNote}`,
    `- **Recommended service:** ${lead.recommendedService}`,
    "",
    "**Why this lead is a fit**",
    list(lead.reasons),
    "",
    "**Observed services/business context**",
    list(lead.servicesObserved),
    "",
    "**Likely pain points**",
    list(lead.likelyPainPoints),
    "",
    "**Suggested approach channels**",
    list(lead.approachPlan),
    "",
    "**Sales qualification (what to confirm first)**",
    list(
      Object.entries(lead.salesQualification || {}).map(
        ([key, value]) => `${key}: ${value}`
      )
    ),
    "",
    "**Discovery questions for your call**",
    list(lead.discoveryQuestions),
    "",
    "**Pain-to-solution mapping**",
    list(
      (lead.clientSolutionMapping || []).map(
        (item) => `${item.painPoint} -> ${item.solution}`
      )
    ),
    "",
    "**Value proposition angle**",
    list(lead.valueProposition),
    "",
    "**Objection handling**",
    renderObjectList(lead.objectionResponses),
    "",
    "**Execution plan (what sales team would do)**",
    list(lead.salesExecutionPlan),
    "",
    "**Suggested subject lines**",
    list(lead.subjectVariants),
    "",
    "**First-contact email**",
    "```",
    `Subject: ${lead.email.subject}`,
    "",
    lead.email.body,
    "```",
    "",
    "**Follow-up email**",
    "```",
    `Subject: ${lead.followUpEmail.subject}`,
    "",
    lead.followUpEmail.body,
    "```",
    "",
    "**Call talking points**",
    list(lead.callTalkingPoints),
    "",
    "---",
    "",
  ].join("\n");
}

export function generateLeadReport(rankedLeads, profile) {
  mkdirSync(join(process.cwd(), "output"), { recursive: true });
  const outputPath = join(process.cwd(), "output", "top-leads-report.md");

  const lines = [
    "# Freelance Lead Engine Report (Real Leads + Sales Playbook)",
    "",
    "## Profile Snapshot",
    `- Name: ${profile.name}`,
    `- Role: ${profile.title}`,
    `- Experience: ${profile.yearsExperience}+ years`,
    `- Positioning: ${profile.positioning}`,
    `- ICP focus: ${profile.salesPlaybook.targetMarket}`,
    `- Qualification model: ${profile.salesPlaybook.qualifier}`,
    "",
    "### Portfolio Links",
    `- Behance: ${profile.contact.behance}`,
    `- GitHub: ${profile.contact.github}`,
    `- LinkedIn: ${profile.contact.linkedin}`,
    "",
    "## Real Leads Ranked",
    "",
    ...rankedLeads.map((lead, index) => renderLead(lead, index)),
    "## Studio Sales Operating System",
    "1. Source 10-15 new leads/week from official websites and local directories.",
    "2. Verify contact channel and one strong business signal before outreach.",
    "3. Run a 5-touch sequence (email -> social -> follow-up -> value add -> close loop).",
    "4. Pitch a low-risk pilot first, then upsell monthly/quarterly content support.",
    "5. Track every lead by stage and next action date.",
    "",
    "## Recommended Pipeline Stages",
    "- New Lead",
    "- Qualified",
    "- Contacted",
    "- Replied",
    "- Discovery Call Booked",
    "- Proposal Sent",
    "- Negotiation",
    "- Won / Lost",
    "",
    "## Next Step",
    "Keep updating `src/data/sampleLeads.mjs` with newly found real companies and rerun `npm run generate`.",
    "",
  ];

  writeFileSync(outputPath, lines.join("\n"), "utf8");
  return outputPath;
}
