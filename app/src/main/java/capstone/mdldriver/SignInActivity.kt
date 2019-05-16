package capstone.mdldriver

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.sigin_activity.createAccountButton
import kotlinx.android.synthetic.main.sigin_activity.nameTextView
import kotlinx.android.synthetic.main.sigin_activity.phoneTextView
import kotlinx.android.synthetic.main.sigin_activity.singInButton

private const val TAG = "SignInActivity"

class SignInActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sigin_activity)
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        singInButton.setOnClickListener {
            //TODO: socket to join event. does starting the app profucve a join event oin the server?
            //TODO: user logij is the user able to log in using credentials stored on the db?

            if (nameTextView.text.isNullOrEmpty() || phoneTextView.text.isNullOrEmpty()) {
                Toast.makeText(this,"Please fill in all fields to sign in!", Toast.LENGTH_SHORT).show()
            } else {
                signInDriver(nameTextView.text.toString(), phoneTextView.text.toString())
            }
        }

        createAccountButton.setOnClickListener {
            startActivity(SignUpActivity.intent(this))
        }
    }

    private fun signInDriver(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        database.child("users").child(user!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                                Toast.makeText(this@SignInActivity, "error", Toast.LENGTH_SHORT).show()
                            }
                            override fun onDataChange(p0: DataSnapshot) {
                                Log.e(TAG, p0.toString())
                                val value = p0.value
                                Log.e(TAG, "snapshot: " + value)
                                val driverVals = value as HashMap<String, String>
                                val driver = Driver(user.uid, driverVals.get("displayName")!!, driverVals.get("email")!!, driverVals.get("phoneNumber")!!, driverVals.get("passWord")!!)
                                Log.e(TAG, "driver!!!" + driver.toString())

                                startActivity(MainActivity.intent(this@SignInActivity, driver ))


                            }
                        })

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Incorrect email or password", Toast.LENGTH_SHORT).show()
                    }
                }
    }
}