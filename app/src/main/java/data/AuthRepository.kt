package com.dentalmarket.app.data

import com.dentalmarket.app.model.DentalUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Every screen that needs login/signup talks to Firebase through this one
// class, instead of calling FirebaseAuth directly everywhere. That way,
// if anything about how login works ever needs to change, it only needs
// to change in this one file.
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            // Step 1: ask Firebase Authentication to create the login credentials
            val authResult = auth.createUserWithEmailAndPassword(email, password).awaitResult()
            val uid = authResult.user?.uid
                ?: throw Exception("Account created but no user ID was returned")

            // Step 2: save their name in Firestore, tied to that same ID,
            // so we can look up "who posted this listing" later
            val user = DentalUser(uid = uid, name = name, email = email)
            firestore.collection("users").document(uid).set(user).awaitResult()

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

    fun signOut() {
        auth.signOut()
    }
}
