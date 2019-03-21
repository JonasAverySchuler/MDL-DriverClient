package capstone.mdldriver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.textView1
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val baseUrl:HttpUrl =  HttpUrl.get("http://jl-m.org:8000/")


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

        httpClient.newCall(request).enqueue( object: Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("YO", "fail")
                //TODO: add fail state
            }

            override fun onResponse(call: Call?, response: Response?) {
                Log.e("YO", response!!.body()!!.source().readUtf8())
                textView1.text = response.body()!!.source().readUtf8()
                //TODO: put into objects
            }

        })
        //TODO: listen for new riders popping up. in manual refresh button

        //TODO: show current lat/long of driver on map, then pop up near riders

    }
}
