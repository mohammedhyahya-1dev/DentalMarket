package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.dentalmarket.app.R
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.data.SafetyFeeConfigRepository
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.Inquiry
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.model.Report
import com.dentalmarket.app.model.ReportReason
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.RatingBadge
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.InquiryViewModel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.viewmodel.RatingViewModel
import com.dentalmarket.app.viewmodel.ReportViewModel
import com.dentalmarket.app.viewmodel.WatchlistViewModel
import com.dentalmarket.app.model.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    listingId: String,
    cartViewModel: CartViewModel,
    inquiryViewModel: InquiryViewModel,
    buyerId: String,
    buyerName: String,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    // Same handoff CategoriesScreen's leaf taps use — see navigateToCategory
    // in MainActivity; a blank subcategory means "whole category".
    onCategoryClick: (category: String, subcategory: String?) -> Unit,
    onSellClick: () -> Unit,
    onSellerClick: (sellerId: String, sellerName: String) -> Unit,
    watchlistViewModel: WatchlistViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel()
) {
    var listing by remember { mutableStateOf<Listing?>(null) }
    var showAddedMessage by remember { mutableStateOf(false) }
    val repository = remember { ListingRepository() }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf(ReportReason.COUNTERFEIT) }
    var reportDetails by remember { mutableStateOf("") }
    var showReportSentMessage by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showBuyerSafetyDetails by remember { mutableStateOf(false) }
    // Whether the top Add to Cart button is still on screen. This is a plain
    // verticalScroll Column, not a LazyColumn, so there's no layoutInfo to ask
    // — instead the button reports its own clipped bounds (verticalScroll
    // clips, so a button scrolled past the viewport measures zero height).
    var topAddToCartVisible by remember { mutableStateOf(true) }
    val authRepository = remember { AuthRepository() }
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isGuest) showGuestPrompt = true else action()
    }

    val watchedIds by watchlistViewModel.watchedIds.collectAsState()
    val isWatched = watchedIds.contains(listingId)
    val sellerSummaries by ratingViewModel.sellerSummaries.collectAsState()
    val inquiryErrorMessage by inquiryViewModel.errorMessage.collectAsState()
    val reportErrorMessage by reportViewModel.errorMessage.collectAsState()

    val safetyFeeConfigRepository = remember { SafetyFeeConfigRepository() }
    var safetyFeeAmount by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(listingId) {
        val result = repository.getListingById(listingId)
        result.onSuccess { listing = it }
        watchlistViewModel.loadWatchlistOnce()
        // Fire-and-forget: no dedup, every screen open counts, and a missed
        // tick isn't worth surfacing an error for.
        repository.incrementViewCount(listingId)
    }

    LaunchedEffect(listing?.sellerId) {
        listing?.sellerId?.takeIf { it.isNotBlank() }?.let {
            ratingViewModel.loadSellerSummaries(listOf(it))
        }
    }

    // Purely a preview of what the buyer could opt into later — the real
    // opt-in checkbox still lives on CartScreen, same config/safetyFee
    // value CartViewModel already reads at checkout.
    LaunchedEffect(Unit) {
        safetyFeeConfigRepository.getSafetyFeeConfig()
            .onSuccess { safetyFeeAmount = it.feeAmount }
    }

    Scaffold { padding ->
        val currentListing = listing
        if (currentListing == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Shared by the inline button and the sticky bar below, so the two can
        // never drift apart.
        val onAddToCart = {
            requireLogin {
                // Always exactly 1 — quantity is adjusted in CartScreen now.
                // addToCart increments an existing line item, so tapping again
                // bumps 2, 3, ... rather than resetting.
                cartViewModel.addToCart(currentListing)
                showAddedMessage = true
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Full-width hero image — still the emoji placeholder, no real
                // photos yet, so no multi-photo page indicator. Back/overflow
                // float on top of it instead of a separate TopAppBar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentListing.emoji, fontSize = 96.sp)
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .padding(12.dp)
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ask a Question") },
                                onClick = {
                                    showOverflowMenu = false
                                    requireLogin { showQuestionDialog = true }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Listing") },
                                onClick = {
                                    showOverflowMenu = false
                                    requireLogin { showReportDialog = true }
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            currentListing.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { requireLogin { watchlistViewModel.toggleWatch(listingId) } }) {
                            Icon(
                                if (isWatched) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                                tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Condition | Subcategory (falls back to Category when blank)
                    // | Brand (omitted entirely when blank) — one combined line,
                    // no seller-rating-count element per confirmed scope.
                    val subcategoryOrCategory = currentListing.subcategory.ifBlank { currentListing.category }
                    val conditionLabel = Condition.valueOf(currentListing.condition).label
                    val subtitleParts = listOfNotNull(
                        conditionLabel,
                        subcategoryOrCategory.takeIf { it.isNotBlank() },
                        currentListing.brand.takeIf { it.isNotBlank() }
                    )
                    if (subtitleParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            subtitleParts.joinToString(" | "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        formatPrice(currentListing.price),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    safetyFeeAmount?.let { fee ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "+ " + formatPrice(fee) + " Buyer Safety Fee (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                topAddToCartVisible = coords.boundsInWindow().height > 0f
                            }
                    ) {
                        Text("Add to Cart")
                    }
                    if (showAddedMessage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Added to cart ✓",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Pay via QiCard or ZainCash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    BuyerSafetyCard(
                        expanded = showBuyerSafetyDetails,
                        onToggle = { showBuyerSafetyDetails = !showBuyerSafetyDetails }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Fixed, structured facts about the listing — distinct from the
                    // freeform "Item Specifics" table further down, which stays the
                    // seller's own key/value entries. Brand takes the slot Mercari
                    // gives Size; dental equipment doesn't have sizes.
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Condition") {
                        Text(conditionLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                    DetailRow("Category") {
                        Row(verticalAlignment = Alignment.Top) {
                            CategoryLink(
                                text = currentListing.category,
                                onClick = { onCategoryClick(currentListing.category, null) }
                            )
                            if (currentListing.subcategory.isNotBlank()) {
                                Text(", ", style = MaterialTheme.typography.bodyMedium)
                                CategoryLink(
                                    text = currentListing.subcategory,
                                    onClick = {
                                        onCategoryClick(
                                            currentListing.category,
                                            currentListing.subcategory
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (currentListing.brand.isNotBlank()) {
                        DetailRow("Brand") {
                            Text(currentListing.brand, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    DetailRow("Posted") {
                        Text(
                            remember(currentListing.createdAt) {
                                SimpleDateFormat("MM/dd/yy", Locale.US)
                                    .format(Date(currentListing.createdAt))
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Description", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentListing.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Delivery", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Shipping") {
                        Text(
                            // DENTALMARKET_DELIVERS never bills the buyer for
                            // delivery — that fee comes out of the seller's payout
                            // (see CartViewModel.checkout) — and a SELLER_DELIVERS
                            // listing with shippingPrice 0.0 is free shipping. Both
                            // read "Free" to the buyer, same as SearchViewModel's
                            // free-shipping filter treats them.
                            if (currentListing.deliveryMethod == "SELLER_DELIVERS" &&
                                currentListing.shippingPrice > 0.0
                            ) {
                                formatPrice(currentListing.shippingPrice)
                            } else {
                                "Free"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (currentListing.sellerProvince.isNotBlank()) {
                        DetailRow("Ships from") {
                            Text(
                                currentListing.sellerProvince,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Only the two methods this app actually settles through — no
                    // card-network logos, which would imply acceptance we don't have.
                    Text("Payment", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PaymentMethodLogo(R.drawable.qicard_logo, "QiCard")
                        Spacer(modifier = Modifier.width(12.dp))
                        PaymentMethodLogo(R.drawable.zaincash_logo, "ZainCash")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { requireLogin(onSellClick) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Have a similar item? Sell yours")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (currentListing.sellerName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                onSellerClick(currentListing.sellerId, currentListing.sellerName)
                            }
                        ) {
                            Text(
                                "Sold by ${currentListing.sellerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                            sellerSummaries[currentListing.sellerId]?.let { summary ->
                                Spacer(modifier = Modifier.width(8.dp))
                                RatingBadge(summary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (currentListing.viewCount > 0) {
                        Text(
                            if (currentListing.viewCount == 1) {
                                "1 person viewed this"
                            } else {
                                "${currentListing.viewCount} people viewed this"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyTipsCard()
                    Spacer(modifier = Modifier.height(20.dp))

                    if (currentListing.specifics.isNotEmpty()) {
                        Text("Item Specifics", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            currentListing.specifics.entries.forEachIndexed { index, (key, value) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text(
                                        key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (index < currentListing.specifics.size - 1) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (showReportSentMessage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Report submitted – thank you, our team will review it",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (showQuestionDialog) {
                        AlertDialog(
                            onDismissRequest = { showQuestionDialog = false },
                            title = { Text("Ask a Question") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = questionText,
                                        onValueChange = { questionText = it },
                                        label = { Text("Your question") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    inquiryErrorMessage?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        inquiryViewModel.submitInquiry(
                                            Inquiry(
                                                listingId = currentListing.id,
                                                listingName = currentListing.name,
                                                buyerId = buyerId,
                                                buyerName = buyerName,
                                                sellerName = currentListing.sellerName
                                            ).copy(question = questionText)
                                        ) {
                                            showQuestionDialog = false
                                            questionText = ""
                                        }
                                    },
                                    enabled = questionText.isNotBlank()
                                ) {
                                    Text("Send")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showQuestionDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showReportDialog) {
                        var reasonExpanded by remember { mutableStateOf(false) }
                        AlertDialog(
                            onDismissRequest = { showReportDialog = false; reportViewModel.clearError() },
                            title = { Text("Report Listing") },
                            text = {
                                Column {
                                    ExposedDropdownMenuBox(
                                        expanded = reasonExpanded,
                                        onExpandedChange = { reasonExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = reportReason.label,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Reason") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = reasonExpanded,
                                            onDismissRequest = { reasonExpanded = false }
                                        ) {
                                            ReportReason.entries.forEach { r ->
                                                DropdownMenuItem(
                                                    text = { Text(r.label) },
                                                    onClick = {
                                                        reportReason = r
                                                        reasonExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = reportDetails,
                                        onValueChange = { reportDetails = it },
                                        label = { Text("Details (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    reportErrorMessage?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        reportViewModel.submitReport(
                                            Report(
                                                listingId = currentListing.id,
                                                listingName = currentListing.name,
                                                reason = reportReason.name,
                                                details = reportDetails
                                            )
                                        ) {
                                            showReportDialog = false
                                            reportDetails = ""
                                            showReportSentMessage = true
                                        }
                                    }
                                ) {
                                    Text("Submit")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showReportDialog = false; reportViewModel.clearError() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    // The sticky bar floats over this content, so keep clear
                    // space at the end rather than covering the last elements.
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
            // Sticky shortcut: shows only once the button above has scrolled
            // out of the viewport, so the buyer never has to scroll back up.
            AnimatedVisibility(
                visible = !topAddToCartVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Add to Cart")
                    }
                }
            }
        }
    }

    if (showGuestPrompt) {
        GuestSignInPrompt(
            onDismiss = { showGuestPrompt = false },
            onSignIn = {
                showGuestPrompt = false
                onRequireLogin()
            }
        )
    }
}

@Composable
private fun SafetyTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Safety Tips", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "Meet in a public place when possible.",
                "Never send payment outside the app — DentalMarket can't help recover off-platform payments.",
                "Review the listing's photos and description carefully before ordering — payment happens upfront, so make sure it's what you expect.",
                "Report suspicious listings or messages so our team can review them."
            ).forEach { tip ->
                Text(
                    "• $tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// One label/value line of the Details block. Label column is fixed-width so
// the values line up regardless of label length.
@Composable
private fun DetailRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(110.dp)
        )
        Box(modifier = Modifier.weight(1f)) { value() }
    }
}

// Payment-method logo for the Payment section. Both assets are opaque square
// tiles carrying their own brand background, so they're clipped to rounded
// corners and sized by height — width follows each logo's own aspect ratio.
// The brand name lives in contentDescription, since the mark itself is the
// only label sighted users get.
@Composable
private fun PaymentMethodLogo(@DrawableRes resId: Int, name: String) {
    Image(
        painter = painterResource(resId),
        contentDescription = name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(40.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
    )
}

@Composable
private fun CategoryLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// Distinct from SafetyTipsCard above — this explains the paid, optional
// Buyer Safety Fee itself (previewed near the price), not general
// transaction-safety advice. No refund-guarantee language: the only
// grounded facts about this fee are that it's optional, chosen per item at
// checkout, and (per Order.safetyFee) never reaches the seller.
@Composable
private fun BuyerSafetyCard(expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buyer Safety", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "A small optional fee that helps us maintain buyer protection and platform safety standards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "It's entirely optional and chosen per item at checkout — not required to complete your purchase. " +
                        "The fee goes toward DentalMarket's ongoing platform safety efforts, not to the seller.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            TextButton(onClick = onToggle, modifier = Modifier.padding(top = 4.dp)) {
                Text(if (expanded) "Show less" else "Learn more")
            }
        }
    }
}
