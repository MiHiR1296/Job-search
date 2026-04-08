(() => {
  const PROFILE = window.__JOB_APPLY_PROFILE__ || {};

  if (!Object.keys(PROFILE).length) {
    console.warn("[autofill] No profile detected.");
    console.warn(
      '[autofill] Set window.__JOB_APPLY_PROFILE__ first, then run this script again.'
    );
    return;
  }

  const FIELD_MAP = [
    // Name
    { patterns: ["first name", "firstname", "given name"], value: PROFILE.firstName || "" },
    { patterns: ["last name", "lastname", "surname", "family name"], value: PROFILE.lastName || "" },
    { patterns: ["full name", "your name", "applicant name"], value: PROFILE.fullName || "" },

    // Contact
    { patterns: ["email", "e-mail"], value: PROFILE.email || "" },
    { patterns: ["phone", "mobile", "contact number"], value: PROFILE.phone || "" },

    // Links
    { patterns: ["linkedin"], value: PROFILE.linkedin || "" },
    { patterns: ["github"], value: PROFILE.github || "" },
    {
      patterns: ["portfolio", "behance", "website", "personal site"],
      value: PROFILE.portfolio || PROFILE.website || "",
    },

    // Location
    { patterns: ["location", "current location"], value: PROFILE.location || "" },
    { patterns: ["city"], value: PROFILE.city || "" },
    { patterns: ["state"], value: PROFILE.state || "" },
    { patterns: ["country"], value: PROFILE.country || "" },

    // Work details
    { patterns: ["current company", "company name"], value: PROFILE.currentCompany || "" },
    { patterns: ["current title", "job title", "designation"], value: PROFILE.currentTitle || "" },
    { patterns: ["experience", "years of experience"], value: PROFILE.yearsExperience || "" },
    { patterns: ["notice period"], value: PROFILE.noticePeriodDays || "" },
    {
      patterns: ["current ctc", "current salary", "current compensation"],
      value: PROFILE.currentCtcLpa || PROFILE.currentCompensation || "",
    },
    {
      patterns: ["expected ctc", "expected salary", "expected compensation"],
      value: PROFILE.expectedCtcLpa || PROFILE.expectedCompensation || "",
    },
    {
      patterns: ["minimum salary", "minimum compensation"],
      value: PROFILE.minimumAcceptableLpa || "",
    },

    // Yes/No and authorization
    { patterns: ["relocate", "willing to relocate"], value: PROFILE.willingToRelocate || "" },
    { patterns: ["sponsorship", "require visa"], value: PROFILE.requiresSponsorship || "" },
    { patterns: ["work authorization", "authorized to work"], value: PROFILE.workAuthorization || "" },
    { patterns: ["work mode", "remote/hybrid", "preferred mode"], value: PROFILE.preferredWorkMode || "" },

    // Free text
    {
      patterns: ["cover letter", "why do you want", "about yourself"],
      value: PROFILE.coverLetterShort || "",
    },
  ];

  const normalize = (text) =>
    (text || "")
      .toLowerCase()
      .replace(/[_-]+/g, " ")
      .replace(/\s+/g, " ")
      .trim();

  const matchValue = (labelText) => {
    const normalized = normalize(labelText);
    for (const rule of FIELD_MAP) {
      if (rule.patterns.some((p) => normalized.includes(p))) {
        return rule.value;
      }
    }
    return null;
  };

  const textForElement = (el) => {
    const id = el.getAttribute("id");
    const name = el.getAttribute("name");
    const placeholder = el.getAttribute("placeholder");
    const ariaLabel = el.getAttribute("aria-label");
    let label = "";

    if (id) {
      const forLabel = document.querySelector(`label[for="${id}"]`);
      if (forLabel) {
        label = forLabel.innerText || forLabel.textContent || "";
      }
    }
    const parentLabel = el.closest("label");
    const parentLabelText = parentLabel ? parentLabel.innerText || parentLabel.textContent || "" : "";

    return [id, name, placeholder, ariaLabel, label, parentLabelText].filter(Boolean).join(" ");
  };

  const setNativeValue = (el, value) => {
    const tag = (el.tagName || "").toLowerCase();
    const type = (el.getAttribute("type") || "").toLowerCase();

    if (tag === "select") {
      const options = Array.from(el.options || []);
      const target = normalize(String(value));
      let option =
        options.find((o) => normalize(o.textContent).includes(target)) ||
        options.find((o) => normalize(o.value).includes(target));
      if (!option && (value === "Yes" || value === "No")) {
        option =
          options.find((o) => normalize(o.textContent) === normalize(value)) ||
          options.find((o) => normalize(o.value) === normalize(value));
      }
      if (option) {
        el.value = option.value;
      }
    } else if (type === "checkbox") {
      if (value === "Yes") el.checked = true;
      if (value === "No") el.checked = false;
    } else if (type === "radio") {
      const group = document.querySelectorAll(`input[type="radio"][name="${el.name}"]`);
      for (const radio of group) {
        const radioText = textForElement(radio);
        if (normalize(radioText).includes(normalize(String(value)))) {
          radio.checked = true;
        }
      }
    } else {
      el.value = value;
    }

    el.dispatchEvent(new Event("input", { bubbles: true }));
    el.dispatchEvent(new Event("change", { bubbles: true }));
    el.dispatchEvent(new Event("blur", { bubbles: true }));
  };

  const visibleAndEditable = (el) => {
    if (!el) return false;
    if (el.disabled || el.readOnly) return false;
    const style = window.getComputedStyle(el);
    if (style.display === "none" || style.visibility === "hidden") return false;
    return true;
  };

  const candidates = Array.from(
    document.querySelectorAll("input, textarea, select")
  ).filter(visibleAndEditable);

  let filled = 0;
  for (const el of candidates) {
    const type = (el.getAttribute("type") || "").toLowerCase();
    if (["hidden", "file", "submit", "button", "password"].includes(type)) continue;

    if ((type === "checkbox" || type === "radio") && el.checked) continue;
    if (el.value && String(el.value).trim() !== "") continue;

    const labelText = textForElement(el);
    const value = matchValue(labelText);
    if (!value) continue;

    setNativeValue(el, value);
    filled += 1;
  }

  console.log(`[autofill] Completed. Fields filled: ${filled}`);
  console.log("[autofill] Verify all fields before final submit.");
})();
