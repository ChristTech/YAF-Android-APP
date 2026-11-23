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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

// --- SECURITY BOUNDS & DIRTY DOZEN TESTS ---

test("1. Unauthenticated user: cannot read users or execute unfiltered fellowship queries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("users").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("daily_scriptures").get());
  await assertFails(unauthDb.collection("quiz_sessions").get());
  await assertFails(unauthDb.collection("game_quizzes").get());
  await assertFails(unauthDb.collection("game_scores").get());
});

test("2. PII Isolation: Authenticated user cannot read another user's profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Gideon",
      email: "bob@gideonites.org",
      role: "YOUTH_MEMBER",
      department: "CHOIR",
      preferredLanguage: "yo",
      englishOverride: false,
      xpPoints: 100,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("3. Self-Privilege Escalation: User cannot create profile with ADMINISTRATOR role", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Gideon",
      email: "alice@gideonites.org",
      role: "ADMINISTRATOR",
      department: "MEDIA",
      preferredLanguage: "yo",
      englishOverride: false,
      xpPoints: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("4. Valid User Profile creation and self-update (language & override)", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Gideon",
      email: "alice@gideonites.org",
      role: "YOUTH_MEMBER",
      department: "MEDIA",
      preferredLanguage: "yo",
      englishOverride: false,
      xpPoints: 50,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-update preferredLanguage & englishOverride succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ig",
      englishOverride: true,
      updatedAt: new Date(),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: new Date(),
    })
  );
});

test("5. Email Spoofing Attack: Unverified admin email cannot read Bob's PII", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Gideon",
      email: "bob@gideonites.org",
      role: "YOUTH_MEMBER",
      department: "CHOIR",
      preferredLanguage: "yo",
      englishOverride: false,
      xpPoints: 100,
      createdAt: now,
      updatedAt: now,
    });
  });

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "adebisivictor39@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofedAdminDb.collection("users").doc(BOB_UID).get());

  const verifiedAdminDb = testEnv
    .authenticatedContext("real_admin_uid", {
      email: "adebisivictor39@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(verifiedAdminDb.collection("users").doc(BOB_UID).get());
});

test("6. Daily Scripture: Query alignment enforced and cross-user tampering blocked", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("daily_scriptures").doc("scripture_2026_10_25").set({
      authorId: ALICE_UID,
      authorName: "Elder David",
      fellowshipId: "GIDEONITES",
      dateKey: "2026-10-25",
      referenceEn: "Judges 6:12",
      referenceYo: "Awon Onidajo 6:12",
      verseTextEn: "The Lord is with thee, thou mighty man of valour.",
      verseTextYo: "Oluwa pelu re, iwo alagbara ogun.",
      reflectionEn: "Rise up as a Gideonite in your generation.",
      prayerPromptEn: "Lord, awaken mighty valour in our youth fellowship.",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Filtered query with fellowshipId == GIDEONITES succeeds
  await assertSucceeds(
    bobDb
      .collection("daily_scriptures")
      .where("fellowshipId", "==", "GIDEONITES")
      .get()
  );

  // Unfiltered query fails (rules are not filters)
  await assertFails(bobDb.collection("daily_scriptures").get());

  // Cross-user update by Bob fails
  await assertFails(
    bobDb.collection("daily_scriptures").doc("scripture_2026_10_25").update({
      reflectionEn: "Tampered reflection",
      updatedAt: new Date(),
    })
  );
});

test("7. Programmes & Quiz Sessions: Terminal state locking and RSVP/Participant increment rules", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("quiz_sessions").doc("session_gideon_01").set({
      hostId: ALICE_UID,
      hostName: "Alice Host",
      fellowshipId: "GIDEONITES",
      roomPin: "GID777",
      titleEn: "Gideonites Champions Showdown",
      titleYo: "Idije Awon Odo Gideonites",
      gameType: "BIBLE_QUIZ",
      status: "ACTIVE",
      currentQuestionIndex: 0,
      totalQuestions: 5,
      participantCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob can join by incrementing participantCount by +1
  await assertSucceeds(
    bobDb.collection("quiz_sessions").doc("session_gideon_01").update({
      participantCount: 2,
      updatedAt: new Date(),
    })
  );

  // Bob cannot jump participantCount by +50 (Value Poisoning)
  await assertFails(
    bobDb.collection("quiz_sessions").doc("session_gideon_01").update({
      participantCount: 52,
      updatedAt: new Date(),
    })
  );

  // Non-host Bob CANNOT mutate round state (currentQuestionIndex or status)
  await assertFails(
    bobDb.collection("quiz_sessions").doc("session_gideon_01").update({
      currentQuestionIndex: 1,
      status: "COMPLETED",
      updatedAt: new Date(),
    })
  );

  // Alice marks session COMPLETED
  await assertSucceeds(
    aliceDb.collection("quiz_sessions").doc("session_gideon_01").update({
      status: "COMPLETED",
      currentSubstate: "LEADERBOARD",
      updatedAt: new Date(),
    })
  );

  // Final participant score convergence on COMPLETED session succeeds without mutating round/status
  await assertSucceeds(
    bobDb.collection("quiz_sessions").doc("session_gideon_01").update({
      participantsJson: "[{\"userId\":\"bob_456\",\"currentScore\":150}]",
      participantCount: 2,
      updatedAt: new Date(),
    })
  );

  // Terminal State Locking: Once COMPLETED, even host cannot increment currentQuestionIndex
  await assertFails(
    aliceDb.collection("quiz_sessions").doc("session_gideon_01").update({
      currentQuestionIndex: 2,
      updatedAt: new Date(),
    })
  );
});

test("8. Game Quizzes: Query alignment enforced and cross-user tampering blocked", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("game_quizzes").doc("quiz_bible_01").set({
      createdById: ALICE_UID,
      fellowshipId: "GIDEONITES",
      title: "Gideon's 300 Quiz",
      description: "Judges 7 challenge",
      category: "BIBLE_TRIVIA",
      questionsJson: "[{\"id\":1,\"question\":\"Who?\",\"options\":[\"Gideon\",\"Saul\"],\"correctAnswerIndex\":0}]",
      totalQuestions: 1,
      timeLimitSeconds: 30,
      difficulty: "Medium",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("game_quizzes").where("fellowshipId", "==", "GIDEONITES").get()
  );

  await assertFails(
    bobDb.collection("game_quizzes").doc("quiz_bible_01").update({
      title: "Hacked Quiz",
      updatedAt: new Date(),
    })
  );
});

test("9. Game Scores: Player can submit own score, cannot spoof another player's score", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    bobDb.collection("game_scores").doc("score_bob_01").set({
      quizId: "quiz_bible_01",
      quizTitle: "Gideon's 300 Quiz",
      userId: BOB_UID,
      userName: "Bob Gideon",
      fellowshipId: "GIDEONITES",
      score: 850,
      totalQuestions: 1000,
      timeTakenSeconds: 18,
      completedAt: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot impersonate Bob's score
  await assertFails(
    aliceDb.collection("game_scores").doc("score_spoof_01").set({
      quizId: "quiz_bible_01",
      quizTitle: "Gideon's 300 Quiz",
      userId: BOB_UID,
      userName: "Bob Gideon",
      fellowshipId: "GIDEONITES",
      score: 99999,
      totalQuestions: 1000,
      timeTakenSeconds: 5,
      completedAt: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered leaderboard query succeeds
  await assertSucceeds(
    aliceDb.collection("game_scores").where("fellowshipId", "==", "GIDEONITES").get()
  );
});

test("10. Quiz Session Drafts: Host can create, edit, launch, and delete DRAFT; non-host blocked", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Host creates a DRAFT quiz session for later use
  await assertSucceeds(
    aliceDb.collection("quiz_sessions").doc("session_draft_01").set({
      hostId: ALICE_UID,
      hostName: "Alice Host",
      fellowshipId: "GIDEONITES",
      roomPin: "DRF101",
      titleEn: "Sunday Evening Draft",
      description: "Saved for Sunday fellowship",
      gameType: "WHO_AM_I",
      status: "DRAFT",
      scheduledStartTime: 0,
      timeLimitPerQuestionSeconds: 25,
      leaderboardDurationSeconds: 8,
      currentQuestionIndex: 0,
      currentSubstate: "WAITING_TO_START",
      totalQuestions: 2,
      participantCount: 1,
      questionsJson: "[{\"id\":1},{\"id\":2}]",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-host Bob cannot edit or delete Alice's draft
  await assertFails(
    bobDb.collection("quiz_sessions").doc("session_draft_01").update({
      titleEn: "Hijacked Draft",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("quiz_sessions").doc("session_draft_01").delete()
  );

  // Host Alice edits draft title, question count, and launches it to ACTIVE
  await assertSucceeds(
    aliceDb.collection("quiz_sessions").doc("session_draft_01").update({
      titleEn: "Sunday Evening Live Showdown",
      gameType: "TRIVIA_CHALLENGE",
      totalQuestions: 3,
      questionsJson: "[{\"id\":1},{\"id\":2},{\"id\":3}]",
      status: "ACTIVE",
      scheduledStartTime: Date.now(),
      updatedAt: new Date(),
    })
  );

  // Host Alice can also delete a draft if needed
  await assertSucceeds(
    aliceDb.collection("quiz_sessions").doc("session_draft_01").delete()
  );
});

test("11. Sermon Audio Chunks: Upload, filtered query, member delete blocked, executive replacement allowed", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Exec Bob",
      email: "bob@gideonites.org",
      role: "YOUTH_EXECUTIVE",
      department: "MEDIA",
      preferredLanguage: "en",
      englishOverride: false,
      xpPoints: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const memberDb = testEnv.authenticatedContext("member_789").firestore();

  await assertSucceeds(
    aliceDb.collection("sermon_audio_chunks").doc("sa_old_c0").set({
      audioCloudId: "sa_old",
      fellowshipId: "GIDEONITES",
      uploadedById: ALICE_UID,
      chunkIndex: 0,
      totalChunks: 1,
      dataBase64: "UklGRiQAAABXQVZF",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query allowed for any member
  await assertSucceeds(
    memberDb.collection("sermon_audio_chunks").where("fellowshipId", "==", "GIDEONITES").get()
  );

  // Regular member cannot delete Alice's sermon audio chunk
  await assertFails(
    memberDb.collection("sermon_audio_chunks").doc("sa_old_c0").delete()
  );

  // Youth Executive Bob CAN delete Alice's old sermon audio chunk when replacing with a new sermon
  await assertSucceeds(
    bobDb.collection("sermon_audio_chunks").doc("sa_old_c0").delete()
  );
});

test("12. Programmes Auto-Delete Rules: Active future programme protected from non-creator member; expired programme cleanup allowed", async () => {
  const now = new Date();
  const futureStart = Date.now() + 3 * 24 * 60 * 60 * 1000;
  const futureEnd = futureStart + 2 * 60 * 60 * 1000;
  const pastStart = Date.now() - 3 * 24 * 60 * 60 * 1000;
  const pastEnd = pastStart + 2 * 60 * 60 * 1000;

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("programmes").doc("prog_future_01").set({
      createdById: ALICE_UID,
      fellowshipId: "GIDEONITES",
      titleEn: "Future Youth Revival",
      descriptionEn: "Upcoming revival service",
      category: "REVIVAL",
      venue: "Main Sanctuary",
      startDateMillis: futureStart,
      endDateMillis: futureEnd,
      status: "UPCOMING",
      registeredCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("programmes").doc("prog_expired_01").set({
      createdById: ALICE_UID,
      fellowshipId: "GIDEONITES",
      titleEn: "Past Youth Seminar",
      descriptionEn: "Completed past seminar",
      category: "FELLOWSHIP",
      venue: "Youth Chapel",
      startDateMillis: pastStart,
      endDateMillis: pastEnd,
      status: "UPCOMING",
      registeredCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular member Bob cannot delete Alice's active future programme
  await assertFails(
    bobDb.collection("programmes").doc("prog_future_01").delete()
  );

  // Any signed-in member Bob CAN auto-purge an expired past-date programme
  await assertSucceeds(
    bobDb.collection("programmes").doc("prog_expired_01").delete()
  );
});



