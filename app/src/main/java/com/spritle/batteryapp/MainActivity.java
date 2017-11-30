package com.spritle.batteryapp;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Style;

import java.util.Calendar;

import tourguide.tourguide.ChainTourGuide;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Sequence;
import tourguide.tourguide.ToolTip;

import static android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends AppCompatActivity {


    Button msg_btn, send_btn;
    SeekBar pb;
    TextView level_txt;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    final public static int SEND_SMS = 101;
    SwitchCompat switchCompat;
    final int PI_REQUEST_CODE = 123456;
    int pref_BatteryUpdatePeriod = 120000;  // 2 minutes
    Boolean sms_status = false;
    int level = 0;
    String alertmsg = "";
    private Animation mEnterAnimation, mExitAnimation;

    public ChainTourGuide mTourGuideHandler;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    NavigationView navigationView;
    private SwitchCompat switcher;
    ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_layout);

        sendSMSWithPermission();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        pb = (SeekBar) findViewById(R.id.progressbar);
        level_txt = (TextView) findViewById(R.id.textfield2);
        msg_btn = (Button) findViewById(R.id.msg_btn);
        send_btn = (Button) findViewById(R.id.send_btn);
        Menu menu = navigationView.getMenu();
        MenuItem menuItem = menu.findItem(R.id.notification);
        View actionView = MenuItemCompat.getActionView(menuItem);

        switcher = (SwitchCompat) actionView.findViewById(R.id.notify_switch);
        initNavigationDrawer();

        switchCompat = (SwitchCompat) findViewById(R.id.switchcompat);




        /* setup enter and exit animation */
        mEnterAnimation = new AlphaAnimation(0f, 1f);
        mEnterAnimation.setDuration(600);
        mEnterAnimation.setFillAfter(true);

        mExitAnimation = new AlphaAnimation(1f, 0f);
        mExitAnimation.setDuration(600);
        mExitAnimation.setFillAfter(true);


        String pb_size = preferences.getString("batterylevel", null);
        if (pb_size != null) {
            pb.setProgress(Integer.parseInt(pb_size));
            pb.setThumb(getThumb(Integer.parseInt(pb_size)));
            level_txt.setText(pb_size);
        } else {
            pb.setProgress(0);
            pb.setThumb(getThumb(0));
            level_txt.setText(Integer.toString(0));
        }
        editor = preferences.edit();
        editor.putString("batterylevel", level_txt.getText().toString());
        editor.apply();

        String savedRadioIndex = preferences.getString("radio_btn", null);
        if (savedRadioIndex != null && savedRadioIndex.equalsIgnoreCase("on")) {
            switchCompat.setChecked(true);
            editor = preferences.edit();
            editor.putString("radio_btn", "on");
            editor.apply();
        } else {
            switchCompat.setChecked(false);
            editor = preferences.edit();
            editor.putString("radio_btn", "off");
            editor.apply();
        }


        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.switchcompat:
                        if (isChecked) {
                            editor = preferences.edit();
                            editor.putString("radio_btn", "on");
                            editor.apply();
                        } else {
                            editor = preferences.edit();
                            editor.putString("radio_btn", "off");
                            editor.apply();
                        }
                        break;
                }
            }
        });
        switcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = preferences.edit();
                editor.putBoolean("notify_bool", switcher.isChecked());
                editor.apply();
            }
        });
        alertmsg = preferences.getString("alert_msg", null);
        if (alertmsg != null) {
            alertmsg = preferences.getString("alert_msg", null);
            editor = preferences.edit();
            editor.putString("alert_msg", alertmsg);
            editor.apply();
        } else {
            alertmsg = "My battery level is going to down. I catch you later";
            editor = preferences.edit();
            editor.putString("alert_msg", alertmsg);
            editor.apply();
        }


        msg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeLangDialog();

            }
        });


        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mTourGuideHandler != null) {
                    mTourGuideHandler.cleanUp();
                }
                Intent i = new Intent(MainActivity.this, ContacsActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);

            }
        });

        pb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                level_txt.setText(Integer.toString(progressChangedValue));
                seekBar.setThumb(getThumb(progress));
                editor = preferences.edit();
                editor.putString("batterylevel", Integer.toString(progressChangedValue));
                editor.apply();
                if (level > seekBar.getProgress()) {
                    sms_status = false;
                    editor = preferences.edit();
                    editor.putBoolean("sms_status", false);
                    editor.apply();
                }


            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                level_txt.setText(Integer.toString(progressChangedValue));
                editor = preferences.edit();
                editor.putString("batterylevel", Integer.toString(progressChangedValue));
                editor.apply();
                if (level > seekBar.getProgress()) {
                    sms_status = false;
                    editor = preferences.edit();
                    editor.putBoolean("sms_status", false);
                    editor.apply();
                }


            }
        });


        if (pb_size == null) {
            runOverlay_ContinueMethod();
        }

        Intent monitorIntent = new Intent(this, BatteryLevelReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), PI_REQUEST_CODE, monitorIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().get(Calendar.MILLISECOND), pref_BatteryUpdatePeriod, pendingIntent);
    }

    public Drawable getThumb(int progress) {
        View thumbView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_seekbar_thumb, null, false);
        ((TextView) thumbView.findViewById(R.id.tvProgress)).setText(progress + "%");
        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumbView.layout(0, 0, thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight());
        thumbView.draw(canvas);

        return new BitmapDrawable(getResources(), bitmap);

    }


    public void showChangeLangDialog() {

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        final EditText edt = (EditText) dialog.findViewById(R.id.edit1);

        edt.setText(alertmsg);
        dialog.show();

        Button ok_btn = (Button) dialog.findViewById(R.id.ok_btn);

        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edt.getText().toString().isEmpty()) {
                    editor = preferences.edit();
                    editor.putString("alert_msg", edt.getText().toString());
                    editor.apply();
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "please enter the msg", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }

    public void Send_Msg(int batterylevel) {
        Log.i("batterylevel=", "" + batterylevel);
        if (switchCompat.isChecked()) {
            Log.i("radio_on=", "" + switchCompat.isChecked());

            if (batterylevel == Integer.parseInt(preferences.getString("batterylevel", null))) {

                int size = preferences.getInt("phno_size", 0);
                Log.i("batterylevel=", size + "--" + Integer.parseInt(preferences.getString("batterylevel", null)));
                if (size != 0) {
                    SmsManager smsManager = SmsManager.getDefault();
                    for (int i = 0; i < size; i++) {
                        String msg = preferences.getString("alert_msg", null);
                        String phno1 = preferences.getString("phno_size" + i, null);
                        smsManager.sendTextMessage(phno1, null, msg, null, null);

                    }
                    sms_status = true;
                    Toast.makeText(MainActivity.this, "SMS send successfully", Toast.LENGTH_SHORT).show();


                } else {
                    Toast.makeText(MainActivity.this, "Please add Contacts", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void sendSMSWithPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.SEND_SMS);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.SEND_SMS}, SEND_SMS);
                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));

    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            level = intent.getIntExtra("message", 0);
            Log.i("receiver", "Got message: " + level);

        }
    };

    private void runOverlay_ContinueMethod() {

        ChainTourGuide tourGuide3 = ChainTourGuide.init(this)
                .setToolTip(new ToolTip()
                        .setDescription("Turn on alert switch")
                        .setBackgroundColor(Color.parseColor("#c0392b"))
                        .setGravity(Gravity.CENTER)
                )
                .setOverlay(new Overlay()
                        .setHoleRadius(120)
                        .setEnterAnimation(mEnterAnimation)
                        .setExitAnimation(mExitAnimation)
                        .setBackgroundColor(Color.parseColor("#EE2c3e50"))
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mTourGuideHandler.next();
                            }
                        })
                )
                .playLater(switchCompat);

        ChainTourGuide tourGuide4 = ChainTourGuide.init(this)
                .setToolTip(new ToolTip()
                        .setDescription("Set alert level by dragging over battery icon")
                        .setBackgroundColor(Color.parseColor("#c0392b"))
                        .setGravity(Gravity.CENTER)
                )
                .setOverlay(new Overlay()
                        .setHoleRadius(120)
                        .setEnterAnimation(mEnterAnimation)
                        .setExitAnimation(mExitAnimation)
                        .setBackgroundColor(Color.parseColor("#EE2c3e50"))
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mTourGuideHandler.next();
                            }
                        })
                )
                .playLater(pb);
        ChainTourGuide tourGuide5 = ChainTourGuide.init(this)
                .setToolTip(new ToolTip()
                        .setDescription("Customise your alert message")
                        .setBackgroundColor(Color.parseColor("#c0392b"))
                        .setGravity(Gravity.TOP | Gravity.RIGHT)
                )
                .setOverlay(new Overlay()
                        .setHoleRadius(50)
                        .setEnterAnimation(mEnterAnimation)
                        .setExitAnimation(mExitAnimation)
                        .setBackgroundColor(Color.parseColor("#EE2c3e50"))
                        .setStyle(Overlay.Style.Rectangle)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mTourGuideHandler.next();
                            }
                        })
                )
                .playLater(msg_btn);
        ChainTourGuide tourGuide6 = ChainTourGuide.init(this)
                .setToolTip(new ToolTip()
                        .setDescription("Pick recipient from your contacts")
                        .setBackgroundColor(Color.parseColor("#c0392b"))
                        .setGravity(Gravity.TOP | Gravity.LEFT)
                )
                .setOverlay(new Overlay()
                        .setHoleRadius(50)
                        .setEnterAnimation(mEnterAnimation)
                        .setExitAnimation(mExitAnimation)
                        .setStyle(Overlay.Style.Rectangle)
                        .setBackgroundColor(Color.parseColor("#EE2c3e50")))

                .playLater(send_btn);

        Sequence sequence = new Sequence.SequenceBuilder()
                .add(tourGuide3, tourGuide4, tourGuide5, tourGuide6)
                .setDefaultOverlay(new Overlay()
                        .setEnterAnimation(mEnterAnimation)
                        .setExitAnimation(mExitAnimation)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                mTourGuideHandler.next();
                            }
                        })
                )
                .setDefaultPointer(null)
                .setContinueMethod(Sequence.ContinueMethod.OverlayListener)
                .build();

        mTourGuideHandler = ChainTourGuide.init(this).playInSequence(sequence);
    }

    public void initNavigationDrawer() {


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                int id = menuItem.getItemId();

                switch (id) {
                    case R.id.notification:
                        switcher.setChecked(!switcher.isChecked());
                        editor = preferences.edit();
                        editor.putBoolean("notify_bool", switcher.isChecked());
                        editor.apply();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.share:
                        drawerLayout.closeDrawers();
                        String shareBody = "https://play.google.com/store/apps/details?id=************************";

                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "ATA APplication(Open it in Google Play Store to Download the Application)");

                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

                        startActivity(Intent.createChooser(sharingIntent, "Share via"));

                        break;
                    case R.id.info:
                        new MaterialStyledDialog.Builder(MainActivity.this)
                                .setTitle("Awesome!")
                                .setStyle(Style.HEADER_WITH_ICON)
                                .setIcon(android.R.drawable.ic_menu_send)
                                .setStyle(Style.HEADER_WITH_TITLE)
                                .setHeaderColor(R.color.colorPrimary)
                                .setDescription("What can we improve? Your feedback is always welcome.")
                                .setPositiveText("Feedback")

                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Intent email = new Intent(Intent.ACTION_SEND);
                                        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"info@spritle.com"});
                                        //email.putExtra(Intent.EXTRA_CC, new String[]{ to});
                                        //email.putExtra(Intent.EXTRA_BCC, new String[]{to});
                                        email.putExtra(Intent.EXTRA_SUBJECT, "Feedback of Volta app");
                                        email.putExtra(Intent.EXTRA_TEXT, "");

                                        //need this to prompts email client only
                                        email.setType("message/rfc822");

                                        startActivity(Intent.createChooser(email, "Choose an Email :"));
                                    }
                                })

                                .show();
                        drawerLayout.closeDrawers();
                        break;


                }
                return true;
            }
        });
        View header = navigationView.getHeaderView(0);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);

         actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }


}
