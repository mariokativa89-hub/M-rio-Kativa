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

function validProfile(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Creator Studio",
    defaultAspectRatio: "9:16",
    defaultTone: "Hook Viral",
    defaultCaptionStyle: "Karaokê",
    defaultDurationSec: 45,
    createdAt: now,
    updatedAt: now,
  };
}

function validClip(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    sourceType: "topic",
    sourceValue: "Como escalar produtividade com IA",
    title: "O segredo dos 3 primeiros segundos",
    hook: "Você está perdendo 80% da retenção aqui!",
    viralReason: "Gancho forte com dado numérico logo no início.",
    viralScore: 92,
    startTimeSec: 12,
    endTimeSec: 54,
    aspectRatio: "9:16",
    tone: "Hook Viral",
    captionStyle: "Karaokê",
    captionPosition: "Base",
    themeIndex: 0,
    transcript: [
      { id: "seg_1", startSec: 12, endSec: 25, text: "Você está perdendo 80% da retenção aqui!" },
      { id: "seg_2", startSec: 25, endSec: 54, text: "Veja como corrigir em 3 passos simples." }
    ],
    hashtags: ["#shorts", "#produtividade", "#criadores"],
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write clips or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").set(validClip(ALICE_UID)));
});

test("Authenticated owner can create, list, update, and delete own clip", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const clipRef = aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1");
  await assertSucceeds(clipRef.set(validClip(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertSucceeds(clipRef.update({ title: "Título Editado", updatedAt: new Date(Date.now() - 1000) }));
  await assertSucceeds(clipRef.delete());
});

test("Authenticated user cannot read, create, update, or delete another user's clips", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").set(validClip(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_2").set(validClip(BOB_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").update({ title: "Hacked", updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").delete());
});

test("Shadow update with ghost field or invalid viralScore is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const clipRef = aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1");
  await assertSucceeds(clipRef.set(validClip(ALICE_UID)));

  await assertFails(clipRef.update({ isAdmin: true, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(clipRef.update({ viralScore: 999, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(clipRef.update({ userId: BOB_UID, updatedAt: new Date(Date.now() - 1000) }));
});

test("Authenticated owner can create and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.update({ defaultDurationSec: 60, updatedAt: new Date(Date.now() - 1000) }));
});
