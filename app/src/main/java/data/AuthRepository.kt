package com.dentalmarket.app.data

import com.dentalmarket.app.model.DentalUser
import com.dentalmarket.app.model.PublicSellerProfile
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.SetOptions

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val adminEmails = listOf("p.mohammed.h.yahya@gmail.com")

    private val usernameReservedWords = setOf(
        "admin", "administrator", "support", "dentalmarket", "moderator", "official"
    )

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAdmin: Boolean
        get() = auth.currentUser?.email in adminEmails

    val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous == true

    // Reflects Firebase's on-device cached copy. Call reloadUser() first if
    // you need the freshest value (e.g. right after the user might have
    // clicked the link in their inbox).
    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified ?: false

    // For gates that act on the result (Sell, Checkout) rather than just
    // displaying it (Profile). reload() alone refreshes the cached
    // FirebaseUser so isEmailVerified is accurate, but firestore.rules
    // reads request.auth.token.email_verified from the ID token, which
    // isn't reissued by reload() and can otherwise lag up to ~an hour
    // behind — getIdToken(true) forces that reissue too, so the write that
    // follows this check actually carries the fresh claim.
    suspend fun isEmailVerifiedFresh(): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.success(false)
            user.reload().awaitResult()
            user.getIdToken(true).awaitResult()
            val verified = user.isEmailVerified
            // Best-effort, one-way sync onto the public copy SellerProfileScreen
            // reads for other users — only ever written true, never false.
            // Real verification can't revert, so there's no legitimate case for
            // unsetting it, and this keeps a rare transient reload/token glitch
            // from ever regressing an already-true badge. Same
            // try/catch-and-ignore posture as completeProfile()'s name sync
            // below: this doc may not exist yet (no owned username), which is
            // a real, tolerated race, not worth blocking this call over.
            if (verified) {
                try {
                    firestore.collection("publicProfiles").document(user.uid)
                        .set(mapOf("emailVerified" to true), SetOptions.merge())
                        .awaitResult()
                } catch (e: Exception) {
                    // Ignored — see comment above.
                }
            }
            Result.success(verified)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).awaitResult()
            val uid = authResult.user?.uid
                ?: throw Exception("Account created but no user ID was returned")

            val user = DentalUser(uid = uid, name = name, email = email)
            firestore.collection("users").document(uid).set(user).awaitResult()
            ensureUsername()

            // Fire the verification email right away so it's waiting in
            // their inbox by the time they check.
            authResult.user?.sendEmailVerification()?.awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<Unit> {
        return try {
            auth.signInAnonymously().awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Result carries whether the session after this call is the SAME account
    // as before (true only for the link-succeeded branch, which preserves the
    // guest's uid) vs a different or brand-new one (false) — callers use this
    // to decide whether an in-progress guest cart should survive the call.
    suspend fun logInOrSignUp(email: String, password: String): Result<Boolean> {
        val guestUser = auth.currentUser?.takeIf { it.isAnonymous }
        if (guestUser != null) {
            return try {
                val credential = EmailAuthProvider.getCredential(email, password)
                val authResult = guestUser.linkWithCredential(credential).awaitResult()
                val linkedUser = authResult.user
                    ?: throw Exception("Account created but no user ID was returned")

                // Same new-account policy as a normal signup: only now do we
                // enforce strong passwords, since this really is a brand-new
                // email (linking would have thrown a collision otherwise).
                if (!isPasswordStrong(password)) {
                    linkedUser.delete().awaitResult()
                    Result.failure(
                        Exception(
                            "Please choose a stronger password (8+ characters, with an uppercase letter, a number, and a special character)."
                        )
                    )
                } else {
                    val user = DentalUser(uid = linkedUser.uid, name = "", email = email)
                    firestore.collection("users").document(linkedUser.uid).set(user).awaitResult()
                    ensureUsername()
                    linkedUser.sendEmailVerification().awaitResult()
                    Result.success(true)
                }
            } catch (collisionError: FirebaseAuthUserCollisionException) {
                // This email already belongs to a real account — sign into
                // that one instead. The guest's anonymous UID is abandoned,
                // but guests can never have created any data under it (every
                // restricted action is gated in the UI), so nothing is lost.
                try {
                    auth.signInWithEmailAndPassword(email, password).awaitResult()
                    Result.success(false)
                } catch (signInError: Exception) {
                    Result.failure(Exception("Incorrect password. Please try again."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        return try {
            auth.signInWithEmailAndPassword(email, password).awaitResult()
            Result.success(false)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).awaitResult()
                val newUser = authResult.user
                    ?: throw Exception("Account created but no user ID was returned")

                // This really was a brand-new email (Firebase didn't reject it as
                // already-in-use), so this is genuinely a sign-up \u2014 only now do we
                // enforce strong-password rules. Existing accounts logging in never
                // reach this branch, so their older, weaker passwords still work.
                if (!isPasswordStrong(password)) {
                    newUser.delete().awaitResult()
                    Result.failure(
                        Exception(
                            "Please choose a stronger password (8+ characters, with an uppercase letter, a number, and a special character)."
                        )
                    )
                } else {
                    val user = DentalUser(uid = newUser.uid, name = "", email = email)
                    firestore.collection("users").document(newUser.uid).set(user).awaitResult()
                    ensureUsername()
                    newUser.sendEmailVerification().awaitResult()
                    Result.success(false)
                }
            } catch (collisionError: FirebaseAuthUserCollisionException) {
                Result.failure(Exception("Incorrect password. Please try again."))
            } catch (signUpError: Exception) {
                Result.failure(signUpError)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasNumber = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasMinLength && hasUppercase && hasNumber && hasSpecialChar
    }

    // Same sameAccount-flag contract as logInOrSignUp above.
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Boolean> {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val guestUser = auth.currentUser?.takeIf { it.isAnonymous }

        return try {
            val authResult = if (guestUser != null) {
                guestUser.linkWithCredential(firebaseCredential).awaitResult()
            } else {
                auth.signInWithCredential(firebaseCredential).awaitResult()
            }
            val user = authResult.user ?: throw Exception("Signed in but no user was returned")

            // First time this Google account signs in, create their profile doc —
            // same as email/password sign-up does. Skip it if they've signed in before.
            val existingDoc = firestore.collection("users").document(user.uid).get().awaitResult()
            if (!existingDoc.exists()) {
                val newUser = DentalUser(
                    uid = user.uid,
                    name = user.displayName ?: "",
                    email = user.email ?: ""
                )
                firestore.collection("users").document(user.uid).set(newUser).awaitResult()
                ensureUsername()
            }

            // Reaching here with guestUser != null means linkWithCredential
            // above succeeded (no collision thrown) — same uid preserved.
            Result.success(guestUser != null)
        } catch (e: FirebaseAuthUserCollisionException) {
            if (guestUser != null) {
                // This Google account already exists under a different UID —
                // sign into that one instead. The guest's anonymous UID is
                // abandoned, same trade-off as the email/password path.
                try {
                    val authResult = auth.signInWithCredential(firebaseCredential).awaitResult()
                    val user = authResult.user ?: throw Exception("Signed in but no user was returned")
                    val existingDoc = firestore.collection("users").document(user.uid).get().awaitResult()
                    if (!existingDoc.exists()) {
                        val newUser = DentalUser(
                            uid = user.uid,
                            name = user.displayName ?: "",
                            email = user.email ?: ""
                        )
                        firestore.collection("users").document(user.uid).set(newUser).awaitResult()
                    }
                    Result.success(false)
                } catch (signInError: Exception) {
                    Result.failure(signInError)
                }
            } else {
                Result.failure(Exception("An account already exists with this email. Please log in with your email and password instead."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<DentalUser?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(null)
            val doc = firestore.collection("users").document(uid).get().awaitResult()
            Result.success(doc.toObject(DentalUser::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeProfile(
        title: String,
        firstName: String,
        lastName: String,
        specialty: String,
        province: String,
        mobile: String,
        extraMobile: String
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val email = auth.currentUser?.email ?: ""
            val displayName = "$title $firstName $lastName".trim()
            val updates = mapOf(
                "uid" to uid,
                "email" to email,
                "title" to title,
                "firstName" to firstName,
                "lastName" to lastName,
                "specialty" to specialty,
                "province" to province,
                "mobile" to mobile,
                "extraMobile" to extraMobile,
                "name" to displayName,
                "profileComplete" to true
            )
            firestore.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .awaitResult()

            // Best-effort, not part of the Result this function returns:
            // ensureUsername() (started fire-and-forget from authGate,
            // before this screen is even reached) usually wins the race and
            // already has publicProfiles/{uid} holding a real, owned
            // username by the time a user submits this form — but that's
            // not guaranteed, and if it hasn't, this doc doesn't exist yet
            // and publicProfiles' rule (which requires a validly-owned
            // username on the resulting write) denies a name-only merge
            // into it. That's a real, confirmed case (see
            // firestore-tests/usernames.rules.test.mjs), not a hypothetical
            // — letting it fail the whole completeProfile() call would
            // block onboarding over a cosmetic sync. publicProfiles.name
            // just stays blank until the username is next set/changed
            // instead, same fallback SellerProfileScreen already has.
            try {
                firestore.collection("publicProfiles").document(uid)
                    .set(mapOf("name" to displayName), SetOptions.merge())
                    .awaitResult()
            } catch (e: Exception) {
                // Ignored — see comment above.
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeUsername(username: String): String = username.trim().lowercase()

    // "3-20 chars, must start with a letter, otherwise letters/digits/underscore
    // only" — checked client-side for UX (same posture as isPasswordStrong
    // above, which is also never enforced in firestore.rules); the part that
    // actually needs server-side enforcement is uniqueness, which the
    // usernames/{name} reservation collection's rules handle instead.
    private fun isValidUsernameFormat(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]{2,19}$"))
    }

    private fun isUsernameAllowed(username: String): Boolean {
        return isValidUsernameFormat(username) &&
            normalizeUsername(username) !in usernameReservedWords
    }

    // Claims usernames/{candidate} via .set() — same convention every other
    // reservation collection in this file uses (follows/reports/watchlist):
    // rules distinguish create (doc doesn't exist) from update (doc exists),
    // so a collision surfaces as PERMISSION_DENIED here, not a thrown
    // "already exists" error. Retries with a fresh candidate on collision;
    // gives up silently after a handful of tries rather than blocking
    // whatever login/signup flow called this.
    private suspend fun generateAndClaimUsername(uid: String): String? {
        repeat(5) {
            val candidate = "user" + (10_000_000..99_999_999).random()
            try {
                firestore.collection("usernames").document(candidate)
                    .set(mapOf("uid" to uid)).awaitResult()
                return candidate
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                // Collision — candidate already claimed by someone else, retry.
            }
        }
        return null
    }

    // Step A for a user-typed rename: claims (or, if this uid already owns
    // it, confirms) usernames/{normalized} in its own transaction, separate
    // from step B below. firestore.rules' isOwnedUsername can only see state
    // that was already committed BEFORE the transaction it's guarding
    // started — verified empirically against the emulator, a rule can never
    // see a write staged earlier in that SAME transaction — so this has to
    // fully commit before step B (writeUsernameToProfile) even starts, or
    // isOwnedUsername would find nothing there yet.
    private suspend fun claimUsernameReservation(uid: String, normalized: String): Result<Unit> {
        val ref = firestore.collection("usernames").document(normalized)
        return try {
            firestore.runTransaction { txn ->
                val doc = txn.get(ref)
                if (doc.exists() && doc.getString("uid") != uid) {
                    throw Exception("That username is already taken.")
                }
                if (!doc.exists()) {
                    txn.set(ref, mapOf("uid" to uid))
                }
                null
            }.awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Step B, shared by both ensureUsername and changeUsername: only ever
    // called after step A (claiming the NEW reservation) has already
    // committed, so firestore.rules' isOwnedUsername check on this write can
    // see it. Frees whatever OLD reservation is being replaced, if any —
    // for ensureUsername there never is one (it only runs when the profile
    // has no username yet), so that branch is simply a no-op there. Retried
    // once on failure: step A is already durable at this point, so a
    // transient failure here just needs a second attempt rather than
    // surfacing an error and leaving the user's reservation claimed but not
    // yet reflected on their profile.
    private suspend fun writeUsernameToProfile(uid: String, username: String): Result<Unit> {
        val normalizedNew = normalizeUsername(username)
        val userRef = firestore.collection("users").document(uid)
        val publicProfileRef = firestore.collection("publicProfiles").document(uid)
        // Real Firebase Auth account-creation time for the signed-in user —
        // only readable for yourself client-side, but that's exactly who this
        // always runs for (both ensureUsername and changeUsername pass their
        // own auth.currentUser?.uid). Falls back to now only in the
        // unexpected case metadata is unavailable.
        val accountCreatedAt = auth.currentUser?.metadata?.creationTimestamp
            ?.takeIf { it > 0 } ?: System.currentTimeMillis()

        suspend fun attempt(): Result<Unit> = try {
            firestore.runTransaction { txn ->
                // Same read this transaction already needed for
                // currentUsername — name rides along for free, no extra
                // read. publicProfiles/{uid}.name exists so SellerProfileScreen
                // can show a real name for a seller opened via a shared deep
                // link, which arrives with no sellerName nav argument (see
                // that screen's own fallback).
                val userDoc = txn.get(userRef)
                val currentUsername = userDoc.getString("username")
                val name = userDoc.getString("name") ?: ""
                // Write-once: a later username CHANGE must never reset this,
                // so only stamp it the first time this doc is created.
                val existingCreatedAt = txn.get(publicProfileRef).getLong("createdAt")
                if (!currentUsername.isNullOrBlank() && normalizeUsername(currentUsername) != normalizedNew) {
                    txn.delete(firestore.collection("usernames").document(normalizeUsername(currentUsername)))
                }
                txn.set(userRef, mapOf("username" to username), SetOptions.merge())
                txn.set(
                    publicProfileRef,
                    mapOf(
                        "username" to username,
                        "name" to name,
                        "createdAt" to (existingCreatedAt ?: accountCreatedAt)
                    ),
                    SetOptions.merge()
                )
                null
            }.awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        val first = attempt()
        if (first.isSuccess) return first
        val second = attempt()
        // Both attempts failed — the reservation from step A is still valid
        // and still theirs (claimUsernameReservation already committed), so
        // this is recoverable, not a dead end: simply retrying changeUsername
        // with the same name will find they already own the reservation and
        // go straight to this step again. Worth saying so rather than
        // surfacing whatever raw SDK exception (e.g. an offline/timeout
        // message) attempt() caught — same posture as
        // resendVerificationEmail's rate-limit translation above.
        return second.takeIf { it.isSuccess } ?: Result.failure(
            Exception("Your new username was reserved, but we couldn't save it to your profile — please try again.")
        )
    }

    // Backfills a username for an account that doesn't have one yet — called
    // right after every new-account DentalUser doc is created (signUp,
    // logInOrSignUp's two branches, signInWithGoogleIdToken), and once per
    // login for existing accounts via MainActivity's authGate (fire-and-forget,
    // doesn't delay reaching marketplace). A no-op if the profile already has
    // a username, so it's always safe to call speculatively.
    suspend fun ensureUsername(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val userRef = firestore.collection("users").document(uid)
            val existing = userRef.get().awaitResult().getString("username")
            if (!existing.isNullOrBlank()) return Result.success(Unit)

            // Step A: generateAndClaimUsername already claims the reservation
            // on its own, via .set(), before returning — same shape as
            // claimUsernameReservation above, just with a random candidate
            // instead of a user-typed one.
            val username = generateAndClaimUsername(uid)
                ?: return Result.failure(Exception("Couldn't generate a unique username"))
            // Step B.
            writeUsernameToProfile(uid, username)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User-initiated rename, two steps: claimUsernameReservation (step A)
    // must fully commit before writeUsernameToProfile (step B) starts — see
    // both functions' own comments for why a single transaction spanning
    // both can't work with firestore.rules' isOwnedUsername check.
    suspend fun changeUsername(newUsername: String): Result<Unit> {
        val trimmed = newUsername.trim()
        if (!isValidUsernameFormat(trimmed)) {
            return Result.failure(Exception("Usernames must be 3-20 characters, start with a letter, and use only letters, numbers, and underscores."))
        }
        if (!isUsernameAllowed(trimmed)) {
            return Result.failure(Exception("That username isn't available."))
        }

        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        val normalizedNew = normalizeUsername(trimmed)

        val claimResult = claimUsernameReservation(uid, normalizedNew)
        if (claimResult.isFailure) return claimResult

        return writeUsernameToProfile(uid, trimmed)
    }

    // The only cross-user profile read in the app — everything else reads
    // denormalized fields off listings/orders/ratings instead (see
    // users/{userId}'s own rule comment). Used by SellerProfileScreen so a
    // changed username/name shows up immediately rather than being frozen at
    // whatever it was when the seller last posted a listing, and for the
    // About section's "Member since"/"Identity verified" — one read for the
    // whole doc rather than a separate round trip per field.
    suspend fun getPublicProfile(uid: String): Result<PublicSellerProfile> {
        return try {
            val doc = firestore.collection("publicProfiles").document(uid).get().awaitResult()
            Result.success(
                PublicSellerProfile(
                    name = doc.getString("name") ?: "",
                    username = doc.getString("username") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    emailVerified = doc.getBoolean("emailVerified") ?: false,
                    identityVerified = doc.getBoolean("identityVerified") ?: false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Denormalizes an approved identityVerifications/{uid} submission onto
    // the public profile SellerProfileScreen reads for OTHER users — a
    // different uid than the one being written, which only firestore.rules'
    // admin-only publicProfiles carve-out permits (hasOnly(['identityVerified']),
    // true-only, no revoke path yet). Called by AdminIdentityVerificationsViewModel
    // right after IdentityVerificationRepository.approve() — two separate
    // writes for one admin action, same shape as OrderViewModel.verifyPayment()
    // writing to orders then separately to sellerNotifications.
    suspend fun markIdentityVerified(uid: String): Result<Unit> {
        return try {
            firestore.collection("publicProfiles").document(uid)
                .set(mapOf("identityVerified" to true), SetOptions.merge())
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Saves the buyer's delivery default back to their profile — called from
    // CartViewModel.checkout() every time, so an edit made at checkout
    // becomes next time's pre-filled default.
    suspend fun updateDeliveryInfo(address: String, contactPhone: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val updates = mapOf(
                "deliveryAddress" to address,
                "deliveryContactPhone" to contactPhone
            )
            firestore.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.awaitResult()
            Result.success(Unit)
        } catch (e: FirebaseTooManyRequestsException) {
            Result.failure(Exception("You've requested this recently — please wait a bit before trying again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Firebase caches user info on-device; this pulls the latest state from
    // Google's servers so isEmailVerified reflects a link just clicked.
    suspend fun reloadUser(): Result<Unit> {
        return try {
            auth.currentUser?.reload()?.awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val settings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
                .setUrl("https://dentalmarket-abdf6.firebaseapp.com/resetPassword")
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.dentalmarket.app", true, null)
                .build()
            auth.sendPasswordResetEmail(email, settings).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordResetCode(code: String): Result<String> {
        return try {
            val email = auth.verifyPasswordResetCode(code).awaitResult()
            Result.success(email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPasswordReset(code: String, newPassword: String): Result<Unit> {
        return try {
            auth.confirmPasswordReset(code, newPassword).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

