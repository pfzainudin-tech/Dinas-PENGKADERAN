const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_123";
const MEMBER_UID = "member_456";
const OTHER_UID = "other_789";
const ADMIN_DIGEST = "1fbc93077e21e0f32bb1e6a41de3c018ab7457498f0965c9479a2984c3d71606";

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

function buildValidSchedule(creatorUid) {
  const now = new Date(Date.now() - 2000);
  return {
    organizationId: "pc_mojokerto",
    title: "MAKESTA Raya PAC Mojoanyar",
    programType: "MAKESTA",
    executorLevel: "PAC",
    executorName: "PAC IPNU IPPNU Mojoanyar",
    location: "MI Sunan Kalijaga Mojoanyar",
    description: "Pengkaderan tingkat dasar untuk pelajar NU Mojoanyar.",
    startDate: "2026-11-22",
    endDate: "2026-11-24",
    timeText: "08:00 WIB - Selesai",
    year: 2026,
    month: 11,
    completed: false,
    createdBy: creatorUid,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("schedules").where("organizationId", "==", "pc_mojokerto").get()
  );
  await assertFails(unauthDb.collection("users").doc(MEMBER_UID).get());
  await assertFails(
    unauthDb.collection("schedules").doc("sched_1").set(buildValidSchedule(ADMIN_UID))
  );
  await assertFails(
    unauthDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "admin",
      verificationDigest: ADMIN_DIGEST,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile PII isolation and partial update rules", async () => {
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    memberDb.collection("users").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      fullName: "Ahmad Fauzi",
      unitOrigin: "PAC Bangsal",
      email: "ahmad@example.com",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update allowed fields
  await assertSucceeds(
    memberDb.collection("users").doc(MEMBER_UID).update({
      fullName: "Ahmad Fauzi S.Pd",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & immutable createdAt mutation
  await assertFails(
    memberDb.collection("users").doc(MEMBER_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    memberDb.collection("users").doc(MEMBER_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user PII read or write
  await assertFails(otherDb.collection("users").doc(MEMBER_UID).get());
  await assertFails(
    otherDb.collection("users").doc(MEMBER_UID).update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Admin security digest verification prevents unauthorized privilege escalation", async () => {
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Wrong digest fails
  await assertFails(
    memberDb.collection("admins").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      role: "admin",
      verificationDigest: "0".repeat(64),
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Valid digest succeeds for own UID
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "admin",
      verificationDigest: ADMIN_DIGEST,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Cannot create admin doc for another UID
  await assertFails(
    memberDb.collection("admins").doc(OTHER_UID).set({
      userId: OTHER_UID,
      role: "admin",
      verificationDigest: ADMIN_DIGEST,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Schedules: only Admin can create/update/delete; all signed-in members can query filtered list", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();

  // Provision admin session
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "admin",
      verificationDigest: ADMIN_DIGEST,
      createdAt: new Date(Date.now() - 3000),
    })
  );

  // Non-admin member cannot create a schedule
  await assertFails(
    memberDb.collection("schedules").doc("sched_1").set(buildValidSchedule(MEMBER_UID))
  );

  // Admin creates schedule (NO @DocumentId 'id' field in document body)
  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_1").set(buildValidSchedule(ADMIN_UID))
  );

  // Including 'id' inside body fails hasOnly check
  const invalidWithId = { ...buildValidSchedule(ADMIN_UID), id: "sched_2" };
  await assertFails(adminDb.collection("schedules").doc("sched_2").set(invalidWithId));

  // Unfiltered schedules query fails; organization-scoped query succeeds
  await assertFails(memberDb.collection("schedules").get());
  await assertSucceeds(
    memberDb.collection("schedules").where("organizationId", "==", "pc_mojokerto").get()
  );

  // Admin can update description & mark completed checkbox anytime
  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_1").update({
      description: "Deskripsi diperbarui oleh Admin.",
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Regular member cannot mark completed or modify schedule
  await assertFails(
    memberDb.collection("schedules").doc("sched_1").update({
      completed: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Member attendance (IKUT / TIDAK) and user participations subcollection", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await adminDb.collection("admins").doc(ADMIN_UID).set({
    userId: ADMIN_UID,
    role: "admin",
    verificationDigest: ADMIN_DIGEST,
    createdAt: new Date(Date.now() - 4000),
  });
  await adminDb.collection("schedules").doc("sched_1").set(buildValidSchedule(ADMIN_UID));

  const now = new Date(Date.now() - 2000);

  // Member marks IKUT on schedule attendances & own participations
  await assertSucceeds(
    memberDb
      .collection("schedules")
      .doc("sched_1")
      .collection("attendances")
      .doc(MEMBER_UID)
      .set({
        userId: MEMBER_UID,
        scheduleId: "sched_1",
        memberName: "Ahmad Fauzi",
        status: "IKUT",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    memberDb
      .collection("users")
      .doc(MEMBER_UID)
      .collection("participations")
      .doc("sched_1")
      .set({
        userId: MEMBER_UID,
        scheduleId: "sched_1",
        status: "IKUT",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Member toggles to TIDAK
  await assertSucceeds(
    memberDb
      .collection("schedules")
      .doc("sched_1")
      .collection("attendances")
      .doc(MEMBER_UID)
      .update({
        status: "TIDAK",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Invalid status rejected
  await assertFails(
    memberDb
      .collection("schedules")
      .doc("sched_1")
      .collection("attendances")
      .doc(MEMBER_UID)
      .update({
        status: "MAYBE",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Filtered attendance list succeeds for signed-in users; cross-user overwrite fails
  await assertSucceeds(
    otherDb
      .collection("schedules")
      .doc("sched_1")
      .collection("attendances")
      .where("scheduleId", "==", "sched_1")
      .get()
  );
  await assertFails(
    otherDb
      .collection("schedules")
      .doc("sched_1")
      .collection("attendances")
      .doc(MEMBER_UID)
      .update({
        status: "IKUT",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // User-scoped participations list succeeds for owner and fails for other user
  await assertSucceeds(
    memberDb.collection("users").doc(MEMBER_UID).collection("participations").get()
  );
  await assertFails(
    otherDb.collection("users").doc(MEMBER_UID).collection("participations").get()
  );
});
