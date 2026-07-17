package com.dentalmarket.app.data

import com.dentalmarket.app.model.DentalUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Emails allowed to see the Admin Orders screen (order fulfillment
    // management). Add more emails here later if others help with delivery.
    private val adminEmails = listOf("test@test.com")

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAdmin: Boolean
        get() = auth.currentUser?.email in adminEmails

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).awaitResult()
            val uid = authResult.user?.uid
                ?: throw Exception("Account created but no user ID was returned")

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

    suspend fun getCurrentUserProfile(): Result<DentalUser?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(null)
            val doc = firestore.collection("users").document(uid).get().awaitResult()
            Result.success(doc.toObject(DentalUser::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}