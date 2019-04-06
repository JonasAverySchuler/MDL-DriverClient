package capstone.mdldriver

import android.Manifest
import android.app.Activity
import android.content.Context
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
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_main.textView1
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val baseUrl: HttpUrl =  HttpUrl.get("http://jl-m.org:8000/")
    private var map: GoogleMap? = null
    private var latLng: LatLng? = null

    companion object {
        fun intent(context: Context)= Intent(context, MainActivity::class.java) //TODO: maybe take in drivers id?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val httpClient = OkHttpClient.Builder().build()

        val url: HttpUrl = baseUrl.newBuilder("nearbyRiders")!!
                .addQueryParameter("latitude", "-7.955169") //TODO take in current driver location
                .addQueryParameter("longitude", "-5.524351")
                .build()

        val request: Request = Request.Builder().url(url).build()
        val handler = Handler(Looper.getMainLooper())

        httpClient.newCall(request).enqueue( object: Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("YO", "fail")
                handler.post { //UI manipulation must be ran on Main thread
                    //TODO error
                }
                //TODO: add fail state
            }

            override fun onResponse(call: Call?, response: Response?) {
                Log.e("YO", response!!.body()!!.source().readUtf8())

                handler.post { //UI manipulation must be ran on Main thread
                    textView1.text = response.body()!!.source().readUtf8()
                }
                //TODO: put into objects
            }

        })
        //TODO: listen for new riders popping up. in manual refresh button

        //TODO: show current lat/long of driver on map, then pop up near riders


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        map = googleMap
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ask for permissions
            return
        }
        map?.setMyLocationEnabled(true)
        initialUISetup()

    }

    private fun updateUI(location: Location) {
        val zoom = 16.0f
        latLng = LatLng(location.latitude, location.longitude)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

    }

    private fun initialUISetup() {
        val zoom = 16.0f
        //latLng = new LatLng(38.941034,-92.338482);
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //ask for permissions
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

    }


}
