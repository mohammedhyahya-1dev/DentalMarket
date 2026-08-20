// Regression suite for the follows/{docId} rule added alongside
// SellerProfileScreen's Follow button — same "lock in the security
// behavior we actually decided on" role as OrderViewModelTest's dispute-
// blocking cases and ContactInfoFilterTest, except those test pure Kotlin
// mirrors of rules logic; this runs the real firestore.rules against the
// Firestore emulator, so it catches drift those two can't.
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
    // Own project id per test file — Node's test runner runs files
    // concurrently by default, and every file's beforeEach calls
    // clearFirestore(); sharing one project id let one file's clear wipe
    // data another file's test had just seeded mid-run.
    projectId: 'demo-dentalmarket-rules-test-follows',
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

test('1. self-follow (followerId == followingId) is denied', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('follows').doc('alice_alice').set({
      followerId: 'alice',
      followingId: 'alice',
      createdAt: Date.now()
    })
  );
});

test('2. following a different seller, followerId == own uid, is allowed', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('follows').doc('alice_bob').set({
      followerId: 'alice',
      followingId: 'bob',
      createdAt: Date.now()
    })
  );
});

test('3. unauthenticated create is denied', async () => {
  const anon = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    anon.collection('follows').doc('mallory_bob').set({
      followerId: 'mallory',
      followingId: 'bob',
      createdAt: Date.now()
    })
  );
});

test("4. deleting another user's follow doc is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('follows').doc('bob_charlie').set({
      followerId: 'bob',
      followingId: 'charlie',
      createdAt: Date.now()
    });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(alice.collection('follows').doc('bob_charlie').delete());
});

test('5. deleting your own follow doc is allowed', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('follows').doc('alice_dave').set({
      followerId: 'alice',
      followingId: 'dave',
      createdAt: Date.now()
    });
  });

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(alice.collection('follows').doc('alice_dave').delete());
});
