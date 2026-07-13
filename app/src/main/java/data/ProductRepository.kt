package com.dentalmarket.app.data

import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.Product

// Phase 1: hardcoded sample listings so you can test the browsing/cart flow
// without needing a real backend yet. Swap this out for a database or API later.
object ProductRepository {
    val products = listOf(
        Product(
            1, "Autoclave Sterilizer", "Sterilization", Condition.GOOD, 850.0,
            "Class B autoclave, 18L chamber. Fully serviced, new door gasket installed. Ideal backup unit for a busy clinic.",
            "\uD83E\uDDEA"
        ),
        Product(
            2, "Dental Chair Unit", "Chairs & Units", Condition.LIKE_NEW, 3200.0,
            "Complete chair unit with LED operating light and delivery arm. Barely used, from a clinic closure.",
            "\uD83E\uDDB7"
        ),
        Product(
            3, "Digital X-Ray Sensor", "Imaging", Condition.GOOD, 1450.0,
            "Size 2 intraoral sensor, USB. Calibrated and tested against current radiography software.",
            "\uD83D\uDCE1"
        ),
        Product(
            4, "Ultrasonic Scaler", "Hygiene", Condition.FAIR, 180.0,
            "Piezo scaler with 3 tips included. Works well, some cosmetic wear on the handpiece housing.",
            "\uD83E\uDEA5"
        ),
        Product(
            5, "LED Curing Light", "Restorative", Condition.LIKE_NEW, 210.0,
            "Cordless curing light, barely used. Comes with original charging stand.",
            "\uD83D\uDCA1"
        ),
        Product(
            6, "High-Speed Handpiece Set", "Handpieces", Condition.GOOD, 320.0,
            "Set of 3 turbine handpieces, freshly serviced and lubricated, new bearings.",
            "\uD83D\uDD27"
        ),
        Product(
            7, "Dental Loupes 3.5x", "Instruments", Condition.LIKE_NEW, 260.0,
            "Titanium frame loupes with headlight mount. Excellent optics, light use.",
            "\uD83D\uDD0D"
        ),
        Product(
            8, "Amalgamator Mixer", "Restorative", Condition.FAIR, 95.0,
            "Digital amalgamator, tested and working. Casing shows some scuffs.",
            "\u2699\uFE0F"
        )
    )
}
