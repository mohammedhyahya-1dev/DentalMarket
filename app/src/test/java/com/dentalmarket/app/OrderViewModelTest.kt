package com.dentalmarket.app

import com.dentalmarket.app.viewmodel.nextOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderViewModelTest {

    @Test
    fun `PLACED only advances once payment is verified`() {
        assertNull(nextOrderStatus("PLACED", "AWAITING_PAYMENT"))
        assertNull(nextOrderStatus("PLACED", "PENDING_VERIFICATION"))
        assertNull(nextOrderStatus("PLACED", "REJECTED"))
        assertEquals("PICKED_UP", nextOrderStatus("PLACED", "VERIFIED"))
    }

    @Test
    fun `later pipeline stages are unaffected by payment status`() {
        assertEquals("DELIVERED", nextOrderStatus("PICKED_UP", "AWAITING_PAYMENT"))
        assertEquals("DELIVERED", nextOrderStatus("PICKED_UP", "VERIFIED"))
        assertEquals("PAID_TO_SELLER", nextOrderStatus("DELIVERED", "AWAITING_PAYMENT"))
        assertEquals("PAID_TO_SELLER", nextOrderStatus("DELIVERED", "VERIFIED"))
    }

    @Test
    fun `terminal statuses have no next step`() {
        assertNull(nextOrderStatus("PAID_TO_SELLER", "VERIFIED"))
        assertNull(nextOrderStatus("CANCELLED", "VERIFIED"))
        assertNull(nextOrderStatus("CANCELLED", "AWAITING_PAYMENT"))
    }
}
