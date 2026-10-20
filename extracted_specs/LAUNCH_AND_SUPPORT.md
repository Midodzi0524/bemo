# Launch Kit & Support Playbook

Companion to `DESIGN_SYSTEM.md` (§16–§19). Replace `<APP_NAME>`, `<SUPPORT_EMAIL>`, `<DEVELOPER_NAME>`.

---

## 1. Google Play listing copy (ready to paste)

| Field | Limit | Text |
|---|---|---|
| **App name** | 30 | `<APP_NAME> – Budget & Wishlist` |
| **Short description** | 80 | `Give every cedi a job. Budget monthly, track spending, save for what you want.` (78) |
| **Category** | | Finance |
| **Tags** | | Budgeting, Personal finance, Expense tracker |
| **Contact email** | | `<SUPPORT_EMAIL>` |
| **Privacy policy** | | `https://<you>.github.io/<app>/privacy` |

**Full description (≤ 4,000 characters)**

```
Give every cedi a job.

<APP_NAME> is a calm, simple budgeting app built for everyday life. Plan your month, log what you spend in seconds, and save toward the things you want, all in Ghana cedis (₵).

WHAT YOU CAN DO
• Budget by month: see exactly how much of your income is still unassigned
• Log spending fast: add an expense in a few taps, with a built-in calculator
• Track every account: cash, mobile money and bank in one place, with transfers between them
• Never miss a bill: recurring bills with reminders
• Save for goals and wishlist items: see your progress and when you'll reach them
• Understand your money: clear charts for spending by category and income vs spending
• See what's safe to spend today

PRIVATE BY DESIGN
• You enter your own transactions. We never connect to your bank or mobile-money accounts
• Lock the app with your fingerprint or PIN
• Hide amounts with one tap
• Export your data any time, and delete your account whenever you want
• No ads. No tracking. Free.

WORKS WHERE YOU DO
• Works offline and syncs when you reconnect
• Use it on more than one phone with the same account
• Light and dark themes, large-text and screen-reader friendly

<APP_NAME> is a personal budgeting tool and does not provide financial, tax or legal advice.

Questions or feedback? <SUPPORT_EMAIL>
```

**Release notes, v1.0.0** (≤ 500 characters)

```
Welcome to <APP_NAME>! Budget by month, log spending in seconds, track cash, mobile money and bank accounts, set bill reminders and save for your goals. Works offline and syncs across your devices.
```

**Policy-safe wording rules for the listing**

- Do not claim to be a bank, lender, investment or tax service.
- Do not say "official", "No. 1", "best", or show awards you don't have.
- Do not imply it moves or accesses real money: it is manual-entry.
- Screenshots and graphics must show real app screens (no fake UI).
- No keyword stuffing, no repeated terms, no emoji spam in the title.

---

## 2. Asset briefs

### 2.1 App icon

| Item | Spec |
|---|---|
| Play Store icon | 512×512 PNG, 32-bit, ≤ 1 MB, square (no pre-rounded corners, no drop shadow) |
| Adaptive icon foreground | 432×432 px (108dp @ xxxhdpi) with the artwork inside the central **264×264** safe zone (66dp) |
| Adaptive background | Solid `#C84E26` (or `#FAECE7` for a lighter look) |
| Monochrome layer | Single-color silhouette of the foreground for Android 13+ themed icons |
| Concept | A simple coin/cedi mark `₵` inside a rounded "sprout" or a stack of three tiles (echoing the bento grid). Flat, 2 colors max, readable at 48px. |
| Avoid | Gradients that vanish at small sizes, text, thin lines, stock-photo cliches |

### 2.2 Feature graphic (1024×500)

- Background `#F7FAF8`, left side: headline **"Give every cedi a job."** in bold Inter (≥ 64px, color `#2D3436`, with "cedi" in `#C84E26`).
- Right side: a phone mock showing the Home bento screen (hero tile visible).
- Keep key content inside the central 924×400 area (edges get cropped on some devices).
- No pricing, rankings, or "download now" text.

### 2.3 Screenshots (phone, 9:16, e.g. 1080×1920)

Capture from the **production-like** build on a clean demo account. Show light mode for 5 and dark mode for 1.

| # | Screen | Caption (≤ 6 words, overlaid at top) |
|---|---|---|
| 1 | Home bento | See where every cedi goes |
| 2 | Add transaction | Log spending in seconds |
| 3 | Budget | Plan your month, your way |
| 4 | Accounts | Cash, MoMo and bank in one place |
| 5 | Wishlist and goals | Save for what matters |
| 6 | Dark mode Home | Easy on the eyes, day or night |
| 7 (optional) | Bills / recurring | Never miss a bill |
| 8 (optional) | Privacy center | Private by design |

Rules: ≥ 2 and ≤ 8 screenshots; 16:9 or 9:16; each side 320–3,840 px; no device frames required; no personal data in the images; use realistic Ghanaian sample data (Bolt ride, ECG, trotro fare) so it feels local.

Add 7-inch and 10-inch tablet screenshots if the tablet layout ships.

### 2.4 Optional promo video

30–60 seconds, screen recording only, no music with unclear licence, upload to YouTube (unlisted is fine) and paste the link. Start with the "log an expense" flow in the first 5 seconds.

---

## 3. Release playbook

### 3.1 Release types

| Type | When | Path |
|---|---|---|
| **Regular** | Planned features/fixes | PR → CI green → tag `vX.Y.Z` → EAS build → Internal testing → 3-day soak → Production staged 20/50/100 |
| **Hotfix** | Crash or data issue in production | Branch from the release tag → minimal fix → tests → build → Internal testing (hours) → Production staged 50/100 |
| **Policy fix** | Play policy email | See §5 |

### 3.2 Pre-release checklist (every release)

- [ ] `npm run check` passes locally and in CI
- [ ] Version bumped (`version` in `app.config.ts`; `versionCode` is automatic)
- [ ] Release notes written (plain language, ≤ 500 chars)
- [ ] Data/schema changes: rules + indexes deployed to **staging** first and Maestro flows pass
- [ ] Migration tested on a copy of real-shaped data (REL-6)
- [ ] Pre-launch report from Play shows no new crashes/accessibility errors
- [ ] `config/app.latestVersion` will be updated **after** rollout reaches 100%

### 3.3 Staged rollout monitoring

For each stage (20% → 50% → 100%), wait at least 24 hours and check:

| Signal | Where | Pause the rollout if |
|---|---|---|
| Crash-free users | Crashlytics / Android vitals | < 99% in the new version, or a spike versus the previous version |
| User-perceived ANR rate | Android vitals | Approaching Google's "bad behavior" threshold (about 0.47% overall; verify the current number in Play Console) |
| User-perceived crash rate | Android vitals | Approaching the threshold (about 1.09% overall; verify the current number) |
| Review rating/keywords | Play Console → Ratings and reviews | Several reviews about lost data, wrong balances, or sign-in failures |
| Support email | Inbox | Two or more reports of the same serious issue |

To pause: *Play Console → Production → Manage rollout → Halt rollout.* You can **resume** or ship a fixed build. Play Console cannot roll users back to an older build; the fix is a **new build with a higher versionCode**.

### 3.4 Emergency controls (no release needed)

1. Set `config/app.flags.<feature> = false` in the prod Firebase project to disable a feature remotely (REL-5). Document each flag name in `docs/flags.md` and check it before using.
2. Raise `minSupportedVersion` only after a fixed build is live for ≥ 48 hours, so users aren't blocked with no way to update.

### 3.5 Incident severity

| Level | Definition | Response target | Example |
|---|---|---|---|
| **S1** | Data loss, wrong balances, security exposure, can't sign in for many users | Acknowledge within 4 hours; fix or mitigate within 24 hours | Rules deployment blocks all writes |
| **S2** | Major feature broken, crashes for a subset | Within 2 days | Recurring engine creates duplicates |
| **S3** | Minor bug, cosmetic | Next regular release | Misaligned label in dark mode |

**Incident steps:** (1) halt the rollout or flip a flag → (2) reproduce on staging → (3) fix and add a regression test → (4) release → (5) write a short post-mortem in `docs/incidents/YYYY-MM-DD.md` (what happened, impact, cause, fix, prevention).

**Security issues:** if personal data may have been exposed, check Ghana's Data Protection Act breach-notification duties and Google Play's requirements; notify affected users and the Data Protection Commission where required. Keep this contact list in `docs/incidents/contacts.md`.

### 3.6 Release calendar

| When | Task |
|---|---|
| Every release | §3.2 checklist |
| Monthly | `npm audit`, dependency updates (Dependabot PRs), check Android vitals |
| Quarterly | Review Play *Policy → Policy deadlines*; re-run TalkBack and low-end device tests; rotate any shared credentials |
| Before **31 Aug** each year | Raise `targetSdk` to the new required level; ship the update |
| Annually | Re-read the Privacy Policy and Terms; confirm the Data safety form still matches what the app does |

---

## 4. Support playbook

**Channels:** support email `<SUPPORT_EMAIL>` (shown on the Play listing), the in-app Send feedback button, and Play reviews. Check them at least twice a week. Target: first reply within **2 business days**.

### 4.1 Triage labels

`bug` · `data-issue` · `sync` · `account` · `how-to` · `feature-request` · `privacy-request` · `policy`

`privacy-request` (export or deletion by email) must be completed within **30 days**, per the Privacy Policy. Verify identity by replying from the registered email address before deleting anything.

### 4.2 Email templates

**Acknowledgement (bug report)**
```
Subject: Re: <their subject>

Hi <name>,

Thanks for telling us about this. I'm looking into it now.

To help me find it faster, could you tell me:
1. Your phone model and Android version
2. The app version (Settings → About)
3. What you were doing when it happened

I'll reply again within 2 business days.

<DEVELOPER_NAME>
<APP_NAME> support
```

**Data looks wrong / missing**
```
Hi <name>,

I'm sorry this happened, and I know how important your numbers are. Please try this first:
1. Open Settings → Data → Sync status and tap Sync now.
2. Check Settings → Data → Recently deleted. Anything removed in the last 30 days can be restored.
3. Make sure you're signed in to the same account on both phones.

If that doesn't fix it, reply with what you expected to see and I'll investigate. Please do not delete the app or your account yet.

<DEVELOPER_NAME>
```

**Deletion request by email**
```
Hi <name>,

I received your request to delete your <APP_NAME> account and data. To confirm it's you, please reply "DELETE" from this email address.

Once confirmed, I will delete your account and all stored data within 30 days and confirm by email. Backups and logs are purged within 30 days. This cannot be undone. If you'd like a copy first, you can export your data in the app under Settings → Data → Export.

<DEVELOPER_NAME>
```

**Feature request**
```
Hi <name>,

Thanks for the suggestion. I've added it to the ideas list. I can't promise a date, but I read every request and use them to decide what to build next.

<DEVELOPER_NAME>
```

### 4.3 Play review replies

| Situation | Reply (≤ 350 characters) |
|---|---|
| 5 stars | Thank you! Glad it's helping you keep track of your cedis. If you have ideas, email <SUPPORT_EMAIL>. |
| Bug, low rating | Sorry about that. Please email <SUPPORT_EMAIL> with your phone model and what happened, and I'll look into it right away. |
| Missing feature | Thanks for the feedback. I've noted it for future updates. You're welcome to send details to <SUPPORT_EMAIL>. |
| "Is it safe?" | Your data is encrypted, only you can read it, and you can lock the app and delete your account any time. The app never connects to your bank. |

Never ask users to change a rating in exchange for help, and never share personal data in a public reply.

---

## 5. Handling Google Play policy emails

1. **Don't ignore or delay.** Policy emails have deadlines; set a calendar reminder the same day.
2. Read the exact policy cited and the screenshot/section Google flagged (Play Console → Policy status).
3. Fix the real cause (listing text, declaration, permission, behaviour), not just the symptom.
4. Submit the fix as a new release or listing update, then use **Appeal / Request review** with a short, factual explanation: what was wrong, what you changed, where to see it.
5. Never create a second developer account to evade a suspension; it leads to permanent termination.
6. Keep a log in `docs/policy-log.md`: date, policy, action, outcome.

**Most likely flags for this app and the fix**

| Flag | Likely cause | Fix |
|---|---|---|
| Data safety mismatch | Form says "no data collected" or omits financial info | Align with Appendix B in DESIGN_SYSTEM |
| Missing account deletion | No web URL, or URL broken | Publish the deletion page; keep the URL live |
| Financial features declaration | Option chosen doesn't match the app | Re-read the options; choose the one that says no loans/banking/payments/trading |
| App access | Reviewers can't log in | Provide a test account and mention Try demo |
| Target API | Below the required level | Rebuild with the required targetSdk |
| Permissions | Unneeded permission left in the manifest | Remove via `blockedPermissions` |

---

## 6. Backups and continuity (solo developer risks)

- **Credentials:** keep EAS credentials export, upload-key details, Google account recovery info, and the Play Console 2-step recovery codes in a password manager **and** an offline copy.
- **Account access:** add a second trusted Google account as a Play Console user (limited role) so the app isn't lost if one account is locked.
- **Repo:** private GitHub repo plus a second remote or periodic offline bundle (`git bundle`).
- **Firebase:** schedule a monthly Firestore export for prod if you upgrade to Blaze; on the free plan rely on users' in-app exports and keep security rules in git.
- **Domain/pages:** if the privacy and deletion pages go down, the app can be removed from Play. Host on two independent services or monitor with a free uptime checker.
- **Bus factor doc:** keep `docs/RUNBOOK.md` with how to build, release, rotate keys and respond to incidents so a future helper (or you in a year) can act quickly.
