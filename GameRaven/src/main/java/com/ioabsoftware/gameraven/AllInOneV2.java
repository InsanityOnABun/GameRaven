package com.ioabsoftware.gameraven;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.ioabsoftware.gameraven.db.HighlightListDBHelper;
import com.ioabsoftware.gameraven.db.HighlightedUser;
import com.ioabsoftware.gameraven.networking.GF_URLS;
import com.ioabsoftware.gameraven.networking.GRUserAgent;
import com.ioabsoftware.gameraven.networking.NetDesc;
import com.ioabsoftware.gameraven.networking.Session;
import com.ioabsoftware.gameraven.prefs.HeaderSettings;
import com.ioabsoftware.gameraven.prefs.SettingsHighlightedUsers;
import com.ioabsoftware.gameraven.util.AccountManager;
import com.ioabsoftware.gameraven.util.DocumentParser;
import com.ioabsoftware.gameraven.util.Theming;
import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.BaseRowData.ReadStatus;
import com.ioabsoftware.gameraven.views.ViewAdapter;
import com.ioabsoftware.gameraven.views.rowdata.BoardRowData;
import com.ioabsoftware.gameraven.views.rowdata.BoardRowData.BoardType;
import com.ioabsoftware.gameraven.views.rowdata.GameSearchRowData;
import com.ioabsoftware.gameraven.views.rowdata.HeaderRowData;
import com.ioabsoftware.gameraven.views.rowdata.MentionRowData;
import com.ioabsoftware.gameraven.views.rowdata.MessageRowData;
import com.ioabsoftware.gameraven.views.rowdata.NotifRowData;
import com.ioabsoftware.gameraven.views.rowdata.PMDetailRowData;
import com.ioabsoftware.gameraven.views.rowdata.PMRowData;
import com.ioabsoftware.gameraven.views.rowdata.TopicRowData;
import com.ioabsoftware.gameraven.views.rowdata.TopicRowData.TopicType;
import com.ioabsoftware.gameraven.views.rowdata.UserDetailRowData;
import com.ioabsoftware.gameraven.views.rowview.MessageRowView;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.MaterialCommunityIcons;
import com.joanzapata.iconify.fonts.MaterialIcons;
import com.koushikdutta.ion.Ion;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codechimp.apprater.AppRater;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class AllInOneV2 extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final int SEND_PM_DIALOG = 102;
    public static final int MESSAGE_ACTION_DIALOG = 103;
    public static final int REPORT_MESSAGE_DIALOG = 104;
    public static final int POLL_OPTIONS_DIALOG = 105;

    public static final String EMPTY_STRING = "";

    public static String defaultSig;

    private Session session = null;

    public Session getSession() {
        return session;
    }

    private String boardID;
    private String topicID;
    private String messageIDForEditing;
    private String postPostUrl;

    private String savedPostBody;

    public String getSavedPostBody() {
        return savedPostBody;
    }

    private String savedPostTitle;

    public String getSavedPostTitle() {
        return savedPostTitle;
    }

    private String savedBoardID;

    public String getSavedBoardID() {
        return savedBoardID;
    }

    private String savedTopicID;

    public String getSavedTopicID() {
        return savedTopicID;
    }

    private String savedMessageID;

    public String getSavedMessageID() {
        return savedMessageID;
    }

    private static SharedPreferences settings = null;

    public static SharedPreferences getSettingsPref() {
        return settings;
    }

    private LinearLayout titleWrapper;
    private EditText postTitle;
    private EditText postBody;

    private TextView titleCounter;
    private TextView bodyCounter;

    private Button postCancelButton, pollButton, flairButton, postSubmitButton;

    private View pollSep, flairSep;

    private JSONObject pollJSON = new JSONObject();

    public JSONObject getPollJSON() {
        return pollJSON;
    }

    private int flairForNewTopic = 0;

    public int getFlairForNewTopicAsInt() {
        return flairForNewTopic;
    }

    public String getFlairForNewTopicAsString() {
        return String.valueOf(flairForNewTopic);
    }

    private LinkedHashMap<Integer, Boolean> boardFlairsCurrent;
    private LinkedHashMap<Integer, String> boardFlairsNames;

    private LinkedHashMap<String, String> updateTopicFlairs;
    private int currTopicFlair;

    private LinearLayout postWrapper;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView contentList;

    private String tlUrl;

    private enum PostMode {ON_BOARD, ON_TOPIC, NEW_PM}

    private PostMode pMode;

    private enum FavMode {ON_BOARD, ON_TOPIC}

    private FavMode fMode;
    private String favKey;

    private Button pageLabel;
    private Button firstPage, prevPage, nextPage, lastPage;
    private String firstPageUrl, prevPageUrl, nextPageUrl, lastPageUrl, jumperPageUrl;
    private AlertDialog.Builder jumperDialogBuilder;
    private NetDesc pageJumperDesc;

    private View pageJumperWrapper;

    @SuppressWarnings("ConstantConditions")
    public int[] getScrollerVertLoc() {
        try {
            int firstVis = contentList.getFirstVisiblePosition();
            return new int[]{firstVis, contentList.getChildAt(0).getTop()};
        } catch (NullPointerException npe) {
            return new int[]{0, 0};
        }
    }


    private static HighlightListDBHelper hlDB;

    public static HighlightListDBHelper getHLDB() {
        return hlDB;
    }

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Spinner accountsSpinner, notifsSpinner;
    private ArrayAdapter<String> accountsAdapter, notifsAdapter;
    private ArrayList<String> notifsLinks = new ArrayList<>();
    private MenuItem dwrNavHeadItem, dwrPMInboxItem, dwrAMPItem;

    private FloatingActionButton fab;

    @SuppressLint("StaticFieldLeak")
    private static AllInOneV2 me;

    public static AllInOneV2 get() {
        return me;
    }

    private Theming themingInstance;


    /**
     * *******************************************
     * START METHODS
     * ********************************************
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        me = this;
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // get an instance of Theming to ensure values don't get GC'd
        // Will they get GC'd? I have no idea. Better safe than sorry.
        themingInstance = new Theming();

        Ion.getDefault(this).configure().userAgent(GRUserAgent.get(this));

        // convert old startAtAMP setting to new loggedInStartLocation
        if (settings.contains("startAtAMP")) {
            String loc = settings.getBoolean("startAtAMP", false) ?
                    getString(R.string.amp_list) : getString(R.string.boards_explore);

            settings.edit().remove("startAtAMP").putString("loggedInStartLocation", loc).apply();
        }

        // Init notification job if it isn't running and should be
        if (settings.getBoolean("notifsEnable", false)) {
            int seconds = 60 * NumberUtils.toInt(settings.getString("notifsFrequency", "60"));
            NotifierJobService.dispatchJob(this, seconds, false);
        }

        Theming.preInit(settings);
        setTheme(Theming.theme());
        Theming.init(this, settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Theming.colorPrimaryDark());
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.allinonev2);

        Theming.colorOverscroll(this);

        AccountManager.init(this);

        setSupportActionBar(findViewById(R.id.aioToolbar));
        ActionBar aBar = getSupportActionBar();
        assert aBar != null : "Action bar is null";

        aBar.setDisplayHomeAsUpEnabled(true);
        aBar.setDisplayShowTitleEnabled(true);

        drawerLayout = findViewById(R.id.aioDrawerLayout);
        NavigationView navigationView = findViewById(R.id.aioNavigationDrawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);

        assert navigationView != null;
        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.dwrBoardsExplore).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_bulletin_board).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrBoardsFavorites).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_heart).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrAMPList).setIcon(new IconDrawable(
                this, MaterialIcons.md_edit).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrTrackedTopics).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_pin).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrPMInbox).setIcon(new IconDrawable(
                this, MaterialIcons.md_markunread_mailbox).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrCopyCurrURL).setIcon(new IconDrawable(
                this, MaterialIcons.md_content_cut).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrHighlightList).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_lightbulb).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrSettings).setIcon(new IconDrawable(
                this, MaterialIcons.md_settings).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrAbout).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_help_circle).color(Theming.colorPrimary()));
        navMenu.findItem(R.id.dwrExit).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_close_circle).color(Theming.colorPrimary()));
        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.dwrBoardsExplore:
                    session.get(NetDesc.BOARDS_EXPLORE, GF_URLS.BOARDS_EXPLORE);
                    break;
                case R.id.dwrBoardsFavorites:
                    showBoardFavoritesQuickList();
                    break;
                case R.id.dwrAMPList:
                    session.get(NetDesc.AMP_LIST, buildAMPLink());
                    break;
                case R.id.dwrTrackedTopics:
                    session.get(NetDesc.TRACKED_TOPICS, "/user/tracked");
                    break;
                case R.id.dwrPMInbox:
                    session.get(NetDesc.PM_INBOX, "/pm/");
                    break;
                case R.id.dwrCopyCurrURL:
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    assert clipboard != null;
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("simple text", session.getLastPath()));
                    Crouton.showText(AllInOneV2.this, "URL copied to clipboard.", Theming.croutonStyle());
                    break;
                case R.id.dwrHighlightList:
                    startActivity(new Intent(AllInOneV2.this, SettingsHighlightedUsers.class));
                    break;
                case R.id.dwrSettings:
                    startActivity(new Intent(AllInOneV2.this, HeaderSettings.class));
                    break;
                case R.id.dwrAbout:
                    startActivity(new Intent(AllInOneV2.this, About.class));
                    break;
                case R.id.dwrExit:
                    AllInOneV2.this.finish();
                    break;
            }
            drawerLayout.closeDrawers();

            return false;
        });


        View headerView = navigationView.getHeaderView(0);
        accountsSpinner = headerView.findViewById(R.id.dwrHdrAccounts);
        notifsSpinner = headerView.findViewById(R.id.dwrHdrNotifications);
        accountsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        notifsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notifsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountsAdapter.add("No Accounts");
        notifsAdapter.add("0 " + getString(R.string.notifications));
        accountsSpinner.setAdapter(accountsAdapter);
        notifsSpinner.setAdapter(notifsAdapter);

        Menu drawerMenu = navigationView.getMenu();
        dwrNavHeadItem = drawerMenu.findItem(R.id.dwrNavHeader);
        dwrPMInboxItem = drawerMenu.findItem(R.id.dwrPMInbox);
        dwrAMPItem = drawerMenu.findItem(R.id.dwrAMPList);

        aBar.setDisplayHomeAsUpEnabled(true);
        aBar.setHomeButtonEnabled(true);

        if (!settings.contains("defaultAccount")) {
            // settings need to be set to default
            PreferenceManager.setDefaultValues(this, R.xml.prefsaccountsnotifs, false);
            PreferenceManager.setDefaultValues(this, R.xml.prefsadvanced, false);
            PreferenceManager.setDefaultValues(this, R.xml.prefsgeneral, false);
            PreferenceManager.setDefaultValues(this, R.xml.prefstheming, false);
            Editor sEditor = settings.edit();
            sEditor.putString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT)
                    .putString("timezone", TimeZone.getDefault().getID())
                    .apply();
        }

        swipeRefreshLayout = findViewById(R.id.ptr_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setColorSchemeColors(Theming.colorPrimary(), Theming.colorPrimaryDark());
            swipeRefreshLayout.setOnRefreshListener(this);
        }

        contentList = findViewById(R.id.aioMainList);
        contentList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                settings.edit().putInt("contentListScrollState", scrollState).apply();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // stub
            }
        });

        titleWrapper = findViewById(R.id.aioPostTitleWrapper);
        postTitle = findViewById(R.id.aioPostTitle);
        postBody = findViewById(R.id.aioPostBody);
        titleCounter = findViewById(R.id.aioPostTitleCounter);
        bodyCounter = findViewById(R.id.aioPostBodyCounter);

        pageJumperWrapper = findViewById(R.id.aioHeader);
        firstPage = findViewById(R.id.aioFirstPage);
        firstPage.setOnClickListener(v -> session.get(pageJumperDesc, firstPageUrl));
        prevPage = findViewById(R.id.aioPreviousPage);
        prevPage.setOnClickListener(v -> session.get(pageJumperDesc, prevPageUrl));
        nextPage = findViewById(R.id.aioNextPage);
        nextPage.setOnClickListener(v -> session.get(pageJumperDesc, nextPageUrl));
        lastPage = findViewById(R.id.aioLastPage);
        lastPage.setOnClickListener(v -> session.get(pageJumperDesc, lastPageUrl));
        pageLabel = findViewById(R.id.aioPageLabel);
        pageLabel.setOnClickListener(v -> jumperDialogBuilder.show());

        Theming.setTextSizeBases(firstPage.getTextSize(), pageLabel.getTextSize());

        postTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int length = StringEscapeUtils.escapeHtml4(s.toString()).length();
                if (length > 70) {
                    String t = length + "/80";
                    titleCounter.setText(t);
                    titleCounter.setVisibility(View.VISIBLE);
                } else
                    titleCounter.setVisibility(View.GONE);
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

        postBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // GFAQs adds 13(!) characters onto bodies when they have a sig, apparently.
                int length = StringEscapeUtils.escapeHtml4(s.toString()).length() + getSig().length() + 13;
                if (length > 4000) {
                    String t = length + "\n/4096";
                    bodyCounter.setText(t);
                    bodyCounter.setVisibility(View.VISIBLE);
                } else
                    bodyCounter.setVisibility(View.GONE);
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

        postSubmitButton = findViewById(R.id.aioPostDo);
        postCancelButton = findViewById(R.id.aioPostCancel);
        pollButton = findViewById(R.id.aioPollOptions);
        flairButton = findViewById(R.id.aioFlairSet);

        pollSep = findViewById(R.id.aioPollSep);
        flairSep = findViewById(R.id.aioFlairSep);

        postWrapper = findViewById(R.id.aioPostWrapper);

        defaultSig = "Posted with GameRaven *grver*";

        hlDB = new HighlightListDBHelper(this);

        View foot = new View(this);
        foot.setMinimumHeight(Theming.convertDPtoPX(80));
        contentList.addFooterView(foot);

        adapterRows.add(new HeaderRowData("Loading..."));
        contentList.setAdapter(viewAdapter);

        fab = findViewById(R.id.fab);
        fab.setImageDrawable(new IconDrawable(this, MaterialIcons.md_add).colorRes(R.color.white).sizeDp(24));
        fab.setBackgroundTintList(new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{Theming.colorPrimary()}));
        fab.setRippleColor(Theming.colorAccent());
        fab.setOnClickListener(v -> {
            if (pMode == PostMode.ON_BOARD)
                postSetup(false);
            else if (pMode == PostMode.ON_TOPIC)
                postSetup(true);
            else if (pMode == PostMode.NEW_PM)
                pmSetup(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        });
        fab.hide();

        AppRater.app_launched(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null && intent.getData().getPath() != null) {
            String url = intent.getData().getPath();
            NetDesc desc = Session.determineNetDesc(url);
            if (desc != NetDesc.UNSPECIFIED)
                session.get(desc, url);
            else
                Crouton.showText(this, "Page not recognized: " + url, Theming.croutonStyle());
        }
    }

    private boolean firstResume = true;

    @Override
    protected void onResume() {
        super.onResume();

        swipeRefreshLayout.setEnabled(settings.getBoolean("enablePTR", false));
        contentList.setFastScrollEnabled(settings.getBoolean("enableFastScroll", true));

        int lastUpdateYear = settings.getInt("lastUpdateYear", 0);
        int lastUpdateYearDay = settings.getInt("lastUpdateYearDay", 0);
        @SuppressWarnings("deprecation") Time now = new Time();
        now.setToNow();
        if (lastUpdateYear != now.year || lastUpdateYearDay != now.yearDay) {
            try {
                Ion.with(this)
                        .load("GET", "http://ioabsoftware.com/gameraven/latest.txt")
                        .asString()
                        .setCallback((e, result) -> {
                            if (NumberUtils.isCreatable(result)) {
                                int netVersion = Integer.valueOf(result);

                                if (netVersion > BuildConfig.VERSION_CODE) {
                                    AlertDialog.Builder b = new AlertDialog.Builder(AllInOneV2.this);
                                    b.setTitle("New Version Found");
                                    b.setMessage("Open Google Play Market to download new version? Note that although " +
                                            "care is taken to make sure this notification only goes out once the update " +
                                            "has spread to all Google servers, there is still a chance the update may not " +
                                            "show up in the Play Store at first. Rest assured, there is a new version. " +
                                            "It just hasn't reached your local Google Play server yet.");
                                    b.setPositiveButton("Yes", (dialog, which) -> {
                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                        i.setData(Uri.parse("market://details?id=com.ioabsoftware.gameraven"));
                                        AllInOneV2.this.startActivity(i);
                                    });
                                    b.setNegativeButton("No", null);
                                    b.show();
                                }
                            }
                        });

                settings.edit().putInt("lastUpdateYear", now.year).putInt("lastUpdateYearDay", now.yearDay).apply();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        if (Theming.updateTextScale(settings.getInt("textScale", 100) / 100f)) {
            int px = TypedValue.COMPLEX_UNIT_PX;

            firstPage.setTextSize(px, Theming.getScaledPJButtonTextSize());
            prevPage.setTextSize(px, Theming.getScaledPJButtonTextSize());
            nextPage.setTextSize(px, Theming.getScaledPJButtonTextSize());
            lastPage.setTextSize(px, Theming.getScaledPJButtonTextSize());
            pageLabel.setTextSize(px, Theming.getScaledPJLabelTextSize());
        }

        Theming.swapTopicViewButtons(settings.getBoolean("swapTopicViewButtons", false));

        MessageRowView.setUsingAvatars(settings.getBoolean("usingAvatars", false));

        accountsAdapter.clear();
        accountsAdapter.addAll(AccountManager.getUsernames(this));
        accountsAdapter.add(getString(R.string.log_out));

        if (firstResume) {
            accountsSpinner.setOnItemSelectedListener(accountsListener);
            notifsSpinner.setOnItemSelectedListener(notifsListener);
        }

        if (session == null) {
            String initUrl = null;
            NetDesc initDesc = null;
            if (firstResume) {
                Uri uri = getIntent().getData();
                if (uri != null && uri.getScheme() != null && uri.getHost() != null) {
                    if (uri.getScheme().equals("http") && uri.getHost().contains("gamefaqs.com")) {
                        initUrl = uri.getPath();
                        initDesc = Session.determineNetDesc(initUrl);
                    }
                }
            }

            String defaultAccount = settings.getString("defaultAccount", HeaderSettings.NO_DEFAULT_ACCOUNT);
            String resumeAccount = settings.getString("resumeSessionUser", HeaderSettings.NO_DEFAULT_ACCOUNT);
            boolean resumeSession = settings.getBoolean("resumeSession", false);

            if (firstResume && resumeSession && AccountManager.containsUser(this, resumeAccount)) {
                session = new Session(this, resumeAccount, AccountManager.getPassword(this, resumeAccount), Session.RESUME_INIT_URL, NetDesc.UNSPECIFIED);
            } else if (AccountManager.containsUser(this, defaultAccount)) {
                session = new Session(this, defaultAccount, AccountManager.getPassword(this, defaultAccount), initUrl, initDesc);
            } else {
                session = new Session(this, null, null, initUrl, initDesc);
            }
        } else {
            setLoginName(Session.getUser());
            if (settings.getBoolean("reloadOnResume", false)) {
                isRoR = true;
                session.refresh();
            }
        }

        if (!settings.contains("beenWelcomed")) {
            settings.edit().putBoolean("beenWelcomed", true).apply();
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Welcome!");
            b.setMessage("Would you like to view the quick start help files? This dialog won't be shown again.");
            b.setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://ioabsoftware.com/gameraven/quickstart.html"))));
            b.setNegativeButton("No", null);
            b.show();
        }

        firstResume = false;
    }

    @Override
    protected void onStop() {
        Editor e = settings.edit();
        e.putBoolean("resumeSession", true);
        session.addHistoryBeforeStop();
        if (Session.isLoggedIn())
            e.putString("resumeSessionUser", Session.getUser());

        e.apply();

        session.closeHistoryDB();

        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        session.openHistoryDB();
        session.popHistory();
    }

    @Override
    protected void onDestroy() {
        Crouton.clearCroutonsForActivity(this);

        if (isFinishing())
            settings.edit().putBoolean("resumeSession", false).apply();

        super.onDestroy();
    }

    private AlertDialog loginDialog;

    public void showLoggingInDialog(String user) {
        if (loginDialog == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            ProgressBar spinner = new ProgressBar(this);
            spinner.setIndeterminate(true);
            b.setView(spinner);
            loginDialog = b.create();
        }

        loginDialog.setTitle("Logging in as " + user + "...");
        loginDialog.show();
    }

    public void dismissLoginDialog() {
        loginDialog.dismiss();
    }

    private boolean needToSetNavList = true;

    public void navDrawerReset() {
        setMenuItemVisibility(dwrNavHeadItem, false);
        needToSetNavList = true;
    }

    public void setNavDrawerVisibility(boolean isLoggedIn) {
        setMenuItemVisibility(dwrNavHeadItem, true);
        dwrNavHeadItem.getSubMenu().setGroupVisible(R.id.dwrLoggedInNav, isLoggedIn);

        needToSetNavList = false;
    }

    @Override
    public boolean onSearchRequested() {
        if (searchIcon != null && searchIcon.isVisible())
            searchIcon.expandActionView();

        return false;
    }

    public void toggleMenu() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawers();
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleMenu();
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onRefresh() {
        refreshClicked(null);
    }

    private MenuItem refreshIcon, replyIcon, pmInboxIcon, pmOutboxIcon, addFavIcon, remFavIcon,
            searchIcon, topicListIcon, sendUserPMIcon, tagUserIcon, unreadPMsIcon,
            unreadNotifsIcon, clearUnreadNotifsIcon, filterFlairsIcon;

    private final String GAME_SEARCH_URL = "/search_advanced?game=";

    /**
     * Adds menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        searchIcon = menu.findItem(R.id.search).setIcon(new IconDrawable(
                this, MaterialIcons.md_search).colorRes(R.color.white).actionBarSize());
        unreadNotifsIcon = menu.findItem(R.id.unreadNotifs).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_comment_alert).colorRes(R.color.white).actionBarSize());
        unreadPMsIcon = menu.findItem(R.id.unreadPMs).setIcon(new IconDrawable(
                this, MaterialIcons.md_markunread_mailbox).colorRes(R.color.white).actionBarSize());
        clearUnreadNotifsIcon = menu.findItem(R.id.clearUnreadNotifs).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_notification_clear_all).colorRes(R.color.white).actionBarSize());
        topicListIcon = menu.findItem(R.id.topicList).setIcon(new IconDrawable(
                this, MaterialIcons.md_view_list).colorRes(R.color.white).actionBarSize());
        filterFlairsIcon = menu.findItem(R.id.filterBoard).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_filter).colorRes(R.color.white).actionBarSize());
        addFavIcon = menu.findItem(R.id.addFav).setIcon(new IconDrawable(
                this, MaterialIcons.md_favorite_border).colorRes(R.color.white).actionBarSize());
        remFavIcon = menu.findItem(R.id.remFav).setIcon(new IconDrawable(
                this, MaterialIcons.md_favorite).colorRes(R.color.white).actionBarSize());
        pmInboxIcon = menu.findItem(R.id.pmInbox).setIcon(new IconDrawable(
                this, MaterialIcons.md_inbox).colorRes(R.color.white).actionBarSize());
        pmOutboxIcon = menu.findItem(R.id.pmOutbox).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_outbox).colorRes(R.color.white).actionBarSize());
        sendUserPMIcon = menu.findItem(R.id.sendUserPM).setIcon(new IconDrawable(
                this, MaterialIcons.md_mail).colorRes(R.color.white).actionBarSize());
        tagUserIcon = menu.findItem(R.id.tagUser).setIcon(new IconDrawable(
                this, MaterialCommunityIcons.mdi_tag_text_outline).colorRes(R.color.white).actionBarSize());
        replyIcon = menu.findItem(R.id.reply).setIcon(new IconDrawable(
                this, MaterialIcons.md_reply).colorRes(R.color.white).actionBarSize());
        refreshIcon = menu.findItem(R.id.refresh).setIcon(new IconDrawable(
                this, MaterialIcons.md_refresh).colorRes(R.color.white).actionBarSize());

        SearchView searchView = (SearchView) searchIcon.getActionView();
        if (searchView != null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            assert searchManager != null;
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

            SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    // just do default
                    return false;
                }

                public boolean onQueryTextSubmit(String query) {
                    try {
                        String encodedQuery = URLEncoder.encode(query, DocumentParser.CHARSET_NAME);
                        if (session.getLastDesc() == NetDesc.BOARD) {
                            session.get(NetDesc.BOARD, session.getLastPathWithoutData() + "?search=" + encodedQuery);
                        } else if (session.getLastDesc() == NetDesc.BOARDS_EXPLORE || session.getLastDesc() == NetDesc.GAME_SEARCH) {
                            session.get(NetDesc.GAME_SEARCH, GAME_SEARCH_URL + encodedQuery);
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new AssertionError(DocumentParser.CHARSET_NAME + " is unknown");
                        // shouldn't ever happen
                    }
                    return true;
                }
            };

            searchView.setOnQueryTextListener(queryTextListener);
        }

        return true;
    }

    UserDetailRowData userDetailData;

    /**
     * fires when a menu option is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;

            case R.id.addFav:
                AlertDialog.Builder addFavBuilder = new AlertDialog.Builder(this);
                addFavBuilder.setNegativeButton("No", null);

                final HashMap<String, List<String>> afData = new HashMap<>();
                afData.put("key", Collections.singletonList(favKey));

                final String afPath = session.getLastPath().replace("/boards/", "/boardaction/");

                switch (fMode) {
                    case ON_BOARD:
                        addFavBuilder.setTitle("Add Board to Favorites?");
                        addFavBuilder.setPositiveButton("Yes", (dialog, which) -> {
                            afData.put("action", Collections.singletonList("addfav"));
                            session.post(NetDesc.BOARD, afPath, afData);
                        });
                        break;
                    case ON_TOPIC:
                        addFavBuilder.setTitle("Track Topic?");
                        addFavBuilder.setPositiveButton("Yes", (dialog, which) -> {
                            afData.put("action", Collections.singletonList("tracktopic"));
                            session.post(NetDesc.TOPIC, afPath, afData);
                        });
                        break;
                }

                addFavBuilder.show();

                return true;

            case R.id.remFav:
                AlertDialog.Builder remFavBuilder = new AlertDialog.Builder(this);
                remFavBuilder.setNegativeButton("No", null);

                final HashMap<String, List<String>> rfData = new HashMap<>();
                rfData.put("key", Collections.singletonList(favKey));

                final String rfPath = session.getLastPath().replace("/boards/", "/boardaction/");

                switch (fMode) {
                    case ON_BOARD:
                        remFavBuilder.setTitle("Remove Board from Favorites?");
                        remFavBuilder.setPositiveButton("Yes", (dialog, which) -> {
                            rfData.put("action", Collections.singletonList("remfav"));
                            session.post(NetDesc.BOARD, rfPath, rfData);
                        });
                        break;
                    case ON_TOPIC:
                        remFavBuilder.setTitle("Stop Tracking Topic?");
                        remFavBuilder.setPositiveButton("Yes", (dialog, which) -> {
                            rfData.put("action", Collections.singletonList("stoptrack"));
                            session.post(NetDesc.TOPIC, rfPath, rfData);
                        });
                        break;
                }

                remFavBuilder.show();

                return true;

            case R.id.unreadNotifs:
                session.get(NetDesc.NOTIFS_PAGE, NOTIFS_PAGE_LINK);
                return true;

            case R.id.clearUnreadNotifs:
                session.get(NetDesc.NOTIFS_CLEAR, NOTIFS_CLEAR_LINK);
                return true;

            case R.id.topicList:
                session.get(NetDesc.BOARD, tlUrl);
                return true;

            case R.id.filterBoard:
                final int[] flairKeys = new int[boardFlairsNames.size() - 1];
                final String[] flairNames = new String[boardFlairsNames.size() - 1];
                final boolean[] flairCurrent = new boolean[boardFlairsNames.size() - 1];

                final String boardToFilter;
                if (boardID.contains("-")) {
                    boardToFilter = boardID.substring(0, boardID.indexOf('-'));
                } else {
                    boardToFilter = boardID;
                }

                int x = 0;
                for (Map.Entry<Integer, String> flair : boardFlairsNames.entrySet()) {
                    if (flair.getKey() != -1) {
                        flairKeys[x] = flair.getKey();
                        flairNames[x] = flair.getValue();
                        x++;
                    }
                }

                x = 0;
                for (Map.Entry<Integer, Boolean> flair : boardFlairsCurrent.entrySet()) {
                    if (flair.getKey() != -1) {
                        flairCurrent[x] = flair.getValue();
                        x++;
                    }
                }

                AlertDialog.Builder filterBuilder = new AlertDialog.Builder(this);
                filterBuilder.setMultiChoiceItems(flairNames, flairCurrent, (dialog, which, isChecked) -> flairCurrent[which] = isChecked);
                filterBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {
                    StringBuilder myrows = new StringBuilder().append('[');
                    boolean pastFirst = false;
                    boolean checkVal = true;
                    int count = 0;

                    for (boolean isCurrent : flairCurrent) {
                        if (isCurrent) count++;
                    }

                    if (count == 0) checkVal = false;

                    for (int x1 = 0; x1 < flairCurrent.length; x1++) {
                        if (flairCurrent[x1] == checkVal) {
                            if (pastFirst) myrows.append(',');
                            myrows.append('"').append(flairKeys[x1]).append('"');
                            pastFirst = true;
                        }
                    }
                    myrows.append(']');

                    HashMap<String, List<String>> data = new HashMap<>();
                    data.put("key", Collections.singletonList(session.getSessionKey()));
                    data.put("b", Collections.singletonList(boardToFilter));
                    data.put("list", Collections.singletonList(myrows.toString()));
                    session.post(NetDesc.BOARD_UPDATE_FILTER, GF_URLS.AJAX_BOARD_FILTER, data);
                });
                filterBuilder.setNeutralButton("Clear Filter", (dialog, which) -> {
                    StringBuilder myrows = new StringBuilder().append('[');
                    boolean pastFirst = false;
                    for (Integer key : flairKeys) {
                        if (pastFirst) myrows.append(',');
                        myrows.append('"').append(key).append('"');
                        pastFirst = true;
                    }
                    myrows.append(']');

                    HashMap<String, List<String>> data = new HashMap<>();
                    data.put("key", Collections.singletonList(session.getSessionKey()));
                    data.put("b", Collections.singletonList(boardToFilter));
                    data.put("list", Collections.singletonList(myrows.toString()));
                    session.post(NetDesc.BOARD_UPDATE_FILTER, GF_URLS.AJAX_BOARD_FILTER, data);
                });
                filterBuilder.setNegativeButton(R.string.cancel, null);
                filterBuilder.show();
                return true;

            case R.id.unreadPMs:
            case R.id.pmInbox:
                session.get(NetDesc.PM_INBOX, "/pm/");
                return true;

            case R.id.pmOutbox:
                session.get(NetDesc.PM_OUTBOX, "/pm/sent");
                return true;

            case R.id.sendUserPM:
                pmSetup(userDetailData.getName(), null, null);
                return true;

            case R.id.tagUser:
                AlertDialog.Builder tagUserBuilder = new AlertDialog.Builder(this);
                tagUserBuilder.setTitle("Set " + userDetailData.getName() + "'s Tag");
                tagUserBuilder.setMessage("User tags can be up to 30 characters long and cannot contain any banned words." +
                        "If a banned word is detected, the tag will be discarded.");

                final EditText tagText = new EditText(this);
                tagText.setHint(R.string.user_tag_hint);
                tagText.setText(userDetailData.getTagText());
                tagUserBuilder.setView(tagText);

                tagUserBuilder.setPositiveButton("OK", (dialog, which) -> {
                    HashMap<String, List<String>> data = new HashMap<>();
                    data.put("key", Collections.singletonList(userDetailData.getTagKey()));
                    assert tagText.getText() != null : "tagText.getText() is null";
                    data.put("t", Collections.singletonList(tagText.getText().toString()));
                    data.put("user", Collections.singletonList(userDetailData.getID()));

                    hideSoftKeyboard(tagText);
                    AllInOneV2.get().getSession().post(NetDesc.USER_TAG, GF_URLS.ROOT + "/ajax/user_tag", data);
                });
                tagUserBuilder.show();
                return true;

            case R.id.reply:
                pmSetup(replyTo, replySubject, EMPTY_STRING);
                return true;

            case R.id.refresh:
                if (session.getLastPath() == null) {
                    if (Session.isLoggedIn()) {
                        session = new Session(this, Session.getUser(), AccountManager.getPassword(this, Session.getUser()));
                    } else {
                        session = new Session(this);
                    }
                } else
                    session.refresh();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setMenuItemVisibility(MenuItem item, boolean visible) {
        if (item != null)
            item.setVisible(visible);
    }

    private void setMenuItemEnabled(MenuItem item, boolean enabled) {
        if (item != null)
            item.setEnabled(enabled);
    }

    private void setAllMenuItemsExceptRefreshVisibility(boolean visible) {
        setMenuItemVisibility(searchIcon, visible);
        setMenuItemVisibility(replyIcon, visible);
        setMenuItemVisibility(pmInboxIcon, visible);
        setMenuItemVisibility(pmOutboxIcon, visible);
        setMenuItemVisibility(sendUserPMIcon, visible);
        setMenuItemVisibility(tagUserIcon, visible);
        setMenuItemVisibility(addFavIcon, visible);
        setMenuItemVisibility(remFavIcon, visible);
        setMenuItemVisibility(topicListIcon, visible);
        setMenuItemVisibility(unreadNotifsIcon, visible);
        setMenuItemVisibility(unreadPMsIcon, visible);
        setMenuItemVisibility(clearUnreadNotifsIcon, visible);
        setMenuItemVisibility(filterFlairsIcon, visible);

        if (visible)
            fab.show();
        else
            fab.hide();
    }

    private void setAllMenuItemsEnabled(boolean enabled) {
        setMenuItemEnabled(refreshIcon, enabled);
        setMenuItemEnabled(searchIcon, enabled);
        setMenuItemEnabled(replyIcon, enabled);
        setMenuItemEnabled(pmInboxIcon, enabled);
        setMenuItemEnabled(pmOutboxIcon, enabled);
        setMenuItemEnabled(sendUserPMIcon, enabled);
        setMenuItemEnabled(tagUserIcon, enabled);
        setMenuItemEnabled(addFavIcon, enabled);
        setMenuItemEnabled(remFavIcon, enabled);
        setMenuItemEnabled(topicListIcon, enabled);
        setMenuItemEnabled(unreadNotifsIcon, enabled);
        setMenuItemEnabled(unreadPMsIcon, enabled);
        setMenuItemEnabled(clearUnreadNotifsIcon, enabled);
        setMenuItemEnabled(filterFlairsIcon, enabled);

        fab.setEnabled(enabled);
    }

    public void setLoginName(String name) {
        if (name == null)
            name = getString(R.string.log_out);
        accountsSpinner.setSelection(accountsAdapter.getPosition(name), false);
    }

    private void hideSoftKeyboard(View inputView) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(inputView.getWindowToken(), 0);
    }

    public void postError(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setTitle("There was an error. GameFAQs says...");
        builder.setPositiveButton("Ok", null);
        builder.show();

        ptrCleanup();
    }

    public void genError(String errorTitle, String errorMsg, String buttonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMsg);
        builder.setTitle(errorTitle);
        builder.setPositiveButton(buttonText, null);
        builder.show();

        ptrCleanup();
    }

    public void noNetworkConnection() {
        genError("No Network Connection", "Couldn't establish network connection. Check your network settings, then try again.",
                "Dismiss");
    }

    public void timeoutCleanup(NetDesc desc) {
        String msg;
        String title;
        String posButtonText;
        boolean retrySub = false;
        //noinspection EnumSwitchStatementWhichMissesCases
        switch (desc) {
            case LOGIN_S1:
            case LOGIN_S2:
                title = "Login Timeout";
                msg = "Login timed out, press retry to try again.";
                posButtonText = "Retry";
                break;
            case MSG_POST:
            case TOPIC_POST:
                postTimeoutCleanup();
                return;
            default:
                retrySub = true;
                title = "Timeout";
                msg = "Connection timed out, press retry to try again.";
                posButtonText = "Retry";
                break;

        }
        final boolean retry = retrySub;

        AlertDialog.Builder b = new AlertDialog.Builder(AllInOneV2.this);
        b.setTitle(title);
        b.setMessage(msg);
        b.setPositiveButton(posButtonText, (dialog, which) -> {
            if (retry)
                session.get(session.getLastAttemptedDesc(), session.getLastAttemptedPath());
            else
                refreshClicked(new View(AllInOneV2.this));
        });
        b.setNegativeButton("Dismiss", (dialog, which) -> postExecuteCleanup(session.getLastDesc()));
        b.show();
    }

    private void postTimeoutCleanup() {
        AlertDialog.Builder b = new AlertDialog.Builder(AllInOneV2.this);
        b.setTitle("Post Timeout");
        b.setMessage("Post timed out. Refresh the page to check if your post made it through. Dismissing " +
                "and posting again without first checking if the post went through may result in the post " +
                "being submitted twice.");

        b.setPositiveButton("Refresh", (dialog, which) -> session.get(Session.determineNetDesc(postPostUrl), postPostUrl));

        b.setNeutralButton("Copy Post", null);

        b.setNegativeButton("Dismiss", (dialog, which) -> {
            session.setLastPathAndDesc(postPostUrl, Session.determineNetDesc(postPostUrl));
            ptrCleanup();
        });

        b.setCancelable(false);
        final AlertDialog d = b.create();
        d.setOnShowListener(dialog -> d.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            assert clipboard != null;
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("simple text", savedPostBody));

            Crouton.showText(AllInOneV2.this,
                    "Message body copied to clipboard.",
                    Theming.croutonStyle(),
                    (ViewGroup) v.getParent().getParent().getParent());
        }));
        d.show();
    }

    private boolean isRoR = false;

    private void postInterfaceCleanup() {
        if (!isRoR && postWrapper.getVisibility() == View.VISIBLE) {
            postWrapper.setVisibility(View.GONE);
            pollButton.setVisibility(View.GONE);
            pollSep.setVisibility(View.GONE);
            flairButton.setVisibility(View.GONE);
            flairSep.setVisibility(View.GONE);
            postBody.setText(null);
            postTitle.setText(null);
            clearPoll();
            flairForNewTopic = 0;
            messageIDForEditing = null;

            fab.show();

            hideSoftKeyboard(postBody);

            if (!pageLabel.getText().toString().equals("~ 1 / 1 ~"))
                pageJumperWrapper.setVisibility(View.VISIBLE);
        }
    }

    private void ptrCleanup() {
        swipeRefreshLayout.setRefreshing(false);
        setAllMenuItemsEnabled(true);
        if (postWrapper.getVisibility() == View.VISIBLE) {
            postSubmitButton.setEnabled(true);
            postCancelButton.setEnabled(true);
            pollButton.setEnabled(true);
            flairButton.setEnabled(true);
        }
    }

    public void setAMPLinkVisible(boolean visible) {
        dwrAMPItem.setVisible(visible);
    }

    public void preExecuteSetup(NetDesc desc) {
        if (desc != NetDesc.MSG_POST && desc != NetDesc.MSG_EDIT && desc != NetDesc.TOPIC_POST)
            postInterfaceCleanup();

        swipeRefreshLayout.setRefreshing(true);
        setAllMenuItemsEnabled(false);
    }

    /*
     * *****************************************
     * START HNR
     * *****************************************
     */

    ArrayList<BaseRowData> adapterRows = new ArrayList<>();
    ViewAdapter viewAdapter = new ViewAdapter(this, adapterRows);

    public void processContent(NetDesc desc, Document doc, String resUrl) {
        swipeRefreshLayout.setEnabled(false);

        if (searchIcon != null)
            searchIcon.collapseActionView();

        setAllMenuItemsExceptRefreshVisibility(false);

        adapterRows.clear();
        viewAdapter.notifyDataSetChanged();

        boardID = null;
        topicID = null;
        messageIDForEditing = null;

        Element tbody;
        Element pj;
        String headerTitle;
        String firstPage = null;
        String prevPage = null;
        int[] pagesInfo = new int[]{1, 1};
        String nextPage = null;
        String lastPage = null;
        String pagePrefix = null;

        Element masthead = doc.select("div.masthead").first();

        Element boardsDropdown = masthead.getElementById("bdrop");
        if (boardsDropdown != null) {
            Elements dItems = boardsDropdown.getElementsByTag("a");
            if (dItems.last().parent().hasClass("list_foot")) {
                dItems.remove(dItems.size() - 1);
            }
            boardFavoritesQuickListOptions = new String[dItems.size() + 1];
            boardFavoritesQuickListLinks = new String[dItems.size() + 1];
            boardFavoritesQuickListOptions[0] = "Go to Boards Page...";
            int x = 1;
            for (Element e : dItems) {
                boardFavoritesQuickListOptions[x] = e.text();
                boardFavoritesQuickListLinks[x] = e.attr("href");
                x++;
            }
        }

        contentList.setDividerHeight(Theming.convertDPtoPX(1));

        switch (desc) {
            case BOARDS_EXPLORE:
            case LOGIN_S2:
                updateHeaderNoJumper("Explore Boards", NetDesc.BOARDS_EXPLORE);
                setMenuItemVisibility(searchIcon, true);
                processBoardWraps(doc.select("div.board_wrap"), "popular", true);
                processBoardWraps(doc.select("div.board_wrap"), "all", true);

                Elements menuLinks = doc.select("a.menu_link");
                if (!menuLinks.isEmpty()) {
                    String menuLinkHeader = menuLinks.first().parent().parent().parent().parent().
                            previousElementSibling().text();
                    adapterRows.add(new HeaderRowData(menuLinkHeader));
                    for (Element linkElem : menuLinks) {
                        String menuUrl = linkElem.attr("href");
                        String menuTitle = linkElem.selectFirst("div.menu_title").text();
                        String menuDesc = linkElem.selectFirst("p.menu_desc").text();

                        adapterRows.add(new BoardRowData(
                                menuTitle, menuDesc, null, null, null, menuUrl, BoardType.EXPLORER_LINK));
                    }
                }
                break;

            case BOARDS_FAVORITE:
                updateHeaderNoJumper("Your Favorite Boards", NetDesc.BOARDS_FAVORITE);
                processBoardWraps(doc.select("div.board_wrap"), "your favorite", false);
                break;

//            case GFAQS_SETTINGS:
//                Elements gsForms = doc.getElementsByTag("form");
//                Element settingsForm = null;
//                for (Element e : gsForms) {
//                    if (!e.hasClass("search")) {
//                        settingsForm = e;
//                    }
//                }
//
//                // settingsForm should never be null
//                Elements gsRows = settingsForm.select("div.row");
//                GFAQsSetting[] gfaqsSettings = new GFAQsSetting[gsRows.size()];
//                int gsX = 0;
//                for (Element gsRow : gsRows) {
//                    Element title = gsRow.child(0);
//                    Element spinner = gsRow.getElementsByTag("select").first();
//
//                    // option name, title, hint
//                    String[] optionAttrs = {spinner.attr("name"), title.text(),
//                            title.select("i.fa-question-circle").first().attr("title")};
//
//                    // option value, label
//                    SparseArray<String> optionPairs = new SparseArray<>();
//                    Elements options = spinner.getElementsByTag("option");
//                    int currentSelected = 0;
//                    for (Element option : options) {
//                        optionPairs.append(Integer.valueOf(option.attr("value")), option.attr("label"));
//                        if (option.hasAttr("selected"))
//                            currentSelected = Integer.valueOf(option.attr("value"));
//                    }
//                    gfaqsSettings[gsX] = new GFAQsSetting(optionAttrs, optionPairs, currentSelected);
//                }
//                break;

            case NOTIFS_PAGE:
                settings.edit().putLong("notifsLastCheck", System.currentTimeMillis()).apply();
                tbody = doc.getElementsByTag("tbody").first();

                headerTitle = Session.getUser() + "'s Notifications";
                updateHeaderNoJumper(headerTitle, desc);

                if (tbody != null) {
                    for (Element row : tbody.getElementsByTag("tr")) {
                        Elements cells = row.children();
                        // [title, url] [time] [read]
                        Element titleLinkElem = cells.get(0).children().first();
                        String title = titleLinkElem.text();
                        String link = titleLinkElem.attr("href");
                        String time = cells.get(1).text();
                        boolean isOld = false;
                        if (cells.get(2).text().equals("Read"))
                            isOld = true;

                        adapterRows.add(new NotifRowData(title, time, link, isOld));
                    }
                } else {
                    adapterRows.add(new HeaderRowData("You have no notifications at this time."));
                }
                setMenuItemVisibility(clearUnreadNotifsIcon, true);
                break;

            case MENTIONS_PAGE:
                tbody = doc.getElementsByTag("tbody").first();

                headerTitle = Session.getUser() + "'s Mentions";
                updateHeaderNoJumper(headerTitle, desc);

                if (tbody != null) {
                    for (Element row : tbody.getElementsByTag("tr")) {
                        Elements cells = row.children();
                        // [topic] [board] [user] [time]
                        Element topicLinkElem = cells.get(0).children().first();
                        String topic = topicLinkElem.text();
                        String link = topicLinkElem.attr("href");
                        String board = cells.get(1).text();
                        String user = cells.get(2).text();
                        String time = cells.get(3).text();

                        adapterRows.add(new MentionRowData(topic, board, user, time, link));
                    }
                } else {
                    adapterRows.add(new HeaderRowData("You have no mentions at this time."));
                }
                break;

            case PM_INBOX:
            case PM_OUTBOX:
                tbody = doc.getElementsByTag("tbody").first();

                boolean isInbox = false;
                if (desc == NetDesc.PM_INBOX)
                    isInbox = true;

                if (isInbox)
                    headerTitle = Session.getUser() + "'s PM Inbox";
                else
                    headerTitle = Session.getUser() + "'s PM Outbox";

                if (tbody != null) {
                    pj = doc.select("ul.paginate").first();

                    if (pj != null) {
                        pagesInfo = getPageJumperInfo(pj);

                        if (isInbox)
                            pagePrefix = "/pm/?page=";
                        else
                            pagePrefix = "/pm/sent?page=";

                        if (pagesInfo[0] > 1) {
                            firstPage = pagePrefix + 0;
                            prevPage = pagePrefix + (pagesInfo[0] - 2);
                        }
                        if (pagesInfo[0] != pagesInfo[1]) {
                            nextPage = pagePrefix + pagesInfo[0];
                            lastPage = pagePrefix + (pagesInfo[1] - 1);
                        }
                    }

                    updateHeader(headerTitle, firstPage, prevPage, pagesInfo[0],
                            pagesInfo[1], nextPage, lastPage, pagePrefix, desc);

                    for (Element row : tbody.getElementsByTag("tr")) {
                        Elements cells = row.children();
                        // [icon] [sender] [subject] [time] [check]
                        boolean isOld = true;
                        if (cells.get(0).children().first().hasClass("fa-circle"))
                            isOld = false;
                        String sender = cells.get(1).text();
                        Element subjectLinkElem = cells.get(2).children().first();
                        String subject = subjectLinkElem.text();
                        String link = subjectLinkElem.attr("href");
                        String time = cells.get(3).text();

                        adapterRows.add(new PMRowData(subject, sender, time, link, isOld, isInbox));
                    }
                } else {
                    updateHeaderNoJumper(headerTitle, desc);
                    adapterRows.add(new HeaderRowData("There are no private messages here at this time."));
                }

                fab.show();
                pMode = PostMode.NEW_PM;

                if (isInbox)
                    setMenuItemVisibility(pmOutboxIcon, true);
                else
                    setMenuItemVisibility(pmInboxIcon, true);

                break;

            case PM_INBOX_DETAIL:
            case PM_OUTBOX_DETAIL:
                String pmTitle = doc.select("h2.title").first().text();

                String pmMessage = doc.select("div.body").first().outerHtml();

                Element foot = doc.select("div.foot").first();
                foot.child(1).remove();
                String pmFoot = foot.outerHtml();

                //Sent by: P4wn4g3 on 6/1/2013 2:15:55 PM
                String footText = foot.text();

                String sender = footText.substring(9, footText.indexOf(" on "));

                updateHeaderNoJumper(pmTitle, desc);

                if (desc == NetDesc.PM_INBOX_DETAIL) {
                    replyTo = sender;
                    if (!pmTitle.startsWith("Re: "))
                        replySubject = "Re: " + pmTitle;
                    else
                        replySubject = pmTitle;

                    setMenuItemVisibility(replyIcon, true);
                }

                adapterRows.add(new PMDetailRowData(sender, pmTitle, pmMessage + pmFoot));
                break;

            case AMP_LIST:
                tbody = doc.getElementsByTag("tbody").first();

                headerTitle = Session.getUser() + "'s Active Messages";

                if (doc.select("ul.paginate").size() > 1) {
                    pj = doc.select("ul.paginate").get(1);
                    if (pj != null && !pj.hasClass("user")
                            && !pj.hasClass("tsort")) {

                        pagesInfo = getPageJumperInfo(pj);

                        pagePrefix = buildAMPLink() + "&page=";
                        if (pagesInfo[0] > 1) {
                            firstPage = pagePrefix + 0;
                            prevPage = pagePrefix + (pagesInfo[0] - 2);
                        }
                        if (pagesInfo[0] != pagesInfo[1]) {
                            nextPage = pagePrefix + pagesInfo[0];
                            lastPage = pagePrefix + (pagesInfo[1] - 1);
                        }
                    }
                }
                updateHeader(headerTitle, firstPage, prevPage, pagesInfo[0],
                        pagesInfo[1], nextPage, lastPage, pagePrefix, NetDesc.AMP_LIST);

                if (!tbody.children().isEmpty()) {
                    for (Element row : tbody.children()) {
                        // [board] [read status] [title] [msg] [last post] [your last post]
                        Elements cells = row.children();
                        String board = cells.get(0).text();
                        Element titleLinkElem = cells.get(2).child(0);
                        String title = titleLinkElem.text();
                        String link = titleLinkElem.attr("href");
                        String mCount = cells.get(3).textNodes().get(0).text().trim();
                        Element lPostLinkElem = cells.get(4).child(1);
                        String lPost = lPostLinkElem.text();
                        String lPostLink = lPostLinkElem.attr("href");

                        ReadStatus status = ReadStatus.UNREAD;
                        String tImg = cells.get(1).child(0).className();
                        if (tImg.endsWith("_read"))
                            status = ReadStatus.READ;
                        else if (tImg.endsWith("_unread")) {
                            status = ReadStatus.NEW_POST;
                            lPostLink = cells.get(1).child(0).attr("href");
                        }

                        String flair = null;
                        Element flairSpan = row.selectFirst("span.flair");
                        if (flairSpan != null) {
                            flair = flairSpan.text();
                        }

                        adapterRows.add(new TopicRowData(title, flair, board, lPost, mCount, link,
                                lPostLink, null, TopicType.NORMAL, status, 0,
                                TopicRowData.TopicFlavor.AMP));
                    }
                } else {
                    adapterRows.add(new HeaderRowData("You have no active messages at this time."));
                }
                break;

            case TRACKED_TOPICS:
                headerTitle = Session.getUser() + "'s Tracked Topics";
                updateHeaderNoJumper(headerTitle, desc);

                tbody = doc.getElementsByTag("tbody").first();

                if (tbody != null) {
                    for (Element row : tbody.children()) {
                        // [remove] [title] [board name] [msgs] [last [pst]
                        Elements cells = row.children();

                        int rsMod = 0;
                        if (cells.size() == 6)
                            rsMod = 1;

                        String removeLink = cells.get(0).child(0)
                                .attr("href");
                        String topicLink = cells.get(1 + rsMod).child(0)
                                .attr("href");
                        String topicText = cells.get(1 + rsMod).text();
                        String board = cells.get(2 + rsMod).text();
                        String msgs = cells.get(3 + rsMod).text();
                        String lPostLink = cells.get(4 + rsMod).child(0)
                                .attr("href");
                        String lPostText = cells.get(4 + rsMod).text();

                        ReadStatus status = ReadStatus.UNREAD;
                        if (rsMod == 1) {
                            String tImg = cells.get(1).child(0).className();
                            if (tImg.endsWith("_read"))
                                status = ReadStatus.READ;
                            else if (tImg.endsWith("_unread"))
                                status = ReadStatus.NEW_POST;
                        }

                        String flair = null;
                        Element flairSpan = row.selectFirst("span.flair");
                        if (flairSpan != null) {
                            flair = flairSpan.text();
                        }

                        adapterRows.add(new TopicRowData(topicText, flair, board, lPostText, msgs,
                                topicLink, lPostLink, removeLink, TopicType.NORMAL, status, 0,
                                TopicRowData.TopicFlavor.TRACKED));
                    }
                } else {
                    adapterRows.add(new HeaderRowData("You have no tracked topics at this time."));
                }
                break;

            case BOARD:
                boardID = parseBoardID(resUrl);

                boolean isSplitList = false;
                if (doc.getElementsByTag("th").first() != null) {
                    if (doc.getElementsByTag("th").first().text().equals("Board Title")) {

                        updateHeaderNoJumper(doc.select("h1.page-title").first().text(), NetDesc.BOARD);

                        processBoardTbodyRows(doc.selectFirst("tbody").select("tr"));

                        isSplitList = true;
                    }
                }

                if (!isSplitList) {
                    String searchQuery = EMPTY_STRING;
                    String searchPJAddition = EMPTY_STRING;
                    if (resUrl.contains("search=")) {
                        searchQuery = resUrl.substring(resUrl.indexOf("search=") + 7);
                        int i = searchQuery.indexOf('&');
                        if (i != -1)
                            searchQuery = searchQuery.replace(searchQuery.substring(i), EMPTY_STRING);

                        searchPJAddition = "&search=" + searchQuery;
                        try {
                            searchQuery = URLDecoder.decode(searchQuery, DocumentParser.CHARSET_NAME);
                        } catch (UnsupportedEncodingException e) {
                            throw new AssertionError(DocumentParser.CHARSET_NAME + " is unknown");
                            // should never happen
                        }
                    }

                    boardFlairsCurrent = new LinkedHashMap<>();
                    boardFlairsNames = new LinkedHashMap<>();
                    Elements flairsElem = doc.select("div.flair_option");
                    if (flairsElem != null && !flairsElem.isEmpty()) {
                        boolean pastFirstCurrent = false;
                        StringBuilder filterHeader = new StringBuilder("Applied Filters: ");

                        for (Element flair : flairsElem) {
                            boolean isCurrent = flair.hasClass("current");
                            String flairText = flair.text();
                            int flairKey;
                            if (!flair.hasClass("custom") && !flair.hasClass("custom_save")) {
                                if (flair.hasClass("all")) {
                                    flairKey = -1;
                                } else {
                                    Element i = flair.selectFirst("i.filter_option");
                                    flairKey = Integer.valueOf(i.attr("data-id"));
                                }

                                if (isCurrent && flairKey != -1) {
                                    if (pastFirstCurrent) filterHeader.append(", ");
                                    filterHeader.append(flairText);
                                    pastFirstCurrent = true;
                                }

                                boardFlairsCurrent.put(flairKey, isCurrent);
                                boardFlairsNames.put(flairKey, flairText);
                            }
                        }

                        if (pastFirstCurrent) {
                            adapterRows.add(new HeaderRowData(filterHeader.toString()));
                        }

                        filterFlairsIcon.setVisible(true);
                    }

                    Element headerElem = doc.getElementsByClass("page-title").first();
                    if (headerElem != null)
                        headerTitle = headerElem.text();
                    else
                        headerTitle = "GFAQs Cache Error, Board Title Not Found";

                    if (searchQuery.length() > 0)
                        headerTitle += " (search: " + searchQuery + ")";

                    if (doc.select("ul.paginate").size() > 1) {
                        pj = doc.select("ul.paginate").get(1);
                        if (pj != null && !pj.hasClass("user")) {
                            pagesInfo = getPageJumperInfo(pj);

                            pagePrefix = "boards/" + boardID + "?page=";
                            if (pagesInfo[0] > 1) {
                                firstPage = pagePrefix + 0 + searchPJAddition;
                                prevPage = pagePrefix + (pagesInfo[0] - 2) + searchPJAddition;
                            }
                            if (pagesInfo[0] != pagesInfo[1]) {
                                nextPage = pagePrefix + pagesInfo[0] + searchPJAddition;
                                lastPage = pagePrefix + (pagesInfo[1] - 1) + searchPJAddition;

                                if (pagesInfo[0] > pagesInfo[1]) {
                                    session.forceNoHistoryAddition();
                                    session.forceSkipAIOCleanup();
                                    Crouton.showText(this, "Page count higher than page amount, going to last page...", Theming.croutonStyle());
                                    session.get(NetDesc.BOARD, lastPage);
                                    return;
                                }
                            }
                        }
                    }
                    updateHeader(headerTitle, firstPage, prevPage, pagesInfo[0], pagesInfo[1], nextPage,
                            lastPage, pagePrefix + searchPJAddition, NetDesc.BOARD);

                    setMenuItemVisibility(searchIcon, true);

                    if (Session.isLoggedIn()) {
                        Element favbtn = doc.getElementsByClass("user").first().getElementsByAttributeValueStarting("onclick", "post_click").first();
                        if (favbtn != null) {
                            String favtext = favbtn.text().toLowerCase();
                            String onclick = favbtn.attr("onclick");
                            int endPoint = onclick.lastIndexOf('\'');
                            int startPoint = onclick.lastIndexOf('\'', endPoint - 1) + 1;
                            favKey = onclick.substring(startPoint, endPoint);
                            fMode = FavMode.ON_BOARD;
                            if (favtext.contains("add to favorites"))
                                setMenuItemVisibility(addFavIcon, true);
                            else if (favtext.contains("remove favorite"))
                                setMenuItemVisibility(remFavIcon, true);
                        }

                        updatePostingRights(doc, false);
                    }

                    Element splitList = doc.select("p:contains(this is a split board)").first();
                    if (splitList != null) {
                        String splitListLink = splitList.child(0).attr("href");
                        adapterRows.add(new BoardRowData("This is a Split Board.", "Click here to return to the Split List.",
                                null, null, null, splitListLink, BoardType.SPLIT));
                    }

                    Element table = doc.select("table.tlist").first();
                    if (table != null && !table.select("td").first().hasAttr("colspan")) {

                        table.getElementsByTag("col").get(2).remove();
                        table.getElementsByTag("th").get(2).remove();
                        table.getElementsByTag("col").get(0).remove();
                        table.getElementsByTag("th").get(0).remove();

                        boolean skipFirst = true;
                        Set<String> hlUsers = hlDB.getHighlightedUsers().keySet();
                        table.select("div.ad").remove();
                        for (Element row : table.getElementsByTag("tr")) {
                            if (!skipFirst) {
                                Elements cells = row.getElementsByTag("td");
                                // cells = [image] [title] [author] [post count] [last post]
                                String tImg = cells.get(0).child(0).className();
                                Element titleLinkElem = cells.get(1).child(0);
                                String title = titleLinkElem.text();
                                String tUrl = titleLinkElem.attr("href");
                                String tc = cells.get(2).text();
                                Element lPostLinkElem = cells.get(4).child(0);
                                String lastPost = lPostLinkElem.text();
                                String lpUrl = lPostLinkElem.attr("href");
                                String mCount = cells.get(3).text();

                                TopicType type = TopicType.NORMAL;
                                if (tImg.contains("poll"))
                                    type = TopicType.POLL;
                                else if (tImg.contains("sticky"))
                                    type = TopicType.PINNED;
                                else if (tImg.contains("closed"))
                                    type = TopicType.LOCKED;
                                else if (tImg.contains("archived"))
                                    type = TopicType.ARCHIVED;

                                ReadStatus status = ReadStatus.UNREAD;
                                if (tImg.endsWith("_read"))
                                    status = ReadStatus.READ;
                                else if (tImg.endsWith("_unread")) {
                                    status = ReadStatus.NEW_POST;
                                    lpUrl = cells.get(0).child(0).attr("href");
                                }

                                int hlColor = 0;
                                if (hlUsers.contains(tc.toLowerCase(Locale.US))) {
                                    HighlightedUser hUser = hlDB.getHighlightedUsers().get(tc.toLowerCase(Locale.US));
                                    hlColor = hUser.getColor();
                                    tc += " (" + hUser.getLabel() + ")";
                                }

                                String rowFlair = null;
                                Element flairSpan = row.selectFirst("span.flair");
                                if (flairSpan != null) {
                                    rowFlair = flairSpan.text();
                                }

                                adapterRows.add(new TopicRowData(title, rowFlair, tc, lastPost,
                                        mCount, tUrl, lpUrl, null, type, status, hlColor,
                                        TopicRowData.TopicFlavor.BOARD));
                            } else
                                skipFirst = false;
                        }
                    } else {
                        adapterRows.add(new HeaderRowData("There are no topics at this time."));
                    }
                }
                break;

            case TOPIC:
                contentList.setDividerHeight(0);
                boardID = parseBoardID(resUrl);
                topicID = parseTopicID(resUrl);

                tlUrl = "boards/" + boardID;
                setMenuItemVisibility(topicListIcon, true);

                Element headerElem = doc.selectFirst("h1.page-title");
                if (headerElem != null)
                    headerTitle = headerElem.ownText();
                else
                    headerTitle = "GFAQs Cache Error, Title Not Found";

                if (headerTitle.equals("Log In to GameFAQs")) {
                    headerElem = doc.getElementsByClass("title").get(1);
                    if (headerElem != null)
                        headerTitle = headerElem.text();
                }

                if (doc.select("ul.paginate").size() > 1) {
                    pj = doc.select("ul.paginate").get(1);
                    if (pj != null && !pj.hasClass("user")) {
                        pagesInfo = getPageJumperInfo(pj);

                        pagePrefix = "boards/" + boardID + "/" + topicID + "?page=";
                        if (pagesInfo[0] > 1) {
                            firstPage = pagePrefix + 0;
                            prevPage = pagePrefix + (pagesInfo[0] - 2);
                        }
                        if (pagesInfo[0] != pagesInfo[1]) {
                            nextPage = pagePrefix + pagesInfo[0];
                            lastPage = pagePrefix + (pagesInfo[1] - 1);

                            if (pagesInfo[0] > pagesInfo[1]) {
                                session.forceNoHistoryAddition();
                                session.forceSkipAIOCleanup();
                                Crouton.showText(this, "Page count higher than page amount, going to last page...", Theming.croutonStyle());
                                session.get(NetDesc.TOPIC, lastPage);
                                return;
                            }
                        }
                    }
                }
                updateHeader(headerTitle, firstPage, prevPage, pagesInfo[0],
                        pagesInfo[1], nextPage, lastPage, pagePrefix, NetDesc.TOPIC);

                if (Session.isLoggedIn()) {
                    Element favbtn = doc.getElementsByClass("user").first().getElementsByAttributeValueStarting("onclick", "post_click").first();
                    if (favbtn != null) {
                        String favtext = favbtn.text().toLowerCase();
                        String onclick = favbtn.attr("onclick");
                        int endPoint = onclick.lastIndexOf('\'');
                        int startPoint = onclick.lastIndexOf('\'', endPoint - 1) + 1;
                        favKey = onclick.substring(startPoint, endPoint);
                        fMode = FavMode.ON_TOPIC;
                        if (favtext.contains("track topic"))
                            setMenuItemVisibility(addFavIcon, true);
                        else if (favtext.contains("stop tracking"))
                            setMenuItemVisibility(remFavIcon, true);
                    }

                    updatePostingRights(doc, true);
                }

                String goToThisPost = null;
                if (goToUrlDefinedPost) {
                    if (resUrl.indexOf('#') != -1) {
                        goToThisPost = resUrl.substring(resUrl.indexOf('#'));
                    } else {
                        // goToUrlDefinedPost is true when there is no url defined post, oops
                        goToUrlDefinedPost = false;
                    }
                }

                Elements rows = doc.select("table.board").first().getElementsByTag("tr");
                int rowCount = rows.size();

                int msgIndex = 0;

                Set<String> hlUsers = hlDB.getHighlightedUsers().keySet();
                for (int x = 0; x < rowCount; x++) {
                    Element row = rows.get(x);

                    if (row.select("div.msg_deleted").isEmpty()) {

                        String user;
                        String postNum;
                        String postTime;
                        String mID = null;
                        String uID;
                        String userTitles = EMPTY_STRING;
                        Element msgBody;

                        boolean canReport = false, canDelete = false,
                                canEdit = false, canQuote = false, canUpdateFlair = false;

                        Element infoBox = row.select("div.msg_infobox").first();
                        user = infoBox.getElementsByTag("b").first().text();

                        Element userInfo = infoBox.select("span.user_info").first();
                        if (userInfo != null)
                            userTitles = " " + userInfo.text();

                        Element userTag = infoBox.select("span.tag").first();
                        if (userTag != null)
                            userTitles += " (" + userTag.text() + ")";

                        postTime = infoBox.select("span.post_time").first().text();

                        Element userSubmenu = infoBox.selectFirst("div.user_submenu");
                        uID = userSubmenu.attr("data-userid");

                        Element number = infoBox.select("span.message_num").first();
                        postNum = number.text();
                        if (!number.children().isEmpty()) {
                            mID = parseMessageID(number.child(0).attr("href"));
                        }

                        msgBody = row.select("div.msg_body").first();

                        Element msgBelow = row.select("div.msg_below").first();
                        Element edited = msgBelow.select("span.edited").first();
                        if (edited != null)
                            userTitles += " (edited)";

                        Element belowOptions = msgBelow.select("span.options").first();
                        if (belowOptions != null) {
                            String options = belowOptions.text();
                            if (options.contains("report"))
                                canReport = true;
                            if (options.contains("delete"))
                                canDelete = true;
                            if (options.contains("edit"))
                                canEdit = true;
                            if (options.contains("quote"))
                                canQuote = true;
                            if (options.contains("flair"))
                                canUpdateFlair = true;
                        }

                        if (canUpdateFlair) {
                            updateTopicFlairs = new LinkedHashMap<>();
                            Elements flairs = doc.selectFirst("div.flair_dialog").select("option");

                            int currFlair = 0;
                            for (Element e : flairs) {
                                updateTopicFlairs.put(e.attr("value"), e.attr("label"));
                                if (e.hasAttr("selected")) {
                                    currTopicFlair = currFlair;
                                }
                                currFlair++;
                            }
                        }

                        int hlColor = 0;
                        if (hlUsers.contains(user.toLowerCase(Locale.US))) {
                            HighlightedUser hUser = hlDB
                                    .getHighlightedUsers().get(
                                            user.toLowerCase(Locale.US));
                            hlColor = hUser.getColor();
                            userTitles += " (" + hUser.getLabel() + ")";
                        }

                        if (goToUrlDefinedPost) {
                            if (postNum.equals(goToThisPost))
                                goToThisIndex = msgIndex;
                        }

                        String avatarUrl = row.getElementsByClass("imgboxart").first().attr("src");

                        adapterRows.add(new MessageRowData(user, userTitles, avatarUrl, postNum,
                                postTime, msgBody, boardID, topicID, mID, uID, hlColor, canReport,
                                canDelete, canEdit, canQuote, canUpdateFlair));
                    } else {
                        String postNum = row.select("span.message_num").first().text();
                        if (goToUrlDefinedPost) {
                            if (postNum.equals(goToThisPost))
                                goToThisIndex = msgIndex;
                        }

                        adapterRows.add(new MessageRowData(true, postNum));
                    }

                    msgIndex++;
                }

                break;

            case MESSAGE_DETAIL:
                updateHeaderNoJumper("Message Detail", NetDesc.MESSAGE_DETAIL);

                boardID = parseBoardID(resUrl);
                topicID = parseTopicID(resUrl);
                String mID = parseMessageID(resUrl);

                Elements msgRows = doc.select("td.msg");
                adapterRows.add(new HeaderRowData("Current Version"));

                MessageRowData msg;

                int msgRowCount = msgRows.size();
                for (int x = 0; x < msgRowCount; x++) {
                    if (x == 1)
                        adapterRows.add(new HeaderRowData("Previous Version(s)"));

                    Element currRow = msgRows.get(x);
                    Element msgInfobox = currRow.select("div.msg_infobox").first();
                    Element msgBody = currRow.select("div.msg_body").first();
                    String user = msgInfobox.getElementsByTag("b").first().text();
                    String postTime = msgInfobox.select("span.post_time").first().text();

                    msg = new MessageRowData(user, EMPTY_STRING, EMPTY_STRING,
                            "#" + (msgRowCount - x), postTime, msgBody, boardID, topicID,
                            mID, EMPTY_STRING, 0, false, false, false, false, false);
                    msg.disableTopClick();
                    adapterRows.add(msg);
                }
                break;

            case USER_TAG:
                Element error = doc.getElementsByClass("error").first();
                if (error == null) {
                    Crouton.showText(this, "User tag updated successfully.", Theming.croutonStyle());
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setTitle("There was an error tagging the user...");
                    b.setMessage("Error message from GameFAQs:\n\n" + error.text());
                    b.setPositiveButton("OK", null);
                    b.show();
                }
            case USER_DETAIL:
                tbody = doc.select("table.board").first().getElementsByTag("tbody").first();
                String name = null;
                String ID = null;
                String level = null;
                String creation = null;
                String lVisit = null;
                String sig = null;
                String karma = null;
                String AMP = null;
                String tagKey = null;
                String tagText = null;
                for (Element row : tbody.children()) {
                    String label = row.child(0).text().toLowerCase(Locale.US);
                    switch (label) {
                        case "user name":
                            name = row.child(1).text();
                            break;
                        case "user id":
                            ID = row.child(1).text();
                            break;
                        case "board user level":
                            level = row.child(1).html();
                            break;
                        case "account created":
                            creation = row.child(1).text();
                            break;
                        case "last visit":
                            lVisit = row.child(1).text();
                            break;
                        case "signature":
                            sig = row.child(1).html();
                            break;
                        case "karma":
                            karma = row.child(1).text();
                            break;
                        case "active messages posted":
                            AMP = row.child(1).text();
                            break;
                    }
                }

                if (Session.isLoggedIn()) {
                    Element pmIcon = doc.select("i.fa-envelope").last();
                    if (pmIcon != null && pmIcon.attr("title").startsWith("Send a PM to"))
                        setMenuItemVisibility(sendUserPMIcon, true);

                    setMenuItemVisibility(tagUserIcon, true);
                    tagKey = doc.getElementsByAttributeValue("name", "key").attr("value");
                    tagText = doc.getElementsByAttributeValue("name", "tag_text").attr("value");
                    if (tagText == null) tagText = EMPTY_STRING;
                }

                updateHeaderNoJumper(name + "'s Details", NetDesc.USER_DETAIL);

                userDetailData = new UserDetailRowData(name, ID, level, creation, lVisit, sig,
                        karma, AMP, tagKey, tagText, resUrl);
                adapterRows.add(userDetailData);
                break;

            case GAME_SEARCH:
                String searchQuery = resUrl.substring(resUrl.indexOf("game=") + 5);
                int i = searchQuery.indexOf("&");
                if (i != -1)
                    searchQuery = searchQuery.replace(searchQuery.substring(i), EMPTY_STRING);

                int pageIndex = resUrl.indexOf("page=");
                if (pageIndex != -1) {
                    String currPage = resUrl.substring(pageIndex + 5);
                    i = currPage.indexOf("&");
                    if (i != -1)
                        currPage = currPage.replace(currPage.substring(i), EMPTY_STRING);
                    pagesInfo[0] = NumberUtils.toInt(currPage) + 1;
                } else {
                    pagesInfo[0] = 1;
                }

                if (pagesInfo[0] > 1) {
                    firstPage = GAME_SEARCH_URL + searchQuery + "&page=0";
                    prevPage = GAME_SEARCH_URL + searchQuery + "&page=" + (pagesInfo[0] - 2);
                }
                if (!doc.getElementsByClass("fa-angle-right").isEmpty()) {
                    nextPage = GAME_SEARCH_URL + searchQuery + "&page=" + (pagesInfo[0]);
                }

                try {
                    headerTitle = "Searching games: " + URLDecoder.decode(searchQuery, DocumentParser.CHARSET_NAME) + EMPTY_STRING;
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError(DocumentParser.CHARSET_NAME + " is unknown");
                    // should never happen
                }

                updateHeader(headerTitle, firstPage, prevPage, pagesInfo[0],
                        -1, nextPage, lastPage, pagePrefix, NetDesc.GAME_SEARCH);

                setMenuItemVisibility(searchIcon, true);

                Element gameSearchResults = doc.select("div.search_results_product").first();
                if (gameSearchResults != null) {
                    for (Element row : gameSearchResults.select("div.sr_row")) {
                        String platform = row.select("div.sr_platform").text();
                        String bName = row.select("div.sr_title").text();
                        String bYear = row.select("div.sr_release").text();
                        String bUrl = row.select("div.sr_links").select("a:containsOwn(Board)").attr("href");

                        if (!bUrl.equals(EMPTY_STRING))
                            adapterRows.add(new GameSearchRowData(bName, platform, bYear, bUrl));
                    }
                } else {
                    adapterRows.add(new HeaderRowData("No results."));
                }
                break;

            case FRIENDS:
            case FOLLOWERS:
            case FOLLOWING:
            case MODHIST:
                //TODO: implement me
                break;

            case MSG_EDIT:
            case LOGIN_S1:
            case MSG_MARK:
            case MSG_DELETE:
            case TOPIC_UPDATE_FLAIR:
            case PM_SEND_S1:
            case PM_SEND_S2:
            case MSG_POST:
            case TOPIC_CLOSE:
            case NOTIFS_CLEAR:
            case TOPIC_POST:
            case VERIFY_ACCOUNT_S1:
            case VERIFY_ACCOUNT_S2:
                getSupportActionBar().setTitle(desc.name() + " Unhandled");
                break;

            case UNSPECIFIED:
                getSupportActionBar().setTitle("Page unhandled - " + resUrl);
                break;
        }

        Element pmInboxLink = masthead.select("i.fa-envelope").first();
        String pmButtonLabel = getString(R.string.pm_inbox);
        if (pmInboxLink != null) {
            pmButtonLabel += " " + ((TextNode) pmInboxLink.nextSibling()).text();
        }
        setMenuItemVisibility(unreadPMsIcon, pmInboxLink != null && desc != NetDesc.PM_INBOX);

        dwrPMInboxItem.setTitle(pmButtonLabel);

        Element notifsObject = masthead.select("div#ndrop").first();
        notifsAdapter.clear();
        notifsLinks.clear();
        notifsLinks.add("filler");
        if (notifsObject != null) {
            Elements notifsLines = notifsObject.getElementsByTag("li");
            notifsLines.remove(notifsLines.size() - 1); // remove footer
            notifsLines.remove(0); // remove header
            notifsAdapter.add(notifsLines.size() + " " +
                    (notifsLines.size() == 1 ? getString(R.string.notification) : getString(R.string.notifications)));
            for (Element e : notifsLines) {
                notifsAdapter.add(e.text());
                notifsLinks.add(e.select("a").first().attr("href"));
            }
            notifsAdapter.add("View All");
            notifsLinks.add(NOTIFS_PAGE_LINK);
            notifsAdapter.add("Clear All");
            notifsLinks.add(NOTIFS_CLEAR_LINK);
        } else {
            notifsAdapter.add("0 " + getString(R.string.notifications));
            notifsAdapter.add("View All");
            notifsLinks.add(NOTIFS_PAGE_LINK);
        }
        setMenuItemVisibility(unreadNotifsIcon, notifsObject != null && desc != NetDesc.NOTIFS_PAGE);
        notifsAdapter.notifyDataSetChanged();

        swipeRefreshLayout.setEnabled(settings.getBoolean("enablePTR", false));

        viewAdapter.notifyDataSetChanged();

        if (consumeGoToUrlDefinedPost() && !Session.applySavedScroll) {
            contentList.post(() -> contentList.setSelection(goToThisIndex));

        } else if (Session.applySavedScroll) {
            contentList.post(() -> {
                contentList.setSelectionFromTop(Session.savedScrollVal[0], Session.savedScrollVal[1]);
                Session.applySavedScroll = false;
            });

        } else {
            contentList.post(() -> contentList.setSelectionAfterHeaderView());
        }

        if (swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(false);
    }

    /*
     * ********************************
     * END HNR
     * ********************************
     */

    private void processBoardWraps(Elements boardWraps,
                                   String boardWrapHeaderPrefix,
                                   boolean includeBoardWrapHeader) {
        for (Element boardWrap : boardWraps) {
            Element header = boardWrap.previousElementSibling();
            if (header.text().toLowerCase().startsWith(boardWrapHeaderPrefix)) {
                if (includeBoardWrapHeader) {
                    adapterRows.add(new HeaderRowData(header.text()));
                }
                processBoardTbodyRows(boardWrap.selectFirst("tbody").select("tr"));
            }
        }
    }

    private void processBoardTbodyRows(Elements rows) {
        for (Element row : rows) {
            if (row.hasClass("head")) {
                adapterRows.add(new HeaderRowData(row.text()));
            } else {
                // [title + link] [topics] [msgs] [last post]
                Elements cells = row.children();
                Element titleCell = cells.get(0);

                String lvlReq = EMPTY_STRING;
                if (!titleCell.textNodes().isEmpty())
                    lvlReq = titleCell.textNodes().get(0).toString();

                String title = titleCell.child(0).text() + lvlReq;

                String boardDesc = null;
                if (titleCell.children().size() > 2)
                    boardDesc = titleCell.child(2).text();

                String link = titleCell.select("a").first().attr("href");
                if (link.isEmpty())
                    link = null;

                String tCount = null;
                String mCount = null;
                String lPost = null;

                BoardType bvt;

                if (cells.size() > 3) {
                    tCount = cells.get(1).text();
                    mCount = cells.get(2).text();
                    if (cells.get(3).selectFirst("span.show_wide") == null) {
                        lPost = cells.get(3).text();
                    } else {
                        lPost = cells.get(3).selectFirst("span.show_wide").text();
                    }

                    bvt = BoardType.NORMAL;
                } else
                    bvt = BoardType.SPLIT;

                adapterRows.add(new BoardRowData(title, boardDesc, lPost, tCount, mCount, link, bvt));
            }
        }
    }

    private int[] getPageJumperInfo(Element pj) {
        int[] i = new int[]{1, 1};
        if (pj != null && !pj.hasClass("user") && !pj.hasClass("tsort")) {
            String currPage, pageCount;

            int x = 0;
            String pjText = pj.child(x).text();
            while (pjText.contains("First") || pjText.contains("Previous")) {
                x++;
                pjText = pj.child(x).text();
            }
            // Page [dropdown] of 4
            // Page 1 of 3
            if (pj.getElementsByTag("select").isEmpty()) {
                int ofIndex = pjText.indexOf(" of ");
                currPage = pjText.substring(5, ofIndex); // "Page ".length = 5

                int pageCountEnd = pjText.length();
                pageCount = pjText.substring(ofIndex + 4, pageCountEnd);
            } else {
                currPage = pj.select("option[selected=selected]").first().text();
                pageCount = pj.getElementsByTag("option").last().text();
            }

            i[0] = NumberUtils.toInt(currPage);
            i[1] = NumberUtils.toInt(pageCount);
        }
        return i;
    }

    private void updatePostingRights(Document pRes, boolean onTopic) {
        if (onTopic) {
            if (pRes.getElementsByClass("user").first().text().contains("Post New Message")) {
                fab.show();
                pMode = PostMode.ON_TOPIC;
            }
        } else {
            if (pRes.getElementsByClass("user").first().text().contains("New Topic")) {
                fab.show();
                pMode = PostMode.ON_BOARD;
            }
        }
    }

    public void postExecuteCleanup(NetDesc desc) {
        if (needToSetNavList) {
            setNavDrawerVisibility(Session.isLoggedIn());
        }

        ptrCleanup();
        if (desc == NetDesc.BOARD || desc == NetDesc.TOPIC)
            postInterfaceCleanup();

        if (isRoR)
            isRoR = false;

        setLoginName(Session.getUser());
        System.gc();
    }

    private boolean goToUrlDefinedPost = false;
    private int goToThisIndex = 0;

    public void enableGoToUrlDefinedPost() {
        goToUrlDefinedPost = true;
    }

    private boolean consumeGoToUrlDefinedPost() {
        boolean temp = goToUrlDefinedPost;
        goToUrlDefinedPost = false;
        return temp;
    }

    private void updateHeader(String titleIn, String firstPageIn, String prevPageIn, int currPage,
                              int pageCount, String nextPageIn, String lastPageIn,
                              String jumperPageIn, NetDesc desc) {

        if (pageCount == 1) {
            updateHeaderNoJumper(titleIn, desc);
            return;
        }

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(titleIn);

        if (currPage == -1) {
            firstPage.setEnabled(false);
            prevPage.setEnabled(false);
            nextPage.setEnabled(false);
            lastPage.setEnabled(false);
            pageLabel.setEnabled(false);
            pageJumperWrapper.setVisibility(View.GONE);
            pageLabel.setText("~ 1 / 1 ~");
        } else {
            pageJumperWrapper.setVisibility(View.VISIBLE);
            pageJumperDesc = desc;

            if (firstPageIn != null) {
                firstPageUrl = firstPageIn;
                firstPage.setEnabled(true);
            } else {
                firstPage.setEnabled(false);
            }

            if (prevPageIn != null) {
                prevPageUrl = prevPageIn;
                prevPage.setEnabled(true);
            } else {
                prevPage.setEnabled(false);
            }

            if (nextPageIn != null) {
                nextPageUrl = nextPageIn;
                nextPage.setEnabled(true);
            } else {
                nextPage.setEnabled(false);
            }

            if (lastPageIn != null) {
                lastPageUrl = lastPageIn;
                lastPage.setEnabled(true);
            } else {
                lastPage.setEnabled(false);
            }

            if (pageCount != -1) {
                jumperPageUrl = jumperPageIn;

                final String[] items = new String[pageCount];
                for (int x = 0; x < pageCount; x++) {
                    items[x] = String.valueOf(x + 1);
                }

                if (jumperDialogBuilder == null) {
                    jumperDialogBuilder = new AlertDialog.Builder(this);
                    jumperDialogBuilder.setTitle("Select a page...");
                }

                jumperDialogBuilder.setItems(items, (dialog, which) -> {
                    int x = jumperPageUrl.indexOf("?page=") + 6;
                    if (x == 5) // -1 + 6 = 5, when "?page=" is not found
                        x = jumperPageUrl.indexOf("&page=") + 6;

                    String go = jumperPageUrl.substring(0, x) + which + jumperPageUrl.substring(x);
                    session.get(pageJumperDesc, go);
                });

                pageLabel.setEnabled(true);
                String t = "~ " + currPage + " / " + pageCount + " ~";
                pageLabel.setText(t);
            } else {
                pageLabel.setEnabled(false);
                String t = currPage + " / ???";
                pageLabel.setText(t);
            }
        }
    }

    private void updateHeaderNoJumper(String title, NetDesc desc) {
        updateHeader(title, null, null, -1, -1, null, null, null, desc);
    }

    private MessageRowView clickedMsg;
    private String quoteSelection;

    public void messageMenuClicked(MessageRowView msg) {
        clickedMsg = msg;
        quoteSelection = clickedMsg.getSelection();

        //noinspection deprecation
        showDialog(MESSAGE_ACTION_DIALOG);
    }

    private void editPostSetup(String msg, String msgID) {
        postBody.setText(msg);
        messageIDForEditing = msgID;
        postSetup(true);
    }

    private void quoteSetup(String user, String msg, String mID, String uID) {
        // <cite data-quote-id="925122238" data-user-id="3705639">
        String openCite = "<cite data-quote-id=\"" + mID + "\" data-user-id=\"" + uID + "\">";
        String quotedMsg = openCite + user + " posted...</cite>\n" + "<quote>" + msg + "</quote>\n";

        int start = Math.max(postBody.getSelectionStart(), 0);
        int end = Math.max(postBody.getSelectionEnd(), 0);

        assert postBody.getText() != null : "postBody.getText() is null";
        postBody.getText().replace(Math.min(start, end), Math.max(start, end), quotedMsg);

        if (postWrapper.getVisibility() != View.VISIBLE)
            postSetup(true);
        else
            postBody.setSelection(Math.min(start, end) + quotedMsg.length());
    }

    private void postSetup(boolean postingOnTopic) {
        findViewById(R.id.aioHTMLScroller).scrollTo(0, 0);
        pageJumperWrapper.setVisibility(View.GONE);
        fab.hide();
        postSubmitButton.setEnabled(true);
        postCancelButton.setEnabled(true);

        if (postingOnTopic) {
            titleWrapper.setVisibility(View.GONE);
            postBody.requestFocus();
            assert postBody.getText() != null : "postBody.getText() is null";
            postBody.setSelection(postBody.getText().length());
        } else {
            titleWrapper.setVisibility(View.VISIBLE);
            if (boardFlairsNames.size() > 0) {
                flairButton.setEnabled(true);
                flairButton.setVisibility(View.VISIBLE);
                flairSep.setVisibility(View.VISIBLE);
            }
            if (Session.userHasAdvancedPosting()) {
                pollButton.setEnabled(true);
                pollButton.setVisibility(View.VISIBLE);
                pollSep.setVisibility(View.VISIBLE);
            }
            postTitle.requestFocus();
        }

        postWrapper.setVisibility(View.VISIBLE);
        postPostUrl = session.getLastPath();
        if (postPostUrl.contains("#"))
            postPostUrl = postPostUrl.substring(0, postPostUrl.indexOf('#'));
    }

    public void postCancel(View view) {
        if (settings.getBoolean("confirmPostCancel", false)) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage("Cancel this post?");
            b.setPositiveButton("Yes", (dialog, which) -> postInterfaceCleanup());
            b.setNegativeButton("No", null);
            b.show();
        } else
            postInterfaceCleanup();
    }

    public void postPollOptions(View view) {
        //noinspection deprecation
        showDialog(POLL_OPTIONS_DIALOG);
    }

    public void postFlairSet(View view) {
        final int[] flairKeys = new int[boardFlairsNames.size() - 1];
        String[] flairNames = new String[boardFlairsNames.size() - 1];

        int x = 0;
        int activeFlairIndex = 0;
        for (Map.Entry<Integer, String> flair : boardFlairsNames.entrySet()) {
            if (flair.getKey() != -1) {
                flairKeys[x] = flair.getKey();
                flairNames[x] = flair.getValue();

                if (flairForNewTopic == flair.getKey()) {
                    activeFlairIndex = x;
                }

                x++;
            }
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setSingleChoiceItems(flairNames, activeFlairIndex, (dialog, which) -> {
            flairForNewTopic = flairKeys[which];
            dialog.dismiss();
        });
        b.setNegativeButton(R.string.cancel, null);
        b.show();
    }

    public void postDo(View view) {
        if (settings.getBoolean("confirmPostSubmit", false)) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage("Submit this post?");
            b.setPositiveButton("Yes", (dialog, which) -> postSubmit());
            b.setNegativeButton("No", null);
            b.show();
        } else
            postSubmit();
    }

    private void postSubmit() {
        assert postTitle.getText() != null : "postTitle.getText() is null";
        assert postBody.getText() != null : "postBody.getText() is null";

        savedPostTitle = postTitle.getText().toString();
        savedPostBody = postBody.getText().toString();

        savedBoardID = boardID;
        savedTopicID = topicID;
        savedMessageID = messageIDForEditing;

        postSubmitButton.setEnabled(false);
        pollButton.setEnabled(false);
        flairButton.setEnabled(false);
        postCancelButton.setEnabled(false);

        HashMap<String, List<String>> data = new HashMap<>();
        data.put("board", Collections.singletonList(savedBoardID));
        data.put("key", Collections.singletonList(session.getSessionKey()));
        data.put("message", Collections.singletonList(savedPostBody));
        if (!getSettingsPref().getBoolean("useGFAQsSig" + Session.getUser(), false)) {
            data.put("sig", Collections.singletonList(getSig()));
        }

        if (titleWrapper.getVisibility() == View.VISIBLE) {
            // posting on a board
            data.put("topic", Collections.singletonList(savedPostTitle));
            if (pollJSON.length() > 0) {
                data.put("poll", Collections.singletonList(pollJSON.toString()));
            }
            if (flairForNewTopic > 0) {
                data.put("flair", Collections.singletonList(getFlairForNewTopicAsString()));
            } else {
                data.put("flair", Collections.singletonList("1"));
            }
            session.post(NetDesc.TOPIC_POST, GF_URLS.AJAX_TOPIC_POST, data);
        } else {
            // posting on a topic
            data.put("topic", Collections.singletonList(savedTopicID));

            if (messageIDForEditing == null) {
                // posting new message
                session.post(NetDesc.MSG_POST, GF_URLS.AJAX_MSG_POST, data);
            } else {
                // editing existing message, change fields to match
                data.put("message", Collections.singletonList(savedMessageID));
                data.put("message_text", Collections.singletonList(savedPostBody));
                session.post(NetDesc.MSG_EDIT, GF_URLS.AJAX_MSG_EDIT, data);
            }
        }
    }

    /**
     * creates dialogs
     */
    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {

            case SEND_PM_DIALOG:
                dialog = createSendPMDialog();
                break;

            case MESSAGE_ACTION_DIALOG:
                dialog = createMessageActionDialog();
                break;

            case REPORT_MESSAGE_DIALOG:
                dialog = createReportMessageDialog();
                break;

            case POLL_OPTIONS_DIALOG:
                dialog = createPollOptionsDialog();
                break;
        }

        return dialog;
    }

    private Dialog createPollOptionsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);

        b.setTitle("Poll Options");
        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams") final View v = inflater.inflate(R.layout.polloptions, null);
        b.setView(v);
        b.setCancelable(false);

        final EditText[] options = new EditText[10];

        assert v != null : "v is null";
        final EditText poTitle = v.findViewById(R.id.poTitle);
        options[0] = v.findViewById(R.id.po1);
        options[1] = v.findViewById(R.id.po2);
        options[2] = v.findViewById(R.id.po3);
        options[3] = v.findViewById(R.id.po4);
        options[4] = v.findViewById(R.id.po5);
        options[5] = v.findViewById(R.id.po6);
        options[6] = v.findViewById(R.id.po7);
        options[7] = v.findViewById(R.id.po8);
        options[8] = v.findViewById(R.id.po9);
        options[9] = v.findViewById(R.id.po10);
        final Spinner minLevel = v.findViewById(R.id.poMinLevel);

        if (pollJSON.length() > 0) {
            poTitle.setText(pollJSON.optString("poll_title", EMPTY_STRING));
            minLevel.setSelection(pollJSON.optInt("min_level", 0));
            JSONObject optionValues = pollJSON.optJSONObject("poll_option");
            if (optionValues != null) {
                for (int i = 0; i < 10; i++) {
                    options[i].setText(optionValues.optString(String.valueOf(i), EMPTY_STRING));
                }
            }
        }

        b.setPositiveButton("Save", (dialog, which) -> {
            try {
                pollJSON.put("poll_title", poTitle.getText().toString().trim());

                JSONObject optionValues = new JSONObject();
                for (int x = 0; x < 10; x++) {
                    if (options[x].getText().toString().trim().length() > 0) {
                        optionValues.put(String.valueOf(x), options[x].getText().toString().trim());
                    }
                }
                pollJSON.put("poll_option", optionValues);

                pollJSON.put("min_level", String.valueOf(minLevel.getSelectedItemPosition()));
            } catch (JSONException e) {
                clearPoll();
                AlertDialog.Builder failedSaveAlert = new AlertDialog.Builder(this);
                failedSaveAlert.setMessage("There was an error saving the poll!").show();
            }
        });

        b.setNegativeButton("Cancel", null);

        b.setNeutralButton("Clear", (dialog, which) -> clearPoll());

        Dialog dialog = b.create();
        dialog.setOnDismissListener(dialog1 -> {
            //noinspection deprecation
            removeDialog(POLL_OPTIONS_DIALOG);
        });
        return dialog;
    }

    private void clearPoll() {
        pollJSON = new JSONObject();
    }

    private String reportCode;

    private Dialog createReportMessageDialog() {
        AlertDialog.Builder reportMsgBuilder = new AlertDialog.Builder(this);
        reportMsgBuilder.setTitle("Report Message");

        final String[] reportOptions;
        if (clickedMsg.getPostNum().equals("1"))
            reportOptions = getResources().getStringArray(R.array.msgReportReasonsWithOffTopic);
        else
            reportOptions = getResources().getStringArray(R.array.msgReportReasons);

        reportMsgBuilder.setItems(reportOptions, (dialog, which) -> {
            reportCode = getResources().getStringArray(R.array.msgReportCodes)[which];

            /*
            <form action="https://www.gamefaqs.com/features/board_mark/pick.php" method="post">
            <input type="hidden" name="b" value="848">
            <input type="hidden" name="t" value="71881473">
            <input type="hidden" name="m" value="821951056">
            <input type="hidden" name="r" value="8">
            <input type="hidden" name="rt" value="Testing">
            <input type="hidden" name="i" value="0">
            <input type="hidden" name="key" value="[session key]">
            <input type="submit">
            </form>
             */

            HashMap<String, List<String>> markData = new HashMap<>();
            markData.put("b", Collections.singletonList(boardID));
            markData.put("t", Collections.singletonList(topicID));
            markData.put("m", Collections.singletonList(clickedMsg.getMessageID()));
            markData.put("r", Collections.singletonList(reportCode));
            markData.put("rt", Collections.singletonList(EMPTY_STRING));
            markData.put("i", Collections.singletonList("0"));
            markData.put("key", Collections.singletonList(session.getSessionKey()));

            session.post(NetDesc.MSG_MARK, "/features/board_mark/pick.php", markData);


//                session.get(NetDesc.MARKMSG_S1, clickedMsg.getMessageDetailLink());
        });

        reportMsgBuilder.setNegativeButton("Cancel", null);

        Dialog dialog = reportMsgBuilder.create();
        dialog.setOnDismissListener(dialog1 -> {
            //noinspection deprecation
            removeDialog(REPORT_MESSAGE_DIALOG);
        });
        return dialog;
    }

    private Dialog createMessageActionDialog() {
        AlertDialog.Builder msgActionBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams") final View v = inflater.inflate(R.layout.msgaction, null);

        msgActionBuilder.setView(v);

        msgActionBuilder.setTitle("Message Actions");

        ArrayList<String> listBuilder = new ArrayList<>();

        if (clickedMsg.getMessageID() != null) {
            if (clickedMsg.isEdited())
                listBuilder.add("View Previous Version(s)");
            else
                listBuilder.add("Message Detail");
        }

        if (clickedMsg.canQuote())
            listBuilder.add("Quote");
        if (clickedMsg.canEdit())
            listBuilder.add("Edit");
        if (clickedMsg.canUpdateFlair())
            listBuilder.add("Update Flair");
        if (clickedMsg.canDelete())
            listBuilder.add("Delete");
        if (clickedMsg.canReport())
            listBuilder.add("Report");

        listBuilder.add("Highlight User");
        listBuilder.add("User Details");

        assert v != null : "v is null";
        ListView lv = v.findViewById(R.id.maList);
        final LinearLayout wrapper = v.findViewById(R.id.maWrapper);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapter.addAll(listBuilder);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            assert selected != null : "selected is null";
            switch (selected) {
                case "View Previous Version(s)":
                case "Message Detail":
                    session.get(NetDesc.MESSAGE_DETAIL, clickedMsg.getMessageDetailLink());
                    break;
                case "Quote":
                    String msg = (quoteSelection != null ? quoteSelection : clickedMsg.getMessageForQuotingOrEditing());
                    quoteSetup(clickedMsg.getUser(), msg, clickedMsg.getMessageID(), clickedMsg.getUserID());
                    break;
                case "Edit":
                    editPostSetup(clickedMsg.getMessageForQuotingOrEditing(), clickedMsg.getMessageID());
                    break;
                case "Update Flair": //TODO update this to AJAX if available
                    final String[] flairKeys = updateTopicFlairs.keySet().toArray(new String[0]);
                    String[] flairNames = updateTopicFlairs.values().toArray(new String[0]);
                    AlertDialog.Builder flairUpdater = new AlertDialog.Builder(AllInOneV2.this);
                    flairUpdater.setSingleChoiceItems(flairNames, currTopicFlair, (dialog, which) -> {
                        if (which != currTopicFlair) {
                            HashMap<String, List<String>> flairData = new HashMap<>();
                            flairData.put("target_id", Collections.singletonList(flairKeys[which]));
                            flairData.put("action", Collections.singletonList("flair_update"));
                            flairData.put("key", Collections.singletonList(session.getSessionKey()));
                            session.post(NetDesc.TOPIC_UPDATE_FLAIR,
                                    clickedMsg.getBoardActionLink(), flairData);
                        }
                        dialog.dismiss();
                    });
                    flairUpdater.setNegativeButton(R.string.cancel, null);
                    flairUpdater.show();
                    break;
                case "Delete":
                    HashMap<String, List<String>> delData = new HashMap<>();
                    delData.put("action", Collections.singletonList("delete"));
                    delData.put("key", Collections.singletonList(session.getSessionKey()));

                    session.post(NetDesc.MSG_DELETE,
                            clickedMsg.getBoardActionLink(), delData);
                    break;
                case "Report":
                    //noinspection deprecation
                    showDialog(REPORT_MESSAGE_DIALOG);
                    break;
                case "Highlight User":
                    HighlightedUser user = hlDB.getHighlightedUsers().get(clickedMsg.getUser().toLowerCase(Locale.US));
                    HighlightListDBHelper.showHighlightUserDialog(AllInOneV2.this, user, clickedMsg.getUser(), null);
                    break;
                case "User Details":
                    session.get(NetDesc.USER_DETAIL, clickedMsg.getUserDetailLink());
                    break;
                default:
                    Crouton.showText(AllInOneV2.this, "not recognized: " + selected, Theming.croutonStyle());
                    break;
            }

            //noinspection deprecation
            dismissDialog(MESSAGE_ACTION_DIALOG);
        });

        msgActionBuilder.setNegativeButton("Cancel", null);

        Dialog dialog = msgActionBuilder.create();
        dialog.setOnDismissListener(dialog12 -> {
            //noinspection deprecation
            removeDialog(MESSAGE_ACTION_DIALOG);
        });

        dialog.setOnShowListener(dialog1 -> {
            if (quoteSelection != null)
                Crouton.showText(AllInOneV2.this, "Selected text prepped for quoting.", Theming.croutonStyle(), wrapper);
        });

        return dialog;
    }

    private LinearLayout pmSending;

    private Dialog createSendPMDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams") final View v = inflater.inflate(R.layout.sendpm, null);

        b.setView(v);
        b.setTitle("Send Private Message");
        b.setCancelable(false);

        assert v != null : "v is null";
        final EditText to = v.findViewById(R.id.spTo);
        final EditText subject = v.findViewById(R.id.spSubject);
        final EditText message = v.findViewById(R.id.spMessage);
        pmSending = v.findViewById(R.id.spFootWrapper);

        to.setText(savedTo);
        subject.setText(savedSubject);
        message.setText(savedMessage);

        b.setPositiveButton("Send", null);
        b.setNegativeButton("Cancel", null);

        final AlertDialog d = b.create();
        d.setOnShowListener(dialog -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String toContent = to.getText().toString();
            String subjectContent = subject.getText().toString();
            String messageContent = message.getText().toString();

            if (toContent.length() > 0) {
                if (subjectContent.length() > 0) {
                    if (messageContent.length() > 0) {
                        savedTo = toContent;
                        savedSubject = subjectContent;
                        savedMessage = messageContent;

                        pmSending.setVisibility(View.VISIBLE);

                        session.get(NetDesc.PM_SEND_S1, "/pm/new");

                    } else
                        Crouton.showText(AllInOneV2.this,
                                "The message can't be empty.",
                                Theming.croutonStyle(),
                                (ViewGroup) to.getParent());
                } else
                    Crouton.showText(AllInOneV2.this,
                            "The subject can't be empty.",
                            Theming.croutonStyle(),
                            (ViewGroup) to.getParent());
            } else
                Crouton.showText(AllInOneV2.this,
                        "The recipient can't be empty.",
                        Theming.croutonStyle(),
                        (ViewGroup) to.getParent());
        }));

        d.setOnDismissListener(dialog -> {
            pmSending = null;
            //noinspection deprecation
            removeDialog(SEND_PM_DIALOG);
        });
        return d;
    }

    private String[] boardFavoritesQuickListOptions;
    private String[] boardFavoritesQuickListLinks;

    private void showBoardFavoritesQuickList() {
        if (boardFavoritesQuickListOptions != null && boardFavoritesQuickListOptions.length > 1) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("My Boards");
            b.setNegativeButton("Cancel", null);
            b.setItems(boardFavoritesQuickListOptions, (dialog, which) -> {
                if (which == 0) {
                    session.get(NetDesc.BOARDS_FAVORITE, GF_URLS.BOARDS_FAVORITES);
                } else {
                    session.get(NetDesc.BOARD, boardFavoritesQuickListLinks[which]);
                }
            });
            drawerLayout.closeDrawers();
            b.show();
        } else {
            Crouton.showText(this, "You don't have any favorite boards yet!", Theming.croutonStyle());
            session.get(NetDesc.BOARDS_EXPLORE, GF_URLS.BOARDS_EXPLORE);
        }
    }

    private String replyTo, replySubject;
    public String savedTo, savedSubject, savedMessage;

    public void pmSetup(String toIn, String subjectIn, String messageIn) {
        if (toIn != null && !toIn.equals("null"))
            savedTo = toIn;
        else
            savedTo = EMPTY_STRING;

        if (subjectIn != null && !subjectIn.equals("null"))
            savedSubject = subjectIn;
        else
            savedSubject = EMPTY_STRING;

        if (messageIn != null && !messageIn.equals("null"))
            savedMessage = messageIn;
        else
            savedMessage = EMPTY_STRING;

        try {
            savedTo = URLDecoder.decode(savedTo, DocumentParser.CHARSET_NAME);
            savedSubject = URLDecoder.decode(savedSubject, DocumentParser.CHARSET_NAME);
            savedMessage = URLDecoder.decode(savedMessage, DocumentParser.CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(DocumentParser.CHARSET_NAME + " is unknown");
            // should never happen
        }

        //noinspection deprecation
        showDialog(SEND_PM_DIALOG);
    }

    public void pmCleanup(boolean wasSuccessful, String error) {
        if (wasSuccessful) {
            Crouton.showText(this, "PM sent.", Theming.croutonStyle());
            hideSoftKeyboard(pmSending);

            //noinspection deprecation
            dismissDialog(SEND_PM_DIALOG);
        } else {
            Crouton.showText(this, error, Theming.croutonStyle(), (ViewGroup) pmSending.getParent());
            pmSending.setVisibility(View.GONE);
        }
    }


    public void refreshClicked(View view) {
        if (view != null)
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        if (session.getLastPath() == null) {
            if (Session.isLoggedIn()) {
                session = new Session(this, Session.getUser(), AccountManager.getPassword(this, Session.getUser()));
            } else {
                session = new Session(this);
            }
        } else
            session.refresh();
    }

    public String getSig() {
        String sig = EMPTY_STRING;

        if (session != null) {
            if (Session.isLoggedIn())
                sig = settings.getString("customSig" + Session.getUser(), EMPTY_STRING);
        }

        if (sig.length() == 0)
            sig = settings.getString("customSig", EMPTY_STRING);

        if (sig.length() == 0)
            sig = defaultSig;

        return sig.replace("*grver*", BuildConfig.VERSION_NAME);
    }

    private String parseBoardID(String url) {
        // board example: https://www.gamefaqs.com/boards/400-current-events
        String boardUrl = url.substring(GF_URLS.ROOT.length() + 8);

        int i = boardUrl.indexOf('/');
        if (i != -1) {
            String replacer = boardUrl.substring(i);
            boardUrl = boardUrl.replace(replacer, EMPTY_STRING);
        }

        i = boardUrl.indexOf('?');
        if (i != -1) {
            String replacer = boardUrl.substring(i);
            boardUrl = boardUrl.replace(replacer, EMPTY_STRING);
        }
        i = boardUrl.indexOf('#');
        if (i != -1) {
            String replacer = boardUrl.substring(i);
            boardUrl = boardUrl.replace(replacer, EMPTY_STRING);
        }
        return boardUrl;
    }

    private String parseTopicID(String url) {
        // topic example: https://www.gamefaqs.com/boards/400-current-events/64300205
        String topicUrl = url.substring(url.indexOf('/', GF_URLS.ROOT.length() + 8) + 1);
        int i = topicUrl.indexOf('/');
        if (i != -1) {
            String replacer = topicUrl.substring(i);
            topicUrl = topicUrl.replace(replacer, EMPTY_STRING);
        }
        i = topicUrl.indexOf('?');
        if (i != -1) {
            String replacer = topicUrl.substring(i);
            topicUrl = topicUrl.replace(replacer, EMPTY_STRING);
        }
        i = topicUrl.indexOf('#');
        if (i != -1) {
            String replacer = topicUrl.substring(i);
            topicUrl = topicUrl.replace(replacer, EMPTY_STRING);
        }
        return topicUrl;
    }

    private String parseMessageID(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }


    @Override
    public void onBackPressed() {
        if (searchIcon != null && searchIcon.isActionViewExpanded()) {
            searchIcon.collapseActionView();
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else if (postWrapper.getVisibility() == View.VISIBLE) {
            postCancel(postCancelButton);
        } else {
            if (session != null && session.canGoBack()) {
                session.goBack(false);
            } else {
                finish();
            }
        }
    }

    public static String buildAMPLink() {
        return "/user/messages?lp=" + settings.getString("ampSortOption", "-1");
    }

    public void uploadImage(View view) {
        invokeRailgun();
    }

    public void invokeRailgun() {
        PackageManager pm = getPackageManager();
        if (isPackageInstalled("com.ioabsoftware.imgtcuploader", pm) || isPackageInstalled("com.ioabsoftware.imgtcuploader.lite", pm)) {
            startActivity(new Intent("com.ioabsoftware.imgtcuploader.INVOKE_IMAGE_PICKER"));
        } else if (isPackageInstalled("com.android.vending", pm)) {
            AlertDialog.Builder installRailgun = new AlertDialog.Builder(this);
            installRailgun.setTitle("Railgun Image Uploader");
            installRailgun.setMessage("Railgun not installed. Click \"Full\" or \"Lite\" below to install it. The Lite version is ad-supported.\n\n" +
                    "Railgun is a quick and easy method for uploading images to an image host and get a pastable direct link to the image back.");
            installRailgun.setPositiveButton("Full", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ioabsoftware.imgtcuploader"))));
            installRailgun.setNeutralButton("Lite", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ioabsoftware.imgtcuploader.lite"))));
            installRailgun.setNegativeButton("Cancel", null);
            installRailgun.show();
        } else {
            AlertDialog.Builder noAppStore = new AlertDialog.Builder(this);
            noAppStore.setTitle("Railgun Image Uploader");
            noAppStore.setMessage("The Google Play Store is not available on this phone, which is needed to install Railgun.");
            noAppStore.setPositiveButton("OK", null);
            noAppStore.show();
        }
    }

    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void htmlButtonClicked(View view) {
        String tag = (String) view.getTag();
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";

        int start = Math.max(postBody.getSelectionStart(), 0);
        int end = Math.max(postBody.getSelectionEnd(), 0);

        String insert;
        if (start != end)
            insert = open + postBody.getText().subSequence(start, end) + close;
        else
            insert = open + close;

        postBody.getText().replace(Math.min(start, end), Math.max(start, end), insert, 0, insert.length());
    }

    public void processNotif(String text, String url) {
        if (text.startsWith("Your user level has changed to")) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage("Congrats on the new user level!");
            b.setPositiveButton("Thanks!", null);
            b.show();
        } else if (url.equals(NOTIFS_CLEAR_LINK)) {
            session.get(NetDesc.NOTIFS_CLEAR, url);
        } else if (Session.determineNetDesc(url) == NetDesc.UNSPECIFIED) {
            final String deets = "Notif Text: " + text + "\n" +
                    "Notif Link: " + url;
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setMessage("You found a notification that is currently unsupported. Good job! " +
                    "The details of the notification are below. Press OK to copy the details " +
                    "to the clipboard and be taken to correct board to report this.\n" +
                    "\n" +
                    deets);
            b.setPositiveButton("OK", (dialogInterface, i) -> {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                assert clipboard != null;
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("simple text", deets));
                session.get(NetDesc.TOPIC, "/boards/1177-gameraven-development-and-discussion");
            });
            b.setNegativeButton("Dismiss", null);
            b.show();
        } else {
            session.get(Session.determineNetDesc(url), url);
        }
    }

    private AdapterView.OnItemSelectedListener accountsListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String logoutTag = getString(R.string.log_out);
            String currUser = (Session.getUser() == null ? logoutTag : Session.getUser());
            String selUser = accountsAdapter.getItem(position);
            assert selUser != null;

            if (!currUser.equals(selUser)) {
                if (session.hasNetworkConnection()) {
                    if (selUser.equals(logoutTag)) {
                        session = new Session(AllInOneV2.this);
                    } else {
                        session = new Session(AllInOneV2.this,
                                selUser,
                                AccountManager.getPassword(AllInOneV2.this, selUser),
                                session.getLastPath(),
                                session.getLastDesc());
                    }
                } else {
                    noNetworkConnection();
                    setLoginName(currUser);
                }
            }

            drawerLayout.closeDrawers();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };

    private static final String NOTIFS_CLEAR_LINK = "/ajax/notification_clear_all";
    private static final String NOTIFS_PAGE_LINK = "/user/notifications";
    private AdapterView.OnItemSelectedListener notifsListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position > 0) {
                toggleMenu();
                notifsSpinner.setSelection(0);
                processNotif((String) parent.getItemAtPosition(position), notifsLinks.get(position));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // do nothing
        }
    };
}
