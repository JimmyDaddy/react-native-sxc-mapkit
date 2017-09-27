package com.sxc.jimmydaddy.bggeolocation.delegate;

import android.content.Context;

import com.baidu.location.BDLocation;
import com.sxc.jimmydaddy.bggeolocation.LocationStatusManager;
import com.sxc.jimmydaddy.bggeolocation.utils.ConnectUtil;
import com.sxc.jimmydaddy.bggeolocation.utils.PowerManagerUtil;
import com.sxc.jimmydaddy.bggeolocation.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public class ConnectAutoCloseDelegate implements ConnectAutoCloseDelegateInterFace {


    /**
     * 请根据后台数据自行添加。此处只针对小米手机
     * @param context
     * @return
     */
    @Override
    public boolean isUseful(Context context) {
        String manName = Utils.getManufacture(context);
        Pattern pattern = Pattern.compile("xiaomi", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(manName);
        return m.find();
    }

    @Override
    public void initOnServiceStarted(Context context) {
        LocationStatusManager.getInstance().initStateFromPreference(context);
    }

    @Override
    public void onLocateSuccess(Context context, boolean isScreenOn, boolean isMobileable) {
        LocationStatusManager.getInstance().onLocationSuccess(context, isScreenOn, isMobileable);
    }

    @Override
    public void onLocateFail(Context context, int errorCode, boolean isScreenOn, boolean isConnectable) {
        //如果屏幕点亮情况下，因为联网失败，则表示不是屏幕点亮造成的联网失败，并修改参照值
        if (isScreenOn && ConnectUtil.isConnectFail(errorCode) && !isConnectable) {
            LocationStatusManager.getInstance().resetToInit(context);
            return;
        }

        if (!LocationStatusManager.getInstance().isFailOnScreenOff(context, errorCode, isScreenOn, isConnectable)) {
            return;
        }
        PowerManagerUtil.getInstance().wakeUpScreen(context);
    }


}
