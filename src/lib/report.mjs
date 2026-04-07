import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

function renderLead(lead, index) {
  const painPoints = lead.likelyPainPoints.map((item) => `- ${item}`).join("\n");
  const reasons = lead.reasons.map((item) => `- ${item}`).join("\n");
  const approach = lead.approachPlan.map((item) => `- ${item}`).join("\n");
  const callPoints = lead.callTalkingPoints.map((item) => `- ${item}`).join("\n");

  return [
    `### ${index + 1}. ${lead.companyName} (${lead.segment})`,
    `- **Fit score:** ${lead.fitScore}/100`,
    `- **Company size:** ${lead.companySize}`,
    `- **Location:** ${lead.location}`,
    `- **Budget signal:** ${lead.budgetSignal}`,
    `- **Best approach channels:** ${lead.channels.join(", ")}`,
    "",
    "**Why this lead is promising**",
    reasons,
    "",
    "**Likely pain points to address**",
    painPoints,
    "",
    "**Approach plan**",
    approach,
    "",
    "**Email draft**",
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
    callPoints,
    "",
    "---",
    "",
  ].join("\n");
}

export function generateLeadReport(rankedLeads, profile) {
  mkdirSync(join(process.cwd(), "output"), { recursive: true });
  const outputPath = join(process.cwd(), "output", "top-leads-report.md");

  const lines = [
    "# Freelance Lead Engine Report",
    "",
    "## Profile Snapshot",
    `- Name: ${profile.name}`,
    `- Role: ${profile.title}`,
    `- Experience: ${profile.yearsExperience}+ years`,
    `- Positioning: ${profile.positioning}`,
    "",
    "### Portfolio Links",
    `- Behance: ${profile.contact.behance}`,
    `- GitHub: ${profile.contact.github}`,
    `- LinkedIn: ${profile.contact.linkedin}`,
    "",
    "## Top Potential Clients (Small-Business Focus)",
    "",
    ...rankedLeads.map((lead, index) => renderLead(lead, index)),
    "## Next Step",
    "Replace sample leads with real leads from Google Maps, Instagram, LinkedIn, and local business directories. Then rerun `npm run generate`.",
    "",
  ];

  writeFileSync(outputPath, lines.join("\n"), "utf8");
  return outputPath;
}
