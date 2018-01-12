package com.sxc.jimmydaddy.bggeolocation;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.sxc.jimmydaddy.bggeolocation.utils.Utils;
import com.marianhello.bgloc.Config;
import com.marianhello.logging.LoggerManager;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public class NoticeService extends Service {

    private static int NOTI_ID = 651117351;
    protected Config config;
    protected Context context;
    private org.slf4j.Logger log;



    private Utils.CloseServiceReceiver mCloseReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mCloseReceiver = new Utils.CloseServiceReceiver(this);
        registerReceiver(mCloseReceiver, Utils.getCloseServiceFilter());
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (mCloseReceiver != null) {
            unregisterReceiver(mCloseReceiver);
            mCloseReceiver = null;
        }

        super.onDestroy();
    }


    private final String mHelperServiceName = "com.jimmydaddy.bggeolocation.LocationHelperService";
    /**
     * 触发利用notification增加进程优先级
     */
    protected void applyNotiKeepMech() {
        startForeground(NOTI_ID, Utils.buildNotification(getBaseContext()));
        startBindHelperService();
    }

    public void unApplyNotiKeepMech() {
        stopForeground(true);
    }

    public Binder mBinder;

    public class LocationServiceBinder extends LocationServiceAIDL.Stub{
        public void onFinishBind(){
        }
    }

    private LocationHelperServiceAIDL mHelperAIDL;
    private void startBindHelperService() {
        log = LoggerManager.getLogger(NoticeService.class);

        connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                //doing nothing
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationHelperServiceAIDL l = LocationHelperServiceAIDL.Stub.asInterface(service);
                mHelperAIDL = l;
                try {
                    l.onFinishBind(NOTI_ID);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        Intent intent = new Intent();
        intent.setAction(mHelperServiceName);
        intent.putExtra("config", config);
        Intent myIntent = Utils.getExplicitIntent(context, intent);
        log.error(NoticeService.class.getName()+": fuck this startBindHelperService: {}", myIntent);
        bindService(Utils.getExplicitIntent(context, intent), connection, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection connection;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new LocationServiceBinder();
        }
        return mBinder;
    }

}
