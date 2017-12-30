package com.ioabsoftware.gameraven;

import android.app.Application;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialCommunityModule;
import com.joanzapata.iconify.fonts.MaterialModule;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Iconify
                .with(new FontAwesomeModule())
                .with(new MaterialModule())
                .with(new MaterialCommunityModule());
    }
}
