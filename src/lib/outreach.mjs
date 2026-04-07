function titleCase(value) {
  return value.replace(/-/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
}

export function pickService(lead, profile) {
  const searchableText = `${lead.segment} ${lead.likelyPainPoints.join(" ")}`.toLowerCase();
  for (const service of profile.services) {
    if (service.segments.includes(lead.segment)) {
      return service.label;
    }
    if (service.keywords.some((keyword) => searchableText.includes(keyword.toLowerCase()))) {
      return service.label;
    }
  }
  return profile.services[0].label;
}

export function buildEmailTemplate({ profile, lead, score, recommendedService }) {
  const selectedService = recommendedService || pickService(lead, profile);
  const subject = `Idea for ${lead.companyName}: ${selectedService} support`;
  const body = [
    `Hi ${lead.contactPerson || "Team"},`,
    "",
    `I came across ${lead.companyName} and noticed your work in ${titleCase(lead.segment)}.`,
    `I am ${profile.name}, a ${profile.title} with ${profile.yearsExperience}+ years of experience creating product visuals, explainer animations, architecture visualization and web-ready 3D assets.`,
    "",
    `You may currently be facing ${lead.likelyPainPoints.slice(0, 2).join(" and ")}.`,
    `I can help through ${selectedService.toLowerCase()} and deliver focused assets your sales and marketing teams can use immediately.`,
    "This usually improves conversion by making your product/service communication clearer for clients and buyers.",
    "",
    "If useful, I can share two practical concept ideas tailored for your current offerings and a simple 2-week pilot scope.",
    "Would a short 15-minute call this week work to see if this is useful?",
    "",
    `${profile.name}`,
    `${profile.contact.email}`,
    `${profile.contact.behance}`,
    "",
    `(internal note: fit score ${score}/100; remove before sending)`,
  ].join("\n");
  return { subject, body };
}

export function buildFollowUpEmail({ profile, lead, recommendedService }) {
  const selectedService = recommendedService || pickService(lead, profile);
  const subject = `Quick follow-up for ${lead.companyName}`;
  const body = [
    `Hi ${lead.contactPerson || "Team"},`,
    "",
    `Just following up on my earlier note for ${lead.companyName}.`,
    `A low-risk start could be one pilot in ${selectedService.toLowerCase()} so you can evaluate quality and ROI before scaling.`,
    "",
    "If relevant, I can send a one-page plan with timeline, scope options, and expected impact metrics.",
    "",
    "Best,",
    `${profile.name}`,
    `${profile.contact.email}`,
  ].join("\n");
  return { subject, body };
}

export function buildCallTalkingPoints({ profile, lead, recommendedService }) {
  const selectedService = recommendedService || pickService(lead, profile);
  return [
    "1) Opening",
    `- Thank them and reference their business: ${lead.companyName} (${titleCase(lead.segment)}).`,
    `- Positioning line: "${profile.title} focused on practical visual content for small teams."`,
    "",
    "2) Discovery",
    "- What is your immediate business goal this quarter?",
    "- Which product/service is hardest to communicate visually?",
    "- What channels matter most now (website, social, ads, sales deck)?",
    "- Do you rely on in-house creatives or external partners?",
    "",
    "3) Recommendation",
    `- Suggest starting with: ${selectedService}.`,
    `- Why now: it directly addresses ${lead.likelyPainPoints.slice(0, 2).join(" and ")}.`,
    "- Propose a pilot with one measurable outcome.",
    "",
    "4) Objections",
    "- Budget: begin with pilot deliverables and scale by results.",
    "- Turnaround: align milestones and weekly review points.",
    "- Quality: share relevant case studies and process checkpoints.",
    "",
    "5) Close",
    "- Confirm next action (brief/pilot/proposal).",
    "- Lock date for follow-up decision.",
  ];
}

export function buildSalesQualification(lead) {
  const fitCategory = lead.fitScore >= 92 ? "High-priority" : lead.fitScore >= 82 ? "Strong-fit" : "Nurture";
  const contactability =
    lead.channels.includes("email") || lead.channels.includes("phone") ? "direct" : "indirect";
  return [
    `Fit category: ${fitCategory}`,
    `Urgency signal: ${lead.inboundReadiness}`,
    `Budget signal: ${lead.budgetSignal}`,
    `Contactability: ${contactability}`,
    "Recommended stage: Qualified",
  ];
}

export function buildValueProposition(lead, recommendedService) {
  return `Primary value: ${recommendedService} tailored to ${titleCase(
    lead.segment
  )} business goals. Outcome focus: reduce friction around ${
    lead.likelyPainPoints[0]
  }. Commercial angle: start with a pilot and expand after measured success.`;
}

export function buildClientSolutionMapping(lead) {
  return lead.likelyPainPoints.slice(0, 3).map((pain) => {
    const solution =
      lead.segment === "manufacturing"
        ? "3D explainer + product process animation"
        : lead.segment === "real-estate" || lead.segment === "interior-design"
        ? "walkthrough renders + design presentation visuals"
        : "high-conversion product renders + social video creatives";
    return `${pain} -> ${solution}`;
  });
}

export function buildDiscoveryQuestions(lead) {
  return [
    `Which offering of ${lead.companyName} needs the strongest visual push this quarter?`,
    "What has worked and not worked in your current sales creatives?",
    "Who approves external creative/vendor decisions?",
    "What delivery timeline do you ideally expect for first outputs?",
    "If we run a pilot, what KPI would define success for you?",
  ];
}

export function buildObjectionResponses(lead) {
  return [
    {
      objection: "We already have someone for design.",
      response:
        "I can plug in as specialist support for high-impact 3D tasks without changing your current setup.",
    },
    {
      objection: "We are unsure about budget right now.",
      response:
        "Let us begin with one compact pilot deliverable tied to a clear business goal and then scale only if it works.",
    },
    {
      objection: "Not urgent at this moment.",
      response:
        `Understood. We can prepare a ready-to-launch visual pack for your next campaign/project cycle so your team moves faster when needed.`,
    },
  ];
}

export function buildSalesExecutionPlan(lead) {
  return [
    "Step 1: Send personalized outreach with one relevant portfolio proof.",
    "Step 2: Follow up in 2-3 days on the second-best channel.",
    "Step 3: Book 15-minute discovery call.",
    "Step 4: Send pilot proposal with timeline, price, and expected outcome.",
    "Step 5: Close with milestone-based payment and revision scope.",
    `Step 6: Ask for referral to one peer business after first successful delivery for ${lead.companyName}.`,
  ];
}

export function buildSubjectVariants(lead, recommendedService) {
  return [
    `Quick idea for ${lead.companyName}`,
    `${lead.companyName}: ${recommendedService} concept`,
    `Could this help ${lead.companyName} this month?`,
  ];
}

export function buildDiscoveryChecklist(lead) {
  return [
    `Who signs off creative/vendor decisions for ${lead.companyName}?`,
    "What current sales/marketing KPI should improve first?",
    `Existing turnaround expectations and deadlines?`,
    "Approval workflow: founder-led, marketing-led, or project manager-led?",
    "Pilot budget bracket and procurement constraints?",
  ];
}

export function buildObjectionHandling(lead) {
  return [
    {
      objection: "We already have designers.",
      response:
        "Position as overflow/specialist support for high-skill 3D, without replacing current team.",
    },
    {
      objection: "Budget is tight.",
      response:
        "Offer a micro-pilot with one high-impact deliverable and measurable target.",
    },
    {
      objection: "Not urgent right now.",
      response:
        "Tie value to active launch and sales cycles with a ready-to-use pilot plan.",
    },
  ];
}

export function buildFollowUpCadence() {
  return [
    "Day 0: personalized email + relevant portfolio link.",
    "Day 2: LinkedIn/Instagram touch with 1-line context.",
    "Day 4: first follow-up with pilot suggestion.",
    "Day 8: share mini audit idea or relevant case study.",
    "Day 12: final polite close-the-loop note.",
  ];
}

export function buildApproachPlan(lead) {
  const channelPlan = {
    email: "Start with concise email and targeted portfolio links.",
    linkedin: "Follow up on LinkedIn with a short context message.",
    instagram: "DM with one strong visual and clear CTA.",
    phone: "Use a brief call to confirm needs and decision-maker.",
    whatsapp: "Send compact intro + sample + call request.",
  };
  return lead.channels.map(
    (channel) => channelPlan[channel] || "Use direct outreach with clear value."
  );
}
