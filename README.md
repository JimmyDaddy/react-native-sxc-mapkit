# react-native-sxc-mapkit 

* 地图 React Native 模块，支持 react native 0.40+
* 支持后台定位(Android 提供选择高德或百度定位)


## Install 安装
    cnpm install react-native-sxc-mapkit  --save
## Import 导入

### Android Studio
- settings.gradle `
include ':react-native-sxc-mapkit'
project(':react-native-sxc-mapkit').projectDir = new File(settingsDir, '../node_modules/react-native-sxc-mapkit/android')`

- build.gradle `compile project(':react-native-sxc-mapkit')`

- MainApplication`new MapKitPackage(getApplicationContext())`
- 配置百度地图key AndroidMainifest.xml `<meta-data
            android:name="com.baidu.lbsapi.API_KEY" android:value="xx"/>`
- 配置高德key AndroidMainifest.xml `<meta-data
            android:name="com.amap.api.v2.apikey"
            android:value="your key" />`

### Xcode
- Project navigator->Libraries->Add Files to 选择 react-native-sxc-mapkit/ios/RCTMapKit.xcodeproj
- Project navigator->Build Phases->Link Binary With Libraries 加入 libRCTMapKit.a
- Project navigator->Build Settings->Search Paths， Framework search paths 添加 react-native-sxc-mapkit/ios/lib，Header search paths 添加 react-native-sxc-mapkit/ios/RCTMapKit
- 添加依赖, react-native-sxc-mapkit/ios/lib 下的全部 framwordk， CoreLocation.framework和QuartzCore.framework、OpenGLES.framework、SystemConfiguration.framework、CoreGraphics.framework、Security.framework、libsqlite3.0.tbd（xcode7以前为 libsqlite3.0.dylib）、CoreTelephony.framework 、libstdc++.6.0.9.tbd（xcode7以前为libstdc++.6.0.9.dylib）、CoreTelephony.framework
- 添加 BaiduMapAPI_Map.framework/Resources/mapapi.bundle

- 添加 UIBackgroundModes location 到 Info.plist
- 添加 NSLocationAlwaysUsageDescription 到Info.plist 并增加相应描述


- 其它一些注意事项可参考百度地图LBS文档

##### AppDelegate.m init 初始化
    #import "RCTBaiduMapViewManager.h"
    - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
    {
        ...
        [RCTBaiduMapViewManager initSDK:@"api key"];
        ...
    }

## Usage 使用方法

    import { MapView, MapTypes, MapModule, Geolocation } from 'react-native-sxc-mapkit

### MapView Props 属性
| Name                    | Type  | Default  | Extra
| ----------------------- |:-----:| :-------:| -------
| zoomControlsVisible     | bool  | true     | Android only
| trafficEnabled          | bool  | false    |
| baiduHeatMapEnabled     | bool  | false    |
| mapType                 | number| 1        |
| zoom                    | number| 10       |
| center                  | object| null     | {latitude: 0, longitude: 0}
| marker                  | object| null     | {latitude: 0, longitude: 0, title: ''}
| markers                 | array | []       | [marker, maker]
| onMapStatusChangeStart  | func  | undefined| Android only
| onMapStatusChange       | func  | undefined|
| onMapStatusChangeFinish | func  | undefined| Android only
| onMapLoaded             | func  | undefined|
| onMapClick              | func  | undefined|
| onMapDoubleClick        | func  | undefined|
| gesturesEnabled         | bool  | true     |
| showMapScaleBar           | bool  | true     |
| rotateEnabled           | bool  | true     |
| zoomEnabledWithTap           | bool  | true     |
| scrollEnabled           | bool  | true     |
| zoomEnabled           | bool  | true     |
| overlookEnabled           | bool  | true     |


### Geolocation Methods

| Method                    | Result
| ------------------------- | -------
| Promise reverseGeoCode(double lat, double lng) | `{"address": "", "province": "", "city": "", "district": "", "streetName": "", "streetNumber": "", "nearbyPOI": []}`
| Promise reverseGeoCodeGPS(double lat, double lng) |  `{"address": "", "province": "", "city": "", "district": "", "streetName": "", "streetNumber": ""}`
| Promise geocode(String city, String addr) | {"latitude": 0.0, "longitude": 0.0}
| Promise getCurrentPosition() | IOS: `{"latitude": 0.0, "longitude": 0.0}` Android: `{"latitude": 0.0, "longitude": 0.0, "direction": -1, "altitude": 0.0, "radius": 0.0, "address": "", "countryCode": "", "country": "", "province": "", "cityCode": "", "city": "", "district": "", "street": "", "streetNumber": "", "buildingId": "", "buildingName": ""}`
| Promise geoCodeCityKeyWord(string keyword, int pageNum, int pageCapacity) | `{"result": [{"address": "", "name": "", "street": "", "streetNumber": "", "latitude": "", "longitude": "", ···}]}`

### background geolocation service

后台定位服务

#### API

较多 待补充

#### usage

```js

import { BackgroundGeolocation } from '@sxc-test/react-native-sxc-background-geolocation'

const config = {
  desiredAccuracy: 10,
  stationaryRadius: 30,
  distanceFilter: 30,
      // Activity Recognition
  stopTimeout: 1,
  locationTimeout: 30,
      // Application config
      // life-cycle.
  locationProvider: BackgroundGeolocation.provider.ANDROID_DISTANCE_FILTER_PROVIDER,
  stopOnTerminate: false,   // <-- Allow the background-service to continue tracking when user closes the app.
  startOnBoot: true,        // <-- Auto start tracking when device is powered-up.
      // HTTP / SQLite config
  batchSync: false,       // <-- [Default: false] Set true to sync locations to server in a single HTTP request.
  autoSync: true,         // <-- [Default: true] Set true to sync each location to server as it arrives.
  autoSyncThreshold: 1,
  maxDaysToPersist: 1,    // <-- Maximum days to persist a location in plugin's SQLite database when HTTP fails
  preventSuspend: true,
  interval: 5 * 60 * 1000, // provider == TIMER_PROVIDE, timer or default
  startForeground: true,
  locationClient: BackgroundGeolocation.client.ANDROID_BAIDU_LOCATION, // 使用百度还是高德进行定位
  url: 'http://aaa.jkjs.org/location', 
  debug: false,
  locationAuthorizationAlert: {
    titleWhenNotEnabled: '定位服务关闭',
    titleWhenOff: '定位服务关闭',
    instructions: '检测到您已经关闭定位服务，为了能更好地为您服务请点击确定，在"位置"选项中选择"始终"',
    cancelButton: '取消',
    settingsButton: '设置'
  },
  params: {
    apiName: 'com.sxc.wp.user.addUserPosition',
    apiVersion: '1.0',
    bizName: 'wcGetUserInfoQueryDO',
    host: 'http://sssss.org',
    resourceType: Platform.OS === 'ios' ? '1' : '0',
    ...DeviceInfo.getBaseInfo(),
    version: DeviceInfo.deviceInfo.version + ''
  },
  method: 'post'
}



BackgroundGeolocation.configure(config)

// 先停止先前的服务
BackgroundGeolocation.stop(() => {
  console.log('====================================')
  console.log('bg location stop')
  console.log('====================================')
})
BackgroundGeolocation.start(() => {
  console.log('====================================')
  console.log('bg location start')
  console.log('====================================')
})



```