package com.sxc.jimmydaddy.bggeolocation.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static com.sxc.jimmydaddy.bggeolocation.utils.BDConstants.AMAP_LOCATION_CONNECT_FAIL;
import static com.sxc.jimmydaddy.bggeolocation.utils.BDConstants.BDLOCATION_CONNECTION_FAIL;
import static com.sxc.jimmydaddy.bggeolocation.utils.BDConstants.BDLOCATION_REQUEST_FAIL;
import static com.sxc.jimmydaddy.bggeolocation.utils.BDConstants.CONNECT_FAIL;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public class ConnectUtil {
    private static class Holder {
        public static ConnectUtil instance = new ConnectUtil();
    }

    public static ConnectUtil getInstance() {
        return Holder.instance;
    }

    /**
     * 是否手机信号可连接
     * @param context
     * @return
     */
    public boolean isMobileAva(Context context) {

        boolean hasMobileCon = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfos = cm.getAllNetworkInfo();
        for (NetworkInfo net : netInfos) {

            String type = net.getTypeName();
            if (type.equalsIgnoreCase("MOBILE")) {
                if (net.isConnected()) {
                    hasMobileCon = true;
                }
            }
        }
        return hasMobileCon;
    }


    /**
     * 网络是否可连接
     * @param context
     * @return
     */
    public boolean isNetCon(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;

    }

    /**
     *
     * @param errorCode
     * @return
     */
    public static boolean isConnectFail(int errorCode){
        return errorCode == AMAP_LOCATION_CONNECT_FAIL || errorCode == BDLOCATION_CONNECTION_FAIL || errorCode == BDLOCATION_REQUEST_FAIL
                || CONNECT_FAIL == errorCode;
    }
}
