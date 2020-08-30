package com.ioabsoftware.gameraven.networking;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.BuildConfig;
import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.db.History;
import com.ioabsoftware.gameraven.db.HistoryDBAdapter;
import com.ioabsoftware.gameraven.util.DocumentParser;
import com.ioabsoftware.gameraven.util.FinalDoc;
import com.ioabsoftware.gameraven.util.Theming;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.ConnectionClosedException;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Session is used to establish and maintain GFAQs sessions, and to send GET and POST requests.
 *
 * @author Charles Rosaaen, Insanity On A Bun Software
 */
public class Session implements FutureCallback<Response<FinalDoc>> {

    private String lastAttemptedPath = "not set";

    public String getLastAttemptedPath() {
        return lastAttemptedPath;
    }

    private NetDesc lastAttemptedDesc = NetDesc.UNSPECIFIED;

    public NetDesc getLastAttemptedDesc() {
        return lastAttemptedDesc;
    }

    /**
     * The latest page, with excess get data.
     */
    private String lastPath = null;

    /**
     * Get's the path of the latest page.
     */
    public String getLastPath() {
        return lastPath;
    }

    /**
     * Get's the path of the latest page, stripped of any GET data.
     */
    public String getLastPathWithoutData() {
        if (lastPath.contains("?"))
            return lastPath.substring(0, lastPath.indexOf('?'));
        else
            return lastPath;
    }

    /**
     * The latest Response body as an array of bytes.
     */
    private byte[] lastResBodyAsBytes = null;

    /**
     * The latest description.
     */
    private NetDesc lastDesc = null;

    /**
     * Get's the description of the latest page.
     */
    public NetDesc getLastDesc() {
        return lastDesc;
    }

    /**
     * The name of the user for this session.
     */
    private static String user = null;

    /**
     * Get's the name of the session user.
     */
    public static String getUser() {
        return user;
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    private static int userLevel = 0;

    private static boolean userCanViewAMP() {
        return userLevel > 14;
    }

    /**
     * Quickpost and create poll topics
     */
    public static boolean userHasAdvancedPosting() {
        return userLevel > 29;
    }

    public static boolean applySavedScroll;
    public static int[] savedScrollVal;

    /**
     * The password of the user for this session.
     */
    private String password = null;

    private String sessionKey;

    public String getSessionKey() {
        return sessionKey;
    }

    /**
     * The current activity.
     */
    private AllInOneV2 aio;
    private SharedPreferences getPrefObj() {
        return AllInOneV2.getSettingsPref();
    }

    private boolean addToHistory = true;

    public void forceNoHistoryAddition() {
        addToHistory = false;
    }

    private HistoryDBAdapter hAdapter;

    public static String RESUME_INIT_URL = "RESUME-SESSION";
    private String initUrl = null;
    private NetDesc initDesc = null;


    /**
     * Create a new session with no user logged in
     * that starts at the homepage.
     */
    public Session(AllInOneV2 aioIn) {
        this(aioIn, null, null);
    }

    /**
     * Construct a new session for the specified user,
     * using the specified password, that finishes on
     * the GFAQs homepage.
     *
     * @param userIn     The user for this session.
     * @param passwordIn The password for this session.
     */
    public Session(AllInOneV2 aioIn, String userIn, String passwordIn) {
        this(aioIn, userIn, passwordIn, null, null);
    }

    /**
     * Construct a new session for the specified user,
     * using the specified password, that finishes on
     * initUrlIn using initDescIn. Passing null for
     * initUrlIn will finish on the GFAQs homepage.
     *
     * @param userIn     The user for this session.
     * @param passwordIn The password for this session.
     * @param initUrlIn  The URL to load once successfully logged in.
     * @param initDescIn The desc to use once successfully logged in.
     */
    public Session(AllInOneV2 aioIn, String userIn, String passwordIn, String initUrlIn, NetDesc initDescIn) {
        initUrl = initUrlIn;
        initDesc = initDescIn;
        finalConstructor(aioIn, userIn, passwordIn);
    }

    /**
     * Final construction method.
     *
     * @param aioIn      The current activity
     * @param userIn     Username, or null if no user
     * @param passwordIn Password, or null if no user
     */
    private void finalConstructor(AllInOneV2 aioIn, String userIn, String passwordIn) {
        aio = aioIn;
        aio.navDrawerReset();

        netManager = (ConnectivityManager) aio.getSystemService(Context.CONNECTIVITY_SERVICE);

        hAdapter = new HistoryDBAdapter();
        openHistoryDB();

        if (initUrl == null || !initUrl.equals(RESUME_INIT_URL))
            hAdapter.clearTable();

        user = userIn;
        password = passwordIn;

        // reset the Session unread PM and TT counters
        getPrefObj().edit().putInt("unreadPMCount", 0).apply();
        getPrefObj().edit().putInt("unreadTTCount", 0).apply();

        // clear out cookies
        Ion.getDefault(aio).getCookieMiddleware().clear();

        if (user == null) {
            get(NetDesc.BOARDS_EXPLORE, GF_URLS.BOARDS_EXPLORE);
            aio.setLoginName(user);
        } else {
            get(NetDesc.LOGIN_S1, GF_URLS.ROOT + "/boards/");
            aio.setLoginName(user);
            aio.showLoggingInDialog(user);
        }
    }

    /**
     * Builds a URL based on path.
     *
     * @param path The path to build a URL off of. Can
     *             be relative or absolute. If relative, can start
     *             with a forward slash or not.
     * @return The correct absolute URL for the specified
     * path.
     */
    public static String buildURL(String path, NetDesc desc) {
        if (!path.contains("www.gamefaqs.com") && path.contains("gamefaqs.com"))
            path = path.replace("gamefaqs.com", "www.gamefaqs.com");

        if (path.contains("http://www.gamefaqs.com")) {
            path = path.replace("http://www.gamefaqs.com", "https://www.gamefaqs.com");
        }

        if (desc == NetDesc.BOARD && path.matches(".*\\d$")) {
            path += "-";
        }

        // path is absolute, return it
        if (path.startsWith("http"))
            return path;

        // add a forward slash to path if needed
        if (!path.startsWith("/"))
            path = '/' + path;

        // return absolute path
        return GF_URLS.ROOT + path;
    }

    private ConnectivityManager netManager;

    public boolean hasNetworkConnection() {
        NetworkInfo netInfo = netManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }


    private Future currentNetworkTask;

    /**
     * Sends a GET request to a specified page.
     *
     * @param desc Description of this request, to properly handle the response later.
     * @param path The path to send the request to.
     */
    public void get(NetDesc desc, String path) {
        if (hasNetworkConnection()) {
            if (desc == NetDesc.MODHIST) {
                aio.genError("Page Unsupported", "The moderation history page is currently unsupported in-app. Sorry.", "Ok");
            } else if (desc == NetDesc.FRIENDS || desc == NetDesc.FOLLOWERS || desc == NetDesc.FOLLOWING) {
                aio.genError("Page Unsupported", "The friends and followers system is currently unsupported in-app. Sorry.", "Ok");
            } else {
                if (currentNetworkTask != null && !currentNetworkTask.isDone())
                    currentNetworkTask.cancel(true);

                lastAttemptedPath = path;
                lastAttemptedDesc = desc;

                preExecuteSetup(desc);

                currentNetworkTask = Ion.with(aio)
                        .load("GET", buildURL(path, desc))
                        .as(new DocumentParser())
                        .withResponse()
                        .setCallback(this);

            }
        } else
            aio.noNetworkConnection();
    }

    /**
     * Sends a POST request to a specified page.
     *
     * @param desc Description of this request, to properly handle the response later.
     * @param path The path to send the request to.
     * @param data The extra data to send along.
     */
    public void post(NetDesc desc, String path, Map<String, List<String>> data) {
        if (hasNetworkConnection()) {
            if (desc != NetDesc.MODHIST) {
                if (currentNetworkTask != null && !currentNetworkTask.isDone())
                    currentNetworkTask.cancel(true);

                preExecuteSetup(desc);
                currentNetworkTask = Ion.with(aio)
                        .load("POST", buildURL(path, desc))
                        .setBodyParameters(data)
                        .as(new DocumentParser())
                        .withResponse()
                        .setCallback(this);
            } else
                aio.genError("Page Unsupported", "The moderation history page is currently unsupported in-app. Sorry.", "Ok");

        } else
            aio.noNetworkConnection();
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
        if (e instanceof CancellationException)
            return;

        NetDesc thisDesc = currentDesc;
        handleNetworkResult(e, thisDesc, result);
        postExecuteCleanup(thisDesc);
    }

    private void preExecuteSetup(NetDesc desc) {
        currentDesc = desc;
        switch (desc) {
            case AMP_LIST:
            case TRACKED_TOPICS:
            case BOARDS_EXPLORE:
            case BOARDS_FAVORITE:
            case BOARD:
            case BOARD_UPDATE_FILTER:
            case TOPIC:
            case GAME_SEARCH:
            case MESSAGE_DETAIL:
            case USER_DETAIL:
            case USER_TAG:
            case MODHIST:
            case PM_INBOX:
            case PM_INBOX_DETAIL:
            case PM_OUTBOX:
            case PM_OUTBOX_DETAIL:
            case MSG_MARK:
            case TOPIC_CLOSE:
            case MSG_DELETE:
            case TOPIC_UPDATE_FLAIR:
            case TOPIC_POLL_VOTE:
            case LOGIN_S1:
            case MSG_POST:
            case MSG_EDIT:
            case TOPIC_POST:
            case NOTIFS_PAGE:
            case NOTIFS_CLEAR:
            case MENTIONS_PAGE:
            case FRIENDS:
            case FOLLOWERS:
            case FOLLOWING:
            case UNSPECIFIED:
                aio.preExecuteSetup(desc);
                break;

            case LOGIN_S2:
            case VERIFY_ACCOUNT_S1:
            case VERIFY_ACCOUNT_S2:
            case PM_SEND_S1:
            case PM_SEND_S2:
                break;
        }
    }

    private void handleNetworkResult(Exception e, NetDesc desc, Response<FinalDoc> result) {
        try {
            if (e != null)
                throw e;

            if (result != null && result.getResult() != null && result.getResult().doc != null) {

                if (lastDesc == NetDesc.LOGIN_S2)
                    aio.dismissLoginDialog();

                result.getResult().doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

                Document doc = result.getResult().doc;
                String resUrl = result.getRequest().getUri().toString();

                if (!resUrl.startsWith(GF_URLS.ROOT)) {
                    AlertDialog.Builder b = new AlertDialog.Builder(aio);
                    b.setTitle("Redirected");
                    b.setMessage("The request was redirected somewhere away from GameFAQs. " +
                            "This usually happens if you're connected to a network that requires a login, " +
                            "such as a paid-for wifi service. Click below to open the page in your browser.\n" +
                            "\n" +
                            "Redirect: " + resUrl);

                    final String path = resUrl;
                    b.setPositiveButton("Open Page In Browser", (dialog, which) -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(path));
                        aio.startActivity(browserIntent);
                        aio.finish();
                    });

                    b.create().show();
                    return;
                }

                if (!doc.select("header.page_header:contains(CAPTCHA)").isEmpty()) {

                    String captcha = doc.select("iframe").outerHtml();
                    final String key = doc.getElementsByAttributeValue("name", "key").attr("value");

                    AlertDialog.Builder b = new AlertDialog.Builder(aio);
                    b.setTitle("CAPTCHA Required");

                    LinearLayout wrapper = new LinearLayout(aio);
                    wrapper.setOrientation(LinearLayout.VERTICAL);

                    WebView web = new WebView(aio);
                    web.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    web.loadDataWithBaseURL(resUrl,
                            "<p>There have been multiple unsuccessful login attempts!</p>" + captcha,
                            "text/html",
                            null, null);
                    wrapper.addView(web);

                    final EditText form = new EditText(aio);
                    form.setHint("Enter confirmation code (NOT CAPTCHA!!!) here");
                    wrapper.addView(form);

                    b.setView(wrapper);

                    b.setPositiveButton("Login", (dialog, which) -> {
                        HashMap<String, List<String>> loginData = new HashMap<>();
                        // "EMAILADDR", user, "PASSWORD", password, "path", lastPath, "key", key
                        loginData.put("EMAILADDR", Collections.singletonList(user));
                        loginData.put("PASSWORD", Collections.singletonList(password));
                        loginData.put("path", Collections.singletonList(GF_URLS.ROOT));
                        loginData.put("key", Collections.singletonList(key));
                        loginData.put("recaptcha_challenge_field", Collections.singletonList(form.getText().toString()));
                        loginData.put("recaptcha_response_field", Collections.singletonList("manual_challenge"));

                        post(NetDesc.LOGIN_S2, "/user/login_captcha.html", loginData);
                    });

                    b.create().show();
                    return;
                }

                int responseCode = result.getHeaders().code();
                if (BuildConfig.DEBUG && responseCode != 200)
                    Crouton.showText(aio, "HTTP Response Code: " + responseCode, Theming.croutonStyle());

                if (responseCode != 200) {
                    if (responseCode == 404) {
                        Elements paragraphs = doc.getElementsByTag("p");
                        aio.genError("404 Error", paragraphs.get(1).text() + "\n\n" + paragraphs.get(2).text(), "Ok");
                        return;
                    } else if (responseCode == 403) {
                        Elements paragraphs = doc.getElementsByTag("p");
                        aio.genError("403 Error", paragraphs.get(1).text() + "\n\n" + paragraphs.get(2).text(), "Ok");
                        return;
                    } else if (responseCode == 401) {
                        if (lastDesc == NetDesc.LOGIN_S2) {
                            forceSkipAIOCleanup();
                            get(NetDesc.BOARDS_EXPLORE, GF_URLS.BOARDS_EXPLORE);
                        } else {
                            Elements paragraphs = doc.getElementsByTag("p");
                            aio.genError("401 Error", paragraphs.get(1).text() + "\n\n" + paragraphs.get(2).text(), "Ok");
                        }
                        return;
                    }
                }

                Element firstHeader = doc.getElementsByTag("h1").first();
                if (firstHeader != null && firstHeader.text().equals("408 Request Time-out")) {
                    aio.genError("408 Error", "Your browser didn't send a complete request in time.", "Ok");
                    return;
                }

                if (doc.title().equals("GameFAQs - 503 - Temporarily Unavailable")) {
                    aio.genError("503 Error", "GameFAQs is experiencing some temporary difficulties with " +
                            "the site. Please wait a few seconds before refreshing this page to try again.", "Ok");

                    return;
                } else if (doc.title().equals("GameFAQs is Down")) {
                    aio.genError("GameFAQs is Down", "GameFAQs is experiencing an outage at the moment - " +
                            "the servers are overloaded and unable to serve pages. Hopefully, this is a " +
                            "temporary problem, and will be rectified by the time you refresh this page.", "Ok");

                    return;
                }

                if (resUrl.contains("account_suspended.html")) {
                    aio.genError("Account Suspended", "Your account seems to be suspended. Please " +
                            "log in to your account in a web browser for more details.", "Ok");

                    return;
                } else if (resUrl.contains("account_banned.html")) {
                    aio.genError("Account Banned", "Your account seems to be banned. Please " +
                            "log in to your account in a web browser for more details.", "Ok");

                    return;
                } else if (resUrl.contains("welcome.php")) {
                    aio.genError("New Account", "It looks like this is a new account. Welcome to GameFAQs! " +
                            "There are some ground rules you'll have to go over first before you can get " +
                            "access to the message boards. Please log in to your account in a web browser " +
                            "and access the message boards there to view and accept the site terms and rules.", "Ok");

                    return;
                } else if (resUrl.contains("register.html?miss=1")) {
                    aio.genError("Login Required", "You've just tried to access a feature that requires a " +
                            "GameFAQs account. You can manage your accounts and log in through the navigation " +
                            "drawer. If you are currently logged into an account, try removing the account " +
                            "from the app and re-adding it.", "Ok");

                    return;
                }

                updateUserLevel(doc);

                switch (desc) {
                    case AMP_LIST:
                    case TRACKED_TOPICS:
                    case BOARDS_EXPLORE:
                    case BOARDS_FAVORITE:
                    case BOARD:
                    case BOARD_UPDATE_FILTER:
                    case TOPIC:
                    case GAME_SEARCH:
                    case MESSAGE_DETAIL:
                    case USER_DETAIL:
                    case MODHIST:
                    case PM_INBOX:
                    case PM_INBOX_DETAIL:
                    case PM_OUTBOX:
                    case PM_OUTBOX_DETAIL:
                    case UNSPECIFIED:
                    case LOGIN_S1:
                    case LOGIN_S2:
                    case MSG_DELETE:
                    case TOPIC_UPDATE_FLAIR:
                    case TOPIC_POLL_VOTE:
                    case VERIFY_ACCOUNT_S1:
                    case VERIFY_ACCOUNT_S2:
                    case NOTIFS_PAGE:
                    case MENTIONS_PAGE:
                    case FRIENDS:
                    case FOLLOWERS:
                    case FOLLOWING:
                        break;

                    case MSG_POST:
                    case MSG_EDIT:
                    case USER_TAG:
                    case MSG_MARK:
                    case TOPIC_POST:
                    case TOPIC_CLOSE:
                    case PM_SEND_S1:
                    case PM_SEND_S2:
                    case NOTIFS_CLEAR:
                        addToHistory = false;
                        break;
                }

                if (addToHistory) {
                    addHistory();
                }

                switch (desc) {
                    case AMP_LIST:
                    case TRACKED_TOPICS:
                    case BOARDS_EXPLORE:
                    case BOARDS_FAVORITE:
                    case BOARD:
                    case TOPIC:
                    case GAME_SEARCH:
                    case MESSAGE_DETAIL:
                    case USER_DETAIL:
                    case MODHIST:
                    case PM_INBOX:
                    case PM_INBOX_DETAIL:
                    case PM_OUTBOX:
                    case PM_OUTBOX_DETAIL:
                    case MSG_DELETE:
                    case TOPIC_UPDATE_FLAIR:
                    case UNSPECIFIED:
                    case LOGIN_S1:
                    case LOGIN_S2:
                    case TOPIC_POLL_VOTE:
                    case VERIFY_ACCOUNT_S1:
                    case VERIFY_ACCOUNT_S2:
                    case NOTIFS_PAGE:
                    case MENTIONS_PAGE:
                    case FRIENDS:
                    case FOLLOWERS:
                    case FOLLOWING:

                        lastDesc = desc;
                        lastResBodyAsBytes = result.getResult().bytes;
                        lastPath = resUrl;

                        // replace boardaction part of url, don't want it being added to history
                        if (lastPath.contains("/boardaction/"))
                            lastPath = lastPath.replace("/boardaction/", "/boards/");

                        break;

                    case MSG_POST:
                    case MSG_EDIT:
                    case BOARD_UPDATE_FILTER:
                    case USER_TAG:
                    case MSG_MARK:
                    case TOPIC_POST:
                    case TOPIC_CLOSE:
                    case PM_SEND_S1:
                    case PM_SEND_S2:
                    case NOTIFS_CLEAR:
                        break;

                }

                // reset history flag
                addToHistory = true;

                Element keyElem = doc.getElementsByAttributeValue("name", "key").first();
                if (keyElem != null)
                    sessionKey = doc.getElementsByAttributeValue("name", "key").first().attr("value");

                switch (desc) {
                    case LOGIN_S1:
                        String loginKey = doc.getElementsByAttributeValue("name", "key").attr("value");

                        HashMap<String, List<String>> loginData = new HashMap<>();
                        // "EMAILADDR", user, "PASSWORD", password, "path", lastPath, "key", key
                        loginData.put("EMAILADDR", Collections.singletonList(user));
                        loginData.put("PASSWORD", Collections.singletonList(password));
                        loginData.put("path", Collections.singletonList(buildURL("answers", NetDesc.UNSPECIFIED)));
                        loginData.put("key", Collections.singletonList(loginKey));

                        post(NetDesc.LOGIN_S2, "/user/login", loginData);
                        break;

                    case LOGIN_S2:
                        aio.setAMPLinkVisible(userCanViewAMP());

                        String ampStart = aio.getString(R.string.amp_list);
                        String favsStart = aio.getString(R.string.boards_favorites);
                        String loggedInStartLocation = getPrefObj().getString(
                                "loggedInStartLocation", aio.getString(R.string.boards_explore));

                        if (initUrl != null) {
                            if (initUrl.equals(RESUME_INIT_URL) && canGoBack()) {
                                aio.dismissLoginDialog();
                                goBack(true);
                                aio.setNavDrawerVisibility(isLoggedIn());
                            } else
                                get(initDesc, initUrl);
                        } else if (userCanViewAMP() && loggedInStartLocation.equals(ampStart)) {
                            get(NetDesc.AMP_LIST, AllInOneV2.buildAMPLink());
                        } else if (loggedInStartLocation.equals(favsStart)) {
                            get(NetDesc.BOARDS_FAVORITE, GF_URLS.BOARDS_FAVORITES);
                        } else {
                            get(NetDesc.BOARDS_EXPLORE, GF_URLS.BOARDS_EXPLORE);
                        }

                        break;

                    case MSG_POST:
                    case MSG_EDIT:
                        String sanitizedMsgJSONString = doc.body().html()
                                .replace("<br \\=\"\" />", "\n")
                                .replace("\\n", "\n");
                        JSONObject msgJSON = new JSONObject(sanitizedMsgJSONString);

                        String msgStatus = msgJSON.optString("status", "no_status");

                        if (msgStatus.equalsIgnoreCase("success")) {
                            aio.enableGoToUrlDefinedPost();
                            applySavedScroll = false;
                            forceNoHistoryAddition();
                            String postOk = desc == NetDesc.MSG_POST ? "Message posted." : "Message edited.";
                            Crouton.showText(aio, postOk, Theming.croutonStyle());
                            get(NetDesc.TOPIC, msgJSON.getString("message_url"));
                        } else if (msgStatus.equalsIgnoreCase("error")) {
                            aio.postError(msgJSON.optString("status_text", "There was an error, but GameFAQs did not provide any details."));
                            postErrorDetected = true;
                        } else if (msgStatus.equalsIgnoreCase("warning")) {
                            String msgAutoflagPath = desc == NetDesc.MSG_POST ? GF_URLS.AJAX_MSG_POST : GF_URLS.AJAX_MSG_EDIT;
                            HashMap<String, List<String>> msgAutoFlagData = new HashMap<>();
                            msgAutoFlagData.put("board", Collections.singletonList(aio.getSavedBoardID()));
                            msgAutoFlagData.put("topic", Collections.singletonList(aio.getSavedTopicID()));
                            msgAutoFlagData.put("key", Collections.singletonList(getSessionKey()));
                            msgAutoFlagData.put("override", Collections.singletonList("1"));
                            if (desc == NetDesc.MSG_POST) {
                                msgAutoFlagData.put("message", Collections.singletonList(aio.getSavedPostBody()));
                            } else {
                                msgAutoFlagData.put("message", Collections.singletonList(aio.getSavedMessageID()));
                                msgAutoFlagData.put("message_text", Collections.singletonList(aio.getSavedPostBody()));
                            }
                            showAutoFlagWarning(msgAutoflagPath, msgAutoFlagData, desc,
                                    msgJSON.optString("warnings", "There was a warning, but GameFAQs did not provide any details."));
                            postErrorDetected = true;
                        } else {
                            aio.postError("Post status response was unrecognized: " + msgStatus);
                            postErrorDetected = true;
                        }
                        break;

                    case TOPIC_POST:
                        String sanitizedTopicJSONString = doc.body().html()
                                .replace("<br \\=\"\" />", "\n")
                                .replace("\\n", "\n");
                        JSONObject topicJSON = new JSONObject(sanitizedTopicJSONString);

                        String topicStatus = topicJSON.optString("status", "no_status");

                        if (topicStatus.equalsIgnoreCase("success")) {
                            Crouton.showText(aio, "Topic posted.", Theming.croutonStyle());
                            get(NetDesc.TOPIC, topicJSON.getString("topic_url"));
                        } else if (topicStatus.equalsIgnoreCase("error")) {
                            aio.postError(topicJSON.optString("status_text", "There was an error, but GameFAQs did not provide any details."));
                            postErrorDetected = true;
                        } else if (topicStatus.equalsIgnoreCase("warning")) {
                            HashMap<String, List<String>> topicAutoFlagData = new HashMap<>();
                            topicAutoFlagData.put("board", Collections.singletonList(aio.getSavedBoardID()));
                            topicAutoFlagData.put("topic", Collections.singletonList(aio.getSavedPostTitle()));
                            topicAutoFlagData.put("message", Collections.singletonList(aio.getSavedPostBody()));
                            topicAutoFlagData.put("key", Collections.singletonList(getSessionKey()));
                            topicAutoFlagData.put("override", Collections.singletonList("1"));
                            if (aio.getPollJSON().length() > 0) {
//                                data.put("add_poll", Collections.singletonList("1"));
                                topicAutoFlagData.put("poll", Collections.singletonList(aio.getPollJSON().toString()));
                            }
                            if (aio.getFlairForNewTopicAsInt() > 0) {
                                topicAutoFlagData.put("flair", Collections.singletonList(aio.getFlairForNewTopicAsString()));
                            } else {
                                topicAutoFlagData.put("flair", Collections.singletonList("1"));
                            }

                            showAutoFlagWarning(GF_URLS.AJAX_TOPIC_POST, topicAutoFlagData, NetDesc.TOPIC_POST,
                                    topicJSON.optString("warnings", "There was a warning, but GameFAQs did not provide any details."));
                            postErrorDetected = true;
                        } else {
                            aio.postError("Post status response was unrecognized: " + topicStatus);
                            postErrorDetected = true;
                        }
                        break;

                    case MSG_MARK:
                        String response = doc.text();
                        int start = response.indexOf("\":\"") + 3;
                        int end = response.indexOf("\",\"");
                        String markMessage = response.substring(start, end);
                        Crouton.showText(aio, markMessage, Theming.croutonStyle());
                        break;

                    case TOPIC_UPDATE_FLAIR:
                    case MSG_DELETE:
                        Crouton.showText(aio, "Done.", Theming.croutonStyle());
                        applySavedScroll = true;
                        savedScrollVal = aio.getScrollerVertLoc();
                        lastDesc = NetDesc.TOPIC;
                        processTopicsAndMessages(doc, resUrl, NetDesc.TOPIC);
                        break;

                    case TOPIC_CLOSE:
                        Crouton.showText(aio, "Topic closed successfully.", Theming.croutonStyle());
                        goBack(true);
                        break;

                    case USER_TAG:
                    case NOTIFS_CLEAR:
                        refresh();
                        break;

                    case PM_SEND_S1:
                        HashMap<String, List<String>> pmData = new HashMap<>();
                        pmData.put("key", Collections.singletonList(sessionKey));
                        pmData.put("to", Collections.singletonList(aio.savedTo));
                        pmData.put("subject", Collections.singletonList(aio.savedSubject));
                        pmData.put("message", Collections.singletonList(aio.savedMessage));
                        pmData.put("submit", Collections.singletonList("Send Message"));

                        post(NetDesc.PM_SEND_S2, "/pm/new", pmData);
                        break;

                    case PM_SEND_S2:
                        if (doc.select("input[name=subject]").isEmpty()) {
                            aio.pmCleanup(true, null);
                        } else {
                            String error = doc.select("form[action=/pm/new]").first().previousElementSibling().text();
                            aio.pmCleanup(false, error);
                        }
                        break;

                    case TOPIC:
                        processTopicsAndMessages(doc, resUrl, NetDesc.TOPIC);
                        break;

                    case MESSAGE_DETAIL:
                        processTopicsAndMessages(doc, resUrl, NetDesc.MESSAGE_DETAIL);
                        break;

                    case BOARD_UPDATE_FILTER:
                    case TOPIC_POLL_VOTE:
                        goBack(true);
                        break;

                    case GAME_SEARCH:
                    case AMP_LIST:
                    case TRACKED_TOPICS:
                    case BOARDS_EXPLORE:
                    case BOARDS_FAVORITE:
                    case BOARD:
                    case UNSPECIFIED:
                    case USER_DETAIL:
                    case MODHIST:
                    case PM_INBOX:
                    case PM_INBOX_DETAIL:
                    case PM_OUTBOX:
                    case PM_OUTBOX_DETAIL:
                    case VERIFY_ACCOUNT_S1:
                    case VERIFY_ACCOUNT_S2:
                    case NOTIFS_PAGE:
                    case MENTIONS_PAGE:
                    case FRIENDS:
                    case FOLLOWERS:
                    case FOLLOWING:
                        aio.processContent(desc, doc, resUrl);
                        break;
                }
            } else {
                // connection failed for some reason, probably timed out
                aio.timeoutCleanup(desc);
            }
        } catch (TimeoutException timeoutEx) {
            aio.timeoutCleanup(desc);
        } catch (UnknownHostException unknownHostEx) {
            aio.genError("Unknown Host Exception", "Couldn't find the address for the specified host. " +
                    "This usually happens due to a DNS lookup error, which is outside of GameRaven's " +
                    "ability to handle. If you continue to receive this error, try resetting your network. " +
                    "If you are on wifi, you can do this by unplugging your router for 30 seconds, then plugging " +
                    "it back in. If on a cellular connection, toggle airplane mode on and off, or restart " +
                    "the phone.", "Ok");
        } catch (ConnectionClosedException connClosedEx) {
            aio.genError("Connection Closed", "The connection was closed before the the response was completed.", "Ok");
        } catch (Throwable ex) {

            throw new RuntimeException(ex);
        }
    }

    private void addHistory() {
        if (lastPath != null) {
            switch (lastDesc) {
                case AMP_LIST:
                case TRACKED_TOPICS:
                case BOARDS_EXPLORE:
                case BOARDS_FAVORITE:
                case BOARD:
                case TOPIC:
                case GAME_SEARCH:
                case MESSAGE_DETAIL:
                case USER_DETAIL:
                case MODHIST:
                case PM_INBOX:
                case PM_INBOX_DETAIL:
                case PM_OUTBOX:
                case PM_OUTBOX_DETAIL:
                case MSG_DELETE:
                case TOPIC_UPDATE_FLAIR:
                case TOPIC_POLL_VOTE:
                case NOTIFS_PAGE:
                case MENTIONS_PAGE:
                case FRIENDS:
                case FOLLOWERS:
                case FOLLOWING:
                case UNSPECIFIED:
                    int[] vLoc = aio.getScrollerVertLoc();
                    hAdapter.insertHistory(lastPath, lastDesc.name(), lastResBodyAsBytes, vLoc[0], vLoc[1]);
                    break;

                case BOARD_UPDATE_FILTER:
                case USER_TAG:
                case MSG_MARK:
                case TOPIC_CLOSE:
                case LOGIN_S1:
                case LOGIN_S2:
                case MSG_POST:
                case MSG_EDIT:
                case TOPIC_POST:
                case VERIFY_ACCOUNT_S1:
                case VERIFY_ACCOUNT_S2:
                case PM_SEND_S1:
                case PM_SEND_S2:
                case NOTIFS_CLEAR:
                    break;

            }
        }
    }

    private void processTopicsAndMessages(Document doc, String resUrl, NetDesc successDesc) {
        boolean processAsBoard = false;
        if (!doc.select("p:contains(no longer available for viewing)").isEmpty()) {
            if (successDesc == NetDesc.TOPIC)
                Crouton.showText(aio, "The topic you selected is no longer available for viewing.", Theming.croutonStyle());
            else if (successDesc == NetDesc.MESSAGE_DETAIL)
                Crouton.showText(aio, "The message you selected is no longer available for viewing.", Theming.croutonStyle());

            processAsBoard = true;
        } else if (!doc.select("p:contains(Your topic has been deleted)").isEmpty()) {
            Crouton.showText(aio, "Your topic has been deleted.", Theming.croutonStyle());
            processAsBoard = true;
        }

        if (processAsBoard) {
            aio.processContent(NetDesc.BOARD, doc, resUrl);
        } else {
            aio.processContent(successDesc, doc, resUrl);
        }
    }

    private boolean skipAIOCleanup = false;

    public void forceSkipAIOCleanup() {
        skipAIOCleanup = true;
    }

    private boolean postErrorDetected = false;

    private void postExecuteCleanup(NetDesc desc) {
        switch (desc) {
            case AMP_LIST:
            case TRACKED_TOPICS:
            case BOARDS_EXPLORE:
            case BOARDS_FAVORITE:
            case BOARD:
            case TOPIC:
            case MESSAGE_DETAIL:
            case USER_DETAIL:
            case USER_TAG:
            case MODHIST:
            case PM_INBOX:
            case PM_INBOX_DETAIL:
            case PM_OUTBOX:
            case PM_OUTBOX_DETAIL:
            case MSG_MARK:
            case MSG_DELETE:
            case TOPIC_UPDATE_FLAIR:
            case TOPIC_POLL_VOTE:
            case TOPIC_CLOSE:
            case GAME_SEARCH:
            case NOTIFS_PAGE:
            case MENTIONS_PAGE:
            case FRIENDS:
            case FOLLOWERS:
            case FOLLOWING:
            case UNSPECIFIED:
                if (!skipAIOCleanup)
                    aio.postExecuteCleanup(desc);
                break;

            case BOARD_UPDATE_FILTER:
            case LOGIN_S1:
            case LOGIN_S2:
            case MSG_POST:
            case MSG_EDIT:
            case TOPIC_POST:
            case VERIFY_ACCOUNT_S1:
            case VERIFY_ACCOUNT_S2:
            case PM_SEND_S1:
            case PM_SEND_S2:
            case NOTIFS_CLEAR:
                break;
        }

        skipAIOCleanup = false;
        postErrorDetected = false;
    }

    public boolean canGoBack() {
        return hAdapter.hasHistory();
    }

    public void popHistory() {
        if (canGoBack())
            hAdapter.pullHistory();
    }

    public void goBack(boolean forceReload) {
        History h = hAdapter.pullHistory();

        applySavedScroll = true;
        savedScrollVal = h.getVertPos();

        if (forceReload || getPrefObj().getBoolean("reloadOnBack", false)) {
            forceNoHistoryAddition();
            get(h.getDesc(), h.getPath());
        } else {
            lastDesc = h.getDesc();
            lastResBodyAsBytes = h.getResBodyAsBytes();
            lastPath = h.getPath();

            Document d = Jsoup.parse(new String(lastResBodyAsBytes), lastPath);
            d.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            aio.processContent(lastDesc, d, lastPath);
        }
    }

    public void openHistoryDB() {
        hAdapter.open(aio);
    }

    public void closeHistoryDB() {
        hAdapter.close();
    }

    public void addHistoryBeforeStop() {
        addHistory();
    }

    public void setLastPathAndDesc(String path, NetDesc desc) {
        lastPath = path;
        lastDesc = desc;
    }

    public void refresh() {
        forceNoHistoryAddition();
        applySavedScroll = true;
        savedScrollVal = aio.getScrollerVertLoc();

        int i = lastPath.indexOf('#');
        String trimmedPath;
        if (i != -1)
            trimmedPath = lastPath.substring(0, i);
        else
            trimmedPath = lastPath;

        if (lastDesc == NetDesc.AMP_LIST)
            trimmedPath = AllInOneV2.buildAMPLink();

        get(lastDesc, trimmedPath);
    }

    private void showAutoFlagWarning(final String path, final HashMap<String, List<String>> data, final NetDesc desc, String msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(aio);
        b.setTitle("GameFAQs says...");
        b.setMessage(msg);

        b.setPositiveButton("Post Anyway", (dialog, which) -> post(desc, path, data));

        b.setNegativeButton("Cancel", (dialog, which) -> aio.postExecuteCleanup(desc));

        Dialog d = b.create();
        d.setCancelable(false);

        d.show();
    }

    private void updateUserLevel(Document doc) {
        String sc = doc.getElementsByTag("head").first().getElementsByTag("script").html();
        int baseStart = sc.indexOf("UserLevel','");
        if (baseStart > -1) {
            int start = baseStart + 12;
            int end = sc.indexOf('\'', start + 1);
            if (end > start)
                userLevel = NumberUtils.toInt(sc.substring(start, end));
        }
    }

    public static NetDesc determineNetDesc(String url) {
        url = Session.buildURL(url, NetDesc.UNSPECIFIED);

        if (url.startsWith(GF_URLS.ROOT)) {

            if (url.equals(GF_URLS.ROOT + "/pm"))
                url += "/";
            if (url.contains("/pm/")) {
                if (url.contains("/pm/sent?id=")) {
                    return NetDesc.PM_OUTBOX_DETAIL;
                } else if (url.contains("/pm/sent")) {
                    return NetDesc.PM_OUTBOX;
                } else if (url.contains("?id=")) {
                    return NetDesc.PM_INBOX_DETAIL;
                } else {
                    return NetDesc.PM_INBOX;
                }
            } else if (url.contains("/user/")) {
                if (url.contains("/messages")) {
                    return NetDesc.AMP_LIST;
                } else if (url.contains("/notifications")) {
                    return NetDesc.NOTIFS_PAGE;
                } else if (url.contains("/mentions")) {
                    return NetDesc.MENTIONS_PAGE;
                } else if (url.contains("/tracked")) {
                    return NetDesc.TRACKED_TOPICS;
                } else if (url.contains("/moderated")) {
                    return NetDesc.MODHIST;
                } else if (url.contains("/friends")) {
                    return NetDesc.FRIENDS;
                } else if (url.contains("/following")) {
                    return NetDesc.FOLLOWING;
                } else if (url.contains("/followers")) {
                    return NetDesc.FOLLOWERS;
                }
            } else if (url.contains("/boards")) {
                String boardUrl = url.substring(url.indexOf("boards"));
                if (boardUrl.contains("/")) {
                    if (boardUrl.contains("/explore") || boardUrl.contains("/browse")) {
                        return NetDesc.BOARDS_EXPLORE;
                    } else {
                        String checkForTopicSep = boardUrl.substring(boardUrl.indexOf("/") + 1);
                        if (checkForTopicSep.contains("/")) {
                            String checkForMsgSep = checkForTopicSep.substring(checkForTopicSep.indexOf("/") + 1);
                            if (checkForMsgSep.contains("/")) {
                                // should be a message
                                return NetDesc.MESSAGE_DETAIL;
                            } else {
                                // should be a topic
                                return NetDesc.TOPIC;
                            }
                        } else {
                            // should be a board
                            return NetDesc.BOARD;
                        }
                    }
                } else {
                    // should be board explorer
                    return NetDesc.BOARDS_EXPLORE;
                }
            }
        }

        return NetDesc.UNSPECIFIED;
    }
}
