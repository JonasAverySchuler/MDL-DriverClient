package capstone.mdldriver

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.sigin_activity.nameTextView
import kotlinx.android.synthetic.main.sigin_activity.phoneTextView
import kotlinx.android.synthetic.main.sigin_activity.singInButton

class SignInActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sigin_activity)
        singInButton.setOnClickListener {
            //TODO: facebook/google sign in with Oauth
            //TODO: socket to join event. does starting the app profucve a join event oin the server?
            //TODO: user logij is the user able to log in using credentials stored on the db?

            if (nameTextView.text.isNullOrEmpty() || phoneTextView.text.isNullOrEmpty()) {
                Toast.makeText(this,"Please fill in all fields to sign in!", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(MainActivity.intent(this, nameTextView.text.toString(), phoneTextView.text.toString()))
            }
        }
    }
}