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
