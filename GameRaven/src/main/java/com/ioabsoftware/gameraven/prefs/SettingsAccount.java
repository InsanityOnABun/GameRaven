package com.ioabsoftware.gameraven.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.networking.GF_URLS;
import com.ioabsoftware.gameraven.networking.NetDesc;
import com.ioabsoftware.gameraven.util.AccountManager;
import com.ioabsoftware.gameraven.util.DocumentParser;
import com.ioabsoftware.gameraven.util.FinalDoc;
import com.ioabsoftware.gameraven.util.Theming;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class SettingsAccount extends PreferenceActivity implements FutureCallback<Response<FinalDoc>> {

    private static final String ION_INSTANCE = "AccountVerifier";
    private Ion accountVerifier;

    private Dialog addAccountDialog, verifyAccountDialog, modifyAccountDialog;

    String verifyUser;
    String verifyPass;

    PreferenceCategory accounts;
    Preference clickedAccount;
    String clickedAccountName;

    SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Theming.theme());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Theming.colorPrimaryDark());
        }

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settingsaccount);

        accountVerifier = Ion.getInstance(this, ION_INSTANCE);
        accountVerifier.getCookieMiddleware().clear();

        Theming.colorOverscroll(this);

        addAccountDialog = createAddAccountDialog();
        verifyAccountDialog = createVerifyAccountDialog();
        modifyAccountDialog = createModifyAccountV2Dialog();

        accounts = (PreferenceCategory) findPreference("accounts");
        updateAccountList();

        findPreference("addAccount").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                addAccountDialog.show();
                return true;
            }

        });
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.settings_activity, new LinearLayout(this), false);

        Toolbar mActionBar = contentView.findViewById(R.id.saToolbar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mActionBar.setTitle(getTitle());
        mActionBar.setTitleTextColor(Color.WHITE);

        ViewGroup contentWrapper = contentView.findViewById(R.id.saContentWrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    @Override
    public void onDestroy() {
        Crouton.clearCroutonsForActivity(this);
        super.onDestroy();
    }

    private void updateAccountList() {
        accounts.removeAll();

        String def = settings.getString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT);

        for (String s : AccountManager.getUsernames(this)) {
            Preference pref = new Preference(this);
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickedAccount = preference;
                    modifyAccountDialog.show();
                    return true;
                }
            });

            String sig = settings.getString("customSig" + s, "");

            pref.setTitle(s);

            if (s.equals(def) && !sig.equals(""))
                pref.setSummary("Default account, custom signature applied");
            else if (s.equals(def))
                pref.setSummary("Default account");
            else if (!sig.equals(""))
                pref.setSummary("Custom signature applied");

            pref.setPersistent(false);

            accounts.addPreference(pref);
        }
    }

    private LinearLayout addAccWrapper;

    private Dialog createAddAccountDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View v = inflater.inflate(R.layout.addaccount, null);

        final TextView userView = v.findViewById(R.id.addaccUser);
        final TextView passView = v.findViewById(R.id.addaccPassword);

        addAccWrapper = v.findViewById(R.id.addaccWrapper);

        b.setView(v);
        b.setTitle("Add Account");

        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("OK", null);

        final AlertDialog d = b.create();
        d.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                userView.setText("");
                passView.setText("");
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        verifyUser = userView.getText().toString().trim();
                        verifyPass = passView.getText().toString();

                        if (verifyUser.indexOf('@') == -1) {
                            verifyAccountDialog.show();

                            accountVerifier.getCookieMiddleware().clear();
                            currentDesc = NetDesc.VERIFY_ACCOUNT_S1;
                            accountVerifier.build(SettingsAccount.this)
                                    .load("GET", GF_URLS.ROOT)
                                    .as(new DocumentParser())
                                    .withResponse()
                                    .setCallback(SettingsAccount.this);
                        } else {
                            Crouton.showText(SettingsAccount.this,
                                    "Please use your username, not your email address.",
                                    Theming.croutonStyle(),
                                    (ViewGroup) v.findViewById(R.id.addaccUser).getParent());
                        }
                    }
                });
            }
        });

        return d;
    }

    private Dialog createVerifyAccountDialog() {
        ProgressDialog d = new ProgressDialog(this);
        d.setTitle("Verifying Account...");
        d.setCancelable(false);
        return d;
    }

    private boolean modifyAccountInit = false;

    private Dialog createModifyAccountV2Dialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View v = inflater.inflate(R.layout.modifyaccountv2, null);
        b.setView(v);

        Button deleteAcc = v.findViewById(R.id.modaccDeleteAcc);
        final CheckBox defaultAcc = v.findViewById(R.id.modaccDefaultAccount);
        final CheckBox useGFAQsSig = v.findViewById(R.id.modaccUseGfaqsSig);
        final EditText sigContent = v.findViewById(R.id.modaccSigContent);
        final TextView sigCounter = v.findViewById(R.id.modaccSigCounter);

        defaultAcc.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!modifyAccountInit) {
                    if (isChecked) {
                        settings.edit().putString("defaultAccount", clickedAccountName).apply();
                        Crouton.showText(SettingsAccount.this,
                                "Default account saved.",
                                Theming.croutonStyle(),
                                (ViewGroup) buttonView.getParent().getParent());
                    } else {
                        settings.edit().putString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT).apply();
                        Crouton.showText(SettingsAccount.this,
                                "Default account removed.",
                                Theming.croutonStyle(),
                                (ViewGroup) buttonView.getParent().getParent());
                    }
                }
            }
        });

        useGFAQsSig.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!modifyAccountInit) {
                    settings.edit().putBoolean("useGFAQsSig" + clickedAccountName, isChecked).apply();
                    sigContent.setEnabled(!isChecked);
                }
            }
        });

        sigContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String escapedSig = StringEscapeUtils.escapeHtml4(sigContent.getText().toString());
                int length = escapedSig.length();
                int lines = 0;
                for (int i = 0; i < escapedSig.length(); i++) {
                    if (escapedSig.charAt(i) == '\n') lines++;
                }

                String sigCounterString = (1 - lines) + " line break(s), " + (160 - length) + " characters available";
                sigCounter.setText(sigCounterString);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        });

        deleteAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickedAccountName.equals(settings.getString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT)))
                    settings.edit().putString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT).apply();

                settings.edit().remove("customSig" + clickedAccountName).apply();

                AccountManager.removeUser(SettingsAccount.this, clickedAccountName);
                accounts.removePreference(clickedAccount);
                modifyAccountDialog.dismiss();
                Crouton.showText(SettingsAccount.this, "Account removed.", Theming.croutonStyle());
            }
        });

        b.setPositiveButton("Save Sig", null);

        b.setNeutralButton("Clear Sig", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.edit().putString("customSig" + clickedAccountName, "").apply();
                sigContent.setText("");
                Crouton.showText(SettingsAccount.this, "Signature cleared and saved.", Theming.croutonStyle());
            }
        });

        b.setNegativeButton("Close", null);

        final AlertDialog d = b.create();

        d.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                clickedAccountName = clickedAccount.getTitle().toString();
                d.setTitle("Modify " + clickedAccountName);

                modifyAccountInit = true;

                defaultAcc.setChecked(clickedAccountName.equals(
                        settings.getString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT)));

                useGFAQsSig.setChecked(settings.getBoolean("useGFAQsSig" + clickedAccountName, false));

                modifyAccountInit = false;

                sigContent.setText(settings.getString("customSig" + clickedAccountName, ""));

                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String escapedSig = StringEscapeUtils.escapeHtml4(sigContent.getText().toString());
                        int length = escapedSig.length();
                        int lines = 0;
                        for (int i = 0; i < escapedSig.length(); i++) {
                            if (escapedSig.charAt(i) == '\n') lines++;
                        }

                        if (length < 161) {
                            if (lines < 2) {
                                settings.edit().putString("customSig" + clickedAccountName,
                                        sigContent.getText().toString()).apply();

                                Crouton.showText(SettingsAccount.this, "Signature saved.", Theming.croutonStyle());
                                d.dismiss();
                            } else {
                                Crouton.showText(SettingsAccount.this,
                                        "Signatures can only have 1 line break.",
                                        Theming.croutonStyle(),
                                        (ViewGroup) sigContent.getParent());
                            }
                        } else {
                            Crouton.showText(SettingsAccount.this,
                                    "Signatures can only have a maximum of 160 characters.",
                                    Theming.croutonStyle(),
                                    (ViewGroup) sigContent.getParent());
                        }
                    }
                });
            }
        });

        d.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateAccountList();
            }
        });

        return d;
    }

    private NetDesc currentDesc;

    /**
     * onCompleted is called by the Future with the result or exception of the asynchronous operation.
     *
     * @param e      Exception encountered by the operation
     * @param result Result returned from the operation
     */
    @Override
    public void onCompleted(Exception e, Response<FinalDoc> result) {
        if (e == null && result != null) {
            Document doc = result.getResult().doc;

            if (currentDesc == NetDesc.VERIFY_ACCOUNT_S1) {
                String loginKey = doc.getElementsByAttributeValue("name", "key").attr("value");
                HashMap<String, List<String>> loginData = new HashMap<>();
                // "EMAILADDR", user, "PASSWORD", password, "path", lastPath, "key", key
                loginData.put("EMAILADDR", Collections.singletonList(verifyUser));
                loginData.put("PASSWORD", Collections.singletonList(verifyPass));
                loginData.put("path", Collections.singletonList(GF_URLS.ROOT));
                loginData.put("key", Collections.singletonList(loginKey));

                currentDesc = NetDesc.VERIFY_ACCOUNT_S2;
                accountVerifier.build(this)
                        .load("POST", GF_URLS.ROOT + "/user/login")
                        .setBodyParameters(loginData)
                        .as(new DocumentParser())
                        .withResponse()
                        .setCallback(SettingsAccount.this);
            } else if (currentDesc == NetDesc.VERIFY_ACCOUNT_S2) {
                if (!result.getRequest().getUri().toString().endsWith("/user/login")) {
                    String userProperCasing = result.getResult().doc.selectFirst("a.welcome").text().trim();
                    AccountManager.addUser(SettingsAccount.this, userProperCasing, verifyPass);
                    verifyAccountDialog.dismiss();
                    addAccountDialog.dismiss();
                    Crouton.showText(this, "Verification succeeded.", Theming.croutonStyle());
                    updateAccountList();
                } else {
                    verifyAccountDialog.dismiss();
                    Crouton.showText(this,
                            "Verification failed. Check your username and password and try again.",
                            Theming.croutonStyle(),
                            addAccWrapper);
                }
            }
        } else {
            verifyAccountDialog.dismiss();
            Crouton.showText(this, "Network connection failed. Check your network settings.", Theming.croutonStyle(), addAccWrapper);
        }
    }
}
