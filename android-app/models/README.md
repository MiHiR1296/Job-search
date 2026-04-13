# On-device GGUF models (Career Ops Mobile)

Weight files (`.gguf`) are **large** (often 1–8+ GB). They are **not** committed as plain Git blobs.

## Option A — Recommended: keep models on the phone

1. Download a GGUF to device storage (e.g. `/sdcard/Download/`), or keep it in Drive/Files.
2. In the app **AI Setup** tab, tap **Pick file** to choose the `.gguf` (the app copies it to private storage and fills the path), or paste a filesystem path manually.

Use a model that matches the app’s chat format (see main Android build doc): **Qwen2.5 Instruct** or **Meta Llama 3.x Instruct** GGUFs work best with the built-in prompt wrappers.

## Option B — Track binaries with Git LFS (team / reproducible builds)

If you need model files **in this repository** (e.g. CI or teammates):

1. Install [Git LFS](https://git-lfs.com/) and run once in the repo: `git lfs install`
2. Place `.gguf` files under `android-app/models/` (see `manifest.json` for suggested names).
3. `git add android-app/models/*.gguf` — LFS will store the pointer; GitHub bills **LFS storage and bandwidth** separately from the repo.

`.gitattributes` in the repo root already marks `android-app/models/**/*.gguf` for LFS when you add them.

## Option C — Small smoke-test model only

A **very small** quantized instruct model (e.g. sub-100MB) might fit under GitHub’s single-file limit without LFS, but it is still discouraged (repo bloat, slow clones). Prefer LFS or on-device download.

## Manifest

See `manifest.json` for suggested model IDs and Hugging Face **download page** links (not direct file URLs; pick the GGUF variant you want).
