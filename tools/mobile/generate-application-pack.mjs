import fs from "node:fs/promises";
import path from "node:path";

const root = process.cwd();

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const [k, ...rest] = arg.split("=");
    return [k.replace(/^--/, ""), rest.join("=")];
  })
);

const required = ["company", "role", "url"];
for (const key of required) {
  if (!args[key]) {
    console.error(
      `[mobile-pack] Missing required arg --${key}=...`
    );
    process.exit(1);
  }
}

const profilePath = path.join(root, "config/profile.yml");
const templatePath = path.join(
  root,
  "templates/mobile/application-pack-template.md"
);

const now = new Date();
const datePart = now.toISOString().slice(0, 10);
const timePart = now.toISOString();
const safeSlug = `${args.company}-${args.role}`
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, "-")
  .replace(/^-+|-+$/g, "");

const outDir = path.join(root, "mobile", `${datePart}-${safeSlug}`);
await fs.mkdir(outDir, { recursive: true });

const parseYamlLineValue = (yaml, key, fallback = "") => {
  const regex = new RegExp(`^\\s*${key}:\\s*"?([^"\\n]+)"?\\s*$`, "m");
  const match = yaml.match(regex);
  return match ? match[1].trim() : fallback;
};

let profileYaml = "";
try {
  profileYaml = await fs.readFile(profilePath, "utf8");
} catch {
  console.warn(
    "[mobile-pack] config/profile.yml not found. Using placeholders."
  );
}

const parseBoolAsYesNo = (value, fallback = "N/A") => {
  const raw = String(value || "").trim().toLowerCase();
  if (!raw) return fallback;
  if (["true", "yes", "y", "1"].includes(raw)) return "Yes";
  if (["false", "no", "n", "0"].includes(raw)) return "No";
  return value;
};

const profile = {
  fullName: parseYamlLineValue(profileYaml, "full_name", "Your Name"),
  email: parseYamlLineValue(profileYaml, "email", "you@example.com"),
  phone: parseYamlLineValue(profileYaml, "phone", "+91XXXXXXXXXX"),
  location: parseYamlLineValue(profileYaml, "location", "Mumbai, India"),
  linkedin: parseYamlLineValue(profileYaml, "linkedin", "linkedin.com/in/your-handle"),
  github: parseYamlLineValue(profileYaml, "github", "github.com/your-handle"),
  portfolio: parseYamlLineValue(profileYaml, "portfolio_url", "your-portfolio.example.com"),
  currentCompany: parseYamlLineValue(profileYaml, "current_company", ""),
  currentTitle: parseYamlLineValue(profileYaml, "current_title", ""),
  yearsExperience: parseYamlLineValue(profileYaml, "years_total", ""),
  noticePeriodDays: parseYamlLineValue(profileYaml, "notice_period_days", ""),
  currentCtcLpa: parseYamlLineValue(profileYaml, "current_ctc_lpa", ""),
  expectedCtcLpa: parseYamlLineValue(profileYaml, "expected_ctc_lpa", ""),
  minimumAcceptableLpa: parseYamlLineValue(profileYaml, "minimum_acceptable_lpa", ""),
  willingToRelocate: parseBoolAsYesNo(
    parseYamlLineValue(profileYaml, "relocation_possible", "")
  ),
  requiresSponsorship: parseBoolAsYesNo(
    parseYamlLineValue(profileYaml, "sponsorship_required", "")
  ),
};

let template = await fs.readFile(templatePath, "utf8");
const replacements = {
  company: args.company,
  role: args.role,
  source_url: args.url,
  generated_at: timePart,
  safe_slug: safeSlug,
  full_name: profile.fullName,
  email: profile.email,
  phone: profile.phone,
  location: profile.location,
  linkedin: profile.linkedin,
  github: profile.github,
  portfolio: profile.portfolio,
  current_company: profile.currentCompany || "N/A",
  current_title: profile.currentTitle || "N/A",
  years_experience: profile.yearsExperience || "N/A",
  notice_period_days: profile.noticePeriodDays || "N/A",
  current_ctc_lpa: profile.currentCtcLpa || "N/A",
  expected_ctc_lpa: profile.expectedCtcLpa || "N/A",
  minimum_acceptable_lpa: profile.minimumAcceptableLpa || "N/A",
  willing_to_relocate: String(profile.willingToRelocate),
  requires_sponsorship: String(profile.requiresSponsorship),
  cover_letter_short:
    "Generate with modes/cover-letter.md using this JD URL and save in this folder.",
  resume_highlights:
    "- Add JD-specific highlights generated from modes/tailored-cv.md\n- Keep only strongest role-relevant bullets",
  notes:
    "This pack is mobile-friendly. Keep these files available on your phone for copy/paste while applying.",
};

for (const [key, value] of Object.entries(replacements)) {
  template = template.replaceAll(`{{${key}}}`, value);
}

const packPath = path.join(outDir, `${safeSlug}-application-pack.md`);
const formAnswersPath = path.join(outDir, `${safeSlug}-form-answers.md`);
const coverLetterPath = path.join(outDir, `${safeSlug}-cover-letter.md`);
const resumePath = path.join(outDir, `${safeSlug}-resume.md`);

const formAnswers = `# Form Answers - ${args.company} - ${args.role}

- Full Name: ${profile.fullName}
- Email: ${profile.email}
- Phone: ${profile.phone}
- Location: ${profile.location}
- LinkedIn: ${profile.linkedin}
- GitHub: ${profile.github}
- Portfolio: ${profile.portfolio}
- Years Experience: ${profile.yearsExperience || "N/A"}
- Notice Period (Days): ${profile.noticePeriodDays || "N/A"}
- Current CTC (LPA): ${profile.currentCtcLpa || "N/A"}
- Expected CTC (LPA): ${profile.expectedCtcLpa || "N/A"}
- Minimum Acceptable (LPA): ${profile.minimumAcceptableLpa || "N/A"}
- Relocation: ${profile.willingToRelocate}
- Sponsorship Required: ${profile.requiresSponsorship}
`;

await fs.writeFile(packPath, template, "utf8");
await fs.writeFile(formAnswersPath, formAnswers, "utf8");
await fs.writeFile(
  coverLetterPath,
  `# Cover Letter - ${args.company} - ${args.role}\n\n(Generate using modes/cover-letter.md)\n`,
  "utf8"
);
await fs.writeFile(
  resumePath,
  `# Tailored Resume - ${args.company} - ${args.role}\n\n(Generate using modes/tailored-cv.md)\n`,
  "utf8"
);

console.log(`[mobile-pack] Created: ${outDir}`);
console.log(`[mobile-pack] Main pack: ${packPath}`);
