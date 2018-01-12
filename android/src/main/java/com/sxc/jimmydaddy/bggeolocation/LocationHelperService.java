package com.sxc.jimmydaddy.bggeolocation;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.sxc.jimmydaddy.bggeolocation.utils.Utils;
import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;

/**
 * Created by jimmydaddy on 2017/8/8.
 */

public class LocationHelperService extends Service {


    private Config config;
    private ConfigurationDAO configurationDAO;

    private Utils.CloseServiceReceiver mCloseReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        startBind();
        mCloseReceiver = new Utils.CloseServiceReceiver(this);
        configurationDAO = DAOFactory.createConfigurationDAO(this);

        registerReceiver(mCloseReceiver, Utils.getCloseServiceFilter());

    }



    @Override
    public void onDestroy() {
        if (mInnerConnection != null) {
            unbindService(mInnerConnection);
            mInnerConnection = null;
        }

        if (mCloseReceiver != null) {
            unregisterReceiver(mCloseReceiver);
            mCloseReceiver = null;
        }

        super.onDestroy();
    }

    private ServiceConnection mInnerConnection;
    private void startBind() {
        final String locationServiceName = "com.jimmydaddy.bggeolocation.LocationService";
        mInnerConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Intent intent = new Intent();
                intent.setAction(locationServiceName);
                intent.putExtra("config", config);
                startService(Utils.getExplicitIntent(getApplicationContext(), intent));
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationServiceAIDL l = LocationServiceAIDL.Stub.asInterface(service);
                try {
                    l.onFinishBind();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        Intent intent = new Intent();
        intent.setAction(locationServiceName);
        bindService(Utils.getExplicitIntent(getApplicationContext(), intent), mInnerConnection, Service.BIND_AUTO_CREATE);
    }



    private HelperBinder mBinder;

    private class HelperBinder extends LocationHelperServiceAIDL.Stub {
        @Override
        public void onFinishBind(int notiId) throws RemoteException {
            startForeground(notiId, Utils.buildNotification(LocationHelperService.this.getApplicationContext()));
            stopForeground(true);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new HelperBinder();
        }
        if (intent != null && intent.hasExtra("config")){
            config = intent.getParcelableExtra("config");
        }

        return mBinder.asBinder();
    }
}
