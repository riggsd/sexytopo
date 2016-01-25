package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;

import org.hwyl.sexytopo.control.Log;


/**
 * Created by driggs on 1/22/16.
 */
public class MagneticDeclination {

    public static float getDeclination(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(String.format("GPS %s enabled.", isGPSEnabled ? "is" : "is NOT"));
        boolean isNetworkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(String.format("Network %s enabled.", isNetworkEnabled ? "is" : "is NOT"));

        Location loc = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        float lat = (float) loc.getLatitude();
        float lon = (float) loc.getLongitude();
        float alt = (float) (loc.hasAltitude() ? loc.getAltitude() : -0.0);
        Log.d(String.format("Calculating magnetic declination for (%.3f, %.3f, %.1fm)", lat, lon, alt));

        // TODO: Android's GeomagneticField class uses the WMM2010 coefficients, we should use WMM2015 for more accurate model in range 2015 - 2020
        GeomagneticField field = new GeomagneticField(lat, lon, alt, System.currentTimeMillis());
        Log.d(String.format(
                "Geomagnetic field:  declination=%.2f°  inclination=%.2f°  strength=%.1fnT  horizstrength=%.1fnT",
                field.getDeclination(), field.getInclination(), field.getFieldStrength(), field.getHorizontalStrength()));
        return field.getDeclination();
    }

}
