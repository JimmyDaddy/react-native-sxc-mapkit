/**
 * @Author: jimmydaddy
 * @Date:   2017-03-21 06:40:14
 * @Email:  heyjimmygo@gmail.com
 * @Filename: index.js
 * @Last modified by:   jimmydaddy
 * @Last modified time: 2017-07-27 05:41:39
 * @License: GNU General Public License（GPL)
 * @Copyright: ©2015-2017 www.songxiaocai.com 宋小菜 All Rights Reserved.
 */

import MapTypes from './js/MapTypes'
import MapView from './js/MapView'
import MapModule from './js/MapModule'
import Geolocation from './js/Geolocation'

const { DeviceEventEmitter, NativeModules, Platform } = require('react-native')
const RNBackgroundGeolocation = NativeModules.BackgroundGeolocation

function emptyFn () {}

const BackgroundGeolocation = {
  events: ['location', 'stationary', 'error'],

  provider: {
    ANDROID_DISTANCE_FILTER_PROVIDER: 0,
    ANDROID_ACTIVITY_PROVIDER: 1,
    TIMER_PROVIDE: 2
  },
  manufacture: {
    XIAO_MI: 'xiaomi',
    MEIZU: 'meizu',
    OPPO: 'oppo',
    HUAWEI: 'huawei',
    VIVO: 'vivo'
  },
  client: {
    ANDROID_BAIDU_LOCATION: 0,
    ANDROID_AMAP_LOCATION: 1
  },

  mode: {
    BACKGROUND: 0,
    FOREGROUND: 1
  },

  accuracy: {
    HIGH: 0,
    MEDIUM: 100,
    LOW: 1000,
    PASSIVE: 10000
  },

  configure: function (config, successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.configure(config, successFn, errorFn)
  },

  start: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.start(successFn, errorFn)
  },

  stop: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.stop(successFn, errorFn)
  },

  isLocationEnabled: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.isLocationEnabled(successFn, errorFn)
  },

  showAppSettings: function () {
    RNBackgroundGeolocation.showAppSettings()
  },

  showLocationSettings: function () {
    RNBackgroundGeolocation.showLocationSettings()
  },

  watchLocationMode: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.watchLocationMode(successFn, errorFn)
  },

  stopWatchingLocationMode: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.stopWatchingLocationMode(successFn, errorFn)
  },

  getLocations: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.getLocations(successFn, errorFn)
  },
/*
  getValidLocations: function(successFn, errorFn) {
    successFn = successFn || emptyFn;
    errorFn = errorFn || emptyFn;
    RNBackgroundGeolocation.getValidLocations(successFn, errorFn);
  },

  deleteLocation: function(locationId, successFn, errorFn) {
    successFn = successFn || emptyFn;
    errorFn = errorFn || emptyFn;
    RNBackgroundGeolocation.deleteLocation(locationId, successFn, errorFn);
  },

  deleteAllLocations: function(successFn, errorFn) {
    successFn = successFn || emptyFn;
    errorFn = errorFn || emptyFn;
    RNBackgroundGeolocation.deleteAllLocations(successFn, errorFn);
  },
*/
  switchMode: function (modeId, successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.switchMode(modeId, successFn, errorFn)
  },

  getConfig: function (successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.getConfig(successFn, errorFn)
  },

  getLogEntries: function (limit, successFn, errorFn) {
    successFn = successFn || emptyFn
    errorFn = errorFn || emptyFn
    RNBackgroundGeolocation.getLogEntries(limit, successFn, errorFn)
  },

  on: function (event, callbackFn) {
    if (typeof callbackFn !== 'function') {
      throw new Error('RNBackgroundGeolocation: callback function must be provided')
    }
    if (this.events.indexOf(event) < 0) {
      throw new Error('RNBackgroundGeolocation: Unknown event "' + event + '"')
    }

    return DeviceEventEmitter.addListener(event, callbackFn)
  },
  /**
   * get current position
   * @type {Object}
   */
  getCurrentPosition: () => {
    return RNBackgroundGeolocation.getCurrentPosition()
  },
  /**
   * [addAppToWhiteList description]
   * @method  addAppToWhiteList
   * @author JimmyDaddy
   * @date    2017-08-10T20:32:08+080
   * @version [version]
   */
  addAppToWhiteList: () => {
    return RNBackgroundGeolocation.addAppToWhiteList()
  },
  /**
   * [getManufacture description]
   * @method  getManufacture
   * @return  {[type]} [description]
   * @author JimmyDaddy
   * @date    2017-08-10T20:35:52+080
   * @version [version]
   */
  getManufacture: () => {
    return RNBackgroundGeolocation.getManufacture()
  },
  /**
   * Android only
   * @method  isFirstOpen
   * @return  {Boolean} [description]
   * @author JimmyDaddy
   * @date    2017-08-10T20:46:43+080
   * @version [version]
   */
  isFirstOpen: () => {
    if (Platform.OS === 'android') {
      return RNBackgroundGeolocation.isFirstOpenApp()
    }
  }

}

export {
  BackgroundGeolocation,
  MapTypes,
  MapView,
  MapModule,
  Geolocation
}
