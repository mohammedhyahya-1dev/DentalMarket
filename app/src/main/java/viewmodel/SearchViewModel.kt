package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dentalmarket.app.model.Listing

enum class SortOption(val label: String) {
    BEST_MATCH("Best Match"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    NEWEST_FIRST("Newest First"),
    MOST_VIEWED("Most Viewed")
}

// UI state only — the actual filtering/sorting is the pure function below,
// same split as OrderViewModel.kt's calculatePayout()/nextOrderStatus().
// Listings themselves are loaded by a MarketplaceViewModel instance owned
// by SearchResultsScreen, not by this class.
class SearchViewModel : ViewModel() {
    var query = mutableStateOf("")
    var sortOption = mutableStateOf(SortOption.BEST_MATCH)

    var minPrice = mutableStateOf("")
    var maxPrice = mutableStateOf("")
    var selectedCategories = mutableStateOf<Set<String>>(emptySet())
    var selectedSubcategories = mutableStateOf<Set<String>>(emptySet())
    var selectedConditions = mutableStateOf<Set<String>>(emptySet())
    var selectedDeliveryMethods = mutableStateOf<Set<String>>(emptySet())
    var freeShippingOnly = mutableStateOf(false)
    var selectedBrands = mutableStateOf<Set<String>>(emptySet())
    var selectedLocations = mutableStateOf<Set<String>>(emptySet())

    // Each category has its own distinct subcategory list, so any change to
    // which categories are selected invalidates whatever subcategories were
    // picked under a previously-selected category — same reasoning as
    // ListingViewModel.setCategory() on the posting side.
    fun toggleCategory(value: String) {
        selectedCategories.value = selectedCategories.value.toggle(value)
        selectedSubcategories.value = emptySet()
    }

    fun toggleSubcategory(value: String) {
        selectedSubcategories.value = selectedSubcategories.value.toggle(value)
    }

    fun toggleCondition(value: String) {
        selectedConditions.value = selectedConditions.value.toggle(value)
    }

    fun toggleDeliveryMethod(value: String) {
        selectedDeliveryMethods.value = selectedDeliveryMethods.value.toggle(value)
    }

    fun toggleBrand(value: String) {
        selectedBrands.value = selectedBrands.value.toggle(value)
    }

    fun toggleLocation(value: String) {
        selectedLocations.value = selectedLocations.value.toggle(value)
    }

    fun clearFilters() {
        minPrice.value = ""
        maxPrice.value = ""
        selectedCategories.value = emptySet()
        selectedSubcategories.value = emptySet()
        selectedConditions.value = emptySet()
        selectedDeliveryMethods.value = emptySet()
        freeShippingOnly.value = false
        selectedBrands.value = emptySet()
        selectedLocations.value = emptySet()
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (contains(value)) this - value else this + value

// A SELLER_DELIVERS listing with no shipping charge is free just like any
// DENTALMARKET_DELIVERS listing already is to the buyer — same "free
// shipping" bucket either way.
fun isFreeShipping(listing: Listing): Boolean =
    (listing.deliveryMethod == "SELLER_DELIVERS" && listing.shippingPrice == 0.0) ||
        listing.deliveryMethod == "DENTALMARKET_DELIVERS"

// Pure, free of any Firebase/ViewModel dependency — same reasoning as
// calculatePayout()/nextOrderStatus() in OrderViewModel.kt: directly
// unit-testable, and reused as-is by SearchResultsScreen against whatever
// MarketplaceViewModel.listings already holds (no separate Firestore query).
fun filterAndSortListings(
    listings: List<Listing>,
    query: String,
    sortOption: SortOption,
    minPrice: Double?,
    maxPrice: Double?,
    categories: Set<String>,
    subcategories: Set<String>,
    conditions: Set<String>,
    deliveryMethods: Set<String>,
    freeShippingOnly: Boolean,
    brands: Set<String>,
    locations: Set<String>
): List<Listing> {
    val filtered = listings.filter { listing ->
        val matchesQuery = query.isBlank() ||
            listing.name.contains(query, ignoreCase = true) ||
            listing.category.contains(query, ignoreCase = true) ||
            listing.subcategory.contains(query, ignoreCase = true) ||
            listing.brand.contains(query, ignoreCase = true)
        val matchesMin = minPrice == null || listing.price >= minPrice
        val matchesMax = maxPrice == null || listing.price <= maxPrice
        val matchesCategory = categories.isEmpty() || listing.category in categories
        val matchesSubcategory = subcategories.isEmpty() || listing.subcategory in subcategories
        val matchesCondition = conditions.isEmpty() || listing.condition in conditions
        val matchesDelivery = deliveryMethods.isEmpty() || listing.deliveryMethod in deliveryMethods
        val matchesFreeShipping = !freeShippingOnly || isFreeShipping(listing)
        val matchesBrand = brands.isEmpty() || listing.brand in brands
        val matchesLocation = locations.isEmpty() || listing.sellerProvince in locations
        matchesQuery && matchesMin && matchesMax && matchesCategory && matchesSubcategory &&
            matchesCondition && matchesDelivery && matchesFreeShipping && matchesBrand && matchesLocation
    }
    return when (sortOption) {
        SortOption.BEST_MATCH -> filtered
        SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
        SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
        SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
        SortOption.MOST_VIEWED -> filtered.sortedByDescending { it.viewCount }
    }
}
