package com.marianhello.utils;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by jimmydaddy on 2017/8/6.
 */

public class AwakeUtil {

    public static void wakeDevice(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(
                Context.POWER_SERVICE);
        PowerManager.WakeLock mWakelock = pm.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "target");
        //下面这行代码实现点亮屏幕，
        mWakelock.acquire();
        //记得release
        mWakelock.release();
    }
}
