package com.dentalmarket.app.model

// A dentist's profile info, kept in Firestore separately from their
// login credentials (Firebase Authentication only stores email/password
// itself — this "users" collection is where we keep the human-readable
// name so listings can show "Posted by Dr. Ahmed" later).
data class DentalUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val title: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val specialty: String = "",
    val province: String = "",
    val mobile: String = "",
    val extraMobile: String = "",
    // True once the dentist has filled in title/name/specialty/province/mobile.
    // Controls whether MainActivity routes them to CompleteProfileScreen first.
    val profileComplete: Boolean = false,
    // Buyer's saved delivery default — collected/edited at checkout, not
    // during profile completion (only buyers ever need this). Pre-fills the
    // checkout fields next time, and is overwritten whenever the buyer edits
    // it at checkout. See CartViewModel.checkout().
    val deliveryAddress: String = "",
    val deliveryContactPhone: String = "",
    // Public handle, separate from `name` — shown on ProfileScreen (self)
    // and SellerProfileScreen (others). Case preserved here for display;
    // uniqueness is enforced against the lowercased form via the
    // usernames/{name} reservation collection (see AuthRepository).
    // Auto-generated silently ("user" + digits) for every account that
    // doesn't have one yet, editable afterward from ProfileScreen.
    val username: String = ""
)