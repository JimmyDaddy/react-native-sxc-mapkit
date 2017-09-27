package com.marianhello.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.marianhello.bgloc.LocationService;

import java.util.List;

/**
 * Created by jimmydaddy on 2017/8/4.
 *
 */

public class ServiceRunning {

    /**
     * in the context whether the service named 'name' is running
     * @param context
     * @param name
     * @return
     */
    public static Boolean isRunning(Context context, String name) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
//                .getRunningServices(40);
        List<ActivityManager.RunningAppProcessInfo> serviceList = activityManager
                .getRunningAppProcesses();
        if (serviceList != null && serviceList.size() > 0) {
            for (ActivityManager.RunningAppProcessInfo info: serviceList) {
                if (info.processName.equals(name)){
                    return true;
                }
            }
//            for (int i = 0; i < serviceList.size(); i++) {
//                if (serviceList.get(i).service.getClassName().equals(name)) {
//                    return true;
//                }
//            }
        }

        Log.e("sxc location", "is not Running: " + name);

        return false;
    }
}
