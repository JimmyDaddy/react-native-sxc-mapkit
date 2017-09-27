/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.content.Context;
import android.util.Log;

import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.LocationProvider;
import com.tenforwardconsulting.bgloc.DistanceFilterLocationProvider;
import com.marianhello.bgloc.ActivityRecognitionLocationProvider;
import com.tenforwardconsulting.bgloc.TimeTaskLocationProvider;

import java.lang.IllegalArgumentException;

/**
 * LocationProviderFactory
 */
public class LocationProviderFactory {

    private LocationService context;

    public LocationProviderFactory(LocationService context) {
        this.context = context;
    };

    public LocationProvider getInstance (Integer locationProvider) {
        LocationProvider provider;
        switch (locationProvider) {
            case Config.ANDROID_DISTANCE_FILTER_PROVIDER:
                Log.e("LocationProviderFactory", "getInstance: ANDROID_DISTANCE_FILTER_PROVIDER");
                provider = new DistanceFilterLocationProvider(context);
                break;
            case Config.ANDROID_ACTIVITY_PROVIDER:
                Log.e("LocationProviderFactory", "getInstance: ANDROID_ACTIVITY_PROVIDER");
                provider = new ActivityRecognitionLocationProvider(context);
                break;
            case Config.TIMER_PROVIDE:
                Log.e("LocationProviderFactory", "getInstance: TIMER_PROVIDE");
                provider = new TimeTaskLocationProvider(context);
                break;
            default:
                Log.e("LocationProviderFactory", "getInstance: ANDROID_DISTANCE_FILTER_PROVIDER DEFAULT");
                provider = new DistanceFilterLocationProvider(context);
        }

        provider.onCreate();
        return provider;
    }
}
