package com.veliz99.eta

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.veliz99.eta.service.LocationService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener {
            if(it.isSuccessful) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    startForegroundService(Intent(this, LocationService::class.java))
                }
                else{
                    startService(Intent(this, LocationService::class.java))
                }
            }
            else{
                Toast.makeText(this, "Failed to create temporary user. Please try again.", Toast.LENGTH_LONG).show()
            }
        }

    }
}
