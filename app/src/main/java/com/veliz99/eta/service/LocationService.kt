package com.veliz99.eta.service

import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.veliz99.eta.MainActivity
import com.veliz99.eta.R

private const val CHANNEL_ID = "location"
private const val CHANNEL_NAME = "Location Update"
private const val CHANNEL_DESCRIPTION = "Updates your location and traffic information while you drive"

private const val NOTIFICATION_ID = 2799

private const val TAG = "LocationService"

private const val EXTRA_STARTED_FROM_NOTIFICATION = "com.veliz99.eta.service.startedFromNotification"

class LocationService : Service() {

    private lateinit var locationClient: FusedLocationProviderClient

    private lateinit var notificationManager: NotificationManager

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val binder = Binder()

    private lateinit var documentId: String
    private var currentLocation: Location = Location("dummyprovider")

    private val handler = Handler()

    private var lastTimestamp = System.currentTimeMillis()

    override fun onCreate() {
        Log.i(TAG, "Location service created")
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        documentId = FirebaseFirestore.getInstance()
            .collection("etas")
            .document().id

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(documentId, documentId)
        clipboard.setPrimaryClip(clip)

        locationRequest = LocationRequest().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Location service started")
        val fromNotification = intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false) ?: false

        if(fromNotification) {
            Log.i(TAG, "Stopping location updates")
            locationClient.removeLocationUpdates(locationCallback)
            stopSelf()
        }
        else if(intent !== null) {
            startForeground(NOTIFICATION_ID, createNotification())

            locationCallback = object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    super.onLocationResult(locationResult)
                    if(locationResult == null) {
                        return
                    }
                    val location = locationResult.lastLocation
                    val targetLatitude = intent.getDoubleExtra("latitude", 0.0)
                    val targetLongitude = intent.getDoubleExtra("longitude", 0.0)
                    if(currentLocation.latitude != location.latitude || currentLocation.longitude != location.longitude) {
                        currentLocation = location
                        FirebaseFirestore.getInstance()
                            .collection("etas")
                            .document(documentId)
                            .set(mapOf(
                                "creator" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "location" to GeoPoint(location.latitude, location.longitude),
                                "target" to GeoPoint(targetLatitude, targetLongitude),
                                "updated" to FieldValue.serverTimestamp()
                            ), SetOptions.merge())
                    }
                    if(System.currentTimeMillis() - lastTimestamp > 60000) {
                        FirebaseFunctions.getInstance().getHttpsCallable("updateETA")
                            .call(mapOf("id" to documentId))
                        lastTimestamp = System.currentTimeMillis()
                    }
                }
            }

            requestLocationUpdates()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "Service binding")
        return binder
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, this::class.java)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
        val serviceAction = NotificationCompat.Action(R.drawable.stop, "Stop", servicePendingIntent)

        val activityPendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), 0)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText("Keeping your location up to date as your drive")
            .setContentTitle("Updating location...")
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(activityPendingIntent)
            .addAction(serviceAction)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        return builder.build()
    }

    private fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")
        try{
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
        catch (securityException: SecurityException) {
            Log.e(TAG, "Lost location permission")
        }
    }
}
