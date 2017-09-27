package com.sxc.jimmydaddy.map;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import static com.baidu.mapapi.search.core.PoiInfo.POITYPE.BUS_STATION;
import static com.baidu.mapapi.search.core.PoiInfo.POITYPE.POINT;
import static com.baidu.mapapi.search.core.PoiInfo.POITYPE.SUBWAY_STATION;

/**
 * Created by lovebing on 2016/10/28.
 */
public class GeolocationModule extends BaseModule
        implements BDLocationListener, OnGetGeoCoderResultListener, OnGetPoiSearchResultListener {

    private LocationClient locationClient;
    private static GeoCoder geoCoder;
    private static PoiSearch poiSearch;
    private String city = "北京";

    public GeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    public String getName() {
        return "BaiduGeolocationModule";
    }


    private void initLocationClient() {
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setIsNeedAltitude(true);
        option.setIsNeedLocationDescribe(true);
        option.setOpenGps(true);
        locationClient = new LocationClient(context.getApplicationContext());
        locationClient.setLocOption(option);
        Log.i("locationClient", "locationClient");
        locationClient.registerLocationListener(this);
    }
    /**
     *
     * @return
     */
    protected GeoCoder getGeoCoder() {
        if(geoCoder != null) {
            geoCoder.destroy();
        }
        geoCoder = GeoCoder.newInstance();
        geoCoder.setOnGetGeoCodeResultListener(this);
        return geoCoder;
    }

    /**
     *
     * @return
     */
    protected PoiSearch getPoiSearch(){
        if (poiSearch != null) {
            poiSearch.destroy();
        }
        poiSearch = poiSearch.newInstance();
        poiSearch.setOnGetPoiSearchResultListener(this);
        return poiSearch;
    }

    /**
     *
     * @param sourceLatLng
     * @return
     */
    protected LatLng getBaiduCoorFromGPSCoor(LatLng sourceLatLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);
        LatLng desLatLng = converter.convert();
        return desLatLng;

    }

    @ReactMethod
    public void getCurrentPosition() {
        if(locationClient == null) {
            initLocationClient();
        }
        Log.i("getCurrentPosition", "getCurrentPosition");
        locationClient.start();
    }
    @ReactMethod
    public void geocode(String city, String addr) {
        getGeoCoder().geocode(new GeoCodeOption()
                .city(city).address(addr));
    }

    @ReactMethod
    public void reverseGeoCode(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(new LatLng(lat, lng)));
    }

    @ReactMethod
    public void reverseGeoCodeGPS(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(getBaiduCoorFromGPSCoor(new LatLng(lat, lng))));
    }

    @ReactMethod
    public void geoCodeCityKeyWord(String keyword, Integer pageNum, Integer pageCapacity){
        PoiCitySearchOption option = new PoiCitySearchOption();
        option.city(city);
        if (pageNum != null) {
            option.pageNum(pageNum);
        } else {
            option.pageNum(0);
        }
        if (pageCapacity != null){
            option.pageCapacity(pageCapacity);
        } else {
            option.pageCapacity(15);
        }
        option.keyword(keyword);
        getPoiSearch().searchInCity(option);
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        WritableMap params = Arguments.createMap();
        if (poiResult.getAllPoi().isEmpty()) {
            params.putInt("errcode", -1);
        } else {
            WritableArray poiArray = Arguments.createArray();

            for (PoiInfo p : poiResult.getAllPoi()) {
                if (p.type == POINT || p.type == BUS_STATION || p.type == SUBWAY_STATION) {
                    WritableMap tempParams = Arguments.createMap();
                    tempParams.putString("address", p.address);
                    tempParams.putString("name", p.name);
                    tempParams.putString("city", p.city);
                    tempParams.putString("phone", p.phoneNum);
                    tempParams.putString("postCode", p.postCode);
                    tempParams.putBoolean("isPano", p.isPano);
                    tempParams.putDouble("latitude", p.location.latitude);
                    tempParams.putDouble("longitude", p.location.longitude);
                    tempParams.putString("uid", p.uid);
                    poiArray.pushMap(tempParams);
                }
            }

            params.putArray("result", poiArray);
        }

        sendEvent("onGetPoiResult", params);

    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        WritableMap params = Arguments.createMap();
        params.putDouble("latitude", bdLocation.getLatitude());
        params.putDouble("longitude", bdLocation.getLongitude());
        params.putDouble("direction", bdLocation.getDirection());
        params.putDouble("altitude", bdLocation.getAltitude());
        params.putDouble("radius", bdLocation.getRadius());
        params.putString("address", bdLocation.getAddrStr());
        params.putString("countryCode", bdLocation.getCountryCode());
        params.putString("country", bdLocation.getCountry());
        params.putString("province", bdLocation.getProvince());
        params.putString("cityCode", bdLocation.getCityCode());
        params.putString("city", bdLocation.getCity());
        params.putString("district", bdLocation.getDistrict());
        params.putString("street", bdLocation.getStreet());
        params.putString("streetNumber", bdLocation.getStreetNumber());
        params.putString("buildingId", bdLocation.getBuildingID());
        params.putString("buildingName", bdLocation.getBuildingName());

        Log.i("onReceiveLocation", "onGetCurrentLocationPosition");
        sendEvent("onGetCurrentLocationPosition", params);
        locationClient.stop();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            params.putDouble("latitude",  result.getLocation().latitude);
            params.putDouble("longitude",  result.getLocation().longitude);
            params.putString("address", result.getAddress());
        }
        sendEvent("onGetGeoCodeResult", params);
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            ReverseGeoCodeResult.AddressComponent addressComponent = result.getAddressDetail();

            params.putString("address", result.getAddress());
            params.putString("province", addressComponent.province);
            params.putString("city", addressComponent.city);
            params.putString("district", addressComponent.district);
            params.putString("street", addressComponent.street);
            params.putString("streetNumber", addressComponent.streetNumber);
            //设置所在城市
            city = addressComponent.city;

            if (result.getPoiList() != null){
                WritableArray poiArray = Arguments.createArray();

                for (PoiInfo p: result.getPoiList()) {
                    WritableMap poiParams = Arguments.createMap();
                    poiParams.putString("address", p.address);
                    poiParams.putString("city", p.city);
                    poiParams.putString("name", p.name);
                    poiParams.putString("phoneNum", p.phoneNum);
                    poiParams.putString("postCode", p.postCode);
                    poiParams.putString("uid", p.uid);
                    poiParams.putBoolean("isPano", p.isPano);
                    poiParams.putDouble("latitude", p.location.latitude);
                    poiParams.putDouble("longitude", p.location.longitude);

                    poiArray.pushMap(poiParams);
                }

                params.putArray("nearbyPOI", poiArray);
            }
        }

        sendEvent("onGetReverseGeoCodeResult", params);
    }
}
