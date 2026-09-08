# Travel Guide (`travel-guide/`)

Auto tour-guide mobile app: as you move through a place, it tells you what
you're seeing — history, stories, fun facts, what not to miss — out loud,
like a human guide walking with you.

## Where this lives (repo routing)

Subtree of the planning superset, same pattern as `journal-app/`,
`chronolens/`, `foresight/`, `cli-workflow/`, `echoscribe/`:

- Planning superset: `https://github.com/greninja-op/PROJECT-PLANNING-.git`
  (`PROJECT-PLANNING/` → remote `origin`)
- Standalone: `https://github.com/greninja-op/TRAVEL-GUIDE.git`
  (remote `travel-guide`, prefix `travel-guide/` — renamed from TRAVEL-AGENT)

**Naming:** folder/remote use git-safe `travel-guide` (your asked name,
hyphenated — spaces break `subtree --prefix` matching). The GitHub repo was
`TRAVEL-AGENT` and is now `TRAVEL-GUIDE`; remote already updated.

## Push — the only command (from `PROJECT-PLANNING/`)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\push.ps1 -m "message"
```

`git add -A` + commit, `push origin HEAD` (always), then for every touched
`prefix/*`, `git subtree push --prefix=<prefix> <remote> main` — so
`travel-guide/*` changes land in **both** repos in one command. Never
hand-push the standalone repo. Exit code 1 is normal (git progress on
stderr); verify with `git status`.

## Layout (v0.1 scaffold)

```
travel-guide/
  README.md          this file
  SPEC.md            full Product Specification v0.1 (source of truth)
  DESIGN-TOKENS.md   draft tokens — finalize via DesignSoul before UI build
  .env.example       secret *names* only — never real keys
```

Future (per SPEC §4): `mobile/` (Android native per your call — Kotlin +
Compose), `content/` (POI packs: history/facts/see-list per place),
`shared-core/` (geo triggers, narration queue, offline packs contract).

## Secrets rule

Filled credentials live ONLY at workspace root in `../secrets/`
(not a git repo). This repo commits `.env.example` with names only.

## Current status

- [x] Repo registered + `push.ps1` wiring
- [ ] Phase 0: scaffolding, POI pack format, one test city walking loop
- [ ] Open questions (SPEC §8): Android-only vs iOS later, POI data source,
      on-device vs cloud TTS/narration
