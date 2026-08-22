// Regression suite for storage.rules' identityVerifications/{uid}/{fileName}
// path — the most security-sensitive rule set in this app (private-only
// images), so this is exercised directly against the real Storage emulator
// rather than trusted by inspection.
//
// Does NOT test "owner can't overwrite an already-APPROVED submission" —
// that check existed in an earlier version of storage.rules (using
// firestore.get()/exists()) and worked in this same local emulator, but the
// very first real upload against this project's live Storage bucket in
// production failed with a flat 403 until those calls were removed. See
// storage.rules' own comment for the full story. What's left here is
// exactly what production actually runs.
//
// Run with: npm test (from this directory), against a running
// `firebase emulators:start --only storage` — no firestore emulator needed
// anymore, since these rules never call firestore.get()/exists().

import { test, before, after } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';

let testEnv;

const SMALL_IMAGE = new Uint8Array(1024).fill(1);
const IMAGE_META = { contentType: 'image/jpeg' };

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'dentalmarket-abdf6',
    storage: {
      rules: readFileSync('../storage.rules', 'utf8'),
      host: 'localhost',
      port: 9199
    }
  });
});

after(async () => {
  await testEnv.cleanup();
});

test('1. owner can upload their own identity-verification image', async () => {
  const alice = testEnv.authenticatedContext('alice').storage();
  await assertSucceeds(
    alice.ref('identityVerifications/alice/nationalIdFront.jpg').put(SMALL_IMAGE, IMAGE_META)
  );
});

test('2. a different signed-in user cannot upload into someone else\'s path', async () => {
  const bob = testEnv.authenticatedContext('bob').storage();
  await assertFails(
    bob.ref('identityVerifications/alice/nationalIdFront.jpg').put(SMALL_IMAGE, IMAGE_META)
  );
});

test('3. owner can read their own images; a different signed-in user cannot', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.storage().ref('identityVerifications/alice/nationalIdFront.jpg').put(SMALL_IMAGE, IMAGE_META);
  });
  const alice = testEnv.authenticatedContext('alice').storage();
  await assertSucceeds(alice.ref('identityVerifications/alice/nationalIdFront.jpg').getDownloadURL());

  const bob = testEnv.authenticatedContext('bob').storage();
  await assertFails(bob.ref('identityVerifications/alice/nationalIdFront.jpg').getDownloadURL());
});

test('4. admin can read anyone\'s images', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.storage().ref('identityVerifications/alice/nationalIdFront.jpg').put(SMALL_IMAGE, IMAGE_META);
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).storage();
  await assertSucceeds(admin.ref('identityVerifications/alice/nationalIdFront.jpg').getDownloadURL());
});

test('5. an unauthenticated request cannot read or write', async () => {
  const anon = testEnv.unauthenticatedContext().storage();
  await assertFails(anon.ref('identityVerifications/alice/nationalIdFront.jpg').getDownloadURL());
  await assertFails(
    anon.ref('identityVerifications/alice/nationalIdFront.jpg').put(SMALL_IMAGE, IMAGE_META)
  );
});

test('6. a non-image content type is rejected', async () => {
  const alice = testEnv.authenticatedContext('alice').storage();
  await assertFails(
    alice.ref('identityVerifications/alice/nationalIdFront.jpg')
      .put(SMALL_IMAGE, { contentType: 'application/pdf' })
  );
});

test('7. an oversized upload is rejected', async () => {
  const big = new Uint8Array(4 * 1024 * 1024).fill(1);
  const alice = testEnv.authenticatedContext('alice').storage();
  await assertFails(
    alice.ref('identityVerifications/alice/nationalIdFront.jpg').put(big, IMAGE_META)
  );
});
