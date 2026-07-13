package com.dentalmarket.app.model

// A dentist's profile info, kept in Firestore separately from their
// login credentials (Firebase Authentication only stores email/password
// itself — this "users" collection is where we keep the human-readable
// name so listings can show "Posted by Dr. Ahmed" later).
data class DentalUser(
    val uid: String = "",
    val name: String = "",
    val email: String = ""
)
