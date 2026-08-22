// Regression suite for the usernames/{name} and publicProfiles/{uid} rules
// added alongside the username feature — same role as follows.rules.test.mjs:
// exercises the real firestore.rules against the emulator, not a Kotlin
// mirror of the logic. In particular this locks in the format/reserved-word
// backstop (isValidUsername/isReservedUsername in firestore.rules) added
// specifically because AuthRepository's matching Kotlin checks are real UX
// but not a security boundary — a raw write bypassing the app entirely was
// able to claim usernames/admin before that fix.
//
// Run with: npm test (from this directory), against a running
// `firebase emulators:start --only firestore` — or wrap both in one shot
// with `firebase emulators:exec --only firestore "npm test"` from the repo
// root's firestore-tests directory.

import { test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    // Own project id per test file — see follows.rules.test.mjs's matching
    // comment; Node's test runner runs files concurrently by default, and
    // sharing one project id let one file's clearFirestore() wipe data
    // another file's test had just seeded mid-run.
    projectId: 'demo-dentalmarket-rules-test-usernames',
    firestore: {
      rules: readFileSync('../firestore.rules', 'utf8'),
      host: 'localhost',
      port: 8080
    }
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

test('1. claiming an available, valid name succeeds', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('usernames').doc('alice_dds').set({ uid: 'alice' })
  );
});

test('2. claiming a name someone else already owns fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('taken').set({ uid: 'bob' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('usernames').doc('taken').set({ uid: 'alice' })
  );
});

test('3. claiming a reserved word fails', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('usernames').doc('admin').set({ uid: 'alice' })
  );
});

test('4. claiming a malformed name fails (starts with a digit)', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('usernames').doc('1alice').set({ uid: 'alice' })
  );
});

test("5. a user can free their own old reservation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('alice_old').set({ uid: 'alice' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(alice.collection('usernames').doc('alice_old').delete());
});

test("6. a user cannot free someone else's reservation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('bob_name').set({ uid: 'bob' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(alice.collection('usernames').doc('bob_name').delete());
});

test('7. publicProfiles read is open to any signed-in user', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('publicProfiles').doc('bob').set({ username: 'bobsmith' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(alice.collection('publicProfiles').doc('bob').get());
});

test('8. publicProfiles write is owner-only', async () => {
  // bob's own write now also needs a real reservation behind it (see tests
  // 13-15 below) — seeded here so this test stays focused on ownership,
  // not uniqueness.
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('bobsmith').set({ uid: 'bob' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  const bob = testEnv.authenticatedContext('bob').firestore();

  await assertFails(
    alice.collection('publicProfiles').doc('bob').set({ username: 'hijacked' })
  );
  await assertSucceeds(
    bob.collection('publicProfiles').doc('bob').set({ username: 'bobsmith' })
  );
});

// The gap that actually mattered: publicProfiles/{uid}.username is what
// SellerProfileScreen shows OTHER users — unlike users/{uid}.username,
// which only the owner ever sees — so a raw write straight to this field,
// skipping the usernames/ reservation collection entirely, is the real
// public-facing bypass the reserved-word rule exists to close.
test('9. claiming publicProfiles/{uid}.username = "admin" directly (bypassing usernames/) fails', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('publicProfiles').doc('alice').set({ username: 'admin' })
  );
});

test('10. setting users/{uid}.username = "admin" directly (bypassing usernames/) fails', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('users').doc('alice').set(
      { uid: 'alice', name: 'Alice', email: 'a@example.com', username: 'admin' }
    )
  );
});

// Regression check for the fix above: signUp()'s very first users/{uid}
// write always includes username: "" (DentalUser's default, serialized as
// part of the full object) — isValidOrBlankUsername has to let that through
// or account creation itself would break.
test('11. creating users/{uid} with a blank username (pre-ensureUsername) still succeeds', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('users').doc('alice').set(
      { uid: 'alice', name: 'Alice', email: 'a@example.com', username: '' }
    )
  );
});

// Locks in the casing decision: isValidUsername's pattern is [a-zA-Z], not
// [a-z], so mixed-case format validation must never regress into rejecting
// "JohnDoe" — and the usernames/ reservation is keyed by the *lowercased*
// form (see AuthRepository.normalizeUsername), so "JohnDoe" and "johndoe"
// have to collide on the same doc id even though publicProfiles/users store
// the original casing as typed.
test('12. mixed-case "JohnDoe" is a valid claim, and blocks "johndoe" afterward', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('usernames').doc('johndoe').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice').set({ username: 'JohnDoe' })
  );

  const bob = testEnv.authenticatedContext('bob').firestore();
  await assertFails(
    bob.collection('usernames').doc('johndoe').set({ uid: 'bob' })
  );
});

// The exact collision this whole isOwnedUsername check exists to close (see
// firestore.rules' own comment on the function) — confirmed live against a
// real emulator before the fix: two different, unrelated uids could each set
// publicProfiles/{uid}.username to the identical value with zero
// usernames/{name} reservation behind either one. Both must now be denied.
test('13. publicProfiles/{uid}.username with NO matching reservation is now denied', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('publicProfiles').doc('alice').set({ username: 'testcase123' })
  );
});

test('14. the exact two-user collision from before the fix: both now denied, neither wins', async () => {
  const a = testEnv.authenticatedContext('userA').firestore();
  const b = testEnv.authenticatedContext('userB').firestore();

  await assertFails(
    a.collection('publicProfiles').doc('userA').set({ username: 'testcase123' })
  );
  await assertFails(
    b.collection('publicProfiles').doc('userB').set({ username: 'testcase123' })
  );
});

// Same gap, same fix, on users/{uid}.username — isValidOrBlankUsername's
// non-blank branch now requires isOwnedUsername too.
test('15. the exact two-user collision on users/{uid}.username: both now denied', async () => {
  const a = testEnv.authenticatedContext('userA').firestore();
  const b = testEnv.authenticatedContext('userB').firestore();

  await assertFails(
    a.collection('users').doc('userA').set(
      { uid: 'userA', name: 'A', email: 'a@x.com', username: 'testcase456' }
    )
  );
  await assertFails(
    b.collection('users').doc('userB').set(
      { uid: 'userB', name: 'B', email: 'b@x.com', username: 'testcase456' }
    )
  );
});

// The legitimate two-step flow still has to work: claim the reservation as
// its own, separately-committed write (step A — mirrors
// AuthRepository.claimUsernameReservation), THEN the profile write (step B
// — mirrors writeUsernameToProfile) succeeds because isOwnedUsername now
// finds the already-committed reservation.
test('16. legitimate two-step claim-then-write still succeeds for both users and publicProfiles', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();

  await assertSucceeds(
    alice.collection('usernames').doc('legituser').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('users').doc('alice').set(
      { uid: 'alice', name: 'Alice', email: 'a@example.com', username: 'legituser' },
      { merge: true }
    )
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice').set({ username: 'legituser' })
  );
});

// Proves publicProfiles/{uid}'s rule needs no change for
// writeUsernameToProfile to also write `name` alongside `username` (see
// AuthRepository.kt) — the rule only checks ownership and validates the
// username field specifically, with no affectedKeys()/field-restriction
// clause, so an extra field on the same write was never going to be
// rejected. This is the direct proof rather than just inspection.
test('17. publicProfiles create/update with an extra name field, alongside a validly-owned username, still succeeds', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();

  await assertSucceeds(
    alice.collection('usernames').doc('nametest').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice').set({ username: 'nametest', name: 'Alice Example' })
  );
});

// completeProfile()'s new publicProfiles.name sync (AuthRepository.kt) is a
// merge into a doc that ensureUsername() USUALLY already created with a
// real, owned username by the time a user submits the profile form — this
// is that normal case, proving the merge succeeds and leaves username
// untouched.
test('18. completeProfile-style name-only merge succeeds when publicProfiles already has an owned username', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('racewinner').set({ uid: 'alice' });
    await ctx.firestore().collection('publicProfiles').doc('alice').set({ username: 'racewinner' });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice').set({ name: 'Alice Example' }, { merge: true })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const doc = await ctx.firestore().collection('publicProfiles').doc('alice').get();
    assert.equal(doc.data().username, 'racewinner');
    assert.equal(doc.data().name, 'Alice Example');
  });
});

// The race this justifies AuthRepository.completeProfile()'s best-effort
// (try/catch-and-ignore) handling for: ensureUsername() hasn't won yet, so
// publicProfiles/{uid} doesn't exist at all — a name-only merge becomes a
// create with no username field in the resulting data, which
// isValidUsername(request.resource.data.username) can't evaluate, so it's
// denied. Confirms the failure this code path has to tolerate is real, not
// a hypothetical worth over-engineering for.
test("19. completeProfile-style name-only merge is denied when publicProfiles doesn't exist yet (the race case)", async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('publicProfiles').doc('alice').set({ name: 'Alice Example' }, { merge: true })
  );
});

// Same proof as test 17, for the createdAt/emailVerified fields
// writeUsernameToProfile() and isEmailVerifiedFresh() now also write onto
// publicProfiles/{uid} (AuthRepository.kt) — the rule still only checks
// ownership and validates username specifically, so these two extra fields
// on the same write were never going to be rejected either.
test('20. publicProfiles create/update with extra createdAt/emailVerified fields, alongside a validly-owned username, still succeeds', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();

  await assertSucceeds(
    alice.collection('usernames').doc('createdattest').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice')
      .set({ username: 'createdattest', createdAt: 1700000000000, emailVerified: true })
  );
});

// Pins down the SetOptions.merge() fix on writeUsernameToProfile()'s
// publicProfiles write: before that fix, this line was a plain .set() with
// no merge, which would have silently wiped createdAt/emailVerified on
// every username change. This test proves the merge preserves them across
// exactly that kind of write — it would fail if that .set() ever regresses
// back to a full overwrite.
test('21. a writeUsernameToProfile-style merge preserves createdAt/emailVerified across a username change', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('originalname').set({ uid: 'alice' });
    await ctx.firestore().collection('publicProfiles').doc('alice').set({
      username: 'originalname',
      name: 'Alice Example',
      createdAt: 1700000000000,
      emailVerified: true
    });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('usernames').doc('newname').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice')
      .set({ username: 'newname', name: 'Alice Example' }, { merge: true })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const doc = await ctx.firestore().collection('publicProfiles').doc('alice').get();
    assert.equal(doc.data().username, 'newname');
    assert.equal(doc.data().createdAt, 1700000000000);
    assert.equal(doc.data().emailVerified, true);
  });
});

// The push-notification feature's token storage (AuthRepository.updateFcmToken/
// registerFcmToken, IdentityVerificationMessagingService.onNewToken) needed
// no rules CHANGE — users/{userId}'s existing self-write rule already
// covers an arbitrary new field like fcmToken with no extra work. This is
// the direct proof of that claim, same "don't just assert it, test it"
// posture as test 17's own comment.
test('22. a signed-in user can write their own fcmToken, but not another uid\'s', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('users').doc('alice').set(
      { uid: 'alice', name: 'Alice', email: 'a@example.com', username: '', fcmToken: 'token-abc' },
      { merge: true }
    )
  );
  await assertFails(
    alice.collection('users').doc('bob').set(
      { fcmToken: 'hijacked-token' }, { merge: true }
    )
  );
});
