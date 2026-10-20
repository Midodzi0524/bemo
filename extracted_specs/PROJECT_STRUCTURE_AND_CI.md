# Project Structure, Conventions, Environments & CI/CD

Companion to `DESIGN_SYSTEM.md`. Replace `<APP_NAME>`, `<PACKAGE_NAME>`, `<scheme>` before use.
**Rule for AI coders:** follow this file exactly when creating files. If you must deviate, write the reason in `DECISIONS.md`.

---

## 1. Repository layout

```
<app-name>/
├─ app/                          # Expo Router routes ONLY (thin: compose features, no business logic)
│  ├─ _layout.tsx                # providers: theme, i18n, auth gate, lock gate, error boundary
│  ├─ (auth)/                    # welcome.tsx, sign-in.tsx, onboarding.tsx, update-required.tsx
│  ├─ (tabs)/                    # _layout.tsx, index.tsx (Home), budget.tsx, transactions.tsx, wishlist.tsx
│  ├─ add-transaction.tsx        # modal route (bottom sheet)
│  ├─ accounts/                  # index.tsx, [id].tsx
│  ├─ recurring/                 # index.tsx, [id].tsx
│  ├─ insights/                  # index.tsx
│  └─ settings/                  # index.tsx, security.tsx, privacy.tsx, data.tsx, help.tsx, ...
├─ src/
│  ├─ domain/                    # PURE TypeScript. No React, no Firebase, no Expo imports.
│  │  ├─ money.ts                # formatCedi, parseCedi, rounding
│  │  ├─ periods.ts              # monthKey, monthStartDay, daysLeft
│  │  ├─ budget.ts               # unassigned, category status, safe-to-spend, pace
│  │  ├─ balances.ts             # account balance, net, earmarked, available
│  │  ├─ recurring.ts            # due-date engine, deterministic ids
│  │  ├─ parser.ts               # smart text entry (SMT-1)
│  │  ├─ insights.ts             # trends, savings rate, insight cards
│  │  └─ schemas/                # Zod schemas for every Firestore document + inferred types
│  ├─ data/                      # persistence boundary
│  │  ├─ repos/                  # transactionsRepo.ts, accountsRepo.ts, ... (interfaces)
│  │  ├─ firebase/               # Firestore + Auth implementations of the repos
│  │  ├─ demo/                   # in-memory implementations for demo mode
│  │  └─ index.ts                # chooses firebase vs demo at runtime
│  ├─ features/                  # one folder per feature area; screens' building blocks
│  │  ├─ home/ budget/ transactions/ wishlist/ accounts/ recurring/ insights/ settings/ onboarding/
│  │  └─ <feature>/{components,hooks,state}.ts(x)
│  ├─ components/                # design-system primitives (Tile, Button, AmountText, ...)
│  ├─ theme/                     # tokens.ts, light.ts, dark.ts, useTheme.ts
│  ├─ i18n/                      # index.ts, locales/en-GH.json
│  ├─ services/                  # lock.ts, notifications.ts, updates.ts, review.ts, shortcuts.ts, crash.ts
│  ├─ store/                     # Zustand UI stores (never domain data)
│  └─ utils/                     # tiny helpers (id.ts, dates.ts, assert.ts)
├─ firebase/
│  ├─ firestore.rules
│  ├─ firestore.indexes.json
│  └─ tests/                     # rules unit tests (emulator)
├─ tests/
│  ├─ unit/                      # mirrors src/domain
│  ├─ components/
│  └─ e2e/                       # Maestro flows (*.yaml)
├─ assets/                       # icon.png, adaptive-icon-fg.png, adaptive-icon-mono.png, splash.png, fonts/
├─ docs/                         # all companion .md files, ADRs
├─ scripts/                      # seed-data.ts, check-contrast.ts, check-strings.ts
├─ .github/workflows/            # ci.yml, release.yml
├─ app.config.ts                 # dynamic Expo config (per environment)
├─ eas.json
├─ firebase.json
├─ .firebaserc
├─ DECISIONS.md
└─ package.json
```

### 1.1 Dependency rules (enforce with `eslint-plugin-boundaries`)

```
app          → features, components, theme, i18n, services, store
features     → data (interfaces), domain, components, theme, i18n, services, store
data/firebase→ domain (schemas only)
components   → theme, i18n
services     → domain, data (interfaces)
domain       → (nothing outside domain)      ← pure, 100% unit-testable
```

- UI code MUST NOT import `@react-native-firebase/*`. Only `src/data/firebase/**` may.
- `domain/` MUST NOT import React, React Native, Expo or Firebase.
- A feature MUST NOT import another feature's internals; share through `components/` or `domain/`.

---

## 2. Coding conventions

| Topic | Rule |
|---|---|
| Language | TypeScript `strict: true`, `noUncheckedIndexedAccess: true`. No `any` (use `unknown` + Zod). No `// @ts-ignore` without a linked reason. |
| Naming | Files: `camelCase.ts`, components `PascalCase.tsx`, hooks `useThing.ts`. Constants `UPPER_SNAKE`. Feature IDs appear in test names: `it('ACC-2: transfer does not change spent', ...)`. |
| Money | Integer pesewas only. Variables end in `Minor` (`amountMinor`). A function that takes a plain `number` for money MUST say `Minor` in the parameter name. |
| Dates | Store UTC timestamps; compute periods in `Africa/Accra` through `domain/periods.ts` only. Never call `new Date()` inside `domain/`; inject `now` as a parameter (testability). |
| Errors | Domain and repos return `Result<T, AppError>` (no thrown errors across layers). UI maps `AppError.code` to a string key in `en-GH.json`. Never show raw messages. |
| Logging | Use `log.debug/info/warn/error` wrapper. In production only `warn`/`error` go to Crashlytics, with **no** amounts, notes, names or emails. |
| State | Server data via repo subscriptions; Zustand only for UI state (sheet open, filters, quick-hide). |
| Styling | Only tokens through `useTheme()`. No hex codes, no magic numbers outside `theme/`. ESLint rule bans color literals in `src/features` and `app`. |
| Strings | Every user-visible string is a key in `en-GH.json`. ESLint rule `no-literal-string-in-jsx` on. |
| Components | One component per file; props typed; `accessibilityLabel`/`Role` mandatory on touchables; min 48dp hit area. Memoize list rows. |
| Async | No unhandled promises. Wrap fire-and-forget in `safeAsync()` that reports errors. |
| Tests | Pure logic: Jest. UI: RNTL with light + dark + 2× font. Data: emulator. Flows: Maestro. |
| Comments | Explain *why*, not *what*. Link the feature ID or DESIGN_SYSTEM section. |
| Dependencies | Only libraries listed in DESIGN_SYSTEM §3 unless approved in `DECISIONS.md`. Pin exact versions. |
| Commits | Conventional Commits: `feat(ACC-2): transfers between accounts`. One feature ID per commit where possible. |
| Branches | Trunk-based: short-lived `feat/ACC-2-transfers` branches → PR → squash merge to `main`. `main` is always releasable. |

### 2.1 Definition of Done (every PR)

- [ ] Feature ID(s) referenced; acceptance criteria from DESIGN_SYSTEM §2.1 all met
- [ ] Unit tests for new domain logic; component tests for new UI (light/dark/2× font)
- [ ] All states implemented (loading, empty, error, offline)
- [ ] No hard-coded strings, colors or sizes; `npm run check` passes
- [ ] Accessibility: labels, roles, 48dp targets, reduced motion respected
- [ ] No PII in logs; no new permissions
- [ ] `DECISIONS.md` updated if an assumption was made
- [ ] Rules/indexes updated and emulator tests pass if the schema changed

### 2.2 Pull request template (`.github/pull_request_template.md`)

```md
## What and why
Feature ID(s): 

## Screenshots (light / dark / large font)

## Checklist (Definition of Done)
- [ ] Acceptance criteria met  - [ ] Tests added  - [ ] A11y checked
- [ ] Strings externalized  - [ ] No PII in logs  - [ ] Rules/indexes updated if needed
```

---

## 3. Environments (never test against real data)

| Environment | Firebase project | Android application ID | Build profile | Purpose |
|---|---|---|---|---|
| **dev** | `<app>-dev` (or local Emulator Suite) | `<PACKAGE_NAME>.dev` | `development` (dev client APK) | Daily development |
| **staging** | `<app>-staging` | `<PACKAGE_NAME>.staging` | `preview` (internal APK) | Pre-release QA, Maestro runs |
| **prod** | `<app>-prod` | `<PACKAGE_NAME>` | `production` (AAB) | Play Store |

Rules:

- Three Firebase projects, each with its own `google-services.json` (stored as EAS **file secrets**, not committed).
- Different application IDs let all three apps be installed side by side on one phone.
- Prod Firebase: App Check **enforced**, Auth providers limited to Google + Email, billing alert set, deletion protection on.
- Dev/staging apps show a coloured "DEV"/"STAGING" ribbon in Settings → About.
- Seed data (DEV-1) is only available when `APP_ENV !== 'prod'`.

### 3.1 `app.config.ts`

```ts
import type { ExpoConfig } from 'expo/config';

const ENV = (process.env.APP_ENV ?? 'dev') as 'dev' | 'staging' | 'prod';
const SUFFIX = { dev: '.dev', staging: '.staging', prod: '' }[ENV];
const NAME = { dev: '<APP_NAME> (Dev)', staging: '<APP_NAME> (Staging)', prod: '<APP_NAME>' }[ENV];

const config: ExpoConfig = {
  name: NAME,
  slug: '<app-slug>',
  scheme: '<scheme>',
  version: '1.0.0',
  orientation: 'portrait',
  userInterfaceStyle: 'automatic',
  icon: './assets/icon.png',
  newArchEnabled: true,
  android: {
    package: `<PACKAGE_NAME>${SUFFIX}`,
    googleServicesFile: process.env.GOOGLE_SERVICES_JSON ?? './google-services.json',
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon-fg.png',
      monochromeImage: './assets/adaptive-icon-mono.png',
      backgroundColor: '#C84E26',
    },
    allowBackup: false,
    blockedPermissions: [
      'android.permission.READ_EXTERNAL_STORAGE',
      'android.permission.WRITE_EXTERNAL_STORAGE',
      'android.permission.SYSTEM_ALERT_WINDOW',
      'android.permission.RECORD_AUDIO',
      'android.permission.SCHEDULE_EXACT_ALARM',
    ],
    permissions: [
      'android.permission.USE_BIOMETRIC',
      'android.permission.VIBRATE',
      'android.permission.RECEIVE_BOOT_COMPLETED',
      'android.permission.POST_NOTIFICATIONS',
    ],
  },
  plugins: [
    'expo-router',
    'expo-secure-store',
    'expo-local-authentication',
    '@react-native-firebase/app',
    '@react-native-firebase/auth',
    '@react-native-firebase/crashlytics',
    ['expo-build-properties', { android: { compileSdkVersion: 36, targetSdkVersion: 36 } }],
    ['expo-splash-screen', { image: './assets/splash.png', backgroundColor: '#F7FAF8',
                             dark: { image: './assets/splash.png', backgroundColor: '#121413' } }],
  ],
  extra: { appEnv: ENV },
};
export default config;
```

> Confirm that the chosen Expo SDK supports `targetSdkVersion: 36`; if it does not, upgrade the SDK rather than lowering the target (Play requires 36).

### 3.2 `eas.json`

```json
{
  "cli": { "version": ">= 16.0.0", "appVersionSource": "remote" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "android": { "buildType": "apk" },
      "env": { "APP_ENV": "dev" }
    },
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" },
      "env": { "APP_ENV": "staging" }
    },
    "production": {
      "autoIncrement": true,
      "android": { "buildType": "app-bundle" },
      "env": { "APP_ENV": "prod" }
    }
  },
  "submit": {
    "production": {
      "android": {
        "serviceAccountKeyPath": "./play-service-account.json",
        "track": "internal",
        "releaseStatus": "completed"
      }
    }
  }
}
```

Store per-environment `GOOGLE_SERVICES_JSON` as EAS file secrets (`eas secret:create --scope project --type file`). Never commit `google-services.json` for prod or `play-service-account.json`.

---

## 4. `package.json` scripts

```json
{
  "scripts": {
    "start": "expo start --dev-client",
    "lint": "eslint . --max-warnings 0",
    "typecheck": "tsc --noEmit",
    "test": "jest --ci --coverage",
    "test:rules": "firebase emulators:exec --only firestore --project demo-test \"jest --config firebase/tests/jest.config.js\"",
    "check:contrast": "ts-node scripts/check-contrast.ts",
    "check:strings": "ts-node scripts/check-strings.ts",
    "check": "npm run lint && npm run typecheck && npm test && npm run check:contrast && npm run check:strings",
    "e2e": "maestro test tests/e2e",
    "seed": "ts-node scripts/seed-data.ts"
  }
}
```

- `check:contrast` reads the theme tokens and fails if any declared text/background pair is below 4.5:1 (3:1 for large text/graphics).
- `check:strings` fails if `en-GH.json` has unused or missing keys and when the pseudo-locale build contains literal text.

---

## 5. CI: `.github/workflows/ci.yml`

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22, cache: npm }
      - run: npm ci
      - run: npx expo-doctor
      - run: npm run lint
      - run: npm run typecheck
      - run: npm test
      - run: npm run check:contrast
      - run: npm run check:strings
      - run: npm audit --audit-level=high
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: npm i -g firebase-tools
      - run: npm run test:rules
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: coverage, path: coverage }
```

## 6. Release: `.github/workflows/release.yml`

```yaml
name: Release
on:
  push:
    tags: ['v*.*.*']

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 60
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22, cache: npm }
      - uses: expo/expo-github-action@v8
        with: { eas-version: latest, token: ${{ secrets.EXPO_TOKEN }} }
      - run: npm ci
      - run: npm run check
      - run: eas build --platform android --profile production --non-interactive --wait
      - name: Submit to Play (internal track)
        run: |
          echo '${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}' > play-service-account.json
          eas submit --platform android --profile production --latest --non-interactive
          rm -f play-service-account.json
```

**GitHub secrets needed:** `EXPO_TOKEN`, `PLAY_SERVICE_ACCOUNT_JSON`.

> **Important:** Google Play does not let the API create the very first release. Upload your **first** AAB manually in Play Console (Internal testing), then automation can submit later builds. Give the service account only the "Release manager" permission for this one app.

Also add Dependabot (`.github/dependabot.yml`) for `npm` and `github-actions`, weekly.

---

## 7. Firebase setup (once per environment)

1. Create the three projects; enable **Authentication** (Google, Email/Password), **Firestore** (production mode, region close to users, for example `eur3` or `europe-west1`), **Crashlytics**, **App Check** (Play Integrity).
2. Add the Android app (with the matching application ID) and the **SHA-256 fingerprints**: the EAS upload key, **and** the Play App Signing key (from Play Console → Setup → App signing) for prod.
3. Create the public config doc `config/app` in each project: `{ "minSupportedVersion": "1.0.0", "latestVersion": "1.0.0", "flags": {} }`.
4. Deploy: `firebase deploy --only firestore:rules,firestore:indexes --project <app>-prod`.
5. Restrict the Android API key to the app package + SHA-256 in Google Cloud Console.
6. Set a **budget alert** in Google Cloud Billing (even on the free plan) for safety.

`firebase.json`:

```json
{
  "firestore": { "rules": "firebase/firestore.rules", "indexes": "firebase/firestore.indexes.json" },
  "emulators": { "firestore": { "port": 8080 }, "auth": { "port": 9099 }, "ui": { "enabled": true } }
}
```

---

## 8. Rules tests (minimum set, `firebase/tests/rules.test.ts`)

Use `@firebase/rules-unit-testing`. Every case below MUST exist and pass:

```ts
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { serverTimestamp, doc, setDoc, getDoc, Timestamp } from 'firebase/firestore';

let env: Awaited<ReturnType<typeof initializeTestEnvironment>>;
beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-test',
    firestore: { rules: readFileSync('firebase/firestore.rules', 'utf8') },
  });
});
afterAll(() => env.cleanup());

const validTx = () => ({
  type: 'expense', amountMinor: 2500, categoryId: 'c1', accountId: 'a1',
  occurredAt: Timestamp.now(), monthKey: '2026-10', status: 'posted',
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(), deletedAt: null,
});

test('owner can create a valid transaction', async () => {
  const db = env.authenticatedContext('u1').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/u1/transactions/t1'), validTx()));
});
test('another user cannot read it', async () => {
  const db = env.authenticatedContext('u2').firestore();
  await assertFails(getDoc(doc(db, 'users/u1/transactions/t1')));
});
test('unauthenticated cannot read or write', async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, 'users/u1/transactions/t1')));
});
test('rejects float amount', async () => {
  const db = env.authenticatedContext('u1').firestore();
  await assertFails(setDoc(doc(db, 'users/u1/transactions/t2'), { ...validTx(), amountMinor: 25.5 }));
});
test('rejects negative amount for expense', async () => { /* amountMinor: -1 → fails */ });
test('rejects unknown field', async () => { /* extra: 'x' → fails */ });
test('rejects transfer to same account', async () => { /* toAccountId == accountId → fails */ });
test('rejects expense without categoryId', async () => { /* → fails */ });
test('rejects bad monthKey', async () => { /* '2026-13' → fails */ });
test('budget id must equal monthKey_categoryId', async () => { /* mismatch → fails */ });
test('config/app readable by anyone, writable by no one', async () => { /* … */ });
test('cannot read an arbitrary top-level collection', async () => { /* → fails */ });
```

Add the same pattern for accounts, categories, wishlist, recurring and templates. **Update the rules and these tests together whenever `src/domain/schemas` changes.**

> Note on offline restore/import (DAT-5, DAT-6): rules require `updatedAt == request.time`, so import writes must set `updatedAt` with `serverTimestamp()` and may keep the original `createdAt`.

---

## 9. Maestro E2E flows (`tests/e2e/`)

Minimum flows, each a `.yaml` file:

1. `onboarding-demo.yaml`: Welcome → *Try demo* → Home visible
2. `add-expense.yaml`: FAB → amount → category → Save → appears in Transactions
3. `offline-add.yaml`: airplane mode on → add → off → still present after sync
4. `budget-edit.yaml`: edit allocation → Unassigned updates
5. `transfer.yaml`: transfer between accounts → net unchanged, both balances change
6. `recurring-confirm.yaml`: confirm-first rule → pending appears → Confirm
7. `wishlist-contribute.yaml`: contribute → progress bar and % update
8. `lock.yaml`: enable lock → background 31s → lock screen shown
9. `delete-account.yaml`: delete account → returns to Welcome, data gone

---

## 10. Secrets and sensitive files checklist

| Item | Where it lives | Never |
|---|---|---|
| Prod `google-services.json` | EAS file secret | In git |
| Upload keystore | EAS-managed credentials + offline backup in a password manager | Emailed or in git |
| Play service account JSON | GitHub secret | In git or logs |
| Expo token | GitHub secret | In git |
| Firebase admin keys | Not used | Anywhere in the repo |

Add to `.gitignore`: `google-services.json`, `play-service-account.json`, `*.keystore`, `.env*`, `coverage/`, `.expo/`, `android/`, `ios/` (managed workflow).
