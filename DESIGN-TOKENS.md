# Travel Guide design tokens (DRAFT — finalize via DesignSoul before UI build)

Per `AGENTS.md`: for ANY UI work here, open `DesignSoul/SKILL.md` first,
follow its Step 0 reading list, load only the refs it points to, use
authentic icons (`dashboard-icons/` → `project assests/`), run
`DesignSoul/checklist.md` before finishing.

## Direction (draft, to confirm)

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
