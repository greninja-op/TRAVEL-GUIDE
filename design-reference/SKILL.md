---
name: travel-guide-design-reference
description: Use whenever creating, styling, or reviewing ANY UI surface for the Travel Guide app (Compose screens, components, web previews, design docs). Provides the frozen token set and rules, rendered live in reference.html. Required alongside DesignSoul/SKILL.md Step 0 before any UI work.
---

# Travel Guide Design Reference

## Source of truth

`travel-guide/design-reference/reference.html` (reference v1, 2026-09-26) is the canonical, renderable UI contract. Open it and sample values from it directly — never retype hexes, sizes or curves from memory. If this file, `DESIGN-TOKENS.md`, `SPEC.md` §5 or `Tokens.kt` ever disagree, **reference.html wins**: update it first, then mirror the change into the other docs in the same turn. Rev it only on a system change (palette, type, radii, icons, motion) — feature adds never touch it.

Companion in the same folder: `DesignSoul/SKILL.md` governs process (audit, hierarchy, states, motion, accessibility, critique). This skill governs the Travel Guide token layer. Follow both.

## Core tokens (full list + contrast notes in reference.html)

- **Color** — `--bg #FAFAF7` · `--surface #FFFFFF` · `--border #E9E6E0` · `--primary #2F5D50` · `--highlight #C97B4A` · `--text #1F1D1B` · `--text-2 #6B6862` · `--danger #B3453A`. Eight hues, nothing else.
- **State layers** — hover/press/washes/scrim are alpha of the hues above: `--state-hover rgba(31,29,27,.05)`, `--state-press rgba(31,29,27,.10)`, `--primary-wash rgba(47,93,80,.10)`, `--highlight-wash rgba(201,123,74,.14)`, `--danger-wash rgba(179,69,58,.10)`, `--scrim rgba(31,29,27,.32)`. No new hexes, ever.
- **Type** — Inter 400/500/600 chrome; Lora 400/500 place stories at 17–18px / 1.65. Scale only 12 / 14 / 16 / 18 / 24 / 32 / 40. Chrome never below 12; stories never below 17.
- **Spacing** — 4pt base: 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64. Nothing off-grid; between-group gaps larger than within-group.
- **Radius** — 8 buttons/inputs/icon buttons · 10 cards/sheets/fields · pill for tags only.
- **Depth** — one shadow maximum `0 1px 3px rgba(0,0,0,.06)`, only on cards and the bottom sheet. No gradients, no glow, no blur.
- **Motion** — press scale .96 @ 100ms; state changes @ 150ms; page/sheet @ 200ms. Easing `--ease-out` in, `--ease-in` out, `--ease-in-out` within. `prefers-reduced-motion` freeze is mandatory.
- **Icons** — Lucide only (local source `ICONS-ASSETS/system-and-ui/lucide/icons`): 24px box, stroke 2, round caps/joins, unfilled, `currentColor`. Sizes 16/20/24. Never mixed families, fills, or emoji.

## Product rules that shape the UI

- Status is always dot/icon **plus a word** — color never carries meaning alone.
- Every POI shows sources + pack version; History / Legend / Fun fact stay labeled (text + border style + icon, not color alone).
- Mute is always visible; audio starts only after the user starts it; background location is a separate plain-language opt-in; battery cost is disclosed before it happens.
- One primary action per view; 44px minimum targets (48 for primary); map-first layout; one implementation each of the player sheet, POI card, pin and list row.
- Light theme only in beta — no dark mode, no second accent, no per-screen theme tweaks.

## How to use this skill

1. Open `reference.html` before touching any UI; sample exact values (`:root` variables).
2. Run `DesignSoul/SKILL.md` Step 0 (core reads) + `DesignSoul/checklist.md` — this skill does not replace that process.
3. Build with the tokens above; all interactive states (default/hover/focus/pressed/disabled/loading) per the reference page.
4. Verify before finishing: no hardcoded transitions, focus-visible rings, AA contrast, 44px targets, reduced-motion freeze, no invented content or numbers.
5. Need a genuinely new token? Add it to `reference.html` first, then use it — the reference may never lag what ships.
