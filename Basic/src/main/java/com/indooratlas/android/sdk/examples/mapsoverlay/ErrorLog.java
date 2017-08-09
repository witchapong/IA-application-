package com.indooratlas.android.sdk.examples.mapsoverlay;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;


public class ErrorLog {
    private long ts;
    private LatLng checkedIn;
    private LatLng IALocation;
    private double error;

    ErrorLog(LatLng ll1,LatLng ll2){

        checkedIn = ll1;
        IALocation = ll2;
        ts = System.currentTimeMillis()/1000;
        error = getDistanceBtwPnts(ll1, ll2);

    }

    private double getDistanceBtwPnts(LatLng ll1, LatLng ll2){

        Location locationA = new Location("point A");
        locationA.setLatitude(ll1.latitude);
        locationA.setLongitude(ll1.longitude);

        Location locationB = new Location("point B");
        locationB.setLatitude(ll2.latitude);
        locationB.setLongitude(ll2.longitude);

        double error = locationA.distanceTo(locationB);

        return error;
    }

    private long getTimeStamp (){
        return ts;
    }
}
