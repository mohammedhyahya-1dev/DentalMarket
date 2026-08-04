package com.dentalmarket.app

import com.dentalmarket.app.viewmodel.calculatePayout
import com.dentalmarket.app.viewmodel.nextOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderViewModelTest {

    @Test
    fun `PLACED only advances once payment is verified`() {
        assertNull(nextOrderStatus("PLACED", "AWAITING_PAYMENT", "DENTALMARKET_DELIVERS"))
        assertNull(nextOrderStatus("PLACED", "PENDING_VERIFICATION", "DENTALMARKET_DELIVERS"))
        assertNull(nextOrderStatus("PLACED", "REJECTED", "DENTALMARKET_DELIVERS"))
        assertEquals("PICKED_UP", nextOrderStatus("PLACED", "VERIFIED", "DENTALMARKET_DELIVERS"))
    }

    @Test
    fun `later pipeline stages are unaffected by payment status`() {
        assertEquals("DELIVERED", nextOrderStatus("PICKED_UP", "AWAITING_PAYMENT", "DENTALMARKET_DELIVERS"))
        assertEquals("DELIVERED", nextOrderStatus("PICKED_UP", "VERIFIED", "DENTALMARKET_DELIVERS"))
        assertEquals("PAID_TO_SELLER", nextOrderStatus("DELIVERED", "AWAITING_PAYMENT", "DENTALMARKET_DELIVERS"))
        assertEquals("PAID_TO_SELLER", nextOrderStatus("DELIVERED", "VERIFIED", "DENTALMARKET_DELIVERS"))
    }

    @Test
    fun `terminal statuses have no next step`() {
        assertNull(nextOrderStatus("PAID_TO_SELLER", "VERIFIED", "DENTALMARKET_DELIVERS"))
        assertNull(nextOrderStatus("CANCELLED", "VERIFIED", "DENTALMARKET_DELIVERS"))
        assertNull(nextOrderStatus("CANCELLED", "AWAITING_PAYMENT", "DENTALMARKET_DELIVERS"))
    }

    @Test
    fun `admin cannot advance PICKED_UP to DELIVERED when the seller delivers`() {
        assertNull(nextOrderStatus("PICKED_UP", "VERIFIED", "SELLER_DELIVERS"))
        assertNull(nextOrderStatus("PICKED_UP", "AWAITING_PAYMENT", "SELLER_DELIVERS"))
    }

    @Test
    fun `admin can advance PICKED_UP to DELIVERED when DentalMarket delivers`() {
        assertEquals("DELIVERED", nextOrderStatus("PICKED_UP", "VERIFIED", "DENTALMARKET_DELIVERS"))
    }

    @Test
    fun `delivery method does not affect other transitions`() {
        assertEquals("PICKED_UP", nextOrderStatus("PLACED", "VERIFIED", "SELLER_DELIVERS"))
        assertEquals("PAID_TO_SELLER", nextOrderStatus("DELIVERED", "VERIFIED", "SELLER_DELIVERS"))
    }

    @Test
    fun `commission splits item total between platform and seller`() {
        val breakdown = calculatePayout(itemTotal = 250.0, commissionPercentage = 10.0, deliveryFee = 0.0)
        assertEquals(250.0, breakdown.itemTotal, 0.0001)
        assertEquals(25.0, breakdown.commissionAmount, 0.0001)
        assertEquals(225.0, breakdown.sellerReceives, 0.0001)
    }

    @Test
    fun `zero commission gives the seller the full amount`() {
        val breakdown = calculatePayout(itemTotal = 100.0, commissionPercentage = 0.0, deliveryFee = 0.0)
        assertEquals(0.0, breakdown.commissionAmount, 0.0001)
        assertEquals(100.0, breakdown.sellerReceives, 0.0001)
    }

    @Test
    fun `commission handles fractional cents`() {
        val breakdown = calculatePayout(itemTotal = 19.99, commissionPercentage = 7.5, deliveryFee = 0.0)
        assertEquals(1.499250, breakdown.commissionAmount, 0.0001)
        assertEquals(18.49075, breakdown.sellerReceives, 0.0001)
    }

    @Test
    fun `delivery fee is deducted from the seller alongside commission`() {
        val breakdown = calculatePayout(itemTotal = 250.0, commissionPercentage = 10.0, deliveryFee = 5.0)
        assertEquals(25.0, breakdown.commissionAmount, 0.0001)
        assertEquals(5.0, breakdown.deliveryFee, 0.0001)
        assertEquals(220.0, breakdown.sellerReceives, 0.0001)
    }
}
