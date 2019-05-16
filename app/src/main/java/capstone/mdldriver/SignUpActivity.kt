package capstone.mdldriver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.signup_activity.createAccountButton
import kotlinx.android.synthetic.main.signup_activity.displayNameEditText
import kotlinx.android.synthetic.main.signup_activity.emailEditText
import kotlinx.android.synthetic.main.signup_activity.passwordEditText
import kotlinx.android.synthetic.main.signup_activity.phoneNumberEditText

class SignUpActivity : FragmentActivity(){
        private lateinit var auth : FirebaseAuth

    companion object {
        fun intent(context: Context) = Intent(context, SignUpActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)
        auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        createAccountButton.setOnClickListener {
            if (emailEditText.text.isNullOrEmpty() || passwordEditText.text.isNullOrEmpty() || displayNameEditText.text.isNullOrEmpty() || phoneNumberEditText.text.isNullOrEmpty()) {
                Toast.makeText(this, "Please Fill out all fields.", Toast.LENGTH_LONG).show()
            } else {
                auth.createUserWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = auth.currentUser
                        val newDriver = Driver(user!!.uid, displayNameEditText.text.toString(), emailEditText.text.toString(), phoneNumberEditText.text.toString(), passwordEditText.text.toString())
                        database.child("users").child(newDriver.uid).setValue(newDriver)
                        startActivity(MainActivity.intent(this, newDriver))
                        finish()
                    } else {
                        Toast.makeText(this, "Account creation failure", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}