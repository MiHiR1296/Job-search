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
  const subject = `Idea for ${lead.companyName}: ${recommendedService}`;
  const body = [
    "Hi there,",
    "",
    `I came across ${lead.companyName} and noticed your work in ${titleCase(lead.segment)}.`,
    `I am ${profile.name}, a ${profile.title} with ${profile.yearsExperience}+ years of experience creating product visuals, explainer animations, architecture visualization and web-ready 3D assets.`,
    "",
    `You may currently be facing ${lead.likelyPainPoints.slice(0, 2).join(" and ")}.`,
    `I can help through ${selectedService.toLowerCase()} and deliver focused assets your sales and marketing teams can use immediately.`,
    "",
    "If useful, I can share two practical concept ideas tailored for your current offerings.",
    "Would a short 15-minute call this week work?",
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
    "Hi there,",
    "",
    `Just following up on my earlier note for ${lead.companyName}.`,
    `A low-risk start could be one pilot in ${selectedService.toLowerCase()} so you can evaluate quality and ROI before scaling.`,
    "",
    "If relevant, I can send a one-page plan with timeline and scope options.",
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
