// Regression suite for identityVerifications/{uid} and the admin carve-out
// on publicProfiles/{uid}.identityVerified — same "exercise the real
// firestore.rules" role as usernames.rules.test.mjs and follows.rules.test.mjs.
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

const VALID_IMAGES = {
  nationalIdFrontPath: 'identityVerifications/alice/nationalIdFront.jpg',
  nationalIdBackPath: 'identityVerifications/alice/nationalIdBack.jpg',
  dentalAssocIdFrontPath: 'identityVerifications/alice/dentalAssocIdFront.jpg',
  dentalAssocIdBackPath: 'identityVerifications/alice/dentalAssocIdBack.jpg'
};

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-dentalmarket-rules-test-identity',
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

test('1. owner can submit a PENDING verification with all 4 image paths', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('identityVerifications').doc('alice').set({
      uid: 'alice',
      status: 'PENDING',
      submittedAt: 1700000000000,
      ...VALID_IMAGES
    })
  );
});

test('2. submission missing an image path is denied', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  const { dentalAssocIdBackPath, ...incomplete } = VALID_IMAGES;
  await assertFails(
    alice.collection('identityVerifications').doc('alice').set({
      uid: 'alice',
      status: 'PENDING',
      submittedAt: 1700000000000,
      ...incomplete
    })
  );
});

test('3. submitting under a different uid than your own is denied', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('identityVerifications').doc('bob').set({
      uid: 'bob',
      status: 'PENDING',
      submittedAt: 1700000000000,
      ...VALID_IMAGES
    })
  );
});

test('4. submitting already APPROVED (self-approve attempt) is denied', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('identityVerifications').doc('alice').set({
      uid: 'alice',
      status: 'APPROVED',
      submittedAt: 1700000000000,
      ...VALID_IMAGES
    })
  );
});

test('5. owner can get their own submission, but not a stranger\'s', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('bob').set({
      uid: 'bob', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(alice.collection('identityVerifications').doc('bob').get());

  const bob = testEnv.authenticatedContext('bob').firestore();
  await assertSucceeds(bob.collection('identityVerifications').doc('bob').get());
});

test('6. admin can get and list submissions; a non-admin cannot list', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertSucceeds(admin.collection('identityVerifications').doc('alice').get());
  await assertSucceeds(admin.collection('identityVerifications').get());

  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(alice.collection('identityVerifications').get());
});

test('7. owner can resubmit (REJECTED -> PENDING) with fresh images', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice',
      status: 'REJECTED',
      submittedAt: 1700000000000,
      reviewedAt: 1700000100000,
      rejectionReason: 'Blurry photo',
      ...VALID_IMAGES
    });
  });
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('identityVerifications').doc('alice').set({
      status: 'PENDING',
      submittedAt: 1700000200000,
      ...VALID_IMAGES
    }, { merge: true })
  );
});

test('8. owner cannot resubmit while still PENDING (already awaiting review)', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('identityVerifications').doc('alice').set({
      status: 'PENDING',
      submittedAt: 1700000200000,
      ...VALID_IMAGES
    }, { merge: true })
  );
});

test('9. owner cannot approve their own submission (self-approve via update)', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('identityVerifications').doc('alice').update({ status: 'APPROVED' })
  );
});

test('10. admin can approve a PENDING submission, touching only status/reviewedAt', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertSucceeds(
    admin.collection('identityVerifications').doc('alice').update({
      status: 'APPROVED',
      reviewedAt: 1700000300000
    })
  );
});

test('11. admin can reject a PENDING submission with a reason', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertSucceeds(
    admin.collection('identityVerifications').doc('alice').update({
      status: 'REJECTED',
      reviewedAt: 1700000300000,
      rejectionReason: 'Blurry photo'
    })
  );
});

test('12. admin cannot swap in different image paths while reviewing', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'PENDING', submittedAt: 1700000000000, ...VALID_IMAGES
    });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertFails(
    admin.collection('identityVerifications').doc('alice').update({
      status: 'APPROVED',
      nationalIdFrontPath: 'identityVerifications/alice/swapped.jpg'
    })
  );
});

test('13. admin cannot re-approve/modify an already-APPROVED submission', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('identityVerifications').doc('alice').set({
      uid: 'alice', status: 'APPROVED', submittedAt: 1700000000000,
      reviewedAt: 1700000300000, ...VALID_IMAGES
    });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertFails(
    admin.collection('identityVerifications').doc('alice').update({ status: 'REJECTED' })
  );
});

// --- publicProfiles/{uid}.identityVerified admin carve-out ---

test('14. admin CAN set identityVerified=true on a stranger\'s publicProfiles doc', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('aliceuser').set({ uid: 'alice' });
    await ctx.firestore().collection('publicProfiles').doc('alice').set({ username: 'aliceuser' });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertSucceeds(
    admin.collection('publicProfiles').doc('alice').set({ identityVerified: true }, { merge: true })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const doc = await ctx.firestore().collection('publicProfiles').doc('alice').get();
    assert.equal(doc.data().identityVerified, true);
    assert.equal(doc.data().username, 'aliceuser');
  });
});

test('15. admin CANNOT touch any other field while setting identityVerified', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('aliceuser').set({ uid: 'alice' });
    await ctx.firestore().collection('publicProfiles').doc('alice').set({ username: 'aliceuser' });
  });
  const admin = testEnv.authenticatedContext('admin', { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertFails(
    admin.collection('publicProfiles').doc('alice')
      .set({ identityVerified: true, name: 'Hijacked' }, { merge: true })
  );
});

// The regression this whole carve-out exists to prevent: without the
// hasOnly() restriction just added to the OWNER's own branch, a routine
// username/name/createdAt/emailVerified write could smuggle
// identityVerified=true into the same document as an "extra field" — same
// mechanism test 17/18/20 (usernames.rules.test.mjs) already proved works
// for genuinely allowed extra fields.
test('16. owner CANNOT self-approve by smuggling identityVerified into their own username write', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('usernames').doc('smuggletest').set({ uid: 'alice' })
  );
  await assertFails(
    alice.collection('publicProfiles').doc('alice')
      .set({ username: 'smuggletest', identityVerified: true })
  );
});

test('17. owner still CAN write their normal fields (username/name/createdAt/emailVerified) after the hasOnly restriction', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('usernames').doc('normalwrite').set({ uid: 'alice' })
  );
  await assertSucceeds(
    alice.collection('publicProfiles').doc('alice').set({
      username: 'normalwrite',
      name: 'Alice Example',
      createdAt: 1700000000000,
      emailVerified: true
    })
  );
});

test('18. owner can still update username/name after admin has already set identityVerified (no clobber either direction)', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('usernames').doc('originalname').set({ uid: 'alice' });
    await ctx.firestore().collection('publicProfiles').doc('alice').set({
      username: 'originalname', name: 'Alice Example', identityVerified: true
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
    assert.equal(doc.data().identityVerified, true);
  });
});

// --- sellerNotifications VERIFICATION_SUBMITTED (bell/badge trigger) ---
// isAdminUid() is hardcoded to the real admin account's uid (matches
// AuthRepository.kt's ADMIN_UID) rather than an email allowlist like
// isAdmin() elsewhere in this file — a submitting user has no way to look
// up "the admin's uid" via a list query (users/{userId} has no list rule),
// so the notification's recipientId has to be checked against that same
// fixed value directly.
const ADMIN_UID = 'RsPc63uPStWdxugOKk10l51TCB63';

test('19. a signed-in user can notify the admin that they submitted identity verification', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertSucceeds(
    alice.collection('sellerNotifications').add({
      recipientId: ADMIN_UID,
      listingName: 'Identity Verification Submitted',
      buyerName: 'Alice Example (@aliceuser)',
      type: 'VERIFICATION_SUBMITTED'
    })
  );
});

test('20. a VERIFICATION_SUBMITTED notification cannot target a non-admin uid (spam prevention)', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(
    alice.collection('sellerNotifications').add({
      recipientId: 'bob',
      listingName: 'Identity Verification Submitted',
      buyerName: 'Alice Example (@aliceuser)',
      type: 'VERIFICATION_SUBMITTED'
    })
  );
});

test('21. only the admin (recipient) can read a VERIFICATION_SUBMITTED notification', async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection('sellerNotifications').doc('notif1').set({
      recipientId: ADMIN_UID,
      listingName: 'Identity Verification Submitted',
      buyerName: 'Alice Example (@aliceuser)',
      type: 'VERIFICATION_SUBMITTED',
      read: false
    });
  });
  const alice = testEnv.authenticatedContext('alice').firestore();
  await assertFails(alice.collection('sellerNotifications').doc('notif1').get());

  const admin = testEnv.authenticatedContext(ADMIN_UID, { email: 'p.mohammed.h.yahya@gmail.com' }).firestore();
  await assertSucceeds(admin.collection('sellerNotifications').doc('notif1').get());
});
