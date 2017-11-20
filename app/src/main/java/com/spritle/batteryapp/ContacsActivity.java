package com.spritle.batteryapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SpritleAndroid on 27/10/17.
 */

public class ContacsActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int RESULT_PICK_CONTACT = 1;
    List<String> name1 = new ArrayList<String>();
    List<String> phno1 = new ArrayList<String>();
    MyAdapter ma;
    ListView lv;
    ImageView add_btn;
    SharedPreferences mSharedPreference1;
    SharedPreferences.Editor editor;
    Button done_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_layout);

        add_btn = (ImageView) findViewById(R.id.add_btn);
        lv = (ListView) findViewById(R.id.lv);
        done_btn = (Button) findViewById(R.id.done_btn);
        mSharedPreference1 = PreferenceManager.getDefaultSharedPreferences(ContacsActivity.this);
        // Read and show the contacts
        showContacts();


        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phno1.size() < 3) {
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
                } else {
                    Toast.makeText(ContacsActivity.this, "You can add maximum 3 contacts", Toast.LENGTH_SHORT).show();
                }

            }
        });
        done_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }


    private void showContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.

            name1.clear();
            phno1.clear();

            int size = mSharedPreference1.getInt("phno_size", 0);
            if (size != 0) {
                for (int i = 0; i < size; i++) {
                    name1.add(mSharedPreference1.getString("name_size" + i, null));
                    phno1.add(mSharedPreference1.getString("phno_size" + i, null));
                }
            }
            ma = new MyAdapter();
            lv.setAdapter(ma);
            lv.setItemsCanFocus(false);
            lv.setTextFilterEnabled(true);
            if (size != 0) {
                done_btn.setVisibility(View.VISIBLE);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }

    /**
     * Query the Uri and read contact details. Handle the picked contact data.
     *
     * @param data
     */
    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null;
            String name = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            if (!phno1.contains(phoneNo)) {
                name1.add(name);
                phno1.add(phoneNo);
                editor = mSharedPreference1.edit();
                editor.putInt("name_size", name1.size());
                editor.putInt("phno_size", phno1.size());

                for (int i = 0; i < phno1.size(); i++) {

                    editor.putString("name_size" + i, name1.get(i));
                    editor.putString("phno_size" + i, phno1.get(i));
                }
                editor.commit();

                ma = new MyAdapter();
                lv.setAdapter(ma);
                lv.setItemsCanFocus(false);
                lv.setTextFilterEnabled(true);
                if (phno1.size() != 0) {
                    done_btn.setVisibility(View.VISIBLE);

                }
            } else {
                Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyAdapter extends BaseAdapter {
        private SparseBooleanArray mCheckStates;
        LayoutInflater mInflater;
        TextView tv1, tv;
        ImageView delete_btn;

        MyAdapter() {
            mCheckStates = new SparseBooleanArray(name1.size());
            mInflater = (LayoutInflater) ContacsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return name1.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub

            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            View vi = convertView;
            if (convertView == null)
                vi = mInflater.inflate(R.layout.row, null);
            TextView tv = (TextView) vi.findViewById(R.id.textView1);
            tv1 = (TextView) vi.findViewById(R.id.textView2);
            delete_btn = (ImageView) vi.findViewById(R.id.delete_btn);
            ImageView image = (ImageView) vi.findViewById(R.id.image_view);
            tv.setText(name1.get(position));
            String desiredString = tv.getText().toString().substring(0, 2);

            Typeface typeface = ResourcesCompat.getFont(ContacsActivity.this, R.font.proxima_nova);
            ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
            int color1 = generator.getRandomColor();
            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .useFont(typeface)
                    .fontSize(50) /* size in px */
                    .toUpperCase()
                    .bold()
                    .endConfig()
                    .buildRound(desiredString, color1);


            image.setImageDrawable(drawable);

            tv1.setText(phno1.get(position));
            delete_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    name1.remove(position);
                    phno1.remove(position);
                    notifyDataSetChanged();
                    //Toast.makeText(ContacsActivity.this, position+""+name1.size(), Toast.LENGTH_SHORT).show();
                    editor = mSharedPreference1.edit();
                    editor.putInt("name_size", name1.size());
                    editor.putInt("phno_size", phno1.size());

                    for (int i = 0; i < phno1.size(); i++) {
                        editor.remove("name_size" + position + 1);
                        editor.remove("phno_size" + position + 1);
                    }
                    editor.commit();
                    if (phno1.size() != 0) {
                        done_btn.setVisibility(View.VISIBLE);
                    } else {
                        done_btn.setVisibility(View.GONE);
                    }
                }
            });
            return vi;
        }

    }
}
