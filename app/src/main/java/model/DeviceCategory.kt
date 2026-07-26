package com.dentalmarket.app.model

enum class DeviceCategory(val label: String, val emoji: String) {
    XRAY_MACHINE("X-Ray Machine", "\uD83E\uDE7B"),
    CT_SCAN_MACHINE("CT Scan Machine", "\uD83C\uDF00"),
    ENDODONTICS_EQUIPMENT("Endodontics Equipment", "\uD83E\uDEA1"),
    ORTHODONTICS_EQUIPMENT("Orthodontics Equipment", "\uD83D\uDE2C"),
    SCANNERS("Scanners", "\uD83D\uDCF8"),
    IMPLANT_EQUIPMENT("Implant Equipment", "\uD83D\uDD29"),
    MICROSCOPES("Microscopes", "\uD83D\uDD2C"),
    DENTAL_LOUPES("Dental Loupes", "\uD83D\uDC53"),
    STERILISATION_EQUIPMENT("Sterilisation Equipment", "\u2668\uFE0F"),
    DENTAL_CHAIRS_ACCESSORIES("Dental Chairs and Accessories", "\uD83E\uDE91"),
    ELECTRIC_HANDPIECES("Electric Handpieces", "\u26A1"),
    SEDATION_EQUIPMENT("Sedation Equipment", "\uD83D\uDC89"),
    LAB_EQUIPMENT("Lab Equipment", "\uD83E\uDDEA"),
    PERIODONTICS_EQUIPMENT("Periodontics Equipment", "\uD83E\uDE78"),
    PROSTHODONTICS_EQUIPMENT("Prosthodontics Equipment", "\uD83E\uDDBF"),
    ORAL_SURGERY_EQUIPMENT("Oral Surgery Equipment", "\uD83D\uDD2A")
}