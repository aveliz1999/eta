package com.veliz99.eta

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.auth.FirebaseAuth
import com.veliz99.eta.adapter.AddressAdapter
import com.veliz99.eta.service.LocationService
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var map: GoogleMap
    lateinit var geocoder: Geocoder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        geocoder = Geocoder(this, Locale.getDefault())

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            map = it
            it.isMyLocationEnabled = true
            it.uiSettings.isMyLocationButtonEnabled = true
            it.setOnMapLongClickListener {
                val address = geocoder.getFromLocation(it.latitude, it.longitude, 1)[0] as Address
                this@MainActivity.let {
                    val a = AlertDialog.Builder(it, R.style.ThemeOverlay_AppCompat_Dialog)
                        .setTitle("Title")
                        .setMessage(address.getAddressLine(0))
                        .setPositiveButton("Yes") { _, _ -> startService(address.latitude, address.longitude)}
                        .setNegativeButton("No") {_, _ -> }
                        .create()
                    a.show()
                }
            }
        }

        val autocompleteTextView = findViewById<AutoCompleteTextView>(R.id.autocompleteTextView_search)
        autocompleteTextView.setHorizontallyScrolling(true)
        autocompleteTextView.apply {
            setAdapter(AddressAdapter(this@MainActivity))
            setOnItemClickListener { _, _, position, _ ->
                val address =  (adapter.getItem(position) as Address)
                setText(address.getAddressLine(0))
                this@MainActivity.let {
                    val a = AlertDialog.Builder(it, R.style.ThemeOverlay_AppCompat_Dialog)
                        .setTitle("Confirm Location")
                        .setMessage("Do you want to start navigating to " + address.getAddressLine(0) + "?")
                        .setPositiveButton("Yes") { _, _ -> startService(address.latitude, address.longitude)}
                        .setNegativeButton("No") { _, _ -> setText("") }
                        .create()
                    a.show()
                }
            }
        }
    }

    private fun startService(lat: Double, long: Double) {
        fun start() {
            val intent = Intent(this@MainActivity, LocationService::class.java)
            intent.putExtra("latitude", lat)
            intent.putExtra("longitude", long)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForegroundService(intent)
            }
            else{
                startService(intent)
            }

            val navigationUri = Uri.parse("google.navigation:q=$lat,$long")
            val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri)
            navigationIntent.`package` = "com.google.android.apps.maps"
            if(navigationIntent.resolveActivity(packageManager) !== null) {
                startActivity(navigationIntent)
            }
            else{
                this@MainActivity.let {
                    val a = AlertDialog.Builder(it, R.style.ThemeOverlay_AppCompat_Dialog)
                        .setTitle("Maps Missing")
                        .setMessage("Cannot find the google maps app to launch navigation. Your location will still be tracked in the background.")
                        .setNeutralButton("OK") { _, _ ->  }
                        .create()
                    a.show()
                }
            }
        }

        if(FirebaseAuth.getInstance().currentUser === null) {
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener {
                if(it.isSuccessful) {
                    start()
                }
                else{
                    this@MainActivity.let {
                        val a = AlertDialog.Builder(it, R.style.ThemeOverlay_AppCompat_Dialog)
                            .setTitle("Error Creating ETA")
                            .setMessage("Cannot sign in to create the ETA. Please try again.")
                            .setNeutralButton("OK") { _, _ ->  }
                            .create()
                        a.show()
                    }
                }
            }
        }
        else{
            start()
        }

    }
}
