package com.sxc.jimmydaddy;

import android.content.Context;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.sxc.jimmydaddy.bggeolocation.BackgroundGeolocationModule;
import com.sxc.jimmydaddy.map.BaiduMapModule;
import com.sxc.jimmydaddy.map.BaiduMapViewManager;
import com.sxc.jimmydaddy.map.GeolocationModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by jimmydaddy on 2017/9/27.
 */

public class MapKitPackage implements ReactPackage {
    private Context mContext;

    BaiduMapViewManager baiduMapViewManager;

    public MapKitPackage(Context context) {
        this.mContext = context;
        baiduMapViewManager = new BaiduMapViewManager();
        baiduMapViewManager.initSDK(context);
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<NativeModule>();

        modules.add(new BackgroundGeolocationModule(reactContext));
        modules.add(new BaiduMapModule(reactContext));
        modules.add(new GeolocationModule(reactContext));

        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(
            ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                baiduMapViewManager
        );
    }

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }
}
