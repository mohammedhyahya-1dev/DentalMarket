package com.dentalmarket.app.data

import com.dentalmarket.app.model.DentalUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.SetOptions

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val adminEmails = listOf("test@test.com")

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAdmin: Boolean
        get() = auth.currentUser?.email in adminEmails

    // Reflects Firebase's on-device cached copy. Call reloadUser() first if
    // you need the freshest value (e.g. right after the user might have
    // clicked the link in their inbox).
    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified ?: false

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).awaitResult()
            val uid = authResult.user?.uid
                ?: throw Exception("Account created but no user ID was returned")

            val user = DentalUser(uid = uid, name = name, email = email)
            firestore.collection("users").document(uid).set(user).awaitResult()

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

    suspend fun logInOrSignUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).awaitResult()
            Result.success(Unit)
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
                    newUser.sendEmailVerification().awaitResult()
                    Result.success(Unit)
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

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).awaitResult()
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
            }

            Result.success(Unit)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account already exists with this email. Please log in with your email and password instead."))
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.awaitResult()
            Result.success(Unit)
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