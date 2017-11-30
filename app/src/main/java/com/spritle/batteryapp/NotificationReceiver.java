package com.spritle.batteryapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.widget.Toast;

/**
 * Created by SpritleAndroid on 30/11/17.
 */

public class NotificationReceiver extends BroadcastReceiver {

    Boolean sms_status = false;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        int notificationId = intent.getIntExtra("notificationId", 0);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        sms_status = preferences.getBoolean("sms_status", false);

        String action = intent.getAction();

        if (AppConstant.YES_ACTION.equals(action)) {
            Toast.makeText(context, "YES CALLED", Toast.LENGTH_SHORT).show();

            manager.cancel(notificationId);
            int size = preferences.getInt("phno_size", 0);
            if (size != 0) {
                SmsManager smsManager = SmsManager.getDefault();
                for (int i = 0; i < size; i++) {
                    String msg = preferences.getString("alert_msg", null);
                    String phno1 = preferences.getString("phno_size" + i, null);
                    smsManager.sendTextMessage(phno1, null, msg, null, null);

                }
                sms_status = true;
                editor = preferences.edit();
                editor.putBoolean("sms_status", true);
                editor.apply();

                Toast.makeText(context, "SMS send successfully", Toast.LENGTH_SHORT).show();


            } else {
                Toast.makeText(context, "Please add Contacts", Toast.LENGTH_SHORT).show();
            }
        }
        else  if (AppConstant.STOP_ACTION.equals(action)) {
            Toast.makeText(context, "STOP CALLED", Toast.LENGTH_SHORT).show();

            manager.cancel(notificationId);
        }
    }
}
