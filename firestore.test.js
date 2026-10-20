const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user profile or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Alice",
      currencyCode: "GHS",
      hideAmounts: false,
      pinEnabled: false,
      monthlyIncomeLimitPesewas: 450000,
      monthlyExpenseLimitPesewas: 350000,
      hasCompletedOnboarding: true,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Alice",
      currencyCode: "GHS",
      hideAmounts: false,
      pinEnabled: false,
      monthlyIncomeLimitPesewas: 450000,
      monthlyExpenseLimitPesewas: 350000,
      hasCompletedOnboarding: true,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create an account and transaction in their subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create account
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_cash").set({
      id: "acc_cash",
      userId: ALICE_UID,
      name: "Cash",
      emoji: "💵",
      kind: "CASH",
      openingBalancePesewas: 20000,
      isDefault: true,
      isArchived: false,
      createdAt: new Date(),
    })
  );

  // Create transaction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      id: "tx_1",
      userId: ALICE_UID,
      type: "EXPENSE",
      amountPesewas: 3500,
      accountId: "acc_cash",
      dateMillis: Date.now(),
      note: "Lunch",
      status: "POSTED",
      isDeleted: false,
      createdAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot write to Alice's subcollections", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_cash").set({
      id: "acc_cash",
      userId: BOB_UID,
      name: "Hacked Cash",
      emoji: "💵",
      kind: "CASH",
      openingBalancePesewas: 0,
      isDefault: false,
      isArchived: false,
      createdAt: new Date(),
    })
  );
});

test("Validation rejects invalid transaction payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Negative amount
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      id: "tx_bad",
      userId: ALICE_UID,
      type: "EXPENSE",
      amountPesewas: -500,
      accountId: "acc_cash",
      dateMillis: Date.now(),
      note: "Invalid",
      status: "POSTED",
      isDeleted: false,
      createdAt: new Date(),
    })
  );
});
