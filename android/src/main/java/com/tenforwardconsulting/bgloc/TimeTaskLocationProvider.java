package com.tenforwardconsulting.bgloc;

import com.marianhello.bgloc.AbstractLocationProvider;
import com.marianhello.bgloc.LocationService;
import com.marianhello.logging.LoggerManager;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;

/**
 * Created by jimmydaddy on 2017/6/23.
 */

public class TimeTaskLocationProvider extends AbstractLocationProvider implements LocationListener {
    private static final String TAG = TimeTaskLocationProvider.class.getSimpleName();
    private static final String P_NAME = "com.tenforwardconsulting.cordova.bgloc";

    private static final String TIMER_LOCATION_UPDATE_ACTION   = P_NAME + ".TIMER_LOCATION_UPDATE_ACTION";

    private Boolean isMoving = false;

    private PowerManager.WakeLock wakeLock;

    private PendingIntent singleUpdatePI;

    private String activity;
    private Criteria criteria;

    private LocationManager locationManager;
    private AlarmManager alarmManager;
    private NotificationManager notificationManager;

    private org.slf4j.Logger log;

    public TimeTaskLocationProvider(LocationService context) {
        super(context);
        PROVIDER_ID = 2;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log = LoggerManager.getLogger(TimeTaskLocationProvider.class);
        log.info("Creating TimerTaskLocationProvider");

        long triggerAtTime = SystemClock.elapsedRealtime();//开机至今的时间毫秒数

        triggerAtTime=triggerAtTime+this.config.getInterval();//比开机至今的时间增长10秒

        locationManager = (LocationManager) locationService.getSystemService(Context.LOCATION_SERVICE);
        alarmManager = (AlarmManager) locationService.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(TIMER_LOCATION_UPDATE_ACTION);
        intent.putExtra("config", locationService.getConfig());
        singleUpdatePI = PendingIntent.getBroadcast(locationService, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        registerReceiver(timerUpdateReceiver, new IntentFilter(TIMER_LOCATION_UPDATE_ACTION));
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, singleUpdatePI);

        PowerManager pm = (PowerManager) locationService.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        // Location criteria
        criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
    }

    public void startRecording() {
        log.info("Start recording");
        new Thread(new Runnable(){
            @Override
            public void run() {//可以在该线程中做需要处理的事
                log.info("TimerTaskLocationProvider" + "当前时间："+new Date().toString());
                if (config.isDebugging()){
                    startTone(Tone.BEEP);
                }
                Location location = getLastBestLocation();
                if (location != null){
                    handleLocation(location);
                }
            }
        }).start();

    }

    public void stopRecording() {
        log.info("stopRecording not implemented yet");
    }


    /**
     * Returns the most accurate and timely previously detected location.
     * Where the last result is beyond the specified maximum distance or
     * latency a one-off location update is returned via the {@link LocationListener}
     * @return The most accurate and / or timely previously detected location.
     */
    public Location getLastBestLocation() {
        Location bestResult = null;
        String bestProvider = null;

        log.info("Location Fetching last best location: radius={} minTime={}", config.getStationaryRadius());

        try {
            // Iterate through all the providers on the system, keeping
            // note of the most accurate result within the acceptable time limit.
            // If no result is found within maxTime, return the newest Location.
            List<String> matchingProviders = locationManager.getAllProviders();
            for (String provider: matchingProviders) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    log.debug("Location Test provider={} lat={} lon={} acy={} v={}m/s time={}", provider, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed(), location.getTime());
                    bestProvider = provider;
                    bestResult = location;
                }
            }

            if (bestResult != null) {
                log.debug("Location Best result found provider={} lat={} lon={} acy={} v={}m/s time={}", bestProvider, bestResult.getLatitude(), bestResult.getLongitude(), bestResult.getAccuracy(), bestResult.getSpeed(), bestResult.getTime());
            }
        } catch (SecurityException e) {
            log.error("Location Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }

        return bestResult;
    }

    public void onLocationChanged(Location location) {
        log.debug("Location change: {} isMoving={}", location.toString(), isMoving);

        startTone(Tone.BEEP);

        if (config.isDebugging()) {
            Toast.makeText(locationService, "mv:" + isMoving + ",acy:" + location.getAccuracy() + ",v:" + location.getSpeed(), Toast.LENGTH_LONG).show();
        }
        // Go ahead and cache, push to server
        lastLocation = location;
        handleLocation(location);
    }



    /**
     * Broadcast receiver for receiving a single-update from LocationManager.
     */
    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String key = LocationManager.KEY_LOCATION_CHANGED;
            Location location = (Location)intent.getExtras().get(key);
//            log.info("Timer location update: " + new Date().toString()+"Location Test provider={} lat={} lon={} acy={} v={}m/s time={}", location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed(), location.getTime());

            Intent i=new Intent(context,LocationService.class);
            if (intent.hasExtra("config")) {
                log.info("Location timerUpdateReceiver  has");
                config = intent.getParcelableExtra("config");
            }
            i.putExtra("config", config);

            context.startService(i);//开启AlarmService服务

            if (location != null) {
                log.debug("Single location update: " + location.toString());
            }
        }
    };

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        log.debug("Provider {} was disabled", provider);
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        log.debug("Provider {} was enabled", provider);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        log.debug("Provider {} status changed: {}", provider, status);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.info("Destroying TimerLocation");

        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            //noop
        }

        unregisterReceiver(timerUpdateReceiver);

        wakeLock.release();
    }
}
