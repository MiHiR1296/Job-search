const WEIGHTS = {
  segmentFit: 0.3,
  companySizeFit: 0.2,
  budgetFit: 0.2,
  outreachReadiness: 0.15,
  channelFit: 0.08,
  locationFit: 0.07,
};

const SEGMENT_ALIASES = new Map([
  ["real-estate", "real estate"],
  ["interior-design", "interior design"],
  ["d2c-brand", "d2c"],
  ["marketing-agency", "local agency"],
]);

function clamp(value, min, max) {
  return Math.max(min, Math.min(value, max));
}

function normalizeSegment(segment) {
  return SEGMENT_ALIASES.get(segment) || segment;
}

function segmentScore(lead, profile) {
  const normalized = normalizeSegment(lead.segment);
  return profile.idealClientFilters.preferredIndustries.includes(normalized) ? 1 : 0.45;
}

function companySizeScore(companySize) {
  if (companySize === "small") return 1;
  if (companySize === "mid") return 0.7;
  return 0.5;
}

function budgetScore(budgetSignal) {
  if (budgetSignal === "medium") return 1;
  if (budgetSignal === "high") return 0.8;
  return 0.6;
}

function readinessScore(inboundReadiness) {
  if (inboundReadiness === "hot") return 1;
  if (inboundReadiness === "warm") return 0.8;
  return 0.6;
}

function channelScore(channels) {
  if (channels.includes("email") && channels.includes("phone")) return 1;
  if (channels.includes("email")) return 0.85;
  return 0.7;
}

function locationScore(location, profile) {
  const value = location.toLowerCase();
  if (value.includes("mumbai") || value.includes("india")) return 1;
  return profile.location.toLowerCase().includes("india") ? 0.8 : 0.7;
}

function fitTier(totalScore) {
  if (totalScore >= 92) return "A+";
  if (totalScore >= 84) return "A";
  if (totalScore >= 75) return "B";
  return "C";
}

function confidenceFromLead(lead) {
  if (lead.verificationLevel === "official-website") return "high";
  if (lead.verificationLevel === "directory-cross-checked") return "medium";
  return "low";
}

function weighted(componentScores) {
  return (
    componentScores.segmentFit * WEIGHTS.segmentFit +
    componentScores.companySizeFit * WEIGHTS.companySizeFit +
    componentScores.budgetFit * WEIGHTS.budgetFit +
    componentScores.outreachReadiness * WEIGHTS.outreachReadiness +
    componentScores.channelFit * WEIGHTS.channelFit +
    componentScores.locationFit * WEIGHTS.locationFit
  );
}

export function evaluateLead(lead, profile) {
  const componentScores = {
    segmentFit: segmentScore(lead, profile),
    companySizeFit: companySizeScore(lead.companySize),
    budgetFit: budgetScore(lead.budgetSignal),
    outreachReadiness: readinessScore(lead.inboundReadiness),
    channelFit: channelScore(lead.channels),
    locationFit: locationScore(lead.location, profile),
  };

  const totalScore = Math.round(clamp(weighted(componentScores) * 100, 0, 100));
  const reasons = [];

  if (componentScores.segmentFit === 1) {
    reasons.push("Matches your preferred small-business industry focus.");
  }
  if (componentScores.companySizeFit === 1) {
    reasons.push("Small team profile aligns with your freelance positioning.");
  }
  if (componentScores.outreachReadiness >= 0.8) {
    reasons.push("Shows warm/hot buying intent and practical outsourcing potential.");
  }
  if (componentScores.channelFit >= 0.85) {
    reasons.push("Reachable via strong channels (email/phone/LinkedIn).");
  }
  if (reasons.length === 0) {
    reasons.push("Moderate fit; keep in secondary outreach pipeline.");
  }

  return {
    totalScore,
    fitTier: fitTier(totalScore),
    confidence: confidenceFromLead(lead),
    componentScores,
    reasons,
  };
}

export function rankLeads(leads, profile) {
  return leads
    .map((lead) => {
      const scored = evaluateLead(lead, profile);
      return {
        ...lead,
        fitScore: scored.totalScore,
        fitTier: scored.fitTier,
        confidence: scored.confidence,
        componentScores: scored.componentScores,
        reasons: scored.reasons,
      };
    })
    .sort((a, b) => b.fitScore - a.fitScore);
}
