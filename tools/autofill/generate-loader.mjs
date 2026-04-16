import fs from "node:fs/promises";
import path from "node:path";

const root = process.cwd();
const profilePath = path.join(root, "tools/autofill/profile.autofill.json");
const outputPath = path.join(root, "tools/autofill/generated-loader.js");

async function main() {
  const raw = await fs.readFile(profilePath, "utf8");
  const profile = JSON.parse(raw);

  const script = `(() => {
  const profile = ${JSON.stringify(profile, null, 2)};
  window.__JOB_APPLY_PROFILE__ = profile;
  console.log("[autofill] Profile loaded into window.__JOB_APPLY_PROFILE__");
  console.log("[autofill] Now run tools/autofill/form-autofill.js in this page.");
})();\n`;

  await fs.writeFile(outputPath, script, "utf8");
  console.log(`[autofill] Generated loader: ${outputPath}`);
}

main().catch((error) => {
  console.error("[autofill] Failed to generate loader:", error.message);
  process.exitCode = 1;
});
