package mx.edu.ittepic.ladm_u5_practica1_victorcamberos

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.LocaleList
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var ubicacionCliente : FusedLocationProviderClient
    var baseRemota = FirebaseFirestore.getInstance()//para tener acceso a la base de datos.
    lateinit var locacion : LocationManager
    var posicion = ArrayList<Data>()
    var ubicacionActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        }
//---------------------------------------------------------------------------------------------------------
        //OBTENER DATOS DE FIREBASE
        baseRemota.collection("tecnologico")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException != null){
                    Toast.makeText(this,"ERROR: "+firebaseFirestoreException.message,Toast.LENGTH_LONG)
                    return@addSnapshotListener
                }
                posicion.clear()
                for(document in querySnapshot!!){
                    var data = Data()
                    data.nombre = document.getString("nombre").toString()
                    data.descripcion = document.getString("Descripcion").toString()
                    data.posicion1 = document.getGeoPoint("posicion1")!!

                    data.posicion2 = document.getGeoPoint("posicion2")!!

                    posicion.add(data)
                }
            }
        locacion = getSystemService(Context.LOCATION_SERVICE) as LocationManager //indicamos que usaremos servicio de ubicación
        var oyente = Oyente(this)
        locacion.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,01f,oyente)//proveedor, minutos distancia,minutos actualizacion,oyente

//---------------------------------------------------------------------------------------------------------

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        ubicacionCliente = LocationServices.getFusedLocationProviderClient(this) //trae la última ubicación reconocida por el clinete
    }

    fun alerta(mensaje:String, descripcion:String){
        AlertDialog.Builder(this)
            .setTitle("UBICACIÓN")
            .setMessage("ESTÁS EN: $mensaje \nDESCRIPCIÓN: $descripcion")
            .show()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 700){
            setTitle("SE OTORGÓ PERMISO")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val laboratorio = LatLng(21.477785, -104.867027)
        mMap.addMarker(MarkerOptions().position(laboratorio).title("Laboratorio de Cómputo"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(laboratorio))
        //CONTROLES DE ZOOM
        mMap.uiSettings.isZoomControlsEnabled = true
        //hacer que el mapa muestre el clásico punto azul señalando nuestra ubcación actual
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.isMyLocationEnabled = true
        ubicacionCliente.lastLocation.addOnSuccessListener{
            val posicionActual = LatLng(it.latitude, it.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual,15f))
        }
    }
}

class Oyente(puntero:MapsActivity) : LocationListener {
    var p = puntero
    override fun onLocationChanged(location: Location) {
        var geoPosicionGPS = GeoPoint(location.latitude,location.longitude)
        for(item in p.posicion){
            if(item.estoyEn(geoPosicionGPS)){
                p.alerta(item.nombre,item.descripcion)
            }
        }
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

}

