package com.example.androidocr;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ViewGroup;

public class SettingsActivity extends AppCompatActivity {

    public static final String
            KEY_PREF_HANYU_SWITCH = "hanyu_switch";
    public static final String
            KEY_PREF_COUNTRYCODE_SWITCH = "countrycode_switch";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }
}
