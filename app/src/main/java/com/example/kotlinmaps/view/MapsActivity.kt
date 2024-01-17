package com.example.kotlinmaps.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.LocationListener
import android.location.LocationManager
import android.view.View
import androidx.room.Room
import com.example.kotlinmaps.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.kotlinmaps.databinding.ActivityMapsBinding
import com.example.kotlinmaps.model.Place
import com.example.kotlinmaps.roomdb.PlaceDao
import com.example.kotlinmaps.roomdb.PlaceDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
//import com.google.android.gms.location.LocationListener
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean: Boolean? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.example.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitude = 0.0

        //room başlatma
        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java ,"Places")
        //  .allowMainThreadQueries() // UI tread da çalıştırıyor ama biz RxJava ile yapacağız.
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false

    }


        @SuppressLint("ServiceCast")
        override fun onMapReady(googleMap: GoogleMap) {
            mMap = googleMap
            mMap.setOnMapLongClickListener(this)

            val intent = intent
            val info = intent.getStringExtra("info")

            if(info == "new"){

                binding.saveButton.visibility = View.VISIBLE
                binding.deleteButton.visibility = View.GONE // gone hem görünmez yapar hemde yer tutmaz invisible yerini tutar
                // casting
                locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

                // her konum değiştiğinde bize verecek
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {

                        // daha önce storagede konum var mı kontrol et
                        trackBoolean = sharedPreferences.getBoolean("trackBoolean", false) // ilk defa çalıştıgında bu false olacak
                        if (trackBoolean == false ){
                            val userLocation = LatLng(location.latitude, location.longitude)
                            println(userLocation)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                            sharedPreferences.edit().putBoolean("trackBoolean", true).apply() // değeri true yap

                        }

                    }
                }

                if  (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    // izin yok
                    // mesaj göstermeli miyiz. Kendisi karar veriyor.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                        // true dönerse izin istiyoruz
                        Snackbar.make(binding.root, "Lokasyon izni gerekiyor", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver"){
                            // izin iste
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }.show()
                    }else {
                        // false dönersede izin istiyoruz
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                }else {
                    // 10 metre
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f, locationListener)
                    val lastLocation  = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    println(lastLocation)
                    println("AAA")
                    if (lastLocation != null) {
                        println("${lastLocation.latitude}")
                        println("${lastLocation.longitude}")
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                    }
                    mMap.isMyLocationEnabled = true // harita üzerinde mavi işaret


                }

            }else{
                mMap.clear()
                placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
                print("BEEEEEE")
                println(placeFromMain.toString())

                placeFromMain?.let {
                    // nul değil ise
                    val latlng: LatLng = LatLng(it.latitude, it.longitude)
                    mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15f))
                    binding.editText.setText(it.name)
                    binding.saveButton.visibility = View.GONE
                    binding.deleteButton.visibility = View.VISIBLE
                }
            }
        }


        private fun registerLauncher(){

            permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
                // evet izin verildi veya hayır izin verilmedi
                if(result){
                    // izin verilmiş ise
                    if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED ) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000,
                            10f,
                            locationListener
                        )

                        val lastLocation  = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        println(lastLocation)
                        println("AAA")
                        if (lastLocation != null) {
                            println("${lastLocation.latitude}")
                            println("${lastLocation.longitude}")
                            val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                        }
                        mMap.isMyLocationEnabled = true // harita üzerinde mavi işaret

                    }
                }else {
                    Toast.makeText(this@MapsActivity, "izin gerekli!", Toast.LENGTH_LONG).show()
                }
            }
        }



        override fun onMapLongClick(p0: LatLng) {
            mMap.clear() // önceki seçilenleri sil
            mMap.addMarker(MarkerOptions().position(p0))
            selectedLatitude = p0.latitude
            selectedLongitude = p0.longitude
            binding.saveButton.isEnabled = true
            }



        fun save (view : View){
            if (selectedLatitude != null && selectedLongitude != null){
                val place = Place(binding.editText.text.toString(),selectedLatitude!!,selectedLongitude!!)
                compositeDisposable.add(
                    placeDao.instert(place)
                        .subscribeOn(Schedulers.io()) // arka planda çalıştır
                        .observeOn(AndroidSchedulers.mainThread()) // main thread'de gözlemle
                        .subscribe(this::handleResponse) // bitincede referans verdigimiz handleResponse'yi çalıştır
                )
            }
        }


        private fun handleResponse(){
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }



        fun delete (view : View){
            placeFromMain?.let {
                compositeDisposable.add(
                    placeDao.delete(it)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )
            }
        }



        override fun onDestroy() {
            super.onDestroy()

            compositeDisposable.clear()
        }

}