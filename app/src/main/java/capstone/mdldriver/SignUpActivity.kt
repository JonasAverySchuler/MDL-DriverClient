package capstone.mdldriver

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import kotlinx.android.synthetic.main.activity_main.cancelOrRefreshButton
import kotlinx.android.synthetic.main.activity_main.etaTextView
import kotlinx.android.synthetic.main.activity_main.map
import kotlinx.android.synthetic.main.activity_main.ridersRecyclerView
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class SignUpActivity : FragmentActivity(){
    private val baseUrl: HttpUrl = HttpUrl.get("http://jl-m.org:8000/")


    companion object {
        fun intent(context: Context) = Intent(context, SignUpActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}