https://blog.csdn.net/su749520/article/details/80485071

Android 8.0 WiFi Ap 热点控制接口
2018年05月28日 17:16:00 法迪 阅读数：1642
版权声明：本文为博主原创文章，未经博主允许不得转载。 https://blog.csdn.net/su749520/article/details/80485071
1. Android 7.0 及其以前的 WiFi 热点接口

    /**
     * Gets the Wi-Fi enabled state.
     *
     * @return One of {@link #WIFI_AP_STATE_DISABLED},
     * {@link #WIFI_AP_STATE_DISABLING}, {@link #WIFI_AP_STATE_ENABLED},
     * {@link #WIFI_AP_STATE_ENABLING}, {@link #WIFI_AP_STATE_FAILED}
     * @see #isWifiApEnabled()
     */
    public static boolean isWiFiApOpened(Context mContext) {
        WifiManager mWifiManager = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE));
        int state = mWifiManager.getWifiApState();
        return (state == WifiManager.WIFI_AP_STATE_ENABLING || state == WifiManager.WIFI_AP_STATE_ENABLED);
    }

    /**
     * Start AccessPoint mode with the specified configuration. If the radio is
     * already running in AP mode, update the new configuration Note that
     * starting in access point mode disables station mode operation
     *
     * @param wifiConfig SSID, security and channel details as part of
     *                   WifiConfiguration
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public static void setWiFiApEnable(Context mContext, boolean value) {
        WifiManager mWifiManager = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE));
        mWifiManager.setWifiApEnabled(null, value);
    }

    1
    2
    3
    4
    5
    6
    7
    8
    9
    10
    11
    12
    13
    14
    15
    16
    17
    18
    19
    20
    21
    22
    23
    24
    25
    26
    27

2. Android 8.0 的 WiFi 热点接口
2.1 判断 WiFi Ap 是否打开

========================================================
private WiFiApReceiver mWiFiApReceiver;

        mWiFiApReceiver = new WiFiApReceiver();
        // 注册广播事件
        mWiFiApReceiver.setListening(true);

========================================================
    /**
     * Android 8.0 WiFi Ap Listener
     */
    private static int isWiFiApState = WifiManager.WIFI_AP_STATE_FAILED;

    public static boolean isWiFiApOpened_O() {
        return (isWiFiApState == WifiManager.WIFI_AP_STATE_ENABLING || isWiFiApState == WifiManager.WIFI_AP_STATE_ENABLED);
    }

    private final class WiFiApReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                Log.d(TAG, "Registering receiver");
                final IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
                mContext.registerReceiver(this, filter);
                mRegistered = true;
            } else if (!listening && mRegistered) {
                Log.d(TAG, "Unregistering receiver");
                mContext.unregisterReceiver(this);
                mRegistered = false;
            }
        }

        public void onReceive(Context context, Intent intent) {
            isWiFiApState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
            String result = null;

            switch (isWiFiApState) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    result = "DISABLED";
                    break;
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    result =  "DISABLING";
                    break;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                    result =  "ENABLED";
                    break;
                case WifiManager.WIFI_AP_STATE_ENABLING:
                    result =  "ENABLING";
                    break;
                case WifiManager.WIFI_AP_STATE_FAILED:
                    result =  "FAILED";
                    break;
            }

            Log.d(TAG, "WiFi state : " + result);
        }
    }

    1
    2
    3
    4
    5
    6
    7
    8
    9
    10
    11
    12
    13
    14
    15
    16
    17
    18
    19
    20
    21
    22
    23
    24
    25
    26
    27
    28
    29
    30
    31
    32
    33
    34
    35
    36
    37
    38
    39
    40
    41
    42
    43
    44
    45
    46
    47
    48
    49
    50
    51
    52
    53
    54
    55
    56
    57
    58
    59
    60

2.2 控制 WiFi Ap 打开与关闭
2.2.1 需要的权限

    <!-- WiFi AP startTethering -->
    <uses-permission android:name="android.permission.TETHER_PRIVILEGED" />

    1
    2

2.2.2 Android 8.0 WiFi 热点打开与关闭接口

    /**
     * Android 8.0 WiFi Ap Settings
     * <uses-permission android:name="android.permission.TETHER_PRIVILEGED" />
     */
    public static void setWiFiApEnable_O(Context mContext, boolean value) {
        ConnectivityManager mConnectivityManager= (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (value) {
            mConnectivityManager.startTethering(ConnectivityManager.TETHERING_WIFI, false, new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringStarted() {
                    Log.d(TAG, "onTetheringStarted");
                    // Don't fire a callback here, instead wait for the next update from wifi.
                }

                @Override
                public void onTetheringFailed() {
                  Log.d(TAG, "onTetheringFailed");
                  // TODO: Show error.
                }
            });
        } else {
            mConnectivityManager.stopTethering(ConnectivityManager.TETHERING_WIFI);
        }
    }

    1
    2
    3
    4
    5
    6
    7
    8
    9
    10
    11
    12
    13
    14
    15
    16
    17
    18
    19
    20
    21
    22
    23
    24

2.2.3 相关源码

frameworks/base/core/java/android/net/ConnectivityManager.java

    @SystemApi
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback) {
        startTethering(type, showProvisioningUi, callback, null);
    }

    /**
     * Runs tether provisioning for the given type if needed and then starts tethering if
     * the check succeeds. If no carrier provisioning is required for tethering, tethering is
     * enabled immediately. If provisioning fails, tethering will not be enabled. It also
     * schedules tether provisioning re-checks if appropriate.
     *
     * @param type The type of tethering to start. Must be one of
     *         {@link ConnectivityManager.TETHERING_WIFI},
     *         {@link ConnectivityManager.TETHERING_USB}, or
     *         {@link ConnectivityManager.TETHERING_BLUETOOTH}.
     * @param showProvisioningUi a boolean indicating to show the provisioning app UI if there
     *         is one. This should be true the first time this function is called and also any time
     *         the user can see this UI. It gives users information from their carrier about the
     *         check failing and how they can sign up for tethering if possible.
     * @param callback an {@link OnStartTetheringCallback} which will be called to notify the caller
     *         of the result of trying to tether.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback, Handler handler) {
        Preconditions.checkNotNull(callback, "OnStartTetheringCallback cannot be null.");

        ResultReceiver wrappedCallback = new ResultReceiver(handler) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == TETHER_ERROR_NO_ERROR) {
                    callback.onTetheringStarted();
                } else {
                    callback.onTetheringFailed();
                }
            }
        };

        try {
            String pkgName = mContext.getOpPackageName();
            Log.i(TAG, "startTethering caller:" + pkgName);
            mService.startTethering(type, wrappedCallback, showProvisioningUi, pkgName);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception trying to start tethering.", e);
            wrappedCallback.send(TETHER_ERROR_SERVICE_UNAVAIL, null);
        }
    }

    /**
     * Stops tethering for the given type. Also cancels any provisioning rechecks for that type if
     * applicable.
     *
     * @param type The type of tethering to stop. Must be one of
     *         {@link ConnectivityManager.TETHERING_WIFI},
     *         {@link ConnectivityManager.TETHERING_USB}, or
     *         {@link ConnectivityManager.TETHERING_BLUETOOTH}.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void stopTethering(int type) {
        try {
            String pkgName = mContext.getOpPackageName();
            Log.i(TAG, "stopTethering caller:" + pkgName);
            mService.stopTethering(type, pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    1
    2
    3
    4
    5
    6
    7
    8
    9
    10
    11
    12
    13
    14
    15
    16
    17
    18
    19
    20
    21
    22
    23
    24
    25
    26
    27
    28
    29
    30
    31
    32
    33
    34
    35
    36
    37
    38
    39
    40
    41
    42
    43
    44
    45
    46
    47
    48
    49
    50
    51
    52
    53
    54
    55
    56
    57
    58
    59
    60
    61
    62
    63
    64
    65
    66
    67
    68
    69
    70
    71
    72
    73
    74

3. 参考 SystemUI 的热点关闭
3.1 字符串

通知栏面板的快捷开关

    <string name="quick_settings_hotspot_label" msgid="6046917934974004879">"热点"</string>

    1
    2

3.2 搜索调用位置

grep -irn “quick_settings_hotspot_label” vendor/mediatek/proprietary/packages/apps/SystemUI/

root@69959bbb90c6:/home/suhuazhi/8.1/liangxiang# grep -irn "quick_settings_hotspot_label" vendor/mediatek/proprietary/packages/apps/SystemUI/

vendor/mediatek/proprietary/packages/apps/SystemUI/src/com/android/systemui/qs/tiles/HotspotTile.java:107:        return mContext.getString(R.string.quick_settings_hotspot_label);

vendor/mediatek/proprietary/packages/apps/SystemUI/src/com/android/systemui/qs/tiles/HotspotTile.java:115:        state.label = mContext.getString(R.string.quick_settings_hotspot_label);

    1
    2
    3
    4
    5

3.3 WiFi 热点功能是否支持

package com.android.systemui.statusbar.policy;

public class HotspotControllerImpl implements HotspotController {

    @Override
    public boolean isHotspotSupported() {
        return mConnectivityManager.isTetheringSupported()
                && mConnectivityManager.getTetherableWifiRegexs().length != 0
                && UserManager.get(mContext).isUserAdmin(ActivityManager.getCurrentUser());
    }

    1
    2
    3
    4
    5
    6
    7
    8
    9
    10

3.4 WiFi 热点功能是否打开

package com.android.systemui.statusbar.policy;

public class HotspotControllerImpl implements HotspotController {

    @Override
    public boolean isHotspotEnabled() {
        return mHotspotState == WifiManager.WIFI_AP_STATE_ENABLED;
    }

    1
    2
    3
    4
    5
    6
    7
    8

3.5 WiFi 热点功能控制

package com.android.systemui.statusbar.policy;

public class HotspotControllerImpl implements HotspotController {

    @Override
    public void setHotspotEnabled(boolean enabled) {
        if (enabled) {
            OnStartTetheringCallback callback = new OnStartTetheringCallback();
            mWaitingForCallback = true;
            if (DEBUG) Log.d(TAG, "Starting tethering");
            mConnectivityManager.startTethering(
                    ConnectivityManager.TETHERING_WIFI, false, callback);
            fireCallback(isHotspotEnabled());
        } else {
            mConnectivityManager.stopTethering(ConnectivityManager.TETHERING_WIFI);
        }
    }

    private void fireCallback(boolean isEnabled) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onHotspotChanged(isEnabled);
            }
        }
    }
