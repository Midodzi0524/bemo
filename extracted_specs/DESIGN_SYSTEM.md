# <APP_NAME> — Design System, Build Spec & Google Play Launch Guide

> **Status:** v2.3 (feature-complete spec, batch 3 + companion files) · Last verified against Google's published policies: **2026-10-19**
> **Audience:** Any AI coding assistant or human developer building this app.
> **Replace** `<APP_NAME>`, `<PACKAGE_NAME>`, `<DEVELOPER_NAME>`, `<SUPPORT_EMAIL>` everywhere before building.

---

## 0. How to use this document (read first)

**Rules for the AI coder**

1. Treat this file as the single source of truth. If something is not specified, choose the most conventional Android/Material 3 behavior and note the assumption in a `DECISIONS.md` file.
2. Words **MUST / MUST NOT / SHOULD** are binding. "SHOULD" may be overridden only with a written reason.
3. Never hard-code colors, spacing, radii, or font sizes in components. Always read from the tokens in §4.
4. Never store money as floating point. All amounts are **integer pesewas** (1 cedi = 100 pesewas). See §9.
5. Implement features **by ID** from §2.1 (e.g., `ACC-2`, `REC-1`) and build in the phases listed in §20. Put the feature ID in the commit message and in the test name.
5a. Build in the phases listed in §20. Do not start a phase until the previous phase's acceptance criteria pass.
6. Every screen MUST implement all states in §8 (loading, empty, error, offline), not only the happy path.
7. Every interactive element MUST meet the accessibility rules in §12.

**Companion files (all in the same folder)**

| File | Purpose |
|---|---|
| `README.md` | Start-here index and build order |
| `DESIGN_SYSTEM.md` | **This file.** Product, features, design tokens, data model, Play launch guide |
| `WIREFRAMES_AND_FLOWS.md` | Screen wireframes, user-flow diagrams, microcopy rules |
| `PROJECT_STRUCTURE_AND_CI.md` | Folder layout, conventions, environments, `app.config.ts`, `eas.json`, CI/CD, rules tests |
| `firebase/firestore.rules`, `firebase/firestore.indexes.json` | Deployable production security rules and indexes |
| `locales/en-GH.json` | Every in-app string (copy deck) for i18n |
| `LAUNCH_AND_SUPPORT.md` | Store listing text, asset briefs, release/incident/support playbooks |
| `PRIVACY_POLICY.md`, `TERMS_OF_SERVICE.md` | Legal templates (need lawyer review) |

**Document map**

| Part | Sections |
|---|---|
| Product, decisions & **feature catalog** | 1–2 (catalog = §2.1) |
| Tech stack | 3 |
| Visual design | 4–8 |
| Logic, data, sync | 9–10 |
| Security, privacy, a11y, i18n | 11–13 |
| Quality & Android setup | 14–15 |
| Google Play launch | 16–19 |
| Build plan & prompts | 20–21 |

---

## 1. Decisions and conflict resolutions

The original draft contained contradictions. These are the final decisions.

| # | Conflict in v1 | Final decision |
|---|---|---|
| 1 | Nav said "Charts" in one place and "Budget" in another | **Bottom nav = Home, Budget, Transactions, Wishlist.** Charts live on Home (below the bento grid) and on a dedicated *Insights* screen opened from Home ("See all"). |
| 2 | "No shadows" vs "subtle shadow or border" | **No shadows anywhere.** Separation by background contrast + spacing. In dark mode, tiles get a 1px `border`. |
| 3 | Chart lines "Deep Green" / "Coral Red" not in palette | Added tokens `incomeLine` (deep green) and `expenseLine` (coral). See §4.1. |
| 4 | Progress fill "mint green" vs `#5FCB8F` | Same thing. Token `success` = `#5FCB8F`. Fill shifts to `warning` at ≥80%, `danger` at ≥100%. |
| 5 | Touch target 44px | **48×48dp minimum** (Android/Material standard; 44pt is iOS). |
| 6 | `PRD.md` referenced but missing | Emoji set is defined in Appendix A. |
| 7 | White text on `#D85A30` = ≈3.9:1 (fails WCAG AA for small text) | `primary` darkened to `#C84E26` (≈4.6:1). Original `#D85A30` kept as `primaryBrand` for icons, charts and large graphics only. |
| 8 | Muted gray `#8A8F8C` on white = ≈3.3:1 (fails AA) | `textMuted` is now `#5F6663` (≈5.9:1). `#8A8F8C` is `textDisabled` only. |
| 9 | Only a light theme defined | Light **and** dark themes, user-selectable (System / Light / Dark). |

**Product decisions (from the owner)**

- Single user, personal use first. Free. No ads, no in-app purchases, no analytics SDK that tracks users.
- **Manual entry only.** No bank or mobile-money integration. (This keeps the Play "Financial features" review simple.)
- **Sync across devices** with offline-first behavior.
- Currency: Ghana cedi (GHS), displayed `₵1,200.00`.
- Audience: users **16+**. See §11.5 for a legal flag on this.
- Developer account: **Individual**, registered in Ghana.

---

## 2. Product principles and scope

**Principles**

1. **Calm.** Checked daily on a phone. No visual noise, no red unless it matters.
2. **Fast to log.** Adding a transaction MUST take ≤ 3 taps plus typing the amount.
3. **Offline-first.** Ghana's mobile connectivity is intermittent. The app MUST be fully usable offline and sync silently later.
4. **Private by default.** Locked behind biometrics/PIN if the user enables it. No third-party tracking.
5. **Honest numbers.** Every figure on screen MUST be derivable from the formulas in §9.

**Tiers used below:** **L** = Launch (v1.0, required to publish) · **v1.1** = first update after launch · **Later** = backlog. Anything not listed is out of scope.

**Hard out of scope (v1):** bank/mobile-money auto-sync · multi-currency · shared/family budgets · OCR · server-sent push notifications (needs a paid backend) · iOS release (the codebase must allow it later). See the "Deliberately rejected" table (§2.1 group Z) for ideas that are excluded for policy or safety reasons.

### 2.1 Feature catalog

#### A. Accounts and balances

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| ACC-1 | Multiple accounts: Cash, Mobile Money, Bank, Savings, Other (emoji, name, opening balance) | L | Create/edit/archive. Balance follows §9.5. A "Cash" account is auto-created at onboarding. |
| ACC-2 | Transfers between accounts | L | Not income or spend. Changes both balances. Shown once in the list as "A → B". |
| ACC-3 | Reconcile balance | L | User enters the real balance; app posts an `adjustment` transaction for the difference (excluded from income/spend). |
| ACC-4 | Default account + per-account filter | L | Add sheet preselects the default. Transactions filterable by account. |
| ACC-5 | Net balance and "Available" on Home | L | Net = Σ balances. Available = Net − earmarked savings. |
| ACC-6 | Optional transfer/withdrawal fee | v1.1 | Fee field creates a linked expense in category "Fees". |

#### B. Transactions

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| TXN-1 | Create/edit/delete income, expense, saving, transfer | L | Validation per §6.5. Delete is soft with Undo. |
| TXN-2 | Fast add: FAB, recent categories first, built-in amount calculator (`+ − × ÷`) | L | A typical expense saved in ≤ 3 taps after typing the amount. `12+8.50` evaluates to `20.50`. |
| TXN-3 | Quick-add templates ("Bolt ride ₵25") | L | One tap adds the transaction dated today; managed in Settings. |
| TXN-4 | Duplicate, bulk select (delete, change category, change account) | L | Long-press enters select mode; bulk delete has one Undo. |
| TXN-5 | Search + filters + sort | L | Search note/category/amount; filters per §6.4; results update as you type (debounced 200ms). |
| TXN-6 | Duplicate warning | L | Same amount + category + account within 2 minutes shows a confirm prompt. |
| TXN-7 | Future-dated ("planned") transactions | L | Excluded from balances and totals until their date; shown in a "Planned" section. |
| TXN-8 | Pending status | L | `pending` transactions (created by confirm-first recurring rules) are excluded from all totals and balances until confirmed. |
| TXN-9 | Tags | v1.1 | Multi-tag per transaction; filter by tag. |
| TXN-10 | Split a transaction across categories | v1.1 | Splits must sum exactly to the total; budgets count each part. |
| TXN-11 | Receipt photo (device-only, not synced) | Later | Cloud Storage now needs the paid plan, so attachments stay local. |

#### C. Recurring transactions and bills

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| REC-1 | Recurring rules: daily, weekly, biweekly, monthly, yearly; end date/count; type income/expense/transfer/saving | L | Day 31 clamps to the month's last day. Timezone `Africa/Accra`. |
| REC-2 | Auto-post or "confirm first" mode | L | Auto-post creates a posted transaction. Confirm-first creates a `pending` one under "Needs confirmation". |
| REC-3 | Upcoming bills list (next 30 days) + Home tile | L | Sorted by date; overdue unconfirmed items highlighted. |
| REC-4 | Bill due reminders | L | Local notification the day before and on the due day (§9.7). |
| REC-5 | Skip one occurrence, edit this/all future, pause/resume rule | L | Edits never rewrite past posted transactions. |
| REC-6 | Subscriptions overview (total per month/year) | v1.1 | Lists rules flagged "subscription". |

#### D. Budgeting

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| BUD-1 | Zero-based monthly budget | L | Formulas in §9.2. |
| BUD-2 | Copy last month's budget | L | One tap; shows a preview and asks for confirmation if the month already has allocations. |
| BUD-3 | Move money between categories | L | Moves allocation from A to B without changing the total assigned. |
| BUD-4 | Category management: add, edit, reorder, archive, delete | L | Deleting a category with transactions forces **Reassign to…** (never orphan data). |
| BUD-5 | Budget alerts at 80% and 100% | L | In-app banner/snackbar when a save crosses a threshold; badge on the Budget tab (§9.7). |
| BUD-6 | "Safe to spend today" | L | Formula in §9.5; shown on Home. |
| BUD-7 | Budget templates (50/30/20 helper) | v1.1 | Suggests allocations from income; user edits freely. |
| BUD-8 | Rollover per category (opt-in) | v1.1 | Leftover or overspend carries into next month's allocation. |
| BUD-9 | Category groups (Needs / Wants / Savings) | v1.1 | Optional field; Insights shows group split. |
| BUD-10 | Expected vs actual income | v1.1 | Planned income line; variance shown. |
| BUD-11 | Sinking funds (annual bills with target date) | v1.1 | Monthly set-aside computed to target date. |

#### E. Wishlist and goals

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| WSH-1 | Wishlist CRUD with importance, urgency, drag reorder | L | Non-gesture reorder alternative (§12). |
| WSH-2 | Contributions as `saving` transactions | L | Progress = Σ contributions / cost. |
| WSH-3 | "Reach by" projection | L | Based on the last 3 months' average saving to that item; shows "No estimate yet" with < 2 contributions. |
| WSH-4 | Mark purchased | L | Optionally records the actual price as an expense from a chosen account and releases the earmark. |
| WSH-5 | Price history and notes | v1.1 | Log of price updates. |
| WSH-6 | Milestone nudges (25/50/75/100%) | v1.1 | In-app celebration plus optional local notification. |
| WSH-7 | Susu / group-savings tracker | Later | Rotating contribution schedule and payout turn. |

#### F. Debts and loans

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| DBT-1 | Track "I owe" and "Owed to me": person, amount, due date, payments, status | v1.1 | Remaining balance always = amount − Σ payments. |
| DBT-2 | Optional link of a payment to an account | v1.1 | Creates a transfer/expense/income only if the user opts in. |
| DBT-3 | Due-date reminders | v1.1 | Same mechanism as bill reminders. |

#### G. Insights and reports

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| INS-1 | Spend donut by category + income vs expense trend | L | §6.7; tap-through to filtered list. |
| INS-2 | Average daily spend, spend by weekday, top categories, month-over-month change | L | Computed on the client from cached data. |
| INS-3 | Monthly summary screen | v1.1 | Income, spend, saved, biggest category, best/worst budget. |
| INS-4 | Net balance over time | v1.1 | Line from opening balances and transactions. |
| INS-5 | Month-end forecast using recurring rules | v1.1 | Projects balance to the period end. |
| INS-6 | Year in review; PDF report export | Later | |

#### H. Data management

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| DAT-1 | Export CSV and JSON | L | Opens the system share sheet; CSV opens correctly in Excel (UTF-8 with BOM, `₵` preserved). |
| DAT-2 | Delete data / delete account | L | §11.3–11.4. |
| DAT-3 | Sync status + manual "Sync now" | L | Shows last-synced time and pending writes. |
| DAT-4 | Offer to move demo data into a new account | L | On first sign-in from demo mode, ask: Import / Start fresh. |
| DAT-5 | CSV import with column mapping and preview | v1.1 | Skips duplicates; reports row errors; reversible via one "Undo import". |
| DAT-6 | Full backup and restore (JSON) | v1.1 | Restore merges by ID, never silently overwrites newer documents. |

#### I. Security and account

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| SEC-1 | Google + email sign-in, email verification, password reset | L | Unverified email accounts can use the app but see a verify banner; reset flow works end to end. |
| SEC-2 | App lock (biometric or PIN), auto-lock timeout | L | PIN is 4–6 digits stored as a salted hash in SecureStore. After 5 wrong PINs, lock for 30s, then escalating delays. |
| SEC-3 | Hide amounts, block screenshots | L | Toggle in Settings; `••••` replaces amounts. |
| SEC-4 | Re-authentication for sensitive actions | L | Required for delete account, delete all data, export, and changing the PIN. |
| SEC-5 | Sign out clears local cache on this device | L | After sign-out no financial data remains on the device. |
| SEC-6 | Link/unlink sign-in providers | v1.1 | |

#### J. Notifications

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| NOT-1 | Daily "log your spending" reminder | L | User-set time; off by default. |
| NOT-2 | Bill reminders | L | See REC-4. |
| NOT-3 | Budget threshold alerts | L | In-app only (no server). See §9.7. |
| NOT-4 | Per-channel toggles and quiet hours | L | Android notification channels: Reminders, Bills, Summaries. |
| NOT-5 | Weekly summary nudge | v1.1 | Static text; opens the Insights screen. |

#### K. Android and Play integration

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| PLT-1 | App shortcuts (long-press icon): Add expense, Add income, Add transfer | L | Opens the Add sheet with the type preselected (behind the lock if enabled). |
| PLT-2 | Play In-App Review prompt | L | After ≥ 5 sessions and ≥ 10 transactions, at most once per 90 days, never after an error. |
| PLT-3 | Play In-App Updates (flexible) + minimum-version gate | L | Optional update prompt; a hard "Update required" screen if the installed version is below `minSupportedVersion` (§9.7). |
| PLT-4 | Tablet and foldable layouts | L | §5. |
| PLT-5 | Home-screen widget (quick add + safe to spend) | v1.1 | Does not reveal amounts when app lock or "hide amounts" is on. |
| PLT-6 | Deep links (`<scheme>://add?type=expense`) | v1.1 | Validated; ignores malformed input. |
| PLT-7 | Quick Settings tile | Later | |

#### L. Help, support and trust

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| SUP-1 | Offline Help/FAQ | L | At least 12 answers; searchable. |
| SUP-2 | Send feedback | L | Opens an email draft; device/app info is attached only if the user ticks a box. |
| SUP-3 | First-run coach marks | L | At most 3, each dismissible, never repeated. |
| SUP-4 | "What's new" after update | L | Shown once per version. |
| SUP-5 | Terms, Privacy Policy, open-source licenses | L | Reachable from Settings and the Welcome screen. |
| SUP-6 | Error boundary with friendly crash screen | L | "Something went wrong" with Restart; error sent to Crashlytics without PII. |

#### M. Preferences

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| PRF-1 | Theme, haptics, month start day, week start, default account, default category, compact numbers | L | Persist per user and sync. |
| PRF-2 | Currency code stored in settings (fixed to GHS in v1) | L | Formatter reads `currencyCode` so another currency can be added later without a migration. |

#### N. Smart entry and automation (all on-device; no AI or network calls)

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| SMT-1 | Smart text entry: type `bolt 25 transport yesterday momo` and the fields are prefilled | v1.1 | Parser per §9.11. Always shows a confirm screen; never saves silently. |
| SMT-2 | Learned category/account suggestions | L | After the same note text was saved twice with the same category, suggest it first (one-tap accept). Rules editable in Settings. |
| SMT-3 | Note autocomplete from recent notes | L | Top 5 matches after 2 characters; ignores deleted transactions. |
| SMT-4 | Unusual-amount confirmation | L | If amount ≥ 10× the median of the last 20 expenses in that category (≥ 5 samples), ask "Is ₵X correct?" once. |
| SMT-5 | Percent-of-income rules (e.g., 10% giving, 20% savings) | v1.1 | When income is posted, create the matching planned expense/saving using `floor(income × basisPoints / 10000)`. |
| SMT-6 | Round-up jar | v1.1 | Optional: each expense rounds up to the nearest ₵1/₵5/₵10; the difference accumulates in a jar; one tap moves the jar into a goal as a single `saving` transaction. |
| SMT-7 | Voice entry | Later | Uses the keyboard's dictation; any cloud speech service must be disclosed in the privacy policy first. |

#### O. Net worth, assets and financial health

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| FIN-1 | Savings rate for the month | L | `(income − spent) / income`; shows "—" when income is 0. Explained in a help sheet. |
| PAC-1 | Pace indicator on budget bars | L | A thin tick on each progress bar marks how much of the period has elapsed; label "Ahead of pace" when spend % exceeds elapsed % by more than 15 points. |
| FIN-2 | Emergency-fund coverage (months) | v1.1 | Savings-kind account balances ÷ average monthly spend of the last 3 full months. |
| NW-1 | Manual assets (land, vehicle, investments, deposits) | v1.1 | Name, emoji, value, "value updated" date; stale-value hint after 90 days. |
| NW-2 | Net worth = accounts + assets − money you owe (+ money owed to you) | v1.1 | Formula §9.10; monthly snapshot stored for the trend chart. |
| NW-3 | Investment holdings (T-bills, mutual funds, stocks): units, cost, current value, maturity date with reminder | Later | Values are manual; no market data. |
| FIN-3 | Explainable financial-health score (0–100) | Later | Every point of the score is traceable on screen. |

#### P. Planning and calendar

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| PLN-6 | **Goals** (wishlist items with `kind: 'goal'`: emergency fund, school fees, rent deposit) | L | Target date + required monthly amount (§9.10). Appear in their own section on the Wishlist screen. |
| CAL-1 | Calendar view: dots for transactions, bills and planned items; tap a day for its list | v1.1 | Month swipe; weekday labels follow the week-start setting. |
| PLN-1 | Weekly pace view (monthly budget ÷ weeks) | v1.1 | Optional; never replaces the monthly budget. |
| PLN-2 | Month-close ritual | v1.1 | Review summary → confirm rollover (BUD-8) → mark month **closed**. Editing a closed month asks "Reopen this month?" first. |
| PLN-3 | Irregular-income mode | v1.1 | Suggests budgeting from the lowest income of the last 6 closed months; the user can override. |
| PLN-4 | Payslip template | v1.1 | Enter gross pay + user-defined deductions (percent or fixed); app records net income and deduction expenses. **No tax or pension rates are built in**, so nothing can go stale or be wrong. |
| PLN-5 | "Can I afford it?" check on a wishlist item | v1.1 | Shows the effect on Safe to spend and the projected date; advisory only. |

#### Q. Reliability, recovery and transparency

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| REL-1 | Recently deleted (30 days) with Restore | L | Lists soft-deleted transactions, wishlist items, accounts and categories; restore recreates links; auto-purge after 30 days. |
| REL-2 | Sync diagnostics | L | Lists pending/failed writes with the reason (e.g., permission, offline) and a Retry button. |
| REL-3 | Data integrity check | v1.1 | Finds orphaned references and recomputes balances; shows a repair preview before changing anything. |
| REL-4 | Per-transaction change history (last 5 edits) | Later | |
| REL-5 | Remote feature flags / kill switch | L | `config/app.flags` can disable a feature (e.g., recurring auto-post) without a release; unknown flags are ignored; fails open when offline. |
| REL-6 | Safe schema migrations | L | The app exports a local JSON snapshot before any migration and aborts cleanly if migration fails. |

#### R. Privacy and trust

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| PRV-1 | Privacy center | L | Plain-language list of what is stored and where; toggle for crash reports (`setCrashlyticsCollectionEnabled`); links to export and delete. |
| PRV-2 | Quick-hide eye on Home | L | One tap masks every amount (SEC-3 behavior) until toggled back; resets on cold start when app lock is on. |
| PRV-3 | Consent records | L | Store `ageConfirmedAt` and `policyVersionAccepted`; re-prompt when the policy version changes. |

#### S. Insights extensions

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| INS-7 | Budget vs actual bars per category | v1.1 | |
| INS-8 | Custom date range and compare-two-periods | v1.1 | Ranges up to 24 months. |
| INS-9 | Insights feed: plain-language cards ("Food is 22% above your 3-month average") | v1.1 | Rule-based, on-device; only when the difference is ≥ 20% and ≥ ₵50; max 3 cards per day; each dismissible. |
| INS-10 | Category drill-down trend | v1.1 | Tap a category → 12-month bars + its transactions. |
| INS-11 | Saved filter views ("Transport this year") | v1.1 | Stored in `savedViews`. |

#### T. Engagement (opt-in, calm, never guilt-based)

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| ENG-1 | Share the app (Play Store link via share sheet) | L | |
| ENG-2 | Logging streak (opt-in) | v1.1 | Missing a day never shows a negative message; "freeze" days allowed. |
| ENG-3 | Gentle milestone celebrations (first balanced budget, first goal funded) | v1.1 | Respect reduce-motion; no sounds by default. |
| ENG-4 | Challenges: no-spend day/week, 52-week saving | Later | |

#### U. Localization, platform extras and developer tooling

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| LOC-1 | All strings externalized; pseudo-locale test passes | L | No hard-coded UI text; a pseudo-locale build (`[!! Ñàmé !!]`) shows no untranslated strings and no clipped layouts. |
| LOC-2 | Languages: Twi, Ga, Ewe, Hausa, French | Later | Professionally reviewed translations only. |
| LOC-3 | Per-app language setting (Android 13+ `localeConfig`) | v1.1 | |
| PLT-8 | Android Auto Backup rules | L | Exclude the local database and caches (data returns via sync); never back up secrets. |
| PLT-9 | Receive a CSV via the share sheet to start an import | Later | |
| MCU-1 | Foreign-currency accounts (e.g., USD savings) with a manual rate | Later | Totals converted at the user-entered rate, clearly labeled. |
| DEV-1 | Debug menu (**development builds only**) | L | Seed 100 / 1,000 / 10,000 transactions, simulate offline, clear cache, force-run the recurring engine. Must not exist in release builds. |

#### V. Ghana-focused features

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| GH-1 | Mobile-money fee tiers (user-defined) | v1.1 | Per account the user enters their provider's fee schedule (min, max, fee). When a withdrawal or send is entered, the app **suggests** the fee and adds it as a separate expense in "Fees" with one tap. **No rates are built in** (they change), so nothing can be wrong or out of date. |
| GH-2 | Events and contributions (funerals, weddings, naming ceremonies, outings, church harvest) | v1.1 | An event has an optional budget and date; transactions can be tagged `gave` or `received`; the event page shows totals given, received and net. |
| GH-3 | Cash counter | v1.1 | Enter quantity per note/coin (editable denomination list, defaults ₵1, 2, 5, 10, 20, 50, 100, 200); shows the total; one tap uses it to reconcile the Cash account (ACC-3). |
| GH-4 | School-fees planner | v1.1 | A goal template with per-term amounts and due dates; shows the monthly set-aside needed; reminder before each term. |
| GH-5 | Prepaid electricity and fuel logs | Later | Units/litres, amount, odometer; shows cost per unit and per km. |
| GH-6 | Investment maturity reminders (T-bills, fixed deposits) | Later | Part of NW-3; reminder 7 days before maturity. |
| GH-7 | Susu / group savings | Later | See WSH-7. |
| GH-8 | Local starter presets | L | Starter categories include "Family support" and "Susu / contributions" (Appendix A), plus quick-add templates such as "Trotro fare" the user can edit. |

#### W. Convenience and tools

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| CNV-1 | Bill-reminder notification actions ("Mark paid", "Skip") | v1.1 | Actions open the confirm sheet (behind the lock if enabled); no data is written from the notification without the user confirming in the app. |
| CNV-2 | **PIN recovery** ("Forgot PIN?") | L | Explains, then signs out and clears the local PIN and cache; after signing in again the user may set a new PIN. Cloud data is untouched. |
| CNV-3 | Quick-add widget and Quick Settings tile | Later | See PLT-5, PLT-7. |
| CNV-4 | Monthly statement export as PDF | Later | Same content as INS-3. |
| TLS-1 | Loan payoff calculator | v1.1 | Inputs: amount, yearly rate, months. Output: monthly payment and total interest (formula §9.12). Labeled "estimate". |
| TLS-2 | Savings growth calculator | v1.1 | Inputs: monthly saving, yearly rate (user-entered), months. Labeled "estimate". |
| TLS-3 | Split-the-bill calculator | v1.1 | Splits a total across N people; optional "add my share as an expense". |
| TLS-4 | Currency converter with a manual rate | Later | See MCU-1. |

#### X. Reach and expansion (planned, not in v1)

| ID | Feature | Tier | Notes |
|---|---|---|---|
| RCH-1 | iOS release | Later | Needs an Apple Developer account (paid yearly). Keep Play-specific code (review, updates, shortcuts) behind interfaces in `src/services` so iOS can swap implementations. |
| RCH-2 | Web companion (PWA) | Later | Expo web + Firebase Hosting; responsive layout; same Auth and Firestore rules. |
| RCH-3 | Shared / household budgets | Later | A major redesign: `households/{id}` with members and roles, invitations, new rules, privacy-policy changes. Build only after real demand. |
| AIX-1 | Opt-in AI insights ("why was October expensive?") | Later | Default **off**. Needs explicit consent, sends **aggregates only** (never notes or names), runs through a backend you control (paid plan) so no API keys live in the app, requires a Privacy Policy and Data-safety update, and must avoid giving financial advice. |
| PLN-7 | Custom pay cycles (for example, every 14 days) | Later | Generalizes `monthStartDay`. |

#### Y. Core improvements

| ID | Feature | Tier | Acceptance criteria |
|---|---|---|---|
| PRF-3 | **Feature toggles with presets** (progressive disclosure) | L | Settings → Features. Presets: **Simple** (accounts, budget, transactions, wishlist), **Standard** (adds goals, recurring, extra insights), **Everything**. Advanced modules (goals, debts, assets, events, tools, smart entry, streaks, calendar) are off in Simple. Turning a module off hides its UI and **never deletes data**. |
| TXN-12 | Payee field and spend by payee | v1.1 | Optional `payee` with autocomplete; Insights can group by payee. |
| SRC-1 | Global search | v1.1 | One search across transactions, accounts, categories, goals and bills. |

#### Z. Deliberately rejected (do not build)

| Idea | Why it is excluded |
|---|---|
| Reading SMS or notifications to auto-record MoMo/bank transactions | Google Play restricts SMS and call-log permissions to a few core use cases; a budgeting app generally does not qualify, so the app would likely be rejected or removed. It is also a serious privacy risk. Use manual entry, CSV import and smart text entry instead. |
| Accessibility-service or screen-scraping automation | Play prohibits using accessibility services for non-accessibility purposes. |
| Logging in to banks or mobile-money apps on the user's behalf | Security and legal risk; contradicts "manual entry only". |
| Storing card numbers, CVVs, PINs or passwords for other services | Never. The app must not hold payment credentials. |
| Contacts access (for debts or events) | Unneeded permission; users type names. |
| Ads, analytics SDKs, data sharing or selling | Contradicts the product promise and the Data-safety answers. |
| Credit offers, loans or lending referrals | Triggers Play's personal-loan policies and changes the app's risk profile. |

### 2.2 Release scoping (read before planning the build)

The L tier is large. To ship safely, treat it in two layers:

**Never defer (ship blockers):** authentication and sync, offline use, delete account and delete data, privacy policy and consent (PRV-3), app lock, accessibility, `config/app` update gate, REL-1, REL-2, REL-6, PLT-8, Firestore rules, and every Play requirement in §17.

**Safe to move from L to v1.1 if the schedule slips** (in this order): TXN-3, TXN-4 bulk actions, SMT-2/SMT-3, PLT-2 (review prompt), BUD-3, NOT-4 quiet hours, SUP-3 coach marks, ACC-3 reconcile. Moving an item is a one-line change in this table; update §20 accordingly.

**Minimum viable launch** (the smallest version still worth publishing): ACC-1/2/4/5, TXN-1/2/5/6/7/8, BUD-1/2/4/5/6, REC-1 to REC-5, WSH-1 to WSH-4, PLN-6, INS-1/2, FIN-1, PAC-1, DAT-1 to DAT-4, SEC-1 to SEC-5, PRV-1 to PRV-3, NOT-1/2, REL-1/2/5/6, SUP-1/5/6, PLT-1/3/4/8, LOC-1, CNV-2, PRF-3, GH-8.

---

## 3. Technical stack (decided)

Chosen for: AI-coder familiarity, one codebase, strong offline sync, zero running cost at personal scale.

| Concern | Choice | Notes |
|---|---|---|
| Framework | **React Native + Expo (latest stable SDK), TypeScript strict** | Use **development builds** (not Expo Go), because native Firebase is required. |
| Navigation | **Expo Router** (file-based) | Tabs + stack + modal sheets. |
| Backend | **Firebase** (Auth + Firestore) via **`@react-native-firebase/*`** | Native SDK gives real offline persistence. The Firebase *JS* SDK does **not** persist Firestore offline in React Native. Stay on the free **Spark** plan. |
| Auth | Google Sign-In (Android Credential Manager) + email/password fallback | |
| App Check | **Play Integrity** provider | Blocks abuse of your Firebase project. |
| Crash reporting | **Firebase Crashlytics** only | No Analytics, no Ads SDK, no Advertising ID. |
| State | **Zustand** (UI state) + Firestore realtime listeners (data) | No Redux. |
| Validation | **Zod** (schemas shared by forms and Firestore converters) | |
| Forms | **react-hook-form** | |
| Lists | **@shopify/flash-list** | |
| Sheets | **@gorhom/bottom-sheet** | |
| Drag reorder | **react-native-reorderable-list** | Wishlist ordering. |
| Charts | **react-native-gifted-charts** (donut + line) | Alternative: Victory Native XL. |
| Animation | **react-native-reanimated** + **react-native-gesture-handler** | |
| Dates | **date-fns** (timezone `Africa/Accra`, UTC+0, no DST) | |
| Secure storage | **expo-secure-store** (Android Keystore-backed) | Tokens/PIN hash only. |
| Biometrics | **expo-local-authentication** | |
| Haptics / notifications | **expo-haptics**, **expo-notifications** | Local notifications only. |
| App shortcuts | **expo-quick-actions** | PLT-1 |
| In-app review / updates | **expo-store-review** (uses Play In-App Review on Android), **sp-react-native-in-app-updates** | PLT-2, PLT-3 |
| Files / CSV | **expo-file-system**, **expo-sharing**, **expo-document-picker**, **papaparse** | Export, import, backup. |
| Widget (v1.1) | **react-native-android-widget** | PLT-5 |
| Calendar (v1.1) | **react-native-calendars** | CAL-1 |
| Build/release | **EAS Build + EAS Submit**, GitHub Actions CI | EAS manages the upload key. |
| Testing | **Jest + React Native Testing Library**, **Maestro** (E2E), **Firebase Emulator Suite** | |
| Lint/format | ESLint (`eslint-config-expo`), Prettier, `tsc --noEmit` in CI | |

**Free-tier note:** Cloud Functions require the paid Blaze plan, so this design uses **none**. Account deletion is done client-side (§11.4).

---

## 4. Design tokens

All tokens live in `src/theme/tokens.ts` and are exposed through a `useTheme()` hook. Components receive no raw hex values.

### 4.1 Color tokens

**Light theme**

| Token | Hex | Use |
|---|---|---|
| `bg` | `#F7FAF8` | Page background |
| `surface` | `#FFFFFF` | Tiles, cards, sheets |
| `surfaceAlt` | `#EEF3F0` | Inputs, segmented control, skeleton |
| `border` | `#E4E9E6` | Dividers, progress track |
| `text` | `#2D3436` | Primary text |
| `textMuted` | `#5F6663` | Secondary text (≈5.9:1) |
| `textDisabled` | `#8A8F8C` | Disabled/placeholder only |
| `primary` | `#C84E26` | Hero tile fill, primary buttons, active nav (white text ≈4.6:1) |
| `primaryBrand` | `#D85A30` | Icons, expense chart line, large graphics (≥3:1 non-text) |
| `onPrimary` | `#FFFFFF` | Text/icons on `primary` |
| `primaryTint` | `#FAECE7` | Companion tile, badges |
| `onPrimaryTint` | `#993C1D` | Text on `primaryTint` (≈6:1) |
| `warning` | `#F2C94C` | Fill only (progress ≥80%, dots) |
| `onWarning` | `#5C4A00` | Text on warning fill/tint |
| `warningTint` | `#FDF3D3` | Warning banner background |
| `danger` | `#EB5757` | Fill only (progress ≥100%, dots) |
| `dangerText` | `#C62828` | Danger text (≈5.6:1) |
| `dangerTint` | `#FDE8E8` | Error banner background |
| `success` | `#5FCB8F` | Progress fill, positive icons (fill only) |
| `successText` | `#1F7A4D` | Positive text |
| `successTint` | `#E3F6EC` | Success banner background |
| `incomeLine` | `#1F8A5B` | Income trend line |
| `expenseLine` | `#D85A30` | Expense trend line |

**Dark theme**

| Token | Hex |
|---|---|
| `bg` | `#121413` |
| `surface` | `#1C1F1D` |
| `surfaceAlt` | `#252927` |
| `border` | `#333836` |
| `text` | `#ECEFED` |
| `textMuted` | `#A3A9A6` |
| `textDisabled` | `#6B716E` |
| `primary` | `#C84E26` (hero/buttons keep white text) |
| `primaryBrand` | `#FF8F6B` (icons, links, active nav, expense line) |
| `onPrimary` | `#FFFFFF` |
| `primaryTint` | `#3B221A` |
| `onPrimaryTint` | `#F4A58A` |
| `warning` / `onWarning` / `warningTint` | `#F2C94C` / `#F2C94C` / `#33290E` |
| `danger` / `dangerText` / `dangerTint` | `#FF7B7B` / `#FF9A9A` / `#3A1D1D` |
| `success` / `successText` / `successTint` | `#5FCB8F` / `#7EDDA8` / `#16301F` |
| `incomeLine` / `expenseLine` | `#5FCB8F` / `#FF8F6B` |

**Usage rules**

- Coral = personality: hero tile, primary buttons, active nav icon, FAB.
- Green = positive only: progress filling, income line. Never decorative.
- Yellow = approaching limit. Red = over budget or destructive.
- **Never use `warning`, `danger`, or `success` fills as text color** on white/off-white; use the `*Text` tokens.
- **Never convey status by color alone.** Pair with an icon or label (e.g., "⚠ 85% used", "Over by ₵40.00").
- Charts MUST differ by more than hue: income line solid, expense line solid with distinct markers; donut slices carry a legend with labels and percentages.

**Category tints (icon badges)**

| Category | Light bg / icon | Dark bg / icon |
|---|---|---|
| Rent / Housing | `#FAECE7` / `#993C1D` | `#3B221A` / `#F4A58A` |
| Food & Groceries | `#FAEEDA` / `#854F0B` | `#3A2D14` / `#F2C27A` |
| Transport | `#EEEDFE` / `#3C3489` | `#26244A` / `#B9B5F5` |
| Entertainment | `#FBEAF0` / `#72243E` | `#3D2230` / `#F2A9C2` |
| Savings / Wishlist | `#E1F5EE` / `#085041` | `#14332B` / `#7FD6B5` |
| Utilities / other | `#F1EFE8` / `#444441` | `#2B2C28` / `#C9C8BF` |

New user-created categories rotate through these six in order.

> Contrast ratios above are computed approximately. Add an automated contrast test (e.g., a Jest test using `wcag-contrast`) that asserts every text/background pair used in the app is ≥4.5:1 (≥3:1 for large text and UI graphics).

### 4.2 Typography

- Font: **Inter** (weights 400, 500, 600, 700) via `@expo-google-fonts/inter`; fallback to system Roboto.
- Numbers MUST use tabular figures: `fontVariant: ['tabular-nums']`.
- All sizes in **sp**, scaling with the user's system font size. Layouts MUST reflow at 200% scale without clipping or horizontal scroll.

| Style | Size/Line | Weight | Use |
|---|---|---|---|
| `display` | 32/40 | 700 | Hero amount |
| `headline` | 24/32 | 600 | Screen titles |
| `title` | 18/24 | 600 | Tile titles, list row titles |
| `body` | 16/24 | 400 | Default text |
| `bodySmall` | 14/20 | 400 | Secondary text |
| `label` | 12/16 | 500 | Captions, nav labels (never smaller than 12) |
| `amountLg` | 28/34 | 700 | Tile amounts |
| `amountMd` | 18/24 | 700 | List amounts |

### 4.3 Spacing, radius, elevation

- Spacing scale (4pt grid): `4, 8, 12, 16, 20, 24, 32, 40`. Screen horizontal padding = `16`. Gap between tiles = `12`.
- Radius: `tile 14`, `button 12`, `input 12`, `chip 999`, `badge 999`, `sheet 24 (top corners)`.
- Elevation: **none**. No `shadow*` or `elevation` props anywhere.

### 4.4 Motion

| Token | Value |
|---|---|
| `durFast` | 120ms (press feedback) |
| `durBase` | 200ms (transitions) |
| `durSlow` | 300ms (sheets) |
| `durProgress` | 400ms (progress bars fill) |
| Easing | Material standard `cubic-bezier(0.2, 0, 0, 1)` |

- Respect `AccessibilityInfo.isReduceMotionEnabled()`: when true, replace motion with instant changes or simple fades.
- Haptics: `impactLight` on FAB press and drag pickup; `notificationSuccess` on saved transaction; `notificationWarning` on destructive confirm.

### 4.5 Iconography

- Every category, wishlist item and key stat has an **emoji** inside a circular `IconBadge` (tinted background). No bare text-only rows.
- Users can pick a custom emoji (system emoji picker via a text field restricted to one grapheme).
- Badge sizes: `sm 32`, `md 40`, `lg 56`. Emoji size = 50% of badge.
- Navigation and action icons use **Material Symbols (Rounded)** via `@expo/vector-icons`. 24dp, 2dp stroke feel.

---

## 5. Layout and navigation

- **Bottom navigation** (4 tabs): 🏠 Home · 📊 Budget · 🧾 Transactions · 🎯 Wishlist. Active = `primaryBrand` icon + label + `primaryTint` pill. Height 64dp + bottom inset.
- **FAB** (Add transaction): 56dp circle, `primary` fill, white `+`, bottom-right, 16dp above the nav bar, visible on Home, Budget and Transactions (hidden on Wishlist, which has its own "Add item" action).
- **Edge-to-edge:** The app MUST draw behind system bars and apply insets (`react-native-safe-area-context`). Required for Android 15+/target API 36.
- **Back:** Support Android predictive back. Use Expo Router's default back handling; never override `BackHandler` globally.
- **Mobile-first.** Single column on phones. On widths ≥600dp (tablets/foldables) use a 2-column layout for Home and Insights; keep bottom nav (or switch to a navigation rail ≥840dp).
- **Orientation:** Portrait locked on phones; allow rotation on tablets.

---

## 6. Screens

### 6.1 Welcome and onboarding

1. **Welcome:** logo, one-line value prop, buttons: *Continue with Google*, *Use email*, *Try demo* (local-only sample data, no sign-in; also serves Play reviewers).
2. **Age gate:** "I am 16 or older" confirmation (checkbox + link to Privacy Policy and Terms). Must be accepted to proceed. Store `ageConfirmedAt` and `policyVersionAccepted` (PRV-3).
3. **Starter categories:** pre-selected presets from Appendix A (toggle on/off, rename, change emoji).
4. **First income (optional, skippable):** amount, date, and how you are paid (monthly / weekly / irregular; irregular offers PLN-3 later). Optionally add one **goal** (e.g., emergency fund) to seed PLN-6.
5. **Optional:** enable app lock, enable daily reminder (notification permission requested *here, in context*, never at launch).

Acceptance: a new user reaches a populated Home in ≤ 5 taps (excluding typing).

### 6.2 Home (bento dashboard)

Order top to bottom:

1. **Month switcher** (‹ October 2026 ›) in the top bar, with a 👁 quick-hide toggle (PRV-2) and a small sync-status dot (DAT-3).
2. **Row 1:** `HeroTile` ("Unassigned", ~2/3 width, `primary` fill, white text, `display` amount) + `CompanionTile` (~1/3, `primaryTint`, "Saved" with 💰).
3. **Row 2:** two equal plain tiles — **Income** and **Spent** (`surface`, amount + small delta vs last month).
4. **Safe-to-spend tile (BUD-6):** "You can spend about ₵X today" with days left in the period.
5. **Accounts tile (ACC-5):** Net balance, "Available" below it; tap opens Accounts (§6.10).
6. **Upcoming bills tile (REC-3):** next 3 bills with due dates and a "Needs confirmation (n)" chip when pending items exist.
7. **Top categories tile:** horizontal scroll of `IconBadge` + amount chips (top 5 by spend).
8. **Wishlist progress tile:** top-priority item: emoji badge, name, "% saved", progress bar.
9. **Charts section:** donut (spend by category) and line chart (income vs expenses, last 6 months). "See all" opens Insights.

Hero tile states: Unassigned `> 0` → neutral label "still needs a job"; `= 0` → "Every cedi has a job ✓"; `< 0` → label "Over-assigned by ₵X", tile turns `dangerTint` with `dangerText` (never leave white-on-red).

### 6.3 Budget

- Header: month, "Income ₵X · Assigned ₵Y · Unassigned ₵Z".
- Category rows: `IconBadge`, name, **allocated amount (tap to edit inline)**, spent-so-far, progress bar, remaining text ("₵120.00 left" / "Over by ₵40.00").
- Row actions: swipe to edit/archive; long-press to reorder.
- "Add category" row at the bottom.
- Copy-from-last-month action in the overflow menu.

### 6.4 Transactions

- Grouped by day (sticky date headers, "Today", "Yesterday", then `EEE, d MMM`).
- Search bar + filter chips (type, category, date range, amount range).
- Row: `IconBadge`, note/category, time, amount (income in `successText` with "+", expense in `text`, saving in teal).
- Swipe left = delete (with **Undo snackbar**, 5s); swipe right = edit.
- Pull to refresh = force sync status check (data is already live).

### 6.5 Add / Edit transaction (bottom sheet, full-height on small screens)

Fields in order: **Type** segmented (Expense · Income · Saving · Transfer) → **Amount** (large, numeric keypad with calculator keys, autofocus) → **Category** (horizontal `IconBadge` picker, recent first; for Saving pick a **Wishlist item**; hidden for Transfer) → **Account** (default preselected; Transfer shows From → To) → **Date** (default today; future date = "Planned") → **Note** (optional) → **Repeat** (None / Daily / Weekly / Monthly / Yearly / Custom, REC-1) → *More* (v1.1: tags, split, fee). A row of **Templates** (TXN-3) sits above the form. Primary button "Save". Validation: amount > 0, ≤ 2 decimal places, ≤ ₵999,999,999.99; category required for expense; From ≠ To for transfer; duplicate warning per TXN-6.

### 6.6 Wishlist

- Card list with **visible drag handles** for priority order.
- Card: emoji badge, name, cost, saved/cost, progress bar, **importance** (1–5 dots) and **urgency** (Low/Med/High chip).
- Tap → detail: contribution history, "Add money" (creates a `saving` transaction), edit, mark purchased (moves to "Done" section with a confetti-free subtle check animation).
- Add-item sheet: emoji, name, cost, importance, urgency, optional target date, optional link.

### 6.7 Insights

Donut by category (tap slice → filtered transactions) · income vs expense line (3/6/12 months) · average daily spend · spend by weekday · top 5 categories · month-over-month change (INS-1, INS-2). v1.1 adds monthly summary, net balance over time, month-end forecast (INS-3 to INS-5). Each chart has a text summary for screen readers.

### 6.8 Settings

Sections: **Account** (email, verify email, change password, sign out, sync status + Sync now, delete account) · **Security** (app lock, change PIN, lock timeout, hide amounts, block screenshots) · **Privacy center** (what is stored, crash-report toggle, consent history, export, delete) · **Budget & money** (month start day 1–28, week start, default account, default category, compact numbers) · **Manage** (Accounts, Categories, Recurring & bills, Quick-add templates, Learned rules, Goals; v1.1: Tags, Debts, Assets & net worth, Income rules, Payslip templates, Saved views) · **Features** (turn advanced modules on or off, choose Simple / Standard / Everything) · **Appearance & language** (System/Light/Dark, haptics, language; more languages later) · **Notifications** (channel toggles, daily reminder time, quiet hours) · **Data** (export CSV/JSON, Recently deleted, Sync diagnostics, delete all data; v1.1: import, backup/restore, data integrity check, month close) · **Help & support** (Help/FAQ, Send feedback, What's new, Rate the app, Share the app) · **About** (version, Privacy Policy, Terms, open-source licenses, contact).

### 6.9 Lock screen

Shown on cold start and after the timeout when app lock is on. Biometric prompt auto-launches; fallback to device credential or app PIN. Content hidden in the recents/app-switcher when lock is on.

### 6.10 Accounts (ACC-1 to ACC-5)

List of accounts with `IconBadge`, name, balance; header shows Net and Available. Tap an account → its transactions + "Reconcile" and "Transfer" actions. "Add account" sheet: type (preset emojis: 💵 Cash, 📲 Mobile Money, 🏦 Bank, 💰 Savings), name, opening balance, include-in-net toggle. Archived accounts are hidden but keep history.

### 6.11 Recurring and bills (REC-1 to REC-5)

Tabs: **Upcoming** (next 30 days, grouped by week) · **Rules** (all recurring rules with next date, pause toggle) · **Needs confirmation** (pending items with *Confirm*, *Edit*, *Skip*). Rule editor reuses the Add sheet with the Repeat section expanded: frequency, interval, start date, end (never / on date / after N), mode (Auto-post / Confirm first), reminder (None / 1 day before / On the day).

### 6.12 Categories (BUD-4) and Quick-add templates (TXN-3)

Categories: reorderable list; archive; delete → **Reassign to…** dialog if any transactions exist. Templates: list with name, amount, category, account; tap to edit; reorder; max 12 shown on the Add sheet.

### 6.13 Help, feedback and What's new (SUP-1 to SUP-4)

Offline FAQ (searchable accordion). Feedback opens a pre-filled email. "What's new" bottom sheet after each update. Coach marks appear on first use of Home, Budget and Add sheet only.

### 6.14 Update required (PLT-3)

Full-screen, non-dismissible, shown only when installed version < `minSupportedVersion`: explains why, one button "Update on Google Play". Never shown for optional updates (use the in-app flexible update prompt).

### 6.15 Recently deleted and Sync diagnostics (REL-1, REL-2)

*Recently deleted:* grouped by type, each row shows the deletion date and "Purges in N days"; actions Restore / Delete now (confirm). *Sync diagnostics:* header with last sync time and online state; list of pending writes (type, summary, age) and failures with plain-language reasons and Retry; a "Copy diagnostic code" button that copies non-personal technical info only.

### 6.16 Privacy center (PRV-1, PRV-3)

Top: "Your data stays yours." Then: what we store (email, your entries, settings), where (Google Firebase, encrypted in transit and at rest), what we do **not** collect (location, contacts, ads, analytics). Controls: crash-report toggle, consent history (age confirmed on, policy version), Export, Delete data, Delete account.

### 6.17 Calendar, Net worth and Month close (v1.1)

*Calendar (CAL-1):* month grid with up to 3 dots per day (income, expense, bill/planned); tapping a day opens a sheet listing that day's items. *Net worth (NW-1 to NW-3):* header with the total and a 12-month line, then Accounts / Assets / Money owed sections, each editable. *Month close (PLN-2):* a 3-step flow (Review → Rollover → Confirm) opened from a banner on the first days of the next month.

### 6.18 Smart entry bar (SMT-1, v1.1)

A text field above the Add sheet's form: "Try: bolt 25 transport yesterday". As the user types, recognized parts appear as removable chips (amount, category, account, date). Nothing is saved until the user presses Save.

### 6.19 Features (PRF-3)

Three preset cards at the top (**Simple**, **Standard**, **Everything**) with a one-line description each, then a list of switches per module (Goals, Debts, Assets & net worth, Events, Tools, Smart entry, Streaks, Calendar). A footnote says: "Turning something off only hides it. Your data is kept." New installs start on **Standard**; the onboarding does not ask about this.

### 6.20 Tools (v1.1: TLS-1 to TLS-3, GH-3)

A simple list: Loan payoff, Savings growth, Split the bill, Cash counter. Each opens a single-screen calculator with large inputs, a result card, and the label "Estimate only. Not financial advice." The Cash counter has a "Use for reconcile" action.

---

## 7. Components

Build these once in `src/components/`; screens compose them.

| Component | Spec |
|---|---|
| `Tile` | `surface`, radius 14, padding 16, no shadow. Variants: `plain`, `hero` (`primary`), `tint` (`primaryTint`). |
| `AmountText` | Takes **integer pesewas**; renders `₵1,200.00`; tabular; props `size`, `tone` (neutral/positive/negative), `sign` (show +/−). |
| `IconBadge` | Circle with tint bg + emoji; sizes sm/md/lg. |
| `ProgressBar` | 8dp height, radius 4, `border` track, animated fill. Fill: `success` <80%, `warning` 80–99%, `danger` ≥100% (clamped visually at 100%; text shows the real value). Optional **pace marker**: a 2dp tick at the percentage of the period elapsed (PAC-1). Includes `accessibilityValue`. |
| `Button` | Variants: `primary` (`primary` bg, white text), `tonal` (`primaryTint`), `outline`, `text`, `destructive`. Height 48, radius 12, min width 64. States: default, pressed (−8% lightness), disabled (`textDisabled`), loading (spinner, width fixed). |
| `FAB` | 56dp, see §5. |
| `TextField` | Height 56, `surfaceAlt`, radius 12, floating label, error text under the field with icon. |
| `AmountInput` | Numeric, shows `₵` prefix, formats on blur, accepts `.` and `,` as decimal input, stores pesewas. |
| `Chip` | Height 32 (hit area 48), radius 999. |
| `SegmentedControl` | 3 or fewer options, `surfaceAlt` track. |
| `BottomSheet` | Radius 24 top, drag handle 4×32, scrim 40% black. |
| `ListRow` | Min height 64, leading `IconBadge`, trailing amount, swipe actions. |
| `Snackbar` | Bottom, above nav; supports Undo; auto-dismiss 5s; announced to screen readers. |
| `Dialog` | For destructive confirmations only; destructive action on the right, labelled with a verb ("Delete"), not "OK". |
| `EmptyState` | Large emoji, one-line title, one-line body, one primary action. |
| `Skeleton` | `surfaceAlt` shimmer (static when reduce-motion). |
| `Banner` | Offline/sync/error banners using the tint tokens. |

---

## 8. Required states (every screen)

| State | Behavior |
|---|---|
| **Loading** | Skeletons in the final layout shape; no spinner-only screens. Data from the local cache appears instantly. |
| **Empty** | `EmptyState` with a clear next action ("Add your first transaction"). |
| **Error** | Human message + retry. Never show raw error codes. Log to Crashlytics (no PII). |
| **Offline** | The app works normally. Show a small, dismissible "Offline — changes will sync" chip in the top bar. Never block actions. |
| **Syncing/Synced** | Subtle indicator in Settings → Account ("Last synced 2 min ago"). Pending writes count shown if >0. |
| **Over-budget** | Banner on the Budget screen and category row turns red with text label. |
| **Locked** | See §6.9. |

**Copy tone:** friendly, plain, sentence case, no jargon, no shaming ("You're ₵40 over on Food" not "Budget exceeded!").

---

## 9. Domain logic and data model

### 9.1 Money

- Store as **integer pesewas** (`amountMinor`). `₵1,200.00` ⇒ `120000`.
- Display formatter (do not use `Intl` currency style, which prints `GH₵`):

```ts
export function formatCedi(minor: number, opts?: { sign?: boolean }): string {
  const abs = Math.abs(minor);
  const cedis = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const pesewas = (abs % 100).toString().padStart(2, '0');
  const body = `₵${cedis}.${pesewas}`;
  if (minor < 0) return `\u2212${body}`;           // proper minus sign
  return opts?.sign && minor > 0 ? `+${body}` : body;
}
```

- Parsing user input: strip `₵`, spaces, and commas; accept one decimal separator; round half-up to 2 decimals; reject NaN/negative.

### 9.2 Budget method (zero-based)

For a budget month `M`:

```
income(M)     = Σ amountMinor of transactions where type = 'income'  and in M
spent(M)      = Σ amountMinor of transactions where type = 'expense' and in M
saved(M)      = Σ amountMinor of transactions where type = 'saving'  and in M
allocated(M)  = Σ allocatedMinor of budgets for M
UNASSIGNED(M) = income(M) − allocated(M)          ← hero tile
categorySpent = Σ expense in M for that category
categoryPct   = categorySpent / allocatedMinor    (if allocated = 0 and spent > 0 → treat as 100%+)
status        = pct < 0.8 → 'ok' | 0.8 ≤ pct < 1 → 'warning' | pct ≥ 1 → 'over'
wishlistPct   = Σ saving transactions for item / costMinor   (clamp display to 100%)
```

- A **budget month** runs from `monthStartDay` of one calendar month to the day before `monthStartDay` of the next (default day 1). Store `monthKey` as `YYYY-MM` of the period's start date.
- `Saving` transactions count toward "Saved", **not** "Spent".
- No rollover in v1.

### 9.3 Firestore schema

All data lives under the user: `users/{uid}/...`. Client generates IDs (UUID v4) so offline creates never collide.

```
users/{uid}                         { createdAt, displayName?, settings: { theme, monthStartDay,
                                       weekStart, lockEnabled, lockTimeoutSec, hideAmounts,
                                       reminderEnabled, reminderTime }, schemaVersion: 1 }
users/{uid}/categories/{id}         { name, emoji, tintIndex, order, archived, kind: 'expense'|'saving',
                                       createdAt, updatedAt }
users/{uid}/budgets/{monthKey_catId}{ monthKey, categoryId, allocatedMinor, updatedAt }
users/{uid}/transactions/{id}       { type: 'income'|'expense'|'saving', amountMinor, categoryId?,
                                       wishlistItemId?, note?, occurredAt (Timestamp, UTC),
                                       monthKey, createdAt, updatedAt, deletedAt? }
users/{uid}/wishlist/{id}           { name, emoji, costMinor, importance 1-5, urgency 'low'|'med'|'high',
                                       order, targetDate?, link?, purchasedAt?, createdAt, updatedAt }
```

Rules:

- **Soft delete** transactions (`deletedAt`), hide in queries, hard-purge after 30 days on app start (supports Undo and sync conflicts). **Always write `deletedAt: null` on creation** for every user collection (transactions, accounts, categories, wishlist, recurring, templates) so queries can use `where('deletedAt','==',null)`; Firestore cannot filter on a missing field. The *Recently deleted* screen (REL-1) queries `deletedAt != null`.
- Every document has `updatedAt` (server timestamp on write).
- Queries by `monthKey` (indexed). Compute aggregates **on the client** from the month's documents (cheap at personal scale; avoids paid aggregation reads).
- Validate with Zod on read and write; unknown `schemaVersion` triggers a migration routine.

### 9.4 Security rules (Firestore)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
    match /{document=**} { allow read, write: if false; }
  }
}
```

The **full production rules** (field whitelists, type and size validation, server-timestamp checks, per-collection validators) are in `firebase/firestore.rules`, with indexes in `firebase/firestore.indexes.json` and the required test cases in `PROJECT_STRUCTURE_AND_CI.md` §8. Treat those files as the source of truth; update them together with `src/domain/schemas`. Enable **App Check enforcement** for Firestore and Auth.

For the update gate (PLT-3) add one read-only public document, and keep everything else denied:

```
match /config/app { allow read: if true; allow write: if false; }   // { minSupportedVersion: "1.0.0", latestVersion: "1.0.0" }
```

### 9.5 Additional entities (part of schema version 1)

These extend §9.3. Implement them in the same Zod schemas.

```
users/{uid}/accounts/{id}        { name, emoji, kind: 'cash'|'momo'|'bank'|'savings'|'other', openingBalanceMinor,
                                    includeInNet, order, archived, createdAt, updatedAt }
users/{uid}/recurring/{id}       { type, amountMinor, categoryId?, accountId, toAccountId?, wishlistItemId?, note?,
                                    frequency: 'daily'|'weekly'|'monthly'|'yearly', interval (≥1), startDate, endDate?,
                                    endAfter?, dayOfMonth?, weekday?, mode: 'auto'|'confirm', reminder: 'none'|'1d'|'0d',
                                    isSubscription, paused, lastGeneratedDate?, createdAt, updatedAt }
users/{uid}/templates/{id}       { name, type, amountMinor?, categoryId?, accountId?, order, createdAt, updatedAt }
users/{uid}/tags/{id}            { name }                                   // v1.1
users/{uid}/debts/{id}           { direction: 'owe'|'owed', person, principalMinor, dueDate?, note?, status,
                                    createdAt, updatedAt }                  // v1.1
users/{uid}/debts/{id}/payments/{pid} { amountMinor, paidAt, accountId?, createdAt }   // v1.1
```

Transaction additions: `type` is now `'income'|'expense'|'saving'|'transfer'|'adjustment'`; new fields `accountId`, `toAccountId?` (transfers), `status: 'posted'|'pending'` (default `posted`), `recurringRuleId?`, `tagIds?` (v1.1), `splits?: [{categoryId, amountMinor}]` (v1.1). Category additions: `groupId?: 'needs'|'wants'|'savings'` (v1.1), `rollover: boolean` (v1.1). Settings additions: `currencyCode: 'GHS'`, `defaultAccountId`, `defaultCategoryId`, `compactNumbers`, `haptics`, `quietHours`.

### 9.6 Formulas for the new features

A transaction **counts** only if `deletedAt` is empty, `status = 'posted'`, and `occurredAt ≤ now`.

```
accountBalance(a) = openingBalanceMinor
                  + Σ income(a) − Σ expense(a)
                  + Σ transferIn(a) − Σ transferOut(a)
                  ± Σ adjustment(a)
                  (saving does NOT change a balance: the money stays in its account but is earmarked)
net            = Σ accountBalance(a) for accounts with includeInNet and not archived
earmarked      = Σ saving contributions to wishlist items not yet purchased
AVAILABLE      = net − earmarked
transfer, adjustment: excluded from income(M), spent(M), saved(M) and all category totals

remainingBudget(M)   = Σ over expense categories of max(0, allocated − spent)
upcomingBills(M)     = Σ amount of unposted recurring/pending expenses due between today and period end
daysLeft             = days from today to the period end, inclusive (≥ 1)
SAFE_TO_SPEND_TODAY  = max(0, remainingBudget − upcomingBills) / daysLeft          (rounded down to whole pesewas)

reachByDate(item)    = today + ceil((costMinor − savedMinor) / avgMonthlySaving) months,
                       avgMonthlySaving = Σ contributions to the item in the last 3 full months / 3
                       (show "No estimate yet" if fewer than 2 contributions)
```

Marking a wishlist item purchased releases its earmark. If the user records the actual price, create one `expense` from the chosen account (category "Wishlist purchases"); the item's saving contributions then no longer count as `saved(M)` for future months but stay in history.

### 9.7 Recurring engine, notifications and the update gate (no server)

**Recurring generation (client-side, idempotent)**

1. Run on app start, on foreground, and after sync completes.
2. For each active, unpaused rule compute due dates from `lastGeneratedDate + 1` (or `startDate`) up to today in `Africa/Accra`; cap catch-up at 62 occurrences per run.
3. For each due date create a transaction with the **deterministic ID** `rec_{ruleId}_{yyyyMMdd}` (create only if it does not exist, using a Firestore transaction). Two devices therefore never create duplicates.
4. `mode = auto` → `status: 'posted'`. `mode = confirm` → `status: 'pending'`, shown under "Needs confirmation".
5. Update `lastGeneratedDate`. Clamp day-of-month to the month's length (31 → 28/29/30).
6. Skipping an occurrence writes a soft-deleted document with the same ID so it is never regenerated.

**Local notifications (expo-notifications)**

- Android channels: `reminders` (daily log), `bills`, `summaries`. Each can be toggled in Settings and in system settings. Respect quiet hours.
- Schedule bill reminders for the **next 14 days**; reschedule on app open, on any rule change, and after device reboot (`RECEIVE_BOOT_COMPLETED`). Use **inexact** scheduling; do **not** request `SCHEDULE_EXACT_ALARM`.
- Ask for `POST_NOTIFICATION` permission only after the user enables a reminder. If denied, show an in-app banner "Reminders are off" with a link to system settings.
- Notification text must never include amounts when "Hide amounts" or app lock is on.
- **Budget alerts (NOT-3)** are evaluated when a transaction is saved: if a category crosses 80% or 100%, show a `Snackbar` plus a badge on the Budget tab. There are no background pushes for this (they would need a paid backend).

**Update gate (PLT-3)**

On start, read `config/app` (cache for 12 hours; fail open if offline). Compare the installed version to `minSupportedVersion`; if lower, show §6.14. If `latestVersion` is newer, trigger the Play flexible in-app update.

### 9.8 Edge cases the implementation MUST handle

- Deleting a category or account that has transactions → force **Reassign to…** (or archive instead).
- Editing or deleting a transaction in a past month recomputes that month and all balances; no cached totals are stored.
- Negative account balances are allowed and shown in `dangerText` with a "−" sign.
- A budget month with no income: Unassigned is negative and the hero tile shows the over-assigned state.
- Month-end boundaries use `monthStartDay`; changing it never rewrites past `monthKey` values (store `monthKey` on write; re-key only via an explicit "Recalculate months" action).
- Device clock wrong/changed: use the server timestamp for `createdAt`/`updatedAt`; trust the user's chosen `occurredAt`.
- Very large amounts (≥ ₵1,000,000) switch to compact display in tight tiles (`₵1.2M`) with the full value in the accessibility label.
- Two devices edit the same document: last write wins (§10); show nothing to the user.
- Sign-in on a new device with an existing account never overwrites cloud data with demo data (DAT-4).
- App killed during a multi-document write (e.g., account delete): the operation resumes on the next launch via a persisted `pendingDeletion` flag.

### 9.9 Additional entities (batch 2, part of schema version 1)

```
users/{uid}/assets/{id}              { name, emoji, kind, valueMinor, valueUpdatedAt, maturityDate?, note?, createdAt, updatedAt }   // v1.1
users/{uid}/netWorthSnapshots/{monthKey} { netMinor, accountsMinor, assetsMinor, liabilitiesMinor, createdAt }                   // v1.1
users/{uid}/categoryRules/{id}       { pattern (lowercase note text), categoryId, accountId?, hits, updatedAt }                 // SMT-2
users/{uid}/incomeRules/{id}         { name, basisPoints (0–10000), target: { type: 'category'|'wishlist', id }, active }       // v1.1
users/{uid}/deductionTemplates/{id}  { name, deductions: [{ label, kind: 'percent'|'fixed', value, categoryId? }] }             // v1.1
users/{uid}/months/{monthKey}        { closedAt?, reviewNote? }                                                                  // v1.1
users/{uid}/savedViews/{id}          { name, filters }                                                                           // v1.1
```

Additions to existing documents: wishlist item `kind: 'want' | 'goal'` (goals have `targetDate`, no importance/urgency); transaction `roundUpMinor?` (v1.1); settings `crashReportsEnabled`, `ageConfirmedAt`, `policyVersionAccepted`, `quickHide`; `config/app.flags: { [name]: boolean }`.

### 9.10 Formulas for batch 2

```
savingsRate(M)       = (income(M) − spent(M)) / income(M)               ("—" if income = 0)
elapsedPct           = daysElapsed / daysInPeriod                        (period per monthStartDay)
categoryPace         = categoryPct − elapsedPct  → 'ahead of pace' if > 0.15
goalMonthlyRequired  = ceil((costMinor − savedMinor) / max(1, monthsUntil(targetDate)))
emergencyMonths      = Σ balance of savings-kind accounts / avgMonthlySpend(last 3 full months)
netWorth             = net(accounts) + Σ assets.valueMinor + Σ remaining(owed-to-me) − Σ remaining(I-owe)
incomeRuleAmount     = floor(incomeMinor × basisPoints / 10000)
unusualAmount        = amount ≥ 10 × median(last 20 posted expenses in the category) with ≥ 5 samples
budgetFromLowest     = min(income of the last 6 closed months)           (PLN-3 suggestion only)
insightCard          = category spend ≥ 1.20 × 3-month average AND difference ≥ ₵50   (max 3 per day)
```

Closed months (PLN-2): editing a transaction dated in a closed month first asks "Reopen this month?"; confirming sets `closedAt` to empty and recomputes.

### 9.11 Smart entry parser (SMT-1) — deterministic, on-device

1. Lowercase and tokenize the text on spaces.
2. **Amount:** the first token matching `^(₵|ghs|gh₵)?\d+([.,]\d{1,2})?$` (also `k` suffix: `1.5k` = 150000 pesewas). If none is found, leave the amount empty.
3. **Date:** `today`, `yesterday`, `tomorrow`, a weekday name (the most recent past one), or `d/M` / `d MMM`. Default today.
4. **Account:** a token equal to (or a unique prefix of) an account name or alias (`momo`, `cash`).
5. **Category:** exact or unique-prefix match on category names and learned rules (SMT-2); otherwise leave empty.
6. **Type:** `got`, `received`, `salary`, `+` → income; `to <account>` → transfer; `saved`/`save` → saving; default expense.
7. Remaining tokens become the **note**.
8. Show the result as chips; the user can edit or remove any chip. Never auto-save.

Required test cases: `bolt 25 transport yesterday momo`, `salary 3500 bank`, `1.5k rent`, `25.50`, `₵40 lunch mon`, `saved 100 laptop`, empty string, text with two numbers (first wins, second goes to the note).

### 9.12 Additional entities and formulas (batch 3)

```
users/{uid}/events/{id}   { name, emoji, kind, date?, budgetMinor?, closedAt?, createdAt, updatedAt, deletedAt }   // GH-2
transaction additions     payee?, eventId?, eventRole?: 'gave' | 'received'
account additions         feeTiers?: [{ kind: 'withdraw'|'send'|'receive', minMinor, maxMinor, feeMinor }]          // GH-1
wishlist additions        plan?: { terms: [{ label, dueDate, amountMinor }] }                                       // GH-4
settings additions        preset: 'simple'|'standard'|'everything', featureToggles: { goals, debts, assets, events,
                          tools, smartEntry, streaks, calendar }
```

```
feeSuggestion(account, kind, amountMinor) = feeMinor of the first tier with kind match and minMinor ≤ amountMinor ≤ maxMinor  (none → no suggestion)
eventNet(event)        = Σ amount of 'received' − Σ amount of 'gave'   (only posted, non-deleted transactions)
cashTotal              = Σ denominationMinor × quantity
schoolMonthly(plan)    = Σ remaining term amounts / max(1, months until the last due date)   (round up to whole pesewas)

loanPayment(P, yearlyRate, n)  with r = yearlyRate / 12:
    r = 0 → P / n          otherwise → P × r / (1 − (1 + r)^(−n))        (round up to whole pesewas)
    totalInterest = payment × n − P
savingsFutureValue(PMT, yearlyRate, n)  with r = yearlyRate / 12:
    r = 0 → PMT × n        otherwise → PMT × ((1 + r)^n − 1) / r
```

Calculator outputs are estimates; always show the label "Estimate only. Not financial advice." Feature toggles (PRF-3) affect only visibility: repositories, sync and the recurring engine keep running for hidden modules.

---

## 10. Sync, offline, and conflicts

- Enable Firestore **offline persistence** (default on native SDK; set unlimited cache size or ≥ 100 MB).
- Use **realtime listeners** (`onSnapshot`) for the current month, categories, wishlist. Render from cache first; show server data when it arrives.
- **Optimistic UI:** writes appear instantly; pending state is not shown per-row (only a global pending count).
- **Conflict policy:** last-write-wins **per document**. Transactions are effectively append-only (edits are rare), so conflicts are negligible. Budgets use deterministic IDs (`monthKey_categoryId`) so two devices editing the same allocation resolve to the latest write.
- **First sign-in on a second device:** pull everything, show skeletons, never block on full download.
- **Demo mode** uses an in-memory/local store with the same repository interface; no Firebase calls.
- Wrap data access in a **repository layer** (`src/data/*Repo.ts`) so UI never imports Firebase directly (makes testing and future backend swaps easy).

---

## 11. Security, privacy and compliance

### 11.1 App security (finance-app baseline)

- **App lock:** optional biometric/device-credential lock via `expo-local-authentication`. Lock after the configured timeout (default 30s) when backgrounded. Hide content in the app switcher while locked.
- **Hide amounts** toggle (shows `••••`) and **Block screenshots** toggle (`FLAG_SECURE` through `expo-screen-capture`'s prevent API).
- **Secrets:** tokens and any PIN hash in `expo-secure-store` only. Never in AsyncStorage, logs, or source control.
- **Transport/at rest:** TLS 1.2+ to Firebase; Google encrypts Firestore data at rest. On-device Firestore cache relies on **Android file-based encryption plus the app lock**. *(Honest note: the local cache is not separately encrypted by the app. If client-side encryption of notes/amounts is wanted later, add it as a v1.1 feature.)*
- **Release hardening:** R8/ProGuard on, debuggable off, `allowBackup` disabled (or backup rules excluding app data), cleartext traffic disabled, no logging of amounts or notes, Firebase API key restricted to the app's package name + SHA-256 certificates.
- **Dependencies:** pin versions, run `npm audit` in CI, enable Dependabot.

### 11.2 Privacy by design

Collect only: Google account ID/email (auth), the financial entries the user types, crash logs (no PII). **No** location, contacts, SMS, advertising ID, analytics, or third-party SDKs beyond Firebase Auth/Firestore/Crashlytics/App Check.

### 11.3 User data rights (required features)

- **Export:** Settings → Data → CSV and JSON via the share sheet.
- **Delete data:** deletes all documents under `users/{uid}`.
- **Delete account:** requires recent re-authentication, deletes the Firestore subtree, then the Auth user, then signs out and clears local storage. Show a confirmation dialog stating this is permanent.
- **Web deletion page:** Google Play requires a public URL where users can request deletion when accounts can be created in-app. Host a simple page (see §17 step 7) with instructions and a contact email. Fulfill within 30 days.

### 11.4 Client-side deletion procedure (no Cloud Functions)

1. Re-authenticate. 2. Delete subcollections in batches of ≤ 400 docs (`transactions`, `wishlist`, `budgets`, `categories`). 3. Delete `users/{uid}`. 4. `auth().currentUser.delete()`. 5. Clear SecureStore, local cache (`firestore().clearPersistence()` after terminate), and navigate to Welcome.

### 11.5 Legal flags (verify before public release)

- **Ghana Data Protection Act, 2012 (Act 843).** Check with the Data Protection Commission whether you must **register as a data controller**, even for a free app. Publish the privacy policy (`PRIVACY_POLICY.md` template provided).
- **Age 16+.** To my understanding, Act 843 treats anyone under 18 as a child and requires parental/guardian consent for processing their data in many cases. Confirm with a Ghanaian lawyer or the DPC. **The safest option is to set the audience to 18+.** If you keep 16+, add the age gate (§6.1) and a parental-consent statement in the privacy policy.
- **EU/UK users (GDPR):** if the app is available there, the privacy policy must include lawful basis and rights. The provided template covers this.
- **Google Play:** under the Digital Services Act, declare **non-trader** status if the app is a free personal project; declare trader status if you later monetize.
- This section is guidance, not legal advice.

---

## 12. Accessibility (WCAG 2.2 AA target)

- **Contrast:** text ≥ 4.5:1 (≥ 3:1 for ≥ 24sp or ≥ 18.5sp bold); icons, chart elements and focus indicators ≥ 3:1. Use the tokens in §4.1; they already satisfy this.
- **Touch targets:** ≥ 48×48dp with ≥ 8dp spacing.
- **Screen reader (TalkBack):** every control has `accessibilityLabel`, `accessibilityRole`, and state. Amount reads as "one thousand two hundred cedis" (provide `accessibilityLabel` via a spoken-amount helper). Progress bars expose `accessibilityValue` ("85 percent of budget used"). Charts have a text alternative (table or summary).
- **Font scaling:** support up to 200%; use `allowFontScaling` (default) and flexible layouts; test with Display size and Font size at maximum.
- **Reduce motion**, **bold text** and **dark theme** respected.
- **Focus order** follows visual order; sheets trap focus and return it on close.
- **Errors** are announced and described in text, not color alone.
- **Drag-and-drop** (wishlist) MUST have a non-gesture alternative: "Move up / Move down" actions in each card's menu.
- **Haptics are never the only feedback.**
- Test with TalkBack, Switch Access, and `Accessibility Scanner` before each release.

---

## 13. Localization and formatting

- Language: **English (Ghana)**. All strings in a translation file from day one (`i18n-js` or `i18next`) so Twi/other languages can be added later; no hard-coded UI strings in components.
- Currency: `₵1,200.00` always (symbol first, comma thousands, 2 decimals, proper minus `−₵50.00`).
- Dates: `d MMM yyyy` ("19 Oct 2026"); numeric forms `dd/MM/yyyy`. 24-hour time option follows system.
- Timezone: `Africa/Accra` (UTC+0). Store UTC; render in device timezone.
- Week starts Monday by default (user-changeable).
- Support RTL-safe layout (use `start/end`, not `left/right`) for future languages.

---

## 14. Performance, quality and testing

**Performance budgets** (test on a low-end device: 2–3 GB RAM, Android 10–12; budget Tecno/Infinix/itel phones are common in Ghana)

| Metric | Target |
|---|---|
| Cold start to interactive | ≤ 2.5s (low-end), ≤ 1.5s (mid-range) |
| List scrolling | 60fps (no dropped frames on 500+ transactions) |
| Release AAB size | ≤ 30 MB |
| Memory | ≤ 250 MB steady state |
| Battery | No background work except the optional reminder |

Use Hermes, enable the New Architecture if the chosen Expo SDK defaults to it, lazy-load charts, memoize list rows, and avoid re-rendering the whole month on each snapshot (use selectors).

**Testing**

- Unit: money math, budget formulas, month-key logic, formatter, Zod schemas (≥ 90% coverage on `src/domain`).
- Component: each component in all states, light + dark, font scale 1.0/2.0.
- Integration: repositories against the **Firebase Emulator**.
- E2E (Maestro): onboarding, add transaction offline then reconnect, budget edit, wishlist contribute, delete account.
- Offline test matrix: airplane mode create/edit/delete, then reconnect; two devices editing the same budget.
- Device matrix: Android 8/9 (min), 12, 14, 15, 16; small (360dp), large, tablet, foldable.
- Run Play **Pre-launch report** (automatic when you upload to a testing track) and fix every crash/accessibility finding.

---

## 15. Android project setup

- **Package name:** `<PACKAGE_NAME>` (e.g., `com.yourname.appname`). **Permanent** once published.
- **`compileSdk` / `targetSdk`: 36** (Android 16). `minSdk`: the Expo default (currently 24+) unless a dependency needs more.
- **Permissions (minimum):** `INTERNET`, `ACCESS_NETWORK_STATE`, `USE_BIOMETRIC`, `VIBRATE` (haptics), `RECEIVE_BOOT_COMPLETED` (re-schedule reminders after reboot), `POST_NOTIFICATIONS` (requested at runtime, only after the user opts in to reminders). **Never** request `SCHEDULE_EXACT_ALARM`, location, contacts, SMS or storage permissions. Remove any permission a library adds that is unneeded (use `blockedPermissions` in `app.json`).
- **Edge-to-edge** layouts; **predictive back** compatible.
- **16 KB page size:** all native libraries MUST support 16 KB memory pages (Expo + current RN versions do; verify any extra native module).
- **App icons:** adaptive icon (foreground 432×432px safe-zone-aware inside a 108dp canvas, background solid `#C84E26` or `#FAECE7`), plus a **monochrome** layer for themed icons.
- **Splash:** Android 12+ splash API via `expo-splash-screen` (icon on `bg`, light/dark variants).
- **Versioning:** `version` = semver shown to users (`1.0.0`); `versionCode` auto-incremented by EAS (`appVersionSource: remote`).
- **App shortcuts:** declare three static shortcuts (Add expense, Add income, Add transfer) via `expo-quick-actions`; they must route through the lock screen.
- **EAS profiles:** `development` (dev client, APK), `preview` (internal APK), `production` (AAB, auto-increment).

---

## 16. Google Play: listing assets and content

| Asset | Requirement |
|---|---|
| App name | ≤ 30 characters |
| Short description | ≤ 80 characters |
| Full description | ≤ 4,000 characters; plain, no keyword stuffing, no false claims |
| App icon | 512×512 PNG, 32-bit, ≤ 1 MB (no rounded corners or shadow; Play applies them) |
| Feature graphic | 1024×500 PNG/JPEG |
| Phone screenshots | 2–8; 16:9 or 9:16; each side 320–3840px (provide 6: Home, Budget, Add transaction, Transactions, Wishlist, Dark mode) |
| Tablet screenshots | Recommended (7" and 10") if tablet layout is supported |
| Category | Finance (or Productivity) |
| Contact | Public support email (required); website optional |
| Privacy policy | Public URL (required) |

**Draft listing copy (edit freely)**

- Name: `<APP_NAME> – Budget & Wishlist`
- Short: `Give every cedi a job. Track spending, budget monthly, and save for what you want.`
- Full: Open with the benefit (assign every cedi), then bullets: manual entry you control, monthly zero-based budget, wishlist with progress, charts, works offline and syncs across your devices, app lock, dark mode, free with no ads. State clearly: *"This app does not connect to your bank or mobile money accounts. You enter your own transactions."*

---

## 17. Google Play: step-by-step upload (verified 2026-10-19)

### What Google announced recently (and what it means for you)

1. **Oct 2026 "personal apps" feature.** Google Play now lets users build a custom app with Google AI Studio and run it on their own device **without a developer account**. These apps stay **private and separate from the public store**, available initially in select markets (including the US) for users 18+. This is a *different path* from this project's React Native app and is **not** a way to publish publicly. Check whether it is available in Ghana before relying on it.
2. **Android developer verification.** Takes effect **30 Sep 2026** (first in Brazil, Indonesia, Singapore, Thailand, expanding after). Developers must be verified, and apps must be **registered to a verified developer**. Google says 99% of Play apps registered automatically; **check your Play Console Home page** and register any remaining apps to avoid removal.
3. **Target API 36.** Since **31 Aug 2026**, new apps and updates must target **Android 16 (API 36)**.
4. **12-tester rule.** Personal accounts created after 13 Nov 2023 must run a **closed test with ≥ 12 testers opted in continuously for 14 days** before applying for Production.

### Choose your path

| Path | Account needed | Gate | Best for |
|---|---|---|---|
| **A. AI Studio personal app** | Google account | None | Not applicable to this codebase; mentioned for completeness |
| **B. Internal testing only** *(recommended for "just me")* | Play developer account | **None** (no 12-tester rule; up to 100 testers) | Using the app yourself and with family, installed from Play, auto-updated |
| **C. Public release** | Play developer account | 12 testers × 14 days → production access → review | Anyone can install |

You can do B first and decide on C later. The same account and the same AAB are used.

### Step 1 — Prepare before paying

- A dedicated Google account for development (turn on 2-Step Verification).
- A valid government photo ID (passport, driver's license or national ID), current, legible, matching your legal name. Have proof of address available in case it is requested.
- A phone number you can receive SMS/calls on, and a **physical Android phone** with the **Play Console mobile app** (device verification may be required).
- A debit/credit card that supports **international USD online payments** (the registration fee is **US$25, one-time**; a Ghana-issued Visa/Mastercard or virtual dollar card usually works).
- A public web page for the **privacy policy** and **account deletion** (§17 step 7).
- Finished, tested app + the assets from §16.

### Step 2 — Create the developer account

1. Go to `play.google.com/console/signup`.
2. Choose **Individual**.
3. Enter legal name, address (Ghana), contact email and phone. Contact details may be shown publicly on your listing; use a dedicated support email.
4. Accept the Developer Distribution Agreement and pay **US$25**.
5. Complete **identity verification** (ID upload) and **phone** verification; complete **Android device verification** in the Play Console app if prompted.
6. Wait for approval (can take from hours to several days). Do not create duplicate accounts.

### Step 3 — Create the app in Play Console

*All apps → Create app*: name, default language (English – United Kingdom or Ghana), **App** (not Game), **Free**, accept the declarations (policies and US export laws). Confirm the **package name** you will upload matches `<PACKAGE_NAME>`.

### Step 4 — Build the release bundle

```bash
npm ci
npx expo-doctor
npx tsc --noEmit && npm test
eas build --platform android --profile production      # produces a signed .aab
```

- Let **Play App Signing** manage the app signing key (default). EAS holds the **upload key**; back up its credentials (`eas credentials`).
- Confirm in the build output: `targetSdkVersion 36`, R8 enabled, only the permissions listed in §15.

### Step 5 — Complete the "Set up your app" tasks (Dashboard)

| Task | What to answer for this app |
|---|---|
| **Privacy policy** | Public URL to your hosted policy |
| **App access** | Select "All or some functionality is restricted". Provide a **test Google account** (email + password, 2-Step off) *and* mention the in-app **"Try demo"** button, which needs no login |
| **Ads** | **No**, the app contains no ads |
| **Content rating** | Complete the IARC questionnaire honestly (no violence, no user-generated sharing, finance category, no gambling). Expect a low rating |
| **Target audience and content** | Select **16–17 and 18+** (or **18+** only; see §11.5). Do **not** select any under-16 group; the app must not appeal to children |
| **Data safety** | Use the answers in Appendix B. Declare the account deletion URL |
| **Government apps** | No |
| **Financial features** | Answer the declaration truthfully. This is a manual budgeting tracker with no loans, banking, payments, crypto or trading. Select the option stating it provides **no** such financial features (read the options shown; they change) |
| **Health / News / Other declarations** | Not applicable |
| **Advertising ID** | Declare **No** (do not add the permission) |
| **Store settings** | Category Finance; contact email; website optional |
| **Main store listing** | Upload §16 assets and text |

### Step 6 — Upload to Internal testing (start here)

1. *Test and release → Testing → Internal testing → Create new release*.
2. Upload the `.aab`. Accept Play App Signing if asked.
3. Release name `1.0.0 (1)`; release notes (e.g., "First test build").
4. *Testers* tab: create an email list (your Google accounts; up to 100). Save.
5. Review → **Start rollout to Internal testing**.
6. Copy the **opt-in link**, open it on your phone while signed in with a tester account, accept, then install from the Play Store link.
7. Check the **Pre-launch report** (crashes, accessibility, security). Fix and upload a new build (increment `versionCode`).

**If the app is only for you, you can stop here.** Internal testing updates install automatically through the Play Store.

### Step 7 — Host the privacy policy and deletion page

- Use **GitHub Pages** or **Firebase Hosting** (both free). Publish `PRIVACY_POLICY.md` (rendered as HTML) at a stable URL such as `https://<you>.github.io/<app>/privacy`.
- Add a page `/delete-account` explaining: (a) in-app path *Settings → Account → Delete account*, (b) how to request deletion by emailing `<SUPPORT_EMAIL>` from the registered email, (c) what is deleted and when (within 30 days). Use that URL in the Data safety form.

### Step 8 — Closed testing → Production (only for a public release)

1. *Testing → Closed testing → Create track → Create release*; upload the AAB.
2. Add **at least 12 testers** (a Google Group or an email list works). Each must **accept the opt-in link and stay opted in for 14 continuous days** (counted back from the day you apply). Use friends, family, classmates and colleagues with real Android devices. **Do not buy fake or bot installs; this violates policy and can get the account suspended.** Aim for **15–20 testers** to survive drop-outs.
3. Ask testers to open the app on several days and send feedback. Reply to their feedback in the console.
4. After 14 days, *Dashboard → Apply for production access*. Answer the questionnaire: how you recruited testers, what feedback you received, what you changed, and production readiness. Be specific and truthful.
5. Wait for review (often several days; the full first-launch cycle is commonly about 3 weeks).

### Step 9 — Production release

1. *Production → Create new release*; upload the final AAB (or promote the tested release).
2. Select countries/regions (start with Ghana; add more later).
3. Release notes; **staged rollout** 20% → 50% → 100% over a few days, monitoring crashes.
4. *Publishing overview → Send for review.* First review of a new app can take up to ~7 days or longer.
5. After approval, optionally turn on **managed publishing** to control the go-live moment.

### Step 10 — Common rejection causes (avoid them)

- Privacy policy URL broken, or policy contradicting the Data safety form.
- No in-app **and** web account deletion path.
- Reviewers cannot get past login (provide the test account + demo mode).
- Requesting unnecessary permissions.
- Misleading listing claims ("official bank app", fake reviews, keyword stuffing).
- Crashes or ANRs in the Pre-launch report.
- Target API below 36.

---

## 18. Pre-launch checklist

**Product and design**
- [ ] All screens implement §8 states (loading, empty, error, offline)
- [ ] Light and dark verified on every screen
- [ ] Font scale 200% verified, no clipping
- [ ] Contrast test passes in CI
- [ ] TalkBack pass on onboarding, add transaction, budget, wishlist
- [ ] Wishlist reorder has non-gesture alternative

**Features**
- [ ] Every **L**-tier feature in §2.1 implemented and covered by a test named with its ID
- [ ] Recurring engine verified on two devices (no duplicate transactions, catch-up after 30 days offline)
- [ ] Balances reconcile: Σ account balances = opening balances + transactions (property test)
- [ ] Transfers and adjustments never appear in income, spent or saved
- [ ] Bill reminders fire after a reboot; notification text hides amounts when hiding is on
- [ ] Update gate tested (below-minimum version shows §6.14; offline fails open)
- [ ] CSV export opens correctly in Excel and Google Sheets
- [ ] Smart parser passes every test case in §9.11 (if SMT-1 is shipped)
- [ ] Recently deleted restores links correctly; items purge after 30 days
- [ ] Sync diagnostics shows a forced failure and Retry recovers it
- [ ] Remote flag disables a feature in a release build without redeploying
- [ ] Crash-report toggle off ⇒ no Crashlytics events sent (verify in console)
- [ ] Pseudo-locale build shows no hard-coded strings or clipped layouts
- [ ] Debug menu is absent from the production AAB
- [ ] App is smooth with 10,000 seeded transactions on a low-end device
- [ ] Feature toggles: Simple preset hides advanced modules; toggling off never deletes data
- [ ] Forgot-PIN flow signs out, clears the local PIN and cache, and cloud data remains
- [ ] Every user collection writes `deletedAt: null` on create (verify in the emulator)
- [ ] Rules and indexes from `firebase/` deployed to staging; all rules tests pass
- [ ] `locales/en-GH.json` has no unused or missing keys (`npm run check:strings`)
- [ ] Terms of Service and Privacy Policy published, linked in-app and on the Play listing

**Data and security**
- [ ] Firestore rules deployed and tested in emulator; field validation added
- [ ] App Check (Play Integrity) enforced
- [ ] App lock, hide amounts, screenshot-block work
- [ ] Export (CSV/JSON) works; delete data and delete account work end to end
- [ ] No PII in logs; Crashlytics collects no user content
- [ ] Release is not debuggable; cleartext blocked; Firebase API key restricted

**Play Console**
- [ ] Developer identity + device verification complete
- [ ] Apps registered under verified developer (Console Home page)
- [ ] Target SDK 36 confirmed in the AAB
- [ ] Privacy policy URL live; deletion URL live
- [ ] Data safety, content rating, audience, ads, app access, financial features all complete
- [ ] Listing assets uploaded (icon, feature graphic, 6+ screenshots)
- [ ] Pre-launch report: zero crashes, accessibility issues triaged
- [ ] Internal test installed and used for ≥ 3 days on a real device

---

## 19. Post-launch maintenance

- **Annual (before 31 Aug each year):** raise `targetSdk` to the new required API level (update Expo SDK, retest, ship an update). Existing apps that fall behind become unavailable to new users on newer Android versions.
- **Watch Android vitals** (Play Console): keep crash rate and ANR rate well under Google's "bad behavior" thresholds; fix regressions within one release.
- **Policy watch:** review *Policy → Policy deadlines* quarterly. Known upcoming items include Contacts and Location permission policy changes (deadline 27 Jan 2027; this app uses neither, so no action needed) and native-library 16 KB page-size enforcement (confirm dates in the console).
- **Respond to reviews** within a week.
- **Update cadence:** at least a minor update every 2–3 months; patch security fixes immediately.
- **Backups:** export Firestore periodically (or rely on the in-app export). Keep the EAS credentials and keystore backup in a password manager.
- **Costs:** Play registration is one-time US$25. Firebase Spark (free) is ample for personal use; watch quota (Firestore free tier is roughly 50k reads and 20k writes per day). Upgrade to Blaze only if you exceed it, and set a budget alert.

---

## 20. Build plan for the AI coder

Work in phases. Commit after each. Do not skip acceptance checks.

| Phase | Deliverable | Acceptance |
|---|---|---|
| 0. Setup | Expo + TS strict, Router, ESLint/Prettier, tokens, theme provider (light/dark), i18n, CI | `tsc`, lint and tests pass in CI; theme switch works |
| 1. Domain | `formatCedi`, parse, budget formulas, month keys, Zod schemas | Unit tests ≥ 90% on `src/domain` |
| 2. Data | Repository layer, Firebase config, auth, security rules, emulator tests, demo store | CRUD works offline then syncs; rules tests pass |
| 3. Components | All §7 components + Storybook-style gallery screen | All variants/states render in light/dark and 200% font |
| 4. Screens | Welcome/onboarding, Home, Budget, Add sheet, Transactions | Flows in §6 work; all §8 states present |
| 5. Wishlist + Insights | Wishlist, contributions, charts | Matches §6.6–6.7; charts have text alternatives |
| 6. Security & settings | App lock, hide amounts, export, delete data/account, reminders | §11 and §12 checklist items pass |
| 7. Hardening | Performance pass on a low-end device, a11y audit, Maestro E2E | Meets §14 budgets |
| 8. Release | Icons, splash, listing assets, EAS production build, Play steps | §18 checklist all checked |

**Feature-to-phase map**

| Phase | Feature IDs |
|---|---|
| 1 Domain | BUD-1, ACC-1 to ACC-3 math, §9.6 formulas, REC-1 date engine, TXN-6, TXN-7, TXN-8 |
| 2 Data | All entities in §9.3 and §9.5, SEC-1, SEC-5, DAT-4, PRF-1, PRF-2 |
| 3 Components | UI primitives for every L feature |
| 4 Screens | TXN-1 to TXN-5, ACC-1 to ACC-5, BUD-2 to BUD-4, BUD-6, SUP-3, SUP-6 |
| 5 Wishlist + Insights + Recurring | WSH-1 to WSH-4, INS-1, INS-2, REC-1 to REC-5 |
| 6 Security & settings | SEC-2 to SEC-4, DAT-1 to DAT-3, NOT-1 to NOT-4, BUD-5, SUP-1, SUP-2, SUP-4, SUP-5 |
| 7 Hardening | PLT-1 to PLT-4, performance, accessibility, E2E |
| 8 Release | Play launch (§16–§18) |
| **v1.1** (after launch) | Every row tagged v1.1 in §2.1 |

**Feature-to-phase map (batch 2)**

| Phase | Feature IDs |
|---|---|
| 1 Domain | FIN-1, PAC-1, PLN-6 math, SMT-4, §9.10 formulas, REL-6 migration framework |
| 2 Data | PRV-3, REL-5 (config flags), PLT-8 backup rules, `categoryRules` |
| 4 Screens | SMT-2, SMT-3, PRV-2, ENG-1 |
| 5 Wishlist + Insights + Recurring | PLN-6 goals section, FIN-1 and PAC-1 on Home/Budget |
| 6 Security & settings | PRV-1, REL-1, REL-2, LOC-1 pseudo-locale test |
| 7 Hardening | DEV-1 debug menu (dev builds only), performance runs with 10,000 transactions |
| **v1.1** | SMT-1/5/6, FIN-2, NW-1/2, PLN-1 to PLN-5, CAL-1, REL-3, INS-7 to INS-11, ENG-2/3, LOC-3 |
| **Later** | SMT-7, NW-3, FIN-3, REL-4, ENG-4, LOC-2, PLT-9, MCU-1 |

**Feature-to-phase map (batch 3)**

| Phase | Feature IDs |
|---|---|
| 1 Domain | Calculators and fee/event/cash/school formulas (§9.12) when those features are scheduled |
| 2 Data | PRF-3 settings (`preset`, `featureToggles`), `deletedAt: null` convention, rules + indexes deployed from `firebase/` |
| 4 Screens | CNV-2 forgot-PIN flow, GH-8 presets, §6.19 Features screen |
| 6 Security & settings | PRF-3 wiring (hide/show modules), CNV-2 |
| **v1.1** | GH-1 to GH-4, CNV-1, TLS-1 to TLS-3, TXN-12, SRC-1 |
| **Later** | GH-5 to GH-7, CNV-3, CNV-4, TLS-4, RCH-1 to RCH-3, AIX-1, PLN-7 |

**Master prompt (paste into your AI coding tool)**

> You are building the Android app described in `DESIGN_SYSTEM.md`. Read the whole file first. Follow §0 rules. Work phase by phase from §20, starting with Phase 0. After each phase, list what you built, the acceptance checks you ran and their results, any assumptions (append to `DECISIONS.md`), and ask me before starting the next phase. Never hard-code colors or sizes. Store money as integer pesewas. Do not add libraries not listed in §3 without asking.

---

## 21. Open items for the owner

1. App name and package name (`<APP_NAME>`, `<PACKAGE_NAME>`).
2. Final decision: **16+ or 18+** audience (see §11.5).
3. Whether to register with Ghana's Data Protection Commission.
4. Hosting choice for privacy/deletion pages (GitHub Pages vs Firebase Hosting).
5. Whether you want to go public (Path C) or stay on Internal testing (Path B).
6. Confirm the logo/brand color is fine with the slight `primary` darkening (§1 #7).
7. Confirm the release cut in §2.2 (which L items may slip to v1.1).
8. Decide whether any payslip deductions (PLN-4) should ship as pre-filled presets; the default is none, so no tax or pension rates can go stale.
9. Decide which extra languages (LOC-2) matter most and who will review the translations.
10. Have a lawyer review `TERMS_OF_SERVICE.md` and `PRIVACY_POLICY.md` (liability cap, minors, governing law).
11. Choose the default feature preset for new installs (this spec assumes **Standard**).
12. Decide whether v1.1 should start with the Ghana-focused group (GH-1 to GH-4) or the insights group (INS-3 to INS-11).

---

## Appendix A — Default categories and emoji set

| Category | Emoji | Tint | Kind |
|---|---|---|---|
| Rent / Housing | 🏠 | Soft coral | expense |
| Food & Groceries | 🛒 | Soft amber | expense |
| Dining out | 🍽️ | Soft amber | expense |
| Transport | 🚌 | Soft lavender | expense |
| Fuel | ⛽ | Soft lavender | expense |
| Utilities (ECG, water) | 💡 | Soft gray | expense |
| Airtime & Data | 📱 | Soft gray | expense |
| Health | 💊 | Soft teal | expense |
| Education | 📚 | Soft lavender | expense |
| Entertainment | 🎬 | Soft pink | expense |
| Clothing | 👕 | Soft pink | expense |
| Family & Gifts | 🎁 | Soft coral | expense |
| Church / Giving | 🙏 | Soft gray | expense |
| Family support | 🤝 | Soft coral | expense |
| Susu / contributions | 🪙 | Soft teal | saving |
| Savings | 💰 | Soft teal | saving |
| Emergency fund | 🛟 | Soft teal | saving |

Wishlist item defaults: 🎯 generic, 💻 laptop, 📱 phone, ✈️ travel, 🚗 car, 🏡 property, 🎓 course.
Income sources (labels only): Salary 💼, Business 🧾, Gift 🎁, Other ➕.

## Appendix B — Data safety form answers

| Question | Answer |
|---|---|
| Does the app collect or share user data? | Collects: yes. Shares: **no** (Firebase acts as a service provider/processor) |
| Data types collected | **Personal info:** email address, user IDs · **Financial info:** other financial info (user-entered transactions, budgets) · **App info and performance:** crash logs, diagnostics |
| Purpose | App functionality, account management, (crash logs) analytics-free diagnostics |
| Is data encrypted in transit? | **Yes** |
| Can users request deletion? | **Yes** — in-app (Settings → Account) and web URL |
| Optional or required? | Account data required for sync; app usable in demo mode without it |
| Data processed ephemerally? | No |
| Independent security review | No |
| Families / children | Not designed for children (audience 16+/18+) |

## Appendix C — Sources checked (2026-10-19)

- Google Play Console Help: *App testing requirements for new personal developer accounts* — `support.google.com/googleplay/android-developer/answer/14151465`
- Google Play Console Help: *Target API level requirements for Google Play apps* — `support.google.com/googleplay/android-developer/answer/11926878`
- Android Developers: *Google Play Policies timeline (developer verification, Sept 30, 2026)* — `developer.android.com/distribute/play-policies`
- Google Play Console Help: *Policy deadlines* — `support.google.com/googleplay/android-developer/table/12921780`
- Google blog: *New on Google Play, October 2026* — `blog.google/products-and-platforms/platforms/google-play/google-play-updates-october-2026/`

*Policies change often. Re-check the official pages above before submitting.*
