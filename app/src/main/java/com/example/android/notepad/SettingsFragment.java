package com.example.android.notepad;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 从XML文件加载设置
        addPreferencesFromResource(R.xml.settings);
    }
}