import { freelancerProfile } from "./config/profile.mjs";
import { sampleLeads } from "./data/sampleLeads.mjs";
import { rankLeads } from "./lib/scoring.mjs";
import {
  buildApproachPlan,
  buildCallTalkingPoints,
  buildEmailTemplate,
  buildFollowUpEmail,
  pickService,
} from "./lib/outreach.mjs";
import { generateLeadReport } from "./lib/report.mjs";

function run() {
  const rankedLeads = rankLeads(sampleLeads, freelancerProfile).map((lead) => {
    const recommendedService = pickService(lead, freelancerProfile);
    return {
      ...lead,
      recommendedService,
      approachPlan: buildApproachPlan(lead),
      email: buildEmailTemplate({
        profile: freelancerProfile,
        lead,
        score: lead.fitScore,
        recommendedService,
      }),
      followUpEmail: buildFollowUpEmail({
        profile: freelancerProfile,
        lead,
        recommendedService,
      }),
      callTalkingPoints: buildCallTalkingPoints({
        profile: freelancerProfile,
        lead,
        recommendedService,
      }),
    };
  });

  const reportPath = generateLeadReport(rankedLeads, freelancerProfile);
  process.stdout.write(`Generated report: ${reportPath}\n`);
}

run();
