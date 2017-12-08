package com.spritle.batteryapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

/**
 * Created by SpritleAndroid on 09/11/17.
 */

public class BatteryLevelReceiver extends BroadcastReceiver {

    Boolean sms_status = false;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Notification.Builder notif;
    NotificationManager nm;

    @Override
    public void onReceive(Context context, Intent intent) {

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Intent batteryStatusIntent = context.getApplicationContext()
                .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryStatusIntent.getIntExtra("level", 0);
        //Toast.makeText(context, preferences.getString("batterylevel", null) + "-" + level, Toast.LENGTH_SHORT).show();
        if (preferences.getString("batterylevel", null) != null) {

            sms_status = preferences.getBoolean("sms_status", false);
            Log.i("1.sms_status=", "" + sms_status);
            //Toast.makeText(context, level + "-" + sms_status + "-" + preferences.getString("batterylevel", null), Toast.LENGTH_SHORT).show();
            if ((Integer.parseInt(preferences.getString("batterylevel", null)) > level) && sms_status) {
                sms_status = false;
                editor = preferences.edit();
                editor.putBoolean("sms_status", false);
                editor.apply();
            }
            if (level > (Integer.parseInt(preferences.getString("batterylevel", null)))) {
                sms_status = false;
                editor = preferences.edit();
                editor.putBoolean("sms_status", false);
                editor.apply();
            }
            Log.i("2.sms_status=", "" + sms_status);

            String switchCompat = preferences.getString("radio_btn", null);
            if ((level == Integer.parseInt(preferences.getString("batterylevel", null))) && !sms_status) {
                if (switchCompat.equalsIgnoreCase("on")) {
                    Log.i("switchCompat=", preferences.getString("radio_btn", null));
                    Log.i("batterylevel=", preferences.getString("batterylevel", null));
                    Log.i("level=", "" + level);

                    if (level == Integer.parseInt(preferences.getString("batterylevel", null))) {

                        int size = preferences.getInt("phno_size", 0);
                        Log.i("batterylevel=", size + "--" + Integer.parseInt(preferences.getString("batterylevel", null)));
                        if (preferences.getBoolean("notify_bool", false)) {
                            notification(context);
                        } else {
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

                    }
                }
            }
        }


    }

    public void notification(Context context) {

        notif = new Notification.Builder(context);
        notif.setSmallIcon(R.mipmap.ic_launcher);
        int notificationId = new Random().nextInt();
        notif.setContentTitle("Do you want to send battery level msg to others?");
        Uri path = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notif.setSound(path);
        nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        Intent yesReceive = new Intent();
        yesReceive.putExtra("notificationId",notificationId);
        yesReceive.setAction(AppConstant.YES_ACTION);
        PendingIntent pendingIntentYes = PendingIntent.getBroadcast(context, 12345, yesReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(R.drawable.ic_notifications, "Yes", pendingIntentYes);


        Intent yesReceive2 = new Intent();
        yesReceive.putExtra("notificationId",notificationId);
        yesReceive2.setAction(AppConstant.STOP_ACTION);
        PendingIntent pendingIntentYes2 = PendingIntent.getBroadcast(context, 12345, yesReceive2, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(R.drawable.ic_notifications, "No", pendingIntentYes2);


        nm.notify(10, notif.getNotification());
    }
}
