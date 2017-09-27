package com.sxc.jimmydaddy.bggeolocation;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.sxc.jimmydaddy.bggeolocation.delegate.ConnectAutoCloseDelegate;
import com.sxc.jimmydaddy.bggeolocation.utils.BDConstants;
import com.sxc.jimmydaddy.bggeolocation.utils.ConnectUtil;
import com.sxc.jimmydaddy.bggeolocation.utils.PowerManagerUtil;
import com.sxc.jimmydaddy.bggeolocation.utils.Utils;
import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.HttpPostService;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.bgloc.sync.AccountHelper;
import com.marianhello.bgloc.sync.AuthenticatorService;
import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public class LocationService extends NoticeService {

    //高德地图 client
    private AMapLocationClient aMapLocationClient;
    //高德地图 option
    private AMapLocationClientOption aMapLocationClientOption;
    //百度地图 client
    private LocationClient baiduLocationClient;
    //百度地图 option
    private LocationClientOption baiduLocationClientOption;


    private static String TAG = "JIMMY_BG_LOCATION";

    private LocationDAO dao;
    private ConfigurationDAO configurationDAO;

    private Account syncAccount;
    private org.slf4j.Logger log;


    @Override
    public void onCreate() {
        super.onCreate();
        log = LoggerManager.getLogger(LocationService.class);
        log.info("Creating LocationService");

        dao = (DAOFactory.createLocationDAO(this));
        syncAccount = AccountHelper.CreateSyncAccount(this,
                AuthenticatorService.getAccount(getStringResource(Config.ACCOUNT_TYPE_RESOURCE)));
        configurationDAO = DAOFactory.createConfigurationDAO(this);
        context = this.getApplicationContext();

    }

    protected String getStringResource(String name) {
        return getApplication().getString(getAppResource(name, "string"));
    }

    protected int getAppResource(String name, String type) {
        return getApplication().getResources().getIdentifier(name, type, getApplication().getPackageName());
    }

    public Config getConfig() {
        return this.config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }


    /**
     * 处理息屏关掉wifi的delegate类
     */
    private ConnectAutoCloseDelegate mConnectAutoCloseDelegate = new ConnectAutoCloseDelegate();

    /**
     * 记录是否需要对息屏关掉wifi的情况进行处理
     */
    private boolean mIsConnectCloseable = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        context = this.getApplicationContext();

        if (mConnectAutoCloseDelegate.isUseful(getApplicationContext())) {
            mIsConnectCloseable = true;
            mConnectAutoCloseDelegate.initOnServiceStarted(getApplicationContext());
        }

        log.info(TAG, "Received start startId: {} intent: {}", startId, intent);

        if (intent == null) {
            //service has been probably restarted so we need to load config from db
            try {
                config = configurationDAO.retrieveConfiguration();
            } catch (JSONException e) {
                log.error("Config exception: {}", e.getMessage());
                config = new Config(); //using default config
            }
        } else {
            if (intent.hasExtra("config")) {
                config = intent.getParcelableExtra("config");
            } else {
                config = new Config(); //using default config
            }
        }

        log.debug("Will start service with: {}", config.toString());
        applyNotiKeepMech(); //开启利用notification提高进程优先级的机制

        startLocation();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        unApplyNotiKeepMech();
        stopLocation();

        super.onDestroy();
    }

    /**
     * 启动定位
     */
    void startLocation() {
        stopLocation();
        if (config.getLocationClient() == Config.ANDROID_BAIDU_LOCATION) {
            initAndStartBaiduMap();
        } else {
            initAndStartAmap();
        }
    }

    /**
     * 高德地图初始化 启动
     */
    void initAndStartAmap(){
        if (null == aMapLocationClient) {
            aMapLocationClient = new AMapLocationClient(this.getApplicationContext());
        }

        aMapLocationClientOption = new AMapLocationClientOption();
        // 使用连续
        aMapLocationClientOption.setOnceLocation(false);
        aMapLocationClientOption.setLocationCacheEnable(true);
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        // 每10秒定位一次
        if (config != null && config.getInterval() != null && config.getInterval() > 0) {
            aMapLocationClientOption.setInterval(config.getInterval());
        } else {
            aMapLocationClientOption.setInterval(10 * 1000);
        }
        // 地址信息
        aMapLocationClientOption.setNeedAddress(true);
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.setLocationListener(aMaplocationListener);
        aMapLocationClient.startLocation();
    }

    void initAndStartBaiduMap(){
        if (null == baiduLocationClient) {
            baiduLocationClient = new LocationClient(getApplicationContext());
        }

        baiduLocationClientOption = new LocationClientOption();
        baiduLocationClientOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        if (config != null && config.getInterval() != null && config.getInterval() > 0) {
            baiduLocationClientOption.setScanSpan(config.getInterval());
        } else {
            baiduLocationClientOption.setScanSpan(10*1000);
        }

        //拿地址信息
        baiduLocationClientOption.setIsNeedAddress(true);
        //使用 gps
        baiduLocationClientOption.setOpenGps(true);
        // 设置语义化结果
        baiduLocationClientOption.setIsNeedLocationDescribe(true);
        // poi
        baiduLocationClientOption.setIsNeedLocationPoiList(false);

        baiduLocationClient.setLocOption(baiduLocationClientOption);

        baiduLocationClient.registerLocationListener(myBDLocationListener);

        baiduLocationClient.start();
    }

    /**
     * 停止定位
     */
    void stopLocation() {
        if (null != aMapLocationClient) {
            aMapLocationClient.stopLocation();
        }
        if (null != baiduLocationClient) {
            baiduLocationClient.stop();
        }
    }

    /**
     * 高德地图 listener
     */
    AMapLocationListener aMaplocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            //发送结果的通知
            sendLocationBroadcast(aMapLocation);

            if (!mIsConnectCloseable) {
                return;
            }

            if (aMapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
                mConnectAutoCloseDelegate.onLocateSuccess(getApplicationContext(), PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
            } else {
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , aMapLocation.getErrorCode() , PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
            }

        }

        private void sendLocationBroadcast(AMapLocation aMapLocation) {
            //记录信息并发送广播
            log.debug("get location to json: " + Utils.getLocationStr(aMapLocation));

            AmapPostLocationTask task = new LocationService.AmapPostLocationTask();
            task.execute(aMapLocation);
        }

    };

    /**
     * 百度地图 listener
     */
    BDLocationListener myBDLocationListener = new BDLocationListener() {


        private void sendLocationBroadcast(BDLocation location) {
            //记录信息并发送广播
            log.debug("get location to json: " + Utils.getBDLocationStr(location));

            BaiduPostLocationTask task = new LocationService.BaiduPostLocationTask();
            task.execute(location);
        }

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            sendLocationBroadcast(bdLocation);

            if (!mIsConnectCloseable) {
                return;
            }

            if (locationRight(bdLocation)) {
                mConnectAutoCloseDelegate.onLocateSuccess(getApplicationContext(), PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
            } else {
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , bdLocation.getLocType() , PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
            }

        }

        public void onConnectHotSpotMessage(String s, int i) {

        }

        /**
         * location get correct
         * @param location
         * @return
         */
        private boolean locationRight(BDLocation location){
            return location.getLocType() == 61 || location.getLocType() == 65 || location.getLocType() == 161
                    || location.getLocType() == 66;
        }

    };


    private class BaiduPostLocationTask extends AsyncTask<BDLocation, Integer, Boolean> {

        /**
         * Returns location as JSON object.
         * @throws JSONException
         */
        public JSONObject toJSONObject(BDLocation location) throws JSONException {
            JSONObject json = new JSONObject();
            json.put("time", location.getTime());
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            json.put("accuracy", location.getGpsAccuracyStatus());
            json.put("speed", location.getSpeed());
            json.put("altitude", location.getAltitude());
            json.put("city", location.getCity());
            json.put("bearing", location.getRadius());
            json.put("manufacturer", Utils.getManufacture(getApplicationContext()));

            JSONObject extraJson = new JSONObject();
            extraJson.put("city", location.getCity());
            extraJson.put("country", location.getCountry());
            extraJson.put("address",location.getAddrStr());
            extraJson.put("countryCode", location.getCountryCode());
            extraJson.put("buildingName",location.getBuildingName());
            extraJson.put("buildingID", location.getBuildingID());
            extraJson.put("district", location.getDistrict());
            extraJson.put("province", location.getProvince());
            extraJson.put("street", location.getStreet());
            extraJson.put("streetNumber", location.getStreetNumber());

            json.put("extra", extraJson);

            return json;
        }


        @Override
        protected Boolean doInBackground(BDLocation... locations) {
            Log.d(TAG,"Executing PostLocationTask#doInBackground");
            JSONObject allParams = new JSONObject();
            JSONArray jsonLocations = new JSONArray();
            for (BDLocation location : locations) {
                try {
                    JSONObject jsonLocation = toJSONObject(location);
                    jsonLocations.put(jsonLocation);
                } catch (JSONException e) {
                    Log.e(TAG, "Location to json failed: "+location.toString());
                    return false;
                }
            }
            try {
                allParams.put("locations", jsonLocations);
            } catch (JSONException e) {
                Log.e(TAG, "Locations Json put failed: {}"+jsonLocations.toString());
                return false;
            }

            HashMap<String, String> params = new HashMap<String, String>();

            try {
                allParams.put("params", new JSONObject(params));
            }catch (JSONException e) {
                Log.e(TAG, "params to json failed: {}"+params.toString());
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

            String url = config.getUrl();
            log.debug("Posting json to url: {} headers: {}", url, config.getHttpHeaders());
            log.debug("Posting json to url: {} params: {}", url, config.getParams());
            log.debug("Posting json to url: {} config: {}", url, config);

            int responseCode;

            try {
                responseCode = HttpPostService.postJSON(url, allParams, new HashMap<String, String>());
            } catch (Exception e) {
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , BDConstants.BDLOCATION_REQUEST_FAIL, PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
                e.printStackTrace();
                return false;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , BDConstants.BDLOCATION_REQUEST_FAIL, PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
                Log.e(TAG, "Server error while posting locations responseCode: {}"+responseCode);
                return false;
            }
//
//            for (BackgroundLocation location : locations) {
//                Long locationId = location.getLocationId();
//                if (locationId != null) {
//                    dao.deleteLocation(locationId);
//                }
//            }

            return true;
        }
    }


    private class AmapPostLocationTask extends AsyncTask<AMapLocation, Integer, Boolean> {

        /**
         * Returns location as JSON object.
         * @throws JSONException
         */
        public JSONObject toJSONObject(AMapLocation location) throws JSONException {
            JSONObject json = new JSONObject();
            json.put("provider", location.getProvider());
            json.put("time", location.getTime());
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            json.put("accuracy", location.getAccuracy());
            json.put("speed", location.getSpeed());
            json.put("altitude", location.getAltitude());
            json.put("bearing", location.getBearing());
            json.put("extra", location.toJson(1));
            return json;
        }


        @Override
        protected Boolean doInBackground(AMapLocation... locations) {
            Log.d(TAG,"Executing PostLocationTask#doInBackground");
            JSONObject allParams = new JSONObject();
            JSONArray jsonLocations = new JSONArray();
            for (AMapLocation location : locations) {
                try {
                    JSONObject jsonLocation = toJSONObject(location);
                    jsonLocations.put(jsonLocation);
                } catch (JSONException e) {
                    Log.e(TAG, "Location to json failed: "+location.toString());
                    return false;
                }
            }
            try {
                allParams.put("locations", jsonLocations);
            } catch (JSONException e) {
                Log.e(TAG, "Locations Json put failed: {}"+jsonLocations.toString());
                return false;
            }

            HashMap<String, String> params = new HashMap<String, String>();

            try {
                allParams.put("params", new JSONObject(params));
            }catch (JSONException e) {
                Log.e(TAG, "params to json failed: {}"+params.toString());
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

            String url = config.getUrl();
            log.debug("Posting json to url: {} headers: {}", url, config.getHttpHeaders());
            log.debug("Posting json to url: {} params: {}", url, config.getParams());
            log.debug("Posting json to url: {} config: {}", url, config);

            int responseCode;

            try {
                responseCode = HttpPostService.postJSON(url, allParams, new HashMap<String, String>());
            } catch (Exception e) {
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , BDConstants.BDLOCATION_REQUEST_FAIL, PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
                e.printStackTrace();
                return false;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server error while posting locations responseCode: {}"+responseCode);
                mConnectAutoCloseDelegate.onLocateFail(getApplicationContext() , BDConstants.BDLOCATION_REQUEST_FAIL, PowerManagerUtil.getInstance().isScreenOn(getApplicationContext()), ConnectUtil.getInstance().isNetCon(getApplicationContext()));
                return false;
            }
//
//            for (BackgroundLocation location : locations) {
//                Long locationId = location.getLocationId();
//                if (locationId != null) {
//                    dao.deleteLocation(locationId);
//                }
//            }

            return true;
        }
    }


    /**
     * Handle location from location location provider
     *
     * All locations updates are recorded in local db at all times.
     * Also location is also send to all messenger clients.
     *
     * If option.url is defined, each location is also immediately posted.
     * If post is successful, the location is deleted from local db.
     * All failed to post locations are coalesced and send in some time later in one single batch.
     * Batch sync takes place only when number of failed to post locations reaches syncTreshold.
     *
     * If only option.syncUrl is defined, locations are send only in single batch,
     * when number of locations reaches syncTreshold.
     *
     * @param location
     */
//    public void handleLocation(BackgroundLocation location) {
//        log.error(TAG, PREX + " New location {}", location.toString());
//
//        location.setBatchStartMillis(System.currentTimeMillis() + ONE_MINUTE); // prevent sync of not yet posted location
//        persistLocation(location);
//
//        if (config.hasUrl() || config.hasSyncUrl()) {
//            Long locationsCount = dao.locationsForSyncCount(System.currentTimeMillis());
//            log.debug("Location to sync: {} threshold: {}", locationsCount, config.getSyncThreshold());
//            if (locationsCount >= config.getSyncThreshold()) {
//                log.debug("Attempt to sync locations: {} threshold: {}", locationsCount, config.getSyncThreshold());
//                SyncService.sync(syncAccount, getStringResource(Config.CONTENT_AUTHORITY_RESOURCE));
//            }
//        }
//
//        if (config.hasUrl()) {
//            postLocationAsync(location);
//        }
//
////        Bundle bundle = new Bundle();
////        bundle.putParcelable("location", location);
////        Message msg = Message.obtain(null, MSG_LOCATION_UPDATE);
////        msg.setData(bundle);
//
////        sendClientMessage(msg);
//    }

//    public Long persistLocation (BackgroundLocation location) {
//        Long locationId = -1L;
//        try {
//            locationId = dao.persistLocationWithLimit(location, config.getMaxLocations());
//            location.setLocationId(locationId);
//            log.debug("Persisted location: {}", location.toString());
//        } catch (SQLException e) {
//            log.error("Failed to persist location: {} error: {}", location.toString(), e.getMessage());
//        }
//
//        return locationId;
//    }

//    public void postLocationAsync(BackgroundLocation location) {
//        com.marianhello.bgloc.LocationService.PostLocationTask task = new com.marianhello.bgloc.LocationService.PostLocationTask();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
//        }
//        else {
//            task.execute(location);
//        }
//    }





}
