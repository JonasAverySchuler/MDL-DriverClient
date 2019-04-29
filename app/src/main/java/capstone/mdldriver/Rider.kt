package capstone.mdldriver

data class Rider(
    val _id: String,
    val id: Int?,
    val active: Boolean,
    val name: String,
    val phone: String,
    val location: Location
)

data class Location(
    val type: String,
    val address: String,
    val coordinates: Coordinates
)

data class Coordinates(
    val lat: Double,
    val long: Double
)