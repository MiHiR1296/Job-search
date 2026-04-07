import { freelancerProfile } from "./config/profile.mjs";
import { sampleLeads } from "./data/sampleLeads.mjs";
import { rankLeads } from "./lib/scoring.mjs";
import {
  buildApproachPlan,
  buildClientSolutionMapping,
  buildDiscoveryQuestions,
  buildCallTalkingPoints,
  buildEmailTemplate,
  buildFollowUpEmail,
  buildObjectionResponses,
  buildSalesExecutionPlan,
  buildSalesQualification,
  buildSubjectVariants,
  buildValueProposition,
  pickService,
} from "./lib/outreach.mjs";
import { generateLeadReport } from "./lib/report.mjs";

function run() {
  const rankedLeads = rankLeads(sampleLeads, freelancerProfile).map((lead) => {
    const recommendedService = pickService(lead, freelancerProfile);
    const valueProposition = buildValueProposition(lead, recommendedService);
    return {
      ...lead,
      recommendedService,
      approachPlan: buildApproachPlan(lead),
      salesQualification: buildSalesQualification(lead),
      valueProposition,
      clientSolutionMapping: buildClientSolutionMapping(lead),
      discoveryQuestions: buildDiscoveryQuestions(lead),
      objectionResponses: buildObjectionResponses(lead),
      salesExecutionPlan: buildSalesExecutionPlan(lead),
      subjectVariants: buildSubjectVariants(lead, recommendedService),
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
