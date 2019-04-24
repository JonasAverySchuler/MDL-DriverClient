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
            //TODO: socket to join event. does starting the app profucve a join event oin the server?
            //TODO: user logij is the user able to log in using credentials stored on the db?

            startActivity(MainActivity.intent(this))
        }
    }
}