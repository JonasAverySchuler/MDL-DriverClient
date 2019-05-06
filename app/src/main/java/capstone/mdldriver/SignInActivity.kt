package capstone.mdldriver

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.sigin_activity.createAccountButton
import kotlinx.android.synthetic.main.sigin_activity.nameTextView
import kotlinx.android.synthetic.main.sigin_activity.phoneTextView
import kotlinx.android.synthetic.main.sigin_activity.singInButton

private const val TAG = "SignInActivity"

class SignInActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sigin_activity)
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //fillInFieldsForTesting() //testing only

        singInButton.setOnClickListener {
            //TODO: socket to join event. does starting the app profucve a join event oin the server?
            //TODO: user logij is the user able to log in using credentials stored on the db?

            if (nameTextView.text.isNullOrEmpty() || phoneTextView.text.isNullOrEmpty()) {
                Toast.makeText(this,"Please fill in all fields to sign in!", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(MainActivity.intent(this, nameTextView.text.toString(), phoneTextView.text.toString()))
            }
        }

        createAccountButton.setOnClickListener {
            startActivity(SignUpActivity.Intent())
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        //updateui mayhbe

    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun createNewDriverAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    // ...
                }
    }

    private fun signInDriver(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    // ...
                }
    }

    private fun fillInFieldsForTesting() {
        nameTextView.text = Editable.Factory.getInstance().newEditable("Jonas")
        phoneTextView.text = Editable.Factory.getInstance().newEditable("5555555555")
    }
}