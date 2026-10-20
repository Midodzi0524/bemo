# <APP_NAME> — Project Pack

A personal finance and wishlist tracker for Android (Google Play), built with Expo (React Native) + Firebase. This folder is the complete specification and launch kit. Give **the whole folder** to your AI coding tool.

## Start here

1. Read `DESIGN_SYSTEM.md` §0 (rules for the AI coder) and §2.2 (release scoping).
2. Replace placeholders everywhere: `<APP_NAME>`, `<app-slug>`, `<PACKAGE_NAME>`, `<scheme>`, `<DEVELOPER_NAME>`, `<SUPPORT_EMAIL>`, `<DATE>`, `<AMOUNT>`.
3. Paste the **master prompt** from `DESIGN_SYSTEM.md` §20 into your AI coding tool. It builds in phases and asks you before each next phase.

## File index

| File | What it is |
|---|---|
| `DESIGN_SYSTEM.md` | Source of truth: scope, feature catalog (IDs + tiers + acceptance criteria), design tokens, data model, formulas, security, accessibility, Play launch guide, build phases |
| `WIREFRAMES_AND_FLOWS.md` | Screen wireframes, Mermaid user flows, microcopy rules |
| `PROJECT_STRUCTURE_AND_CI.md` | Repo layout, dependency rules, coding conventions, environments, `app.config.ts`, `eas.json`, GitHub Actions, rules tests, E2E list |
| `firebase/firestore.rules` | Deployable security rules (validated writes, owner-only access) |
| `firebase/firestore.indexes.json` | Deployable composite indexes |
| `locales/en-GH.json` | All in-app text (copy deck) |
| `LAUNCH_AND_SUPPORT.md` | Play listing text, icon/graphics/screenshot briefs, release, incident and support playbooks |
| `PRIVACY_POLICY.md` | Privacy policy template (needs lawyer review) |
| `TERMS_OF_SERVICE.md` | Terms of service template (needs lawyer review) |

## Suggested folder placement in the code repo

```
docs/                     ← DESIGN_SYSTEM.md, WIREFRAMES_AND_FLOWS.md, PROJECT_STRUCTURE_AND_CI.md,
                            LAUNCH_AND_SUPPORT.md, PRIVACY_POLICY.md, TERMS_OF_SERVICE.md
firebase/                 ← firestore.rules, firestore.indexes.json
src/i18n/locales/         ← en-GH.json
README.md                 ← this file
```

## Build order (summary)

Phase 0 setup → 1 domain logic → 2 data layer → 3 components → 4 core screens → 5 wishlist/insights/recurring → 6 security & settings → 7 hardening → 8 release. Details and acceptance checks are in `DESIGN_SYSTEM.md` §20.

## Before you publish

- Lawyer review of the privacy policy and terms; confirm Ghana Data Protection Commission registration needs.
- Decide **16+ or 18+** (see `DESIGN_SYSTEM.md` §11.5).
- Host the privacy policy and account-deletion pages on a public URL.
- Complete the pre-launch checklist in `DESIGN_SYSTEM.md` §18.
- Re-check Google's official Play policy pages; policies change.
