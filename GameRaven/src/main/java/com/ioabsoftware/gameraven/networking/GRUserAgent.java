package com.ioabsoftware.gameraven.networking;

import android.content.Context;
import android.content.pm.PackageManager;

public class GRUserAgent {
    public static String get(Context c) {
        String verName = "";
        try {
            verName = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName + " ";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return System.getProperty("http.agent") + " [ GameRaven " + verName + "]";
    }


}
