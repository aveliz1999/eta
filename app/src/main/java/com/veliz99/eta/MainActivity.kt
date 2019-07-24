package com.veliz99.eta

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.veliz99.eta.adapter.AddressAdapter
import com.veliz99.eta.service.LocationService
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var addressInput: AutoCompleteTextView
    lateinit var avoidTollCheckbox: CheckBox
    lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lateinit var address: Address

        addressInput = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView_address)
            .apply {
                setAdapter(AddressAdapter(this@MainActivity))
                setOnItemClickListener { parent, view, position, id ->
                    startButton.isEnabled = true
                    address = adapter.getItem(position) as Address

                }
            }

        avoidTollCheckbox = findViewById(R.id.checkBox_avoid_tolls)
        startButton = findViewById<Button>(R.id.button_start).apply {
            setOnClickListener {
                addressInput.isEnabled = false
                isEnabled = false

                FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener {
                    if(it.isSuccessful) {
                        val intent = Intent(this@MainActivity, LocationService::class.java)
                        intent.putExtra("latitude", address.latitude)
                        intent.putExtra("longitude", address.longitude)


                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            startForegroundService(intent)
                        }
                        else{
                            startService(intent)
                        }
                    }
                    else{
                        Toast.makeText(this@MainActivity, "Failed to create temporary user. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }
}
