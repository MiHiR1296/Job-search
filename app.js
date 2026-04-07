const serviceLabels = {
  "3d-explainers": "3D explainer videos",
  archviz: "Architecture visualization",
  "product-renders": "Product renders",
  "marketing-content": "Marketing content",
  "small-websites": "Small websites / web-interactive showcases",
};

const segments = [
  {
    id: "machine-makers",
    title: "Industrial machine makers",
    sizeFit: "small",
    baseScore: 92,
    services: ["3d-explainers", "product-renders", "marketing-content", "small-websites"],
    why: [
      "Complex products are hard to explain with flat photos alone.",
      "Many small manufacturers have outdated websites and weak demo content.",
      "Your Aurus Machines work is direct proof for this niche.",
    ],
    leadSources: ["Google search", "IndiaMART", "trade directories", "LinkedIn company pages"],
    searchQueries: [
      "industrial machine manufacturer in {city}",
      "packaging machine company in {city}",
      "automation equipment company in {city}",
    ],
    offer: "a short operation video, cutaway render set, or landing page section that explains the machine clearly",
    proof: "Aurus Machines freelance explainer work plus product optimization experience",
    discoveryQuestions: [
      "Which part of the machine is hardest for buyers to understand quickly?",
      "Do sales teams rely on PDF diagrams because there is no good visual demo?",
      "Are you launching a new model or exhibiting at a trade show soon?",
    ],
  },
  {
    id: "architects",
    title: "Boutique architects and interior studios",
    sizeFit: "small",
    baseScore: 89,
    services: ["archviz", "marketing-content", "small-websites"],
    why: [
      "Smaller studios often need better project presentation to win clients.",
      "You already have architecture visualization experience.",
      "High-end visuals help them pitch premium work without hiring full-time 3D staff.",
    ],
    leadSources: ["Google Maps", "Instagram", "ArchDaily directories", "local design groups"],
    searchQueries: [
      "interior designer in {city}",
      "boutique architect in {city}",
      "residential design studio in {city}",
    ],
    offer: "a photoreal archviz package, short walkthrough animation, or cleaner project landing page",
    proof: "International office visualization work and strong material / lighting skill",
    discoveryQuestions: [
      "Are proposals losing impact because concepts are shown only with drawings or moodboards?",
      "Do you need visuals earlier in the sales process to help close projects?",
      "Which upcoming project would benefit from a stronger walkthrough or render set?",
    ],
  },
  {
    id: "furniture-brands",
    title: "Furniture and home decor brands",
    sizeFit: "small",
    baseScore: 87,
    services: ["product-renders", "marketing-content", "small-websites", "3d-explainers"],
    why: [
      "Catalog and social content demand is constant, but shoots are expensive.",
      "These brands benefit from product renders, room scenes, and web-interactive previews.",
      "Your product material work and web-3D background are a strong match.",
    ],
    leadSources: ["Instagram", "Shopify stores", "Pinterest-heavy brands", "marketplaces"],
    searchQueries: [
      "furniture brand in {city}",
      "home decor brand in {city}",
      "sofa brand India website",
    ],
    offer: "studio renders, room lifestyle visuals, or a product configurator concept for hero SKUs",
    proof: "Lamp material project and product visualization / web optimization experience",
    discoveryQuestions: [
      "Which products are selling well but still look weak online?",
      "Do you need launch visuals faster than photography allows?",
      "Would room scenes or material variations improve conversions?",
    ],
  },
  {
    id: "jewellery",
    title: "Jewellery brands and custom designers",
    sizeFit: "small",
    baseScore: 86,
    services: ["product-renders", "marketing-content", "small-websites"],
    why: [
      "Jewellery is detail-heavy and benefits strongly from premium renders.",
      "Interactive viewing can make smaller brands look more premium.",
      "Your Jewellery Designer Three.js project is valuable proof here.",
    ],
    leadSources: ["Instagram", "bridal jewellery directories", "Shopify stores", "local designer listings"],
    searchQueries: [
      "custom jewellery brand in {city}",
      "bridal jewellery designer in {city}",
      "fine jewellery Shopify India",
    ],
    offer: "hero renders, campaign stills, or a lightweight interactive product viewer",
    proof: "Jewellery Designer built with Three.js plus high-detail material work",
    discoveryQuestions: [
      "Are you relying only on macro photos without a premium rendered campaign set?",
      "Would ring or pendant rotation help buyers understand the piece better?",
      "Is there a bridal or festive launch coming up soon?",
    ],
  },
  {
    id: "real-estate",
    title: "Local real-estate developers",
    sizeFit: "growing",
    baseScore: 82,
    services: ["archviz", "3d-explainers", "marketing-content", "small-websites"],
    why: [
      "Smaller developers need clear project communication but often underinvest in visuals.",
      "Walkthroughs and teaser visuals help sales teams immediately.",
      "This is still accessible when targeting local and regional players rather than giant builders.",
    ],
    leadSources: ["Google search", "project microsites", "property listing sites", "broker networks"],
    searchQueries: [
      "real estate developer in {city}",
      "new residential project in {city}",
      "builder project launch in {city}",
    ],
    offer: "project walkthroughs, amenity stills, or sales-friendly explainer visuals for launches",
    proof: "Archviz background plus ability to simplify technical communication visually",
    discoveryQuestions: [
      "Is the project launch relying on generic brochure visuals?",
      "Do buyers struggle to understand space, amenities, or flow from current materials?",
      "Would a short, polished walkthrough help your brokers or sales team?",
    ],
  },
  {
    id: "agencies",
    title: "Small marketing agencies without 3D capability",
    sizeFit: "small",
    baseScore: 84,
    services: ["marketing-content", "product-renders", "3d-explainers", "small-websites"],
    why: [
      "Agencies often need a reliable execution partner instead of hiring internally.",
      "Retainer-style relationships can create repeat work across multiple clients.",
      "Your range covers product, web, technical visuals, and explainers.",
    ],
    leadSources: ["LinkedIn", "agency directories", "Instagram", "local startup networks"],
    searchQueries: [
      "creative agency in {city}",
      "branding agency in {city}",
      "performance marketing agency e-commerce India",
    ],
    offer: "white-label 3D execution for product launches, paid ads, and site refreshes",
    proof: "Broad commercial range across product, technical, and web-facing visuals",
    discoveryQuestions: [
      "Which client requests do you currently turn down because you lack 3D support?",
      "Would a freelance partner help you pitch higher-value retainers?",
      "Do you need fast render support for ads, e-commerce, or landing pages?",
    ],
  },
];

const starterLeads = [
  {
    id: "starter-1",
    company: "Boutique machine manufacturer with outdated demos",
    contact: "Sales or founder",
    segmentId: "machine-makers",
    channel: "Email",
    signal: "Website shows machine specs but no clean explainer visuals.",
    notes: "Good target when a product is technical and hard to pitch quickly.",
  },
  {
    id: "starter-2",
    company: "Independent interior studio posting only site photos",
    contact: "Principal architect",
    segmentId: "architects",
    channel: "Instagram DM",
    signal: "Strong project quality, weak presentations and no walkthroughs.",
    notes: "Pitch a render upgrade for a new proposal or premium residential project.",
  },
  {
    id: "starter-3",
    company: "D2C furniture brand with plain product pages",
    contact: "Founder or marketing lead",
    segmentId: "furniture-brands",
    channel: "Email",
    signal: "Store has many SKUs but lacks room scenes and material variation visuals.",
    notes: "Offer a pilot pack for top sellers first.",
  },
  {
    id: "starter-4",
    company: "Jewellery brand preparing festive campaigns",
    contact: "Brand owner",
    segmentId: "jewellery",
    channel: "Instagram DM",
    signal: "Only static product photos, no premium motion or interactive views.",
    notes: "Lead with a hero visual campaign idea.",
  },
  {
    id: "starter-5",
    company: "Regional developer launching a new project",
    contact: "Marketing manager",
    segmentId: "real-estate",
    channel: "Email",
    signal: "Project page feels brochure-like with weak visual storytelling.",
    notes: "Offer a short launch teaser or walkthrough section.",
  },
  {
    id: "starter-6",
    company: "Small agency that keeps outsourcing design work",
    contact: "Agency founder",
    segmentId: "agencies",
    channel: "LinkedIn DM",
    signal: "Agency portfolio is strong but has no 3D case studies.",
    notes: "Pitch yourself as a white-label specialist for product and technical work.",
  },
];

const storageKey = "freelance-outreach-custom-leads-v1";

const state = {
  city: "Mumbai",
  size: "small",
  selectedServices: Object.keys(serviceLabels),
  customLeads: loadCustomLeads(),
};

const segmentList = document.querySelector("#segment-list");
const leadBoard = document.querySelector("#lead-board");
const leadSegmentSelect = document.querySelector("#lead-segment");
const outreachSegmentSelect = document.querySelector("#outreach-segment");
const outreachServiceSelect = document.querySelector("#outreach-service");
const outreachOutput = document.querySelector("#outreach-output");

init();

function init() {
  populateSegmentSelects();
  populateServiceSelect();
  renderSegments();
  renderLeadBoard();
  bindEvents();
}

function bindEvents() {
  document.querySelector("#preference-form").addEventListener("change", handlePreferences);
  document.querySelector("#lead-form").addEventListener("submit", handleLeadSubmit);
  document.querySelector("#outreach-form").addEventListener("submit", handleOutreachSubmit);
  document.querySelector("#outreach-segment").addEventListener("change", syncServiceOptions);
  document.body.addEventListener("click", handleBodyClick);
}

function handlePreferences() {
  state.city = document.querySelector("#city-input").value.trim() || "Mumbai";
  state.size = document.querySelector("#size-input").value;
  state.selectedServices = Array.from(
    document.querySelectorAll(".service-grid input:checked"),
    (input) => input.value,
  );

  if (!state.selectedServices.length) {
    state.selectedServices = Object.keys(serviceLabels);
    document.querySelectorAll(".service-grid input").forEach((input) => {
      input.checked = true;
    });
  }

  renderSegments();
  renderLeadBoard();
}

function handleLeadSubmit(event) {
  event.preventDefault();

  const lead = {
    id: `custom-${Date.now()}`,
    company: document.querySelector("#lead-company").value.trim(),
    contact: document.querySelector("#lead-contact").value.trim() || "Owner / manager",
    segmentId: document.querySelector("#lead-segment").value,
    channel: document.querySelector("#lead-channel").value,
    signal: document.querySelector("#lead-signal").value.trim(),
    notes: document.querySelector("#lead-notes").value.trim(),
  };

  state.customLeads.unshift(lead);
  saveCustomLeads();
  renderLeadBoard();
  event.target.reset();
  document.querySelector("#lead-channel").value = "Email";
  document.querySelector("#lead-segment").value = rankedSegments()[0].id;
}

function handleOutreachSubmit(event) {
  event.preventDefault();

  const company = document.querySelector("#outreach-company").value.trim();
  const contact = document.querySelector("#outreach-contact").value.trim() || "there";
  const segmentId = document.querySelector("#outreach-segment").value;
  const serviceId = document.querySelector("#outreach-service").value;
  const channel = document.querySelector("#outreach-channel").value;
  const problem = document.querySelector("#outreach-problem").value.trim();
  const note = document.querySelector("#outreach-note").value.trim();
  const segment = getSegment(segmentId);
  const fitScore = scoreLead(segment, problem, note);
  const serviceLabel = serviceLabels[serviceId];
  const observation = buildObservation(problem, note, segment);
  const proof = buildProofLine(segment);
  const offer = buildOfferLine(segment, serviceLabel);
  const contactLabel = contact === "there" ? "Hi there," : `Hi ${contact},`;

  const emailText = `${contactLabel}

I came across ${company} while researching ${segment.title.toLowerCase()} in ${state.city}. ${observation}

I help businesses like this with ${serviceLabel.toLowerCase()} so they can present their work more clearly and look more premium without building a full in-house 3D team.

From my side, the strongest fit here would likely be ${offer}. ${proof}

If it helps, I can share a rough idea for how I would approach one of your current products, projects, or campaigns.

Would you be open to a short 15-minute call next week?

Best,
Mihir Botle`;

  const dmGreeting = contact === "there" ? "Hi," : `Hi ${contact},`;
  const dmText = `${dmGreeting} came across ${company} and noticed ${shortObservation(
    observation,
  )}. I help ${segment.title.toLowerCase()} with ${serviceLabel.toLowerCase()} so they can show their work better without hiring a full internal 3D team. Happy to share a quick idea if useful.`;

  const callText = `Hi, this is Mihir. I work with ${segment.title.toLowerCase()} on ${serviceLabel.toLowerCase()} and practical visual content. I noticed ${shortObservation(
    observation,
  )}. I had a simple idea that could help ${company} present things more clearly. Is this a bad time, or can I take 30 seconds to explain?`;

  const discoveryText = segment.discoveryQuestions
    .map((question, index) => `${index + 1}. ${question}`)
    .join("\n");

  const followUpText = `Follow-up 1 (after 3-4 days):
Share one specific idea: "${company} could benefit from ${offer} for a current launch or campaign."

Follow-up 2 (after 7-10 days):
Offer a low-friction pilot: one product, one room, one machine, or one hero visual direction.

Follow-up 3:
Close politely and leave the door open: "If timing is off right now, I can reconnect when you have a launch, proposal, or campaign that needs stronger visuals."`;

  outreachOutput.classList.remove("empty");
  outreachOutput.innerHTML = `
    <div class="summary-card">
      <div class="summary-card__header">
        <div>
          <h3>${escapeHtml(company)}</h3>
          <p>${escapeHtml(segment.title)} · ${escapeHtml(serviceLabel)} · ${escapeHtml(channel)}</p>
        </div>
        <div class="score">${fitScore}<small>fit score</small></div>
      </div>
      <p><strong>Why this lead is worth contacting:</strong> ${escapeHtml(segment.why[0])} ${escapeHtml(segment.why[1])}</p>
      <p><strong>Offer angle:</strong> ${escapeHtml(offer)}</p>
      <p><strong>Best proof to mention:</strong> ${escapeHtml(segment.proof)}</p>
    </div>
    <div class="outreach-panels">
      ${copyBlock("Cold email", emailText)}
      ${copyBlock("Short DM", dmText)}
      ${copyBlock("Call opener", callText)}
      ${copyBlock("Discovery questions", discoveryText)}
      ${copyBlock("Follow-up sequence", followUpText)}
    </div>
  `;
}

function handleBodyClick(event) {
  const copyButton = event.target.closest("[data-copy]");
  const useLeadButton = event.target.closest("[data-use-lead]");
  const useSegmentButton = event.target.closest("[data-use-segment]");
  const deleteLeadButton = event.target.closest("[data-delete-lead]");

  if (copyButton) {
    const text = decodeURIComponent(copyButton.getAttribute("data-copy"));
    copyText(text).then(() => {
      copyButton.textContent = "Copied";
      window.setTimeout(() => {
        copyButton.textContent = "Copy text";
      }, 1200);
    });
  }

  if (useLeadButton) {
    const lead = allLeads().find((item) => item.id === useLeadButton.getAttribute("data-use-lead"));
    if (lead) {
      fillOutreachForm(lead);
    }
  }

  if (useSegmentButton) {
    const segmentId = useSegmentButton.getAttribute("data-use-segment");
    const segment = getSegment(segmentId);
    document.querySelector("#outreach-segment").value = segmentId;
    syncServiceOptions();
    document.querySelector("#outreach-company").value = "";
    document.querySelector("#outreach-contact").value = "";
    document.querySelector("#outreach-problem").value = segment.why[0];
    document.querySelector("#outreach-note").value = "";
    document.querySelector(".generator").scrollIntoView({ behavior: "smooth" });
  }

  if (deleteLeadButton) {
    const leadId = deleteLeadButton.getAttribute("data-delete-lead");
    state.customLeads = state.customLeads.filter((lead) => lead.id !== leadId);
    saveCustomLeads();
    renderLeadBoard();
  }
}

function renderSegments() {
  const cards = rankedSegments()
    .map((segment) => {
      const queries = segment.searchQueries
        .map((query) => {
          const text = query.replace("{city}", state.city);
          const url = `https://www.google.com/search?q=${encodeURIComponent(text)}`;
          return `<a class="pill" href="${url}" target="_blank" rel="noreferrer">${text}</a>`;
        })
        .join("");

      return `
        <article class="segment-card">
          <div class="segment-card__header">
            <div>
              <h3>${escapeHtml(segment.title)}</h3>
              <p>${escapeHtml(segment.offer)}</p>
            </div>
            <div class="score">${segment.rankScore}<small>priority</small></div>
          </div>
          <div class="segment-meta">
            ${segment.services.map((service) => `<span class="pill">${serviceLabels[service]}</span>`).join("")}
          </div>
          <ul>
            ${segment.why.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
          </ul>
          <p><strong>Where to find leads:</strong> ${escapeHtml(segment.leadSources.join(", "))}</p>
          <p><strong>Approach angle:</strong> Lead with ${escapeHtml(segment.offer)}.</p>
          <div class="segment-actions">
            ${queries}
            <button class="ghost" type="button" data-use-segment="${segment.id}">Use in generator</button>
          </div>
        </article>
      `;
    })
    .join("");

  segmentList.innerHTML = cards;
  populateSegmentSelects();
}

function renderLeadBoard() {
  const cards = allLeads()
    .map((lead) => {
      const segment = getSegment(lead.segmentId);
      const score = scoreLead(segment, lead.signal, lead.notes);
      const deleteButton = lead.id.startsWith("custom-")
        ? `<button class="ghost" type="button" data-delete-lead="${lead.id}">Delete</button>`
        : "";

      return `
        <article class="lead-card">
          <div class="lead-card__header">
            <div>
              <h3>${escapeHtml(lead.company)}</h3>
              <p>${escapeHtml(segment.title)}</p>
            </div>
            <div class="score">${score}<small>fit score</small></div>
          </div>
          <div class="lead-meta">
            <span class="pill">${escapeHtml(lead.channel)}</span>
            <span class="pill">${escapeHtml(lead.contact)}</span>
          </div>
          <ul>
            <li><strong>Signal:</strong> ${escapeHtml(lead.signal || "No signal added yet.")}</li>
            <li><strong>Notes:</strong> ${escapeHtml(lead.notes || "No notes added yet.")}</li>
          </ul>
          <div class="lead-actions">
            <button class="ghost" type="button" data-use-lead="${lead.id}">Use for outreach</button>
            ${deleteButton}
          </div>
        </article>
      `;
    })
    .join("");

  leadBoard.innerHTML = cards;
}

function populateSegmentSelects() {
  const options = rankedSegments()
    .map((segment) => `<option value="${segment.id}">${segment.title}</option>`)
    .join("");

  leadSegmentSelect.innerHTML = options;
  outreachSegmentSelect.innerHTML = options;
  syncServiceOptions();
}

function populateServiceSelect() {
  outreachServiceSelect.innerHTML = Object.entries(serviceLabels)
    .map(([id, label]) => `<option value="${id}">${label}</option>`)
    .join("");
}

function syncServiceOptions() {
  const segment = getSegment(outreachSegmentSelect.value || rankedSegments()[0].id);
  outreachServiceSelect.innerHTML = segment.services
    .map((serviceId) => `<option value="${serviceId}">${serviceLabels[serviceId]}</option>`)
    .join("");
}

function rankedSegments() {
  return [...segments]
    .map((segment) => ({
      ...segment,
      rankScore: scoreSegment(segment),
    }))
    .sort((a, b) => b.rankScore - a.rankScore);
}

function scoreSegment(segment) {
  let score = segment.baseScore;

  state.selectedServices.forEach((serviceId) => {
    score += segment.services.includes(serviceId) ? 3 : -1;
  });

  if (state.size === "small" && segment.sizeFit === "small") {
    score += 5;
  }

  if (state.size === "growing" && segment.sizeFit === "growing") {
    score += 5;
  }

  if (state.size === "mixed") {
    score += 2;
  }

  return clamp(score, 60, 99);
}

function scoreLead(segment, signal, notes) {
  let score = scoreSegment(segment);
  const text = `${signal} ${notes}`.toLowerCase();

  if (text.includes("outdated") || text.includes("weak") || text.includes("no")) {
    score += 3;
  }

  if (text.includes("launch") || text.includes("new") || text.includes("campaign")) {
    score += 2;
  }

  return clamp(score, 60, 99);
}

function fillOutreachForm(lead) {
  document.querySelector("#outreach-company").value = lead.company;
  document.querySelector("#outreach-contact").value = lead.contact;
  document.querySelector("#outreach-segment").value = lead.segmentId;
  syncServiceOptions();
  document.querySelector("#outreach-channel").value = lead.channel;
  document.querySelector("#outreach-problem").value = lead.signal || "";
  document.querySelector("#outreach-note").value = lead.notes || "";
  document.querySelector(".generator").scrollIntoView({ behavior: "smooth" });
}

function copyBlock(title, text) {
  return `
    <article class="copy-block">
      <small>${escapeHtml(title)}</small>
      <pre>${escapeHtml(text)}</pre>
      <button type="button" data-copy="${encodeURIComponent(text)}">Copy text</button>
    </article>
  `;
}

function buildObservation(problem, note, segment) {
  if (problem && note) {
    return `I noticed ${problem.toLowerCase()}, and from a quick look it seems ${note.charAt(0).toLowerCase()}${note.slice(
      1,
    )}`;
  }

  if (problem) {
    return `I noticed ${problem.toLowerCase()}.`;
  }

  if (note) {
    return `From a quick look, it seems ${note.charAt(0).toLowerCase()}${note.slice(1)}`;
  }

  return `${segment.why[0]}`;
}

function shortObservation(text) {
  return text.replace(/^I noticed /i, "").replace(/\.$/, "");
}

function buildProofLine(segment) {
  return `Relevant proof from my work includes ${segment.proof}.`;
}

function buildOfferLine(segment, serviceLabel) {
  return `${segment.offer}, built around ${serviceLabel.toLowerCase()}`;
}

function getSegment(segmentId) {
  return segments.find((segment) => segment.id === segmentId) || rankedSegments()[0];
}

function allLeads() {
  return [...state.customLeads, ...starterLeads];
}

function loadCustomLeads() {
  try {
    const raw = window.localStorage.getItem(storageKey);
    return raw ? JSON.parse(raw) : [];
  } catch (error) {
    return [];
  }
}

function saveCustomLeads() {
  window.localStorage.setItem(storageKey, JSON.stringify(state.customLeads));
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function escapeHtml(text) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function escapeAttribute(text) {
  return escapeHtml(text).replaceAll('"', "&quot;");
}

async function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.focus();
  textarea.select();
  document.execCommand("copy");
  textarea.remove();
}
