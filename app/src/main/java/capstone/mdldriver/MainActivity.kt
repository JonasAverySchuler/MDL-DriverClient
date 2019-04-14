package capstone.mdldriver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import android.support.v4.app.ActivityCompat.*
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.ridersRecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

private const val TAG = "MainActivity"
private const val MY_PERMISSIONS_REQUEST_LOCATION = 99

class MainActivity : FragmentActivity(), OnMapReadyCallback, RidersRecyclerViewAdapter.Listener {
    private val baseUrl: HttpUrl =  HttpUrl.get("http://jl-m.org:8000/")
    private var map: GoogleMap? = null
    private var latLng: LatLng? = null
    private var riders: List<Rider> = emptyList()
    private val httpClient = OkHttpClient.Builder().build()
    private val zoom = 16.0f

    private lateinit var adapter: RidersRecyclerViewAdapter

    companion object {
        fun intent(context: Context)= Intent(context, MainActivity::class.java) //TODO: maybe take in drivers id?
    }

    override fun onRiderClick(rider: Rider) {
        //TODO go to marker on map
        //When recylerview item is clicked, go to rider location on map and show route
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(rider.location.coordinates.lat,rider.location.coordinates.long), zoom))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        adapter = RidersRecyclerViewAdapter(riders)
        adapter.listener = this
        ridersRecyclerView.adapter = adapter
        //TODO: listen for new riders popping up. in manual refresh button

        //TODO: show current lat/long of driver on map, then pop up near riders

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult!!.getLocations()) {
                    updateUI(location)
                }
            }
        }

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
        Log.e(TAG, "onmapready")
        map = googleMap
        if (checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ask for permissions
            Log.e(TAG, "location access denied")
            checkLocationPermission()
            return
        }
        map?.isMyLocationEnabled = true
        initialUISetup()

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
                        initialUISetup()
                    }
                } else {
                    // permission denied, boo! Disable the
                    //TODO: prompt that we need location to work
                }
                return
        }
    }



    private fun updateUI(location: Location) {
        val zoom = 16.0f
        latLng = LatLng(location.latitude, location.longitude)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

    }

    private fun initialUISetup() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ( checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //ask for permissions, shouldnt happen here since we ask in onmapready
            return
        }
        val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val longitude = location.longitude
        val latitude = location.latitude
        latLng = LatLng(latitude, longitude)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        setMarkers()
    }

    private fun setMarkers() {

        val url: HttpUrl = baseUrl.newBuilder("nearbyRiders")!!
                .addQueryParameter("latitude", "-7.955169") //TODO take in current driver location
                .addQueryParameter("longitude", "-5.524351")
                .build()


        val request: Request = Request.Builder().url(url).build()
        val handler = Handler(Looper.getMainLooper())

        httpClient.newCall(request).enqueue( object: Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e(TAG, "call failure")
                handler.post { //UI manipulation must be ran on Main thread
                    //TODO error
                }
            }

            override fun onResponse(call: Call?, response: Response?) {
                val jsonString = response!!.body()!!.string().toString()
                val jsonObject = JSONObject(jsonString)
                Log.e(TAG, "jsonResponse:$jsonObject")
                val jsonArray = jsonObject.getJSONArray("riders")
                val updatedRiders = mutableListOf<Rider>()
                Log.e(TAG, "riders jsonArray size: " + jsonArray.length())
                for(i in 0 until jsonArray.length()) {
                    val riderJSONObject = jsonArray.getJSONObject(i)
                    val _id = riderJSONObject.getString("_id")
                    val id = riderJSONObject.getInt("id")
                    val active = riderJSONObject.getBoolean("active")
                    val name = riderJSONObject.getString("name")
                    val phone = riderJSONObject.getString("phone")
                    val locationJSONObject = riderJSONObject.getJSONObject("location")
                    val locationType = locationJSONObject.getString("type")
                    val locationAddress = locationJSONObject.getString("address")
                    val locationCoordinatesJSONArray = locationJSONObject.getJSONArray("coordinates")
                    val locationLat = locationCoordinatesJSONArray.getDouble(0)
                    val locationLong = locationCoordinatesJSONArray.getDouble(1)

                    updatedRiders += Rider(_id, id, active, name, phone, Location(locationType, locationAddress, Coordinates(locationLat, locationLong)))
                }

                handler.post { //UI manipulation must be ran on Main thread
                    adapter.updateRiders(updatedRiders)
                    updatedRiders.forEach {
                        map?.addMarker(MarkerOptions().position(LatLng(it.location.coordinates.lat, it.location.coordinates.long)).title(it.location.address))
                    }
                }

            }

        })
    }


}
