package com.jimmy.im.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String TAG = BootBroadcastReceiver.class.getSimpleName();

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            Log.d(TAG, "onReceive intent.getAction():" + intent.getAction());
        }
        if (intent.getAction().equals(ACTION)) {
            Intent mainActivityIntent = new Intent(context, com.jimmy.im.server.WIFIAPActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
        }
    }

}
