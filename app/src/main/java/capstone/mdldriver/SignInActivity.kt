package capstone.mdldriver

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.sigin_activity.singInButton

class SignInActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sigin_activity)
        singInButton.setOnClickListener {
            //TODO: facebook/google sign in with Oauth
            startActivity(MainActivity.intent(this))
        }
    }
}