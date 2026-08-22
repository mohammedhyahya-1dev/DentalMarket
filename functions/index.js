const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// The single admin account's uid — mirrors AuthRepository.kt's ADMIN_UID
// and firestore.rules' isAdminUid(), both of which hardcode the same value
// for the same reason (there's no list rule on users/{userId} a client
// could use to look this up, and here there's no client at all — just
// keeping the third copy in sync). Only this account's device ever
// receives this push.
const ADMIN_UID = "RsPc63uPStWdxugOKk10l51TCB63";

// Fires on identityVerifications/{uid} writes, but only the two that
// actually represent "a user submitted something for review": a brand-new
// submission (before doesn't exist) and a resubmit (status flips back to
// PENDING after being REJECTED). Does NOT fire when admin's own
// approve()/reject() actions write to this same document — those move
// status AWAY from PENDING, which the guard below excludes.
exports.notifyAdminOnVerificationSubmission = onDocumentWritten(
  "identityVerifications/{uid}",
  async (event) => {
    const after = event.data.after.exists ? event.data.after.data() : null;
    if (!after || after.status !== "PENDING") return;

    const before = event.data.before.exists ? event.data.before.data() : null;
    if (before && before.status === "PENDING") return;

    const uid = event.params.uid;

    const adminDoc = await admin.firestore().collection("users").doc(ADMIN_UID).get();
    const token = adminDoc.get("fcmToken");
    if (!token) {
      logger.info("No fcmToken registered for admin yet — skipping push", { uid });
      return;
    }

    // Deliberately generic — no submitter name, no document details. The
    // in-app sellerNotifications bell already carries the real specifics
    // once the admin actually opens the app; this is just the "something
    // needs your attention" nudge. data.uid is what lets the tapped
    // notification deep-link straight to this specific submission (see
    // MainActivity.kt's adminVerificationRouteFor).
    try {
      await admin.messaging().send({
        token,
        notification: {
          title: "New verification request",
          body: "A user submitted documents for identity verification review.",
        },
        data: {
          type: "VERIFICATION_SUBMITTED",
          uid,
        },
        // Found the hard way during live testing: Android's process
        // freezer (Android 12+ cached-app optimization) can pause a
        // backgrounded app's process entirely — and without this, a
        // normal-priority FCM message gets deferred/batched rather than
        // waking a frozen process promptly, so the notification never
        // showed up despite the server confirming a successful send().
        // high priority is what actually gets a backgrounded/killed app
        // woken up in time to display it, which is the entire point of
        // this feature per the "even with the app closed" requirement.
        android: {
          priority: "high",
        },
      });
      logger.info("Sent verification-submitted push", { uid });
    } catch (error) {
      // A stale/invalidated token is the expected failure mode here (app
      // reinstalled, notifications revoked, etc.) — logged, not retried;
      // the in-app bell notification already landed regardless of this.
      logger.error("Failed to send verification-submitted push", { uid, error: String(error) });
    }
  }
);
