package com.dentalmarket.app.model

data class Report(
    val id: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val reporterId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "PENDING", // PENDING or REVIEWED
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReportReason(val label: String) {
    COUNTERFEIT("Counterfeit or fake item"),
    MISLEADING("Misleading listing"),
    PROHIBITED("Prohibited item"),
    OFF_PLATFORM_CONTACT("Trying to move the deal off-platform"),
    OTHER("Other")
}
