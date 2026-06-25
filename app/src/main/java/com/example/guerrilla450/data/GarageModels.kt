package com.example.guerrilla450.data

/** A single fuel fill-up. Mileage (km/l) is derived from the odometer gap to the prior fill. */
data class FuelFillup(
    val id: Long = 0,
    val dateMs: Long,
    val litres: Double,
    val cost: Double,
    val odometerKm: Int,
    val location: String = "",
    val sid: String = "",
)

/**
 * One recorded ride = one connect→disconnect session with the dash. Stats are computed
 * from the GPS track as it streams; [trackPolyline] is the encoded path (for the map
 * snapshot on RidesScreen).
 */
data class Ride(
    val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val distanceMeters: Double,
    val durationSec: Long,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val trackPolyline: String = "",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val sid: String = "",
) {
    val avgSpeedKmh: Double get() = avgSpeedMps * 3.6
    val maxSpeedKmh: Double get() = maxSpeedMps * 3.6
    val distanceKm: Double get() = distanceMeters / 1000.0
}

data class MaintenanceItem(
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val intervalKm: Int,
    val lastDoneOdoKm: Int,
    val lastDoneDateMs: Long,
    val intervalMonths: Int = 0,
    val sid: String = "",
)

data class ServiceRecord(
    val id: Long = 0,
    val sid: String = "",
    val title: String,
    val kind: String = "company",
    val scheduledKey: String = "",
    val itemSids: List<String> = emptyList(),
    val odometerKm: Int,
    val dateMs: Long,
    val cost: Double = 0.0,
    val invoicePath: String = "",
    val note: String = "",
)

data class ScheduledService(
    val id: Long = 0,
    val sid: String = "",
    val label: String,
    val targetKm: Int,
    val targetMonths: Int,
    val free: Boolean = false,
    val orderIdx: Int = 0,
)

data class VehicleDocument(
    val id: Long = 0,
    val sid: String = "",
    val type: String,
    val title: String,
    val number: String = "",
    val issueMs: Long = 0L,
    val expiryMs: Long = 0L,
    val filePath: String = "",
    val note: String = "",
)

data class BikeIdentity(
    val vin: String = "",
    val engineNo: String = "",
    val regNo: String = "",
    val purchaseMs: Long = 0L,
    val colour: String = "",
)
