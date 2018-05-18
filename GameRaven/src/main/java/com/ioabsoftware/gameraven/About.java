package com.ioabsoftware.gameraven;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.ioabsoftware.gameraven.util.Theming;
import com.joanzapata.iconify.Iconify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class About extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Theming.theme());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Theming.colorPrimaryDark());
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        Theming.colorOverscroll(this);

        setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(R.id.abtToolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button donate = findViewById(R.id.abtDonate);
        donate.setText(Iconify.compute(this, getString(R.string.buyMeACoffee), donate));

        try {
            String versionLabel = getString(R.string.version);
            String verName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            String buildNumLabel = getString(R.string.buildNumber);
            int verCode = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
            String verString = versionLabel + " " + verName + "\n" + buildNumLabel + " " + String.valueOf(verCode);
            ((TextView) findViewById(R.id.abtBuildVer)).setText(verString);
        } catch (NameNotFoundException e) {
            ((TextView) findViewById(R.id.abtBuildVer)).setText(R.string.versionNotSet);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void genFeedback(View view) {
        Intent send = new Intent(Intent.ACTION_SENDTO);
        send.setData(Uri.parse("mailto:support@ioabsoftware.com?subject=GameRaven%20Feedback"));
        startActivity(Intent.createChooser(send, getString(R.string.sendEmail)));
    }

    public void donate(View view) {
        Intent donate = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("https://www.paypal.me/CharlesRosaaen");
        donate.setData(uri);
        startActivity(donate);
    }

    public void viewPrivacyPolicy(View view) {
        try {
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.privacypolicy)));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line).append('\n');
            }
            br.close();

            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.view_privacy_policy);
            b.setMessage(text.toString());
            b.setPositiveButton(R.string.ok, null);
            b.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
