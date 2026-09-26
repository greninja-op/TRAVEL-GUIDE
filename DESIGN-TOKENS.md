# Travel Guide design tokens (FINALIZED 2026-09-26 — `design-reference/reference.html` is LAW)

> Finalized by Terminal 2 on 2026-09-26 into `design-reference/reference.html`
> (reference v1) + companion `design-reference/SKILL.md`. Values below were
> carried 1:1. From now on: **reference.html wins on any conflict**; update it
> first, then mirror here and in `mobile/.../ui/theme/Tokens.kt` in the same turn.
> Rev the reference only on a system change (palette, type, radii, icons, motion).

Per `AGENTS.md`: for ANY UI work here, open `DesignSoul/SKILL.md` first,
follow its Step 0 reading list, load only the refs it points to, use
authentic icons (`ICONS-ASSETS/system-and-ui/lucide/icons` for UI glyphs —
the single family; brand marks per the ICONS-ASSETS hierarchy), run
`DesignSoul/checklist.md` before finishing.

## Direction (finalized)

Calm outdoor-readable light theme: near-white base, one deep accent, one
warm highlight for "you are here / playing now". No gradients, max shadow
`0 1px 3px rgba(0,0,0,0.06)`, one icon family (Phosphor or Lucide regular).

## Draft tokens

| Token | Value | Use |
|---|---|---|
| `--bg` | `#FAFAF7` | app background |
| `--surface` | `#FFFFFF` | cards, 1px `#E9E6E0` border |
| `--primary` | `#2F5D50` | main accent (headers, active states) |
| `--highlight` | `#C97B4A` | sparing: now-playing, nearby marker |
| `--text` | `#1F1D1B` | primary text |
| `--text-2` | `#6B6862` | secondary text |
| `--danger` | `#B3453A` | destructive |

## Type (draft)

- Headings: Inter 600; Body/place stories: readable serif (Lora / Source
  Serif 4) 17–18px; Chrome: Inter 400–500
- Scale ONLY: 12 / 14 / 16 / 18 / 24 / 32 / 40

## Components (build once, reuse)

- One bottom-sheet player, one POI card, one map-pin style — never
  re-implement per screen
- Radius: 10px cards/inputs, 8px buttons, 999px pills for tags only
- Motion: press 96% + opacity 100ms; page 200ms slide/fade; narration
  state changes animate, nothing decorative
