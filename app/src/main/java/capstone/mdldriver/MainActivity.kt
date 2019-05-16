package capstone.mdldriver

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import okhttp3.HttpUrl
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_main.cancelOrRefreshButton
import kotlinx.android.synthetic.main.activity_main.etaTextView
import kotlinx.android.synthetic.main.activity_main.ridersRecyclerView
import kotlinx.android.synthetic.main.marker_layout.view.nameTextView
import kotlinx.android.synthetic.main.marker_layout.view.snippetTextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val TAG = "MainActivity"
private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
private const val DRIVER_LOCATION_DEBOUNCE_MILLS = 1000L
private const val DRIVER_NAME_EXTRA_STRING = "DriverName"
private const val DRIVER_PHONE_EXTRA_STRING = "Phone"

class MainActivity : FragmentActivity(), OnMapReadyCallback, RidersRecyclerViewAdapter.Listener, LocationListener{
    private val baseUrl: HttpUrl =  HttpUrl.get("http://jl-m.org:8000/")
    private var map: GoogleMap? = null
    private var latLng: LatLng? = null
    private var riders: List<Rider> = emptyList()
    private val httpClient = OkHttpClient.Builder().build()
    private val zoom = 16.0f
    private var polyline : Polyline? = null

    //Test data if you need it
    private val fakeRider1 = Rider("123", 1, true, "Jon", "55234234", 38.950732, -92.3290123 ,"6 Fyfer Place")
    private val fakeRider2 = Rider("1223", 2, true, "Bobert", "23234234", 38.950730, -92.329642,"400 North 9th Street" )
    private val fakeRider3 = Rider("1234", 3, true, "Marco", "55234244", 38.9506438, -92.3290119 ,"413 North 9th Street")
    private val fakeRider4 = Rider("12235", 4, true, "Lily", "2323734", 38.9506438, -92.3290189, "415 North 9th Street" )
    private val fakeRiderList = listOf(fakeRider1, fakeRider2, fakeRider3, fakeRider4)
    private val useFakeData = false //Toggle to true to use above rider data instead of network data, MAKE SURE IT STAYS FALSE ON MASTER: TESTING PURPOSES ONLY


    private var currentRider: Rider? = null
    private var currentlyOnARide = false
        set(value) {
            if (!value) {
                //make it the refresh button and recyclerview
                cancelOrRefreshButton.text = getString(R.string.refresh_riders)
                ridersRecyclerView.visibility = View.VISIBLE
                etaTextView.visibility = View.GONE
            } else {
                //make it ride complete button and eta and map
                cancelOrRefreshButton.text = getString(R.string.complete_ride)
                ridersRecyclerView.visibility = View.GONE
                etaTextView.visibility = View.VISIBLE
        }
            field = value
        }

    private lateinit var adapter: RidersRecyclerViewAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var driverName: String
    private lateinit var driverPhone: String
    private lateinit var socket: Socket

    companion object {
        fun intent(context: Context, driver: Driver): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(DRIVER_NAME_EXTRA_STRING, driver.displayName)
            intent.putExtra(DRIVER_PHONE_EXTRA_STRING, driver.phoneNumber)
            return intent
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.v(TAG, "updated driver location: " + location.longitude + " : " + location.latitude)
        val longitude = location.longitude
        val latitude = location.latitude
        latLng = LatLng(latitude, longitude)
        socket.emit("driver-movement", latLng) //TODO check if this is correct
    }
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onRiderClick(rider: Rider) {
        //When recylerview item is clicked, go to rider location on map and show route
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(rider.lat, rider.long), zoom))
        polyline?.remove()
        etaTextView.visibility = View.GONE
        showPath(rider)
    }

    override fun onConfirmRideClick(rider: Rider) {
        socket.emit("ride-accepted", rider._id)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(rider.lat, rider.long), zoom))
        currentRider = rider
        currentlyOnARide = true
        showPath(rider)
        Toast.makeText(this, "Confirmed", Toast.LENGTH_SHORT).show() //TODO: custom toast?
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        driverName = intent.getStringExtra(DRIVER_NAME_EXTRA_STRING)
        driverPhone = intent.getStringExtra(DRIVER_PHONE_EXTRA_STRING)

        socket = IO.socket("http://jl-m.org:8000") //Initialize socket
        socket.connect()

        insertNewActiveDriver()



        val onNewRideRequest = Emitter.Listener {
            //TODO make sure this is right and remove listener in ondestory
            setRiderLocations()
        }

        val onRideRequestAccepted = Emitter.Listener {
            //TODO make sure this is right and remove listener in ondestory
            setRiderLocations()
        }

        socket.on("request-for-ride", onNewRideRequest) //Listen for new ride requests and update queue accordingly
        socket.on("ride-accepted", onRideRequestAccepted) //Listen for when a ride is accepted to remove it from your queue if you did not take it


        //TODO: add driver side tab, figure out /insertDriver

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        adapter = RidersRecyclerViewAdapter(riders)
        adapter.listener = this
        ridersRecyclerView.adapter = adapter

        cancelOrRefreshButton.setOnClickListener {
            if (currentlyOnARide) {
                //End Ride
                currentlyOnARide = false
                socket.emit("ride-completed", currentRider?._id) //Tell backend the ride is done, marking it as complete
                currentRider = null
            }
            setRiderLocations()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        centerMapOnDriver()

        //TODO check for permissions here

        setRiderLocations()
    }

    override fun onResume() {
        super.onResume()
        centerMapOnDriver()
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
        //TODO: Emit a driver done event before disconnect
        //TODO: update driver as no longer taking riders on the server
    }

    private fun insertNewActiveDriver() {

        if (!useFakeData) { //If we are testing with fake data, dont bother to insert the driver
            val handler = Handler(Looper.getMainLooper())

            val url: HttpUrl = baseUrl.newBuilder("insertDriver")!!
                    .addQueryParameter("active", "true")
                    .addQueryParameter("latitude", "38.822057")
                    .addQueryParameter("longitude", "-90.700158")
                    .addQueryParameter("name", driverName)
                    .addQueryParameter("phone", driverPhone)
                    .build()


            val request: Request = Request.Builder().url(url).build()
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    handler.post {
                        Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val jsonString = response?.body()!!.string()
                    if (!isJSONValid(jsonString)) {
                        Log.e(TAG, "insertdriver response: " + jsonString)
                        handler.post {
                            Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        //socket.emit("join", driverid)
                    }
                }

            })
        }
    }

    private fun centerMapOnDriver() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                onLocationChanged(it)
                latLng?.let {
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, zoom))
                }
            }
        } catch (ex: SecurityException) {
            Log.e(TAG, "Security Exception: " + ex)
        }
    }

    private fun setRiderLocations() {
        val handler = Handler(Looper.getMainLooper())
        map?.clear()
        if (useFakeData) {
            handler.post {
                //UI manipulation must be ran on Main thread
                adapter.updateRiders(fakeRiderList)

                fakeRiderList.forEach {
                    if (it.active) {
                        map?.addMarker(MarkerOptions()
                            .position(LatLng(it.lat, it.long))
                            .title(it.name)
                            .snippet(it.phone))
                    }
                }
            }
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                onLocationChanged(it)
                val url: HttpUrl = baseUrl.newBuilder("nearbyRiders")!!
                    .addQueryParameter("latitude", it.latitude.toString())
                    .addQueryParameter("longitude", it.longitude.toString())
                    .build()

            val request: Request = Request.Builder().url(url).build()

            httpClient.newCall(request).enqueue( object: Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    Log.e(TAG, "nearby riders call failure:" + e.toString() + " call: " + call.toString())
                    handler.post { //UI manipulation must be ran on Main thread
                        Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call?, response: Response?) {
                    val jsonString = response!!.body()!!.string().toString()
                    if (isJSONValid(jsonString)) {
                        val jsonObject = JSONObject(jsonString)
                        Log.v(TAG, "jsonResponse:$jsonObject")
                        val jsonArray = jsonObject.getJSONArray("riders")
                        val updatedRiders = mutableListOf<Rider>()
                        for (i in 0 until jsonArray.length()) {
                            val riderJSONObject = jsonArray.getJSONObject(i)
                            val _id = riderJSONObject.getString("_id")
                            val id = riderJSONObject.optInt("id") //backend allows to ignore this so it might not be in the response
                            val active = riderJSONObject.getBoolean("active")
                            val name = riderJSONObject.getString("name")
                            val phone = riderJSONObject.getString("phone")
                            val locationJSONObject = riderJSONObject.getJSONObject("location")
                            val locationType = locationJSONObject.getString("type") //TODO: utilize type
                            val locationAddress = locationJSONObject.getString("address")
                            val locationCoordinatesJSONArray = locationJSONObject.getJSONArray("coordinates")
                            val locationLat = locationCoordinatesJSONArray.getDouble(1)
                            val locationLong = locationCoordinatesJSONArray.getDouble(0)

                            updatedRiders += Rider(_id, id, active, name, phone, locationLat, locationLong, locationAddress)
                        }

                        handler.post {
                            //UI manipulation must be ran on Main thread
                            adapter.updateRiders(updatedRiders)

                            updatedRiders.forEach {
                                if (it.active) {
                                    map?.addMarker(MarkerOptions()
                                            .position(LatLng(it.lat, it.long))
                                            .title(it.name)
                                            .snippet(it.phone))
                                }
                            }
                        }

                    } else {
                        handler.post{
                            Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            })

        }}catch (ex: SecurityException) {
            Log.e(TAG, ex.toString())
        }
    }

    private fun showPath(rider: Rider) {

        val url: HttpUrl = HttpUrl.get(getDirectionsUrl(LatLng(rider.lat, rider.long), rider.destinationAddress + "Columbia, Mo 65201")) //Adding Columbia Missouri to destination address, since this will only operate in this city
                .newBuilder()
                .addQueryParameter("key","AIzaSyAGYXAa5fpZnEMVBZ4Vscuu_H3jnkJpxHw").build()

        val handler = Handler(Looper.getMainLooper())

        val request: Request = Request.Builder().url(url).build()
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "showPath call failure: " + e.toString())
                handler.post {
                    Toast.makeText(this@MainActivity, "Network Error: Cannot show path", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body()!!.string().toString()
                val parser = DataParser()
                if (isJSONValid(jsonString)) {
                    //Get polyline data from json result
                    val jsonObject = JSONObject(jsonString)
                    val routes: List<List<HashMap<String,String>>> = parser.parse(jsonObject)
                    var points: ArrayList<LatLng>
                    var lineOptions: PolylineOptions? = null
                    var startPoint: LatLng? = null
                    var endPoint: LatLng? = null

                    // Traversing through all the routes
                    for (i in routes.indices) {
                        points = ArrayList<LatLng>()
                        lineOptions = PolylineOptions()

                        // Fetching i-th route
                        val path = routes[i]

                        // Fetching all the points in i-th route
                        for (j in path.indices) {
                            val point = path[j]

                            val lat = java.lang.Double.parseDouble(point["lat"])
                            val lng = java.lang.Double.parseDouble(point["lng"])
                            val position = LatLng(lat, lng)

                            points.add(position)
                            if (j == 0) {
                                startPoint = position
                            } else if (j == path.indices.last) {
                                endPoint = position
                            }
                        }

                        // Adding all the points in the route to LineOptions
                        lineOptions.addAll(points)
                        lineOptions.width(10f)
                        lineOptions.color(Color.RED)

                    }

                    // Drawing polyline in the Google Map for the i-th route
                    if (lineOptions != null) {
                        handler.post{
                            polyline?.remove()
                            polyline = map!!.addPolyline(lineOptions)
                            startPoint?.let { start ->
                                endPoint?.let { end ->
                                    etaTextView.text = getString(R.string.ride_length_formatted, (SphericalUtil.computeDistanceBetween(start, end).toInt()/500).inc()) //TODO: compute a better eta formula
                                    etaTextView.visibility = View.VISIBLE

                                }
                            }

                        }
                    } else {
                        Log.d(TAG, "polyline draw error")
                    }

                } else {
                    handler.post {
                        Toast.makeText(this@MainActivity, "Network Error: Cannot show path", Toast.LENGTH_LONG).show()
                    }
                }
            }

        })

    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap

        centerMapOnDriver()

        map!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker?) = null

            override fun getInfoWindow(marker: Marker?): View? {
                val v: View = layoutInflater.inflate(R.layout.marker_layout, null)
                if (marker != null) {
                    v.nameTextView.text = marker.title
                    v.snippetTextView.text = marker.snippet
                }
                return (v)
            }

        })

        if ( checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ask for permissions
            Log.e(TAG, "location access denied in onmapready")
            checkLocationPermission()
            return
        }
        Log.e(TAG, "location access granted in onmapready")

        map?.isMyLocationEnabled = true
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //TODO: ensure user has GPS High accuracy enabled and prompt them to if not
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, DRIVER_LOCATION_DEBOUNCE_MILLS, 0f, this)
        } catch (ex: SecurityException) {
            Log.e(TAG, "SEC EX")
        }
    }

    private fun addMarker() {
        val url: HttpUrl = baseUrl.newBuilder("insertRider")!!
                .addQueryParameter("active", "true") //TODO take in current driver location
                .addQueryParameter("latitude", "38.822057")
                .addQueryParameter("longitude", "-90.700158")
                .addQueryParameter("name", "Jonas")
                .addQueryParameter("phone", "6366976421")
                .addQueryParameter("address", "mcdondalds")
                .build()


        val request: Request = Request.Builder().url(url).build()
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e(TAG, "insert call failure:" + e.toString())
            }

            override fun onResponse(call: Call?, response: Response?) {
                Log.e(TAG, "insert response: " + response?.body()!!.string())
            }

        })
    }

    //Below is code for getting directions from current location to rider

    private fun getDirectionsUrl(origin: LatLng, dest: String): String {
        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest

        // Sensor enabled
        val sensor = "sensor=false"

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor"

        // Output format
        val output = "json"

        // Building the url to the web service

        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

    //Helper to check if network response is correct or an error
    fun isJSONValid(test: String): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }


    private fun checkLocationPermission(): Boolean {
        if (checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { p0, p1 -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION) })
                        .create().show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
            }
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,grantResults: IntArray) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        //Request location updates:
                        //locationManager.requestLocationUpdates(provider, 400, 1, this)
                        map?.isMyLocationEnabled = true
                        //TODO set up map w location
                    }
                } else {
                    // permission denied, boo! Disable the
                    //TODO: prompt that we need location to work
                }
                return
        }
    }

}
