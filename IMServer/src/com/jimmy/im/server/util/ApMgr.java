package com.jimmy.im.server.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Created by AA on 2017/3/22.
 */
public class ApMgr {
    private static final String TAG = ApMgr.class.getSimpleName();

    /**
     * 便携热点是否开启
     * @param context 上下文
     * @return
     */
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * 关闭Wi-Fi
     * @param context 上下文
     */
    public static void closeWifi(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (wifimanager.isWifiEnabled()) {
            wifimanager.setWifiEnabled(false);
        }
    }

    /**
     * 开启便携热点
     * @param context 上下文
     * @param SSID 便携热点SSID
     * @param password 便携热点密码
     * @return
     */
    public static boolean openAp(Context context, String SSID, String password) {
        if(TextUtils.isEmpty(SSID)) {
            return false;
        }

        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (wifimanager.isWifiEnabled()) {
            wifimanager.setWifiEnabled(false);
        }

        WifiConfiguration wifiConfiguration = getApConfig(SSID, password);
        try {
            if(isApOn(context)) {
                wifimanager.setWifiEnabled(false);
                closeAp(context);
            }

            //使用反射开启Wi-Fi热点
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wifiConfiguration, true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 关闭便携热点
     * @param context 上下文
     */
    public static void closeAp(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取开启便携热点后自身热点IP地址
     * @param context
     * @return
     */
    public static String getHotspotLocalIpAddress(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifimanager.getDhcpInfo();
        if(dhcpInfo != null) {
            int address = dhcpInfo.serverAddress;
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));
        }
        return null;
    }

    /**
     * 设置有密码的热点信息
     * @param SSID 便携热点SSID
     * @param pwd 便携热点密码
     * @return
     */
    private static WifiConfiguration getApConfig(String SSID, String pwd) {
        if(TextUtils.isEmpty(pwd)) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = SSID;
        config.preSharedKey = pwd;
//        config.hiddenSSID = true;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

        int indexOfWPA2_PSK = 4;
        //从WifiConfiguration.KeyMgmt数组中查找WPA2_PSK的值
        for (int i = 0; i < WifiConfiguration.KeyMgmt.strings.length; i++) {
            if (WifiConfiguration.KeyMgmt.strings[i].equals("WPA2_PSK")) {
                indexOfWPA2_PSK = i;
                break;
            }
        }
        Log.d(TAG, "getApConfig indexOfWPA2_PSK:"+indexOfWPA2_PSK);
        Log.d(TAG, "getApConfig WifiConfiguration.KeyMgmt.WPA_PSK:"+WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedKeyManagement.set(indexOfWPA2_PSK);

        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return config;
    }

    public static void openAPUI(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //打开网络共享与热点设置页面
        ComponentName comp = new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$TetherSettingsActivity");
        intent.setComponent(comp);
        context.startActivity(intent);
    }
}
