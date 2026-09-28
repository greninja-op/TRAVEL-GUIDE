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

## Direction (Updated 2026-09-28 — Luxury Travel App Aesthetic)

AirBnB Luxe & Wanderlust modern travel inspiration: crisp off-white canvas,
sunset coral brand accent (`#FF5A36`), midnight carbon (`#121826`) high contrast,
radiant amber (`#F59E0B`) audio/review highlight, emerald green (`#10B981`) open status,
18dp card radii with subtle elevation, circular 44dp map controls, and floating dock navigation.

## Active tokens

| Token | Value | Use |
|---|---|---|
| `--bg` | `#F8F9FA` | crisp, airy off-white canvas |
| `--surface` | `#FFFFFF` | pure white cards, 1px `#E5E7EB` border |
| `--primary` | `#FF5A36` | sunset coral brand accent (buttons, active pills) |
| `--dark` | `#121826` | midnight carbon (high-contrast chips, dark buttons) |
| `--highlight` | `#F59E0B` | radiant amber (audio playing, guide pulse, ratings) |
| `--success` | `#10B981` | emerald green (open now, verified checkmarks) |
| `--text` | `#111827` | deep charcoal primary text for high readability |
| `--text-2` | `#6B7280` | slate grey secondary text |
| `--danger` | `#EF4444` | warning / destructive |

## Type

- Headings: Inter 600; Body/place stories: readable serif (Lora) 17–18px; Chrome: Inter 400–500
- Scale ONLY: 12 / 14 / 16 / 18 / 24 / 32 / 40

## Components (build once, reuse)

- One bottom-sheet player, one POI card, one map-pin style
- Radius: 18px cards, 14px buttons, 24px sheets, 999px pills/tags
- Motion: press 96% + opacity 100ms; page 200ms slide/fade; spring toggles
