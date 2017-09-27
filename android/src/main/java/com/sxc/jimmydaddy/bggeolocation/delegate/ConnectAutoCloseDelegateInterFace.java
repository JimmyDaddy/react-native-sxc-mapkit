package com.sxc.jimmydaddy.bggeolocation.delegate;

import android.content.Context;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public interface ConnectAutoCloseDelegateInterFace {
    /**
     * 判断在该机型下此逻辑是否有效。目前已知的系统是小米系统存在(用户自助设置的)息屏断掉wifi的功能。
     *
     * @param context
     * @return
     */
    boolean isUseful(Context context);


    /**
     * 点亮屏幕的服务有可能被重启。此处进行初始化
     *
     * @param context
     * @return
     */
    void initOnServiceStarted(Context context);


    /**
     * 定位成功时，如果移动网络无法访问，而且屏幕是点亮状态，则对状态进行保存
     */
    void onLocateSuccess(Context context, boolean isScreenOn, boolean isMobileable);

    /**
     * 对定位失败情况的处理
     */
    void onLocateFail(Context context, int errorCode, boolean isScreenOn, boolean isWifiable);

}
