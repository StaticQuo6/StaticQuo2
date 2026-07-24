package com.staticquo.heatmap

enum class BeaconType(val label: String, val color: Long) {
    MEDIC("Medic", 0xFFE53935),
    NEED("Need Supplies", 0xFFFFA000),
    SUPPLY("Has Supplies", 0xFF43A047),
    SAFEZONE("Safe Zone", 0xFF1E88E5),
    DANGER("Danger", 0xFFD32F2F)
}
