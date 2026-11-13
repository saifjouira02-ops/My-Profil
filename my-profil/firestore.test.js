const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
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

function validProfile(uid, isPrivate = false) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    displayName: "Alice Noor",
    handle: "alice_noor",
    bio: "Designer & traveler",
    avatarSeed: 1,
    isPrivate,
    followersCount: 0,
    followingCount: 0,
    postsCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPost(authorId, visibility = "public") {
  const ts = pastTimestamp();
  return {
    authorId,
    authorName: "Alice Noor",
    authorHandle: "alice_noor",
    authorAvatarSeed: 1,
    content: "Hello my profil!",
    visibility,
    likesCount: 0,
    commentsCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or list posts or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("public_profiles").where("isPrivate", "==", false).get());
});

test("Authenticated user can create and update own public profile, cannot spoof or shadow-update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Updated",
      isAdmin: true,
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Updated bio",
      updatedAt: pastTimestamp(),
    })
  );

  // Bob cannot update Alice's bio
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Private account PII is strictly isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      language: "ar",
      darkMode: "system",
      largeText: false,
      highContrast: false,
      showDemoContent: true,
      updatedAt: pastTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );
});

test("Private profile cannot be queried or read by another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set(validProfile(ALICE_UID, true))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("public_profiles").get());
  await assertSucceeds(bobDb.collection("public_profiles").where("isPrivate", "==", false).get());
});

test("Post creation, query alignment, atomic like, comment, and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("posts").doc("post_1").set(validPost(ALICE_UID, "public")));
  await assertSucceeds(aliceDb.collection("posts").doc("post_priv").set(validPost(ALICE_UID, "private")));

  // Bob cannot list unfiltered posts or read private post
  await assertFails(bobDb.collection("posts").get());
  await assertFails(bobDb.collection("posts").doc("post_priv").get());

  // Bob CAN list public posts
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());

  // Bob cannot arbitrarily inflate likesCount without atomic like document
  await assertFails(bobDb.collection("posts").doc("post_1").update({ likesCount: 10 }));

  // Bob CAN atomically like via batch
  const batch = bobDb.batch();
  batch.set(bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID), {
    userId: BOB_UID,
    postId: "post_1",
    createdAt: pastTimestamp(),
  });
  batch.update(bobDb.collection("posts").doc("post_1"), { likesCount: 1 });
  await assertSucceeds(batch.commit());

  // Bob CAN comment on public post
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c1").set({
      postId: "post_1",
      postVisibility: "public",
      authorId: BOB_UID,
      authorName: "Bob",
      authorAvatarSeed: 2,
      text: "Great post!",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
});

test("Follows, notifications, blocked users, and reports enforce strict boundaries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(bobDb.collection("public_profiles").doc(BOB_UID).set(validProfile(BOB_UID)));

  // Bob follows Alice atomically
  const followId = `${BOB_UID}_${ALICE_UID}`;
  const followBatch = bobDb.batch();
  followBatch.set(bobDb.collection("follows").doc(followId), {
    followerId: BOB_UID,
    followerName: "Bob",
    followerHandle: "bob",
    followerAvatarSeed: 2,
    followingId: ALICE_UID,
    followingName: "Alice Noor",
    followingHandle: "alice_noor",
    followingAvatarSeed: 1,
    isPublicTarget: true,
    createdAt: pastTimestamp(),
  });
  followBatch.update(bobDb.collection("public_profiles").doc(BOB_UID), { followingCount: 1 });
  followBatch.update(bobDb.collection("public_profiles").doc(ALICE_UID), { followersCount: 1 });
  await assertSucceeds(followBatch.commit());

  // Bob notifies Alice; Alice can read and mark read, Bob cannot read Alice's notifications
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").set({
      recipientId: ALICE_UID,
      actorId: BOB_UID,
      actorName: "Bob",
      actorAvatarSeed: 2,
      type: "follow",
      isRead: false,
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());

  // Alice blocks Bob; Bob cannot read Alice's blocked list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blocked").doc(BOB_UID).set({
      blockedUserId: BOB_UID,
      blockedDisplayName: "Bob",
      blockedHandle: "bob",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("blocked").get());

  // Alice submits report; immutable and isolated to reporter
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "spam",
      details: "Spam content",
      status: "submitted",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertFails(aliceDb.collection("reports").doc("rep_1").delete());
});
