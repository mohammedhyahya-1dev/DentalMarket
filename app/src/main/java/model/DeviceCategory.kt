package com.dentalmarket.app.model

enum class DeviceCategory(
    val label: String,
    val emoji: String,
    val subcategories: List<String> = emptyList()
) {
    XRAY(
        "X-Ray", "🩻",
        listOf(
            "X-Ray Portable Unit",
            "X-Ray Digital Sensor",
            "X-Ray Wall-Mounted/Chair-Mounted Unit",
            "Accessories",
            "Other"
        )
    ),
    // These 3 have no natural subcategories of their own — every category
    // now needs at least one entry so the subcategory picker/validation in
    // ListingViewModel/SellScreen can apply uniformly with no exceptions,
    // so each gets one subcategory matching its own name, plus "Other".
    PANORAMIC_CBCT_MACHINE(
        "Panoramic and CBCT Machine", "🌀",
        listOf("Panoramic and CBCT Machine", "Other")
    ),
    MICROSCOPE("Microscope", "🔬", listOf("Microscope Devices", "Accessories", "Other")),
    ENDODONTICS_EQUIPMENT(
        "Endodontics Equipment", "🪡",
        listOf("Apex Locator", "Endo Motor", "Ultrasonic Devices", "Obturation System", "Endo Instruments", "Other")
    ),
    INTRAORAL_SCANNER(
        "Intraoral Scanner", "📸",
        listOf("Intraoral Scanner", "Other")
    ),
    IMPLANT_EQUIPMENT(
        "Implant Equipment", "🔩",
        listOf(
            "Implant Motor",
            "Piezoelectric Unit",
            "Implant Surgical Drilling Kit",
            "Implant Prosthodontics Instruments",
            "Implant Surgical Instruments",
            "Fixtures",
            "Accessories",
            "Other"
        )
    ),
    DENTAL_MAGNIFICATION(
        "Dental Magnification", "👓",
        listOf("Dental Loupes", "Loupes Light", "Accessories", "Other")
    ),
    STERILIZATION_EQUIPMENT(
        "Sterilization Equipment", "♨️",
        listOf(
            "Sterilization Devices",
            "Ultrasonic Cleaner Device",
            "Distal Water Device",
            "Packaging Devices and Materials",
            "Other"
        )
    ),
    DENTAL_CHAIR_ACCESSORIES(
        "Dental Chair and Accessories", "🪑",
        listOf("Dental Chairs", "Compressors", "Dental Stools", "Accessories", "Other")
    ),
    HANDPIECE(
        "Handpiece", "⚡",
        listOf("Turbines and Handpieces", "Electric Turbines and Handpieces", "Accessories", "Other")
    ),
    SEDATION_EQUIPMENT(
        "Sedation Equipment", "💉",
        listOf("Sedation Unit", "Sedation Gases", "Accessories", "Other")
    ),
    LAB_EQUIPMENT(
        "Lab Equipment", "🧪",
        listOf(
            "Ceramist Instruments",
            "3D Printer and Materials",
            "Milling Machines",
            "Lab Desktop Scanners",
            "Sintering Furnaces",
            "Porcelain Furnaces",
            "Micro-Motors",
            "Suction Units",
            "Waxing Equipment",
            "Sandblasters",
            "Accessories",
            "Other"
        )
    ),
    PERIODONTICS_EQUIPMENT(
        "Periodontics Equipment", "🩸",
        listOf(
            "Periodontics Instruments",
            "Ultrasonic Scaling Units",
            "Air Polishing Systems and Materials",
            "Laser Devices and Accessories",
            "Other"
        )
    ),
    PROSTHODONTICS_EQUIPMENT(
        "Prosthodontics Equipment", "🦿",
        listOf(
            "Dental Articulator",
            "Facebows",
            "Digital Shade Guide",
            "3D Curing Units",
            "Chairside CadCam System",
            "Other"
        )
    ),
    ORAL_SURGERY_EQUIPMENT(
        "Oral Surgery Equipment", "🔪",
        listOf(
            "Surgical Handpieces",
            "Surgical Suction Unit",
            "Electrosurgical Unit",
            "PRF/PRP Centrifuges",
            "Bone Mill/Crushers",
            "Surgical LED System",
            "Patient Monitors",
            "Surgical Instruments",
            "Other"
        )
    ),
    ORTHODONTICS_EQUIPMENT(
        "Orthodontics Equipment", "😬",
        listOf("Orthodontics Equipment", "Other")
    )
}
