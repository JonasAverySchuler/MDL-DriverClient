package capstone.mdldriver

data class Rider(
    val _id: String,
    val id: Int?,
    val active: Boolean,
    val name: String,
    val phone: String,
    val lat: Double,
    val long: Double,
    val destinationAddress: String
)