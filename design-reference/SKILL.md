---
name: travel-guide-design-reference
description: Use whenever creating, styling, or reviewing ANY UI surface for the Travel Guide app (Compose screens, components, web previews, design docs). Provides the frozen token set and rules, rendered live in reference.html. Required alongside DesignSoul/SKILL.md Step 0 before any UI work.
---

# Travel Guide Design Reference

## Source of truth

`travel-guide/design-reference/reference.html` (reference v2.0, 2026-09-28) is the canonical, renderable UI contract. Open it and sample values from it directly — never retype hexes, sizes or curves from memory. If this file, `DESIGN-TOKENS.md`, `SPEC.md` §5 or `Tokens.kt` ever disagree, **reference.html wins**: update it first, then mirror the change into the other docs in the same turn. Rev it only on a system change (palette, type, radii, icons, motion) — feature adds never touch it.

Companion in the same folder: `DesignSoul/SKILL.md` governs process (audit, hierarchy, states, motion, accessibility, critique). This skill governs the Travel Guide token layer. Follow both.

## Core tokens (full list + contrast notes in reference.html)

- **Color** — `--bg #F8F9FA` · `--surface #FFFFFF` · `--border #E5E7EB` · `--primary #FF5A36` · `--dark #121826` · `--highlight #F59E0B` · `--success #10B981` · `--text #111827` · `--text-2 #6B7280` · `--danger #EF4444`.
- **State layers** — hover/press/washes/scrim are alpha of the hues above: `--state-hover rgba(18,24,38,.04)`, `--state-press rgba(18,24,38,.08)`, `--primary-wash rgba(255,90,54,.08)`, `--dark-wash rgba(18,24,38,.06)`, `--highlight-wash rgba(245,158,11,.12)`, `--success-wash rgba(16,185,129,.08)`, `--danger-wash rgba(239,68,68,.08)`, `--scrim rgba(18,24,38,.40)`. No new hexes, ever.
- **Type** — Inter 400/500/600/700 chrome; Lora 400/500 place stories at 17–18px / 1.65. Scale only 12 / 14 / 16 / 18 / 24 / 32 / 40. Chrome never below 12; stories never below 17.
- **Spacing** — 4pt base: 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64. Nothing off-grid; between-group gaps larger than within-group.
- **Radius** — 14 buttons/inputs/icon buttons · 18 cards/floating search/header · 24 bottom sheets · 999 pill for tags/pills/circular controls.
- **Depth** — subtle card shadow `0 2px 8px rgba(18,24,38,.04)`, elevated header & sheet shadow `0 8px 24px rgba(18,24,38,.08)`, pin drop shadow `0 4px 12px rgba(18,24,38,.16)`.
- **Motion** — press scale .96 @ 100ms; state changes @ 150ms; page/sheet @ 200–250ms. Easing `--ease-out` in, `--ease-in` out, `--ease-in-out` within. `prefers-reduced-motion` freeze is mandatory.
- **Icons** — Lucide only (local source `ICONS-ASSETS/system-and-ui/lucide/icons`): 24px box, stroke 2, round caps/joins, unfilled, `currentColor`. Sizes 16/20/24. Never mixed families, fills, or emoji.

## Product rules that shape the UI

- Status is always dot/icon **plus a word** — color never carries meaning alone.
- Every POI shows sources + pack version; History / Legend / Fun fact stay labeled (text + border style + icon, not color alone).
- Mute is always visible; audio starts only after the user starts it; background location is a separate plain-language opt-in; battery cost is disclosed before it happens.
- One primary action per view; 44px minimum targets (48 for primary); map-first layout; one implementation each of the player sheet, POI card, pin and list row.
- Filter pills hug content horizontally at 34dp height — never distorted egg pills.
- Light luxury theme only in beta — no dark mode, no second accent, no per-screen theme tweaks.

## How to use this skill

1. Open `reference.html` before touching any UI; sample exact values (`:root` variables).
2. Run `DesignSoul/SKILL.md` Step 0 (core reads) + `DesignSoul/checklist.md` — this skill does not replace that process.
3. Build with the tokens above; all interactive states (default/hover/focus/pressed/disabled/loading) per the reference page.
4. Verify before finishing: no hardcoded transitions, focus-visible rings, AA contrast, 44px targets, reduced-motion freeze, no invented content or numbers.
5. Need a genuinely new token? Add it to `reference.html` first, then use it — the reference may never lag what ships.
