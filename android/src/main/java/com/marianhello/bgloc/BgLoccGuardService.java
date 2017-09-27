package com.marianhello.bgloc;

import android.Manifest;
import android.accounts.Account;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.bgloc.sync.AccountHelper;
import com.marianhello.bgloc.sync.AuthenticatorService;
import com.marianhello.bgloc.sync.SyncService;
import com.marianhello.logging.LoggerManager;
import com.marianhello.utils.AwakeUtil;
import com.marianhello.utils.ServiceRunning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;

import static com.facebook.react.common.ReactConstants.TAG;
import static com.marianhello.bgloc.LocationService.MSG_LOCATION_UPDATE;
import static com.marianhello.bgloc.LocationService.MSG_REGISTER_CLIENT;
import static com.marianhello.bgloc.LocationService.MSG_SWITCH_MODE;
import static com.marianhello.bgloc.LocationService.MSG_UNREGISTER_CLIENT;

/**
 * Created by jimmydaddy on 2017/8/4.
 */

public class BgLoccGuardService extends Service {

    HashMap<Integer, Messenger> mClients = new HashMap();

    private static final String P_NAME = "com.tenforwardconsulting.cordova.bgloc:guardservice";

    private static final String LOCATION_PERIODIC_ACTION              = P_NAME + ".LOCATION_PERIODIC_ACTION";

    private static final int ONE_MINUTE = 1000 * 60;

    private Config config;

    private org.slf4j.Logger log;

    private AlarmManager alarmManager;

    private PendingIntent locationPeriodicPI;

    private LocationDAO dao;
    private ConfigurationDAO configurationDAO;


    private volatile HandlerThread handlerThread;

    private BgLoccGuardService.ServiceHandler serviceHandler;

    private Integer PROVIDER_ID = 2;

    private Account syncAccount;

    private Boolean hasConnectivity = true;

    private static String TAG = "sxc location";

    private static String PREX = "----->>>>> guard service";

    private static String ACTION_FROMBGLOCC_GUARD = "bg location guard service";

    private static String processName = ":bgLocLocation";

    protected ToneGenerator toneGenerator;


//    private MyConn conn;


    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }

    /**
     * Handler of incoming messages from clients.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.put(msg.arg1, msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.arg1);
                    break;
                case MSG_SWITCH_MODE:
                    switchMode(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    public void switchMode(int mode) {
        // TODO: implement
    }


    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return super.registerReceiver(receiver, filter, null, serviceHandler);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        super.unregisterReceiver(receiver);
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger messenger = new Messenger(new IncomingHandler());



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log = LoggerManager.getLogger(BgLoccGuardService.class);
        log.info(TAG, PREX + "  Creating guardService");
        // keep location service running
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("GuardService.HandlerThread");
        handlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new BgLoccGuardService.ServiceHandler(handlerThread.getLooper());

        dao = (DAOFactory.createLocationDAO(this));
        syncAccount = AccountHelper.CreateSyncAccount(this,
                AuthenticatorService.getAccount(getStringResource(Config.ACCOUNT_TYPE_RESOURCE)));

        configurationDAO = DAOFactory.createConfigurationDAO(this);

//        binder = new BgGuardLocationBinder();

//        if(conn==null) {
//            conn = new MyConn();
//        }


        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 1);

        keepLocationService();
    }

    /**
     * bind service
     */
//    class BgGuardLocationBinder extends ProcessService.Stub {
//        @Override
//        public String getServiceName() throws RemoteException {
//            String packageName = getPackageName();
//            return packageName+":bgLocLocGuard";
//        }
//    }
//
//    class  MyConn implements ServiceConnection {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//            log.error(TAG,"与LocationService连接成功");
//            ActivityManager activityManager = (ActivityManager) BgLoccGuardService.this
//                    .getSystemService(Context.ACTIVITY_SERVICE);
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            // 启动FirstService
//            Intent locationIntent = new Intent(getApplicationContext(), LocationService.class);
//            if (config == null){
//                try {
//                    config = configurationDAO.retrieveConfiguration();
//                } catch (JSONException e) {
//                    log.error(TAG, "  ------->>>>>>> Config exception: {}", e.getMessage());
//                    config = new Config(); //using default config
//                }
//            }
//            if (config != null ) {
//                locationIntent.putExtra("config", config);
//            }
//            locationIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
//            BgLoccGuardService.this.startService(locationIntent);
//            //绑定FirstService
//            BgLoccGuardService.this.bindService(new Intent(BgLoccGuardService.this, LocationService.class),conn, Context.BIND_IMPORTANT);
//        }
//    }


    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    protected int getAppResource(String name, String type) {
        return getApplication().getResources().getIdentifier(name, type, getApplication().getPackageName());
    }

    protected String getStringResource(String name) {
        return getApplication().getString(getAppResource(name, "string"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.debug(TAG, PREX + " Received start startId: {} intent: {}", startId, intent);

        if (intent == null) {
            //service has been probably restarted so we need to load config from db
            ConfigurationDAO dao = DAOFactory.createConfigurationDAO(this);
            try {
                config = dao.retrieveConfiguration();
            } catch (JSONException e) {
                Log.e(TAG, PREX + "Config exception: {}"+e.getMessage());
                config = new Config(); //using default config
                e.printStackTrace();
            }
        } else {
            if (intent.hasExtra("config")) {
                config = intent.getParcelableExtra("config");
            } else {
                config = new Config(); //using default config
            }
        }


        long periodic = 15 * 1000;

        if (config.getActivitiesInterval() != null && config.getActivitiesInterval() >= 0){
            periodic = config.getActivitiesInterval();
        }

        log.debug(TAG, PREX + " periodic", periodic);

        // awake device PI
        locationPeriodicPI = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(LOCATION_PERIODIC_ACTION), 0);
        registerReceiver(locationPeriodicReceiver, new IntentFilter(LOCATION_PERIODIC_ACTION));
        /**
         * Android 5.1 以上将会强制为1分钟的周期时间
         */
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), periodic, locationPeriodicPI);

        //We want this service to continue running until it is explicitly stopped

        keepLocationService();
//        bindService(new Intent(this, LocationService.class), conn, Context.BIND_IMPORTANT);

        return super.onStartCommand(intent, flags, startId);
    }


    private BroadcastReceiver locationPeriodicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            log.info(TAG, PREX + " fuck onReceive  LOCATION_CLOCK");

            // Service running or stopped
            String packageName = getPackageName();
            boolean isRun = ServiceRunning.isRunning(context, packageName+processName);

            Location location = getCurrentPosition();
            if (location != null){
                handleLocation(location);
            }

            if (!isRun) {
                log.info(TAG, "  ------->>>>>>>  location service stopped restarting");
                Intent locationIntent = new Intent(context, LocationService.class);

                if (config != null) {
                    log.info(TAG, "--->>> guard service   onReceive  service stopped LocationProvider"+config.getLocationProvider());
                    locationIntent.putExtra("config", config);
                }

                log.info(TAG, "--->>>  guard service  onReceive  service stopped getCurrentPosition "+location.getLatitude());

                context.startService(locationIntent);
            }

            AwakeUtil.wakeDevice(context);
            startTone(AbstractLocationProvider.Tone.BEEP);

        }
    };

    protected void startTone(AbstractLocationProvider.Tone name) {
        if (toneGenerator == null) return;

        int tone = 0;
        int duration = 1000;

        switch (name) {
            case BEEP:
                tone = ToneGenerator.TONE_PROP_BEEP;
                break;
            case BEEP_BEEP_BEEP:
                tone = ToneGenerator.TONE_CDMA_CONFIRM;
                break;
            case LONG_BEEP:
                tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
                break;
            case DOODLY_DOO:
                tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                break;
            case CHIRP_CHIRP_CHIRP:
                tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                break;
            case DIALTONE:
                tone = ToneGenerator.TONE_SUP_RINGTONE;
                break;
        }

        toneGenerator.startTone(tone, duration);
    }

    /**
     * handle location
     * @param mylocation
     */
    private void handleLocation(Location mylocation) {
        if (mylocation != null) {
            log.debug(TAG, PREX + " New location {}", mylocation.toString());

            BackgroundLocation location = new BackgroundLocation(new BackgroundLocation(PROVIDER_ID, mylocation));

            location.setBatchStartMillis(System.currentTimeMillis() + ONE_MINUTE); // prevent sync of not yet posted location
            persistLocation(location);

            if (config.hasUrl() || config.hasSyncUrl()) {
                Long locationsCount = dao.locationsForSyncCount(System.currentTimeMillis());
                log.debug("Location to sync: {} threshold: {}", locationsCount, config.getSyncThreshold());
                if (locationsCount >= config.getSyncThreshold()) {
                    log.debug("Attempt to sync locations: {} threshold: {}", locationsCount, config.getSyncThreshold());
                    SyncService.sync(syncAccount, getStringResource(Config.CONTENT_AUTHORITY_RESOURCE));
                }
            }

            if (config.hasUrl()) {
                postLocationAsync(location);
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable("location", location);
            bundle.putString("action", ACTION_FROMBGLOCC_GUARD);
            Message msg = Message.obtain(null, MSG_LOCATION_UPDATE);
            msg.setData(bundle);

            sendClientMessage(msg);
        }

    }

    public void postLocationAsync(BackgroundLocation location) {
        BgLoccGuardService.PostLocationTask task = new BgLoccGuardService.PostLocationTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
        } else {
            task.execute(location);
        }
    }

    private class PostLocationTask extends AsyncTask<BackgroundLocation, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(BackgroundLocation... locations) {
            log.debug("Executing PostLocationTask#doInBackground");
            JSONObject allParams = new JSONObject();
            JSONArray jsonLocations = new JSONArray();
            for (BackgroundLocation location : locations) {
                log.debug(TAG, "----->>>>> fuck locations"+ location.toString());
                try {
                    JSONObject jsonLocation = location.toJSONObject();
                    jsonLocations.put(jsonLocation);
                } catch (JSONException e) {
                    log.warn("Location to json failed: {}", location.toString());
                    return false;
                }
            }
            try {
                allParams.put("locations", jsonLocations);
            } catch (JSONException e) {
                log.warn("Locations Json put failed: {}", jsonLocations.toString());
                return false;
            }

            if (config.getParams() != null && !config.getParams().isEmpty()) {
                try {
                    allParams.put("params", new JSONObject(config.getParams()));
                }catch (JSONException e) {
                    log.warn("params to json failed: {}", config.getParams().toString());
                    return false;
                }
            }
            log.error(TAG, "----->>>>> fuck config {} ", config.toString());

            String url = config.getUrl();
            log.debug("Posting json to url: {} headers: {}", url, config.getHttpHeaders());
            log.debug("Posting json to url: {} params: {}", url, config.getParams());
            log.debug("Posting json to url: {} config: {}", url, config);
            int responseCode;

            try {
                responseCode = HttpPostService.postJSON(url, allParams, config.getHttpHeaders());
            } catch (Exception e) {
                hasConnectivity = isNetworkAvailable();
                log.warn("Error while posting locations: {}", e.getMessage());
                return false;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("Server error while posting locations responseCode: {}", responseCode);
                return false;
            }

            for (BackgroundLocation location : locations) {
                Long locationId = location.getLocationId();
                if (locationId != null) {
                    dao.deleteLocation(locationId);
                }
            }

            return true;
        }
    }

    /**
     * Broadcast receiver which detects connectivity change condition
     */
    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        private static final String LOG_TAG = "NetworkChangeReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            hasConnectivity = isNetworkAvailable();
            log.info("Network condition changed hasConnectivity: {}", hasConnectivity);
        }
    };


    // method will mutate location
    public Long persistLocation(BackgroundLocation location) {
        Long locationId = -1L;
        try {
            locationId = dao.persistLocationWithLimit(location, config.getMaxLocations());
            location.setLocationId(locationId);
            log.debug("Persisted location: {}", location.toString());
        } catch (SQLException e) {
            log.error("Failed to persist location: {} error: {}", location.toString(), e.getMessage());
        }

        return locationId;
    }


    public void sendClientMessage(Message msg) {
        Iterator<Messenger> it = mClients.values().iterator();
        while (it.hasNext()) {
            try {
                Messenger client = it.next();
                client.send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                it.remove();
            }
        }
    }


    /**
     * getCurrentPosition
     * @Author JimmyDaddy
     * @return location
     */
    public Location getCurrentPosition() {
        // get location manager
        Context context = getApplicationContext();
        Location location;
        if (context != null) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "请打开网络或GPS定位功能!", Toast.LENGTH_SHORT).show();
                return null;
            }
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location == null){
                Log.d(TAG, "onCreate.location = null");
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            Log.e(TAG, "onCreate.location = " + location.getProvider());
            return location;
        }
        return null;
    }

    @Override
    public void onTrimMemory(int level) {
        keepLocationService();
    }

    private void keepLocationService(){
        String packageName = getPackageName();
        Boolean isRunning = ServiceRunning.isRunning(getApplicationContext(), packageName+processName);
        if (!isRunning) {
            log.debug(TAG, "  ------->>>>>>>  location service stopped restarting");
            try {
                Intent locationIntent = new Intent(getApplicationContext(), LocationService.class);
                if (config == null){
                    ConfigurationDAO dao = DAOFactory.createConfigurationDAO(this);
                    try {
                        config = dao.retrieveConfiguration();
                    } catch (JSONException e) {
                        log.error(TAG, "  ------->>>>>>> Config exception: {}", e.getMessage());
                        config = new Config(); //using default config
                    }
                }
                locationIntent.putExtra("config", config);
                locationIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                Context context = getApplicationContext();
                if (context != null) {
//                    bindService(locationIntent, conn, Context.BIND_IMPORTANT);
                    context.startService(locationIntent);
                } else {
                    Log.e("sxc location", " --->>>>> double guard context is null");
                }
            } catch (Exception e) {
                Log.e("sxc loaction", " ----->>>>> double guard start guard service from location service fail");
                e.printStackTrace();
            }
        }
    }
}
