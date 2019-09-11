package com.veliz99.eta

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Criteria
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.veliz99.eta.adapter.AddressAdapter
import com.veliz99.eta.service.LocationService
import java.util.*

const val LOCATION_PERMISSION_REQUEST = 13577

class MainActivity : AppCompatActivity() {

    lateinit var map: GoogleMap
    lateinit var geocoder: Geocoder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupUi()
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun setupUi() {
        geocoder = Geocoder(this, Locale.getDefault())
        val locationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            map = it
            it.isMyLocationEnabled = true
            it.uiSettings.isMyLocationButtonEnabled = true

            map.setOnMyLocationButtonClickListener {
                locationClient.lastLocation.addOnSuccessListener {
                    val latLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
                true
            }

            val myLocationButton = mapFragment.view!!.findViewWithTag<View>("GoogleMapMyLocationButton")
            val myLocationButtonLayoutParams = myLocationButton.layoutParams as RelativeLayout.LayoutParams
            myLocationButtonLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            myLocationButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            myLocationButton.setPadding(myLocationButton.paddingLeft, myLocationButton.paddingTop, 25, 25)


            it.setOnMapLongClickListener {
                val address = geocoder.getFromLocation(it.latitude, it.longitude, 1)[0] as Address
                this@MainActivity.let {
                    val a = AlertDialog.Builder(it, R.style.ThemeOverlay_AppCompat_Dialog)
                        .setTitle("Confirm Location")
                        .setMessage("Do you want to start navigating to " + address.getAddressLine(0) + "?")
                        .setPositiveButton("Yes") { _, _ -> startService(address.latitude, address.longitude, true)}
                        .setNeutralButton("Track Without Navigation") { _, _ -> startService(address.latitude, address.longitude, false)}
                        .setNegativeButton("No") { _, _ -> }
                        .create()
                    a.show()
                }
            }
        }

        locationClient.lastLocation.addOnSuccessListener {
            val latLng = LatLng(it.latitude, it.longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
                        .setPositiveButton("Yes") { _, _ -> startService(address.latitude, address.longitude, true)}
                        .setNeutralButton("Track Without Navigation") { _, _ -> startService(address.latitude, address.longitude, false)}
                        .setNegativeButton("No") { _, _ -> setText("") }
                        .create()
                    a.show()
                }
            }
        }
    }

    private fun startService(lat: Double, long: Double, navigate: Boolean) {
        fun start() {
            val etaDocumentId = FirebaseFirestore.getInstance()
                .collection("etas")
                .document().id

            val intent = Intent(this@MainActivity, LocationService::class.java)
            intent.putExtra("latitude", lat)
            intent.putExtra("longitude", long)
            intent.putExtra("document", etaDocumentId)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForegroundService(intent)
            }
            else{
                startService(intent)
            }

            if(navigate) {
                val navigationUri = Uri.parse("google.navigation:q=$lat,$long")
                val navigationIntent = Intent(Intent.ACTION_VIEW, navigationUri)
                navigationIntent.`package` = "com.google.android.apps.maps"
                if(navigationIntent.resolveActivity(packageManager) !== null) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("documentId", "${getString(R.string.website_path)}${etaDocumentId}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Website URL copied to your clipboard", Toast.LENGTH_LONG).show()
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
            else{
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${getString(R.string.website_path)}${etaDocumentId}")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share ETA")
                startActivityForResult(shareIntent, 0)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray) {
        when(requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupUi()
                }
                else{
                    Toast.makeText(this, "Location permission is required for this app to work!", Toast.LENGTH_LONG).show()
                    finishAndRemoveTask()
                }
            }
        }
    }
}
