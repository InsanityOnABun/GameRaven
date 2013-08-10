package com.ioabsoftware.gameraven;

import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.MenuDrawer.OnDrawerStateChangeListener;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.widget.AdapterView;
import org.holoeverywhere.widget.AdapterView.OnItemSelectedListener;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.Spinner;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.StateSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import com.ioabsoftware.gameraven.db.HighlightListDBHelper;
import com.ioabsoftware.gameraven.db.HighlightedUser;
import com.ioabsoftware.gameraven.networking.HandlesNetworkResult.NetDesc;
import com.ioabsoftware.gameraven.networking.Session;
import com.ioabsoftware.gameraven.views.BoardView;
import com.ioabsoftware.gameraven.views.BoardView.BoardViewType;
import com.ioabsoftware.gameraven.views.HeaderView;
import com.ioabsoftware.gameraven.views.LinkButtonView;
import com.ioabsoftware.gameraven.views.LinkButtonView.Type;
import com.ioabsoftware.gameraven.views.MessageView;
import com.ioabsoftware.gameraven.views.PMDetailView;
import com.ioabsoftware.gameraven.views.PMView;
import com.ioabsoftware.gameraven.views.TopicView;
import com.ioabsoftware.gameraven.views.TopicView.TopicViewType;
import com.ioabsoftware.gameraven.views.TrackedTopicView;
import com.ioabsoftware.gameraven.views.UserDetailView;

//@SuppressWarnings("deprecation")
public class AllInOneV2 extends Activity {
	
	private static boolean needToCheckForUpdate = true;
	public static boolean isReleaseBuild = false;
	
	public static final int NEW_VERSION_DIALOG = 101;
	public static final int SEND_PM_DIALOG = 102;
	public static final int MESSAGE_ACTION_DIALOG = 103;
	public static final int REPORT_MESSAGE_DIALOG = 104;
	public static final int POLL_OPTIONS_DIALOG = 105;
	
	protected static final String ACCOUNTS_PREFNAME = "com.ioabsoftware.DroidFAQs.Accounts";
	protected static final String SALT_FALLBACK = "RIP Man fan died at the room shot up to 97 degrees";
	protected static String secureSalt;
	
	public static final String EMPTY_STRING = "";
	
	public static String defaultSig;
	
	/** current session */
	private Session session = null;
	public Session getSession()
	{return session;}
	
	private String boardID;
	public String getBoardID()
	{return boardID;}
	private String topicID;
	public String getTopicID()
	{return topicID;}
	private String messageIDForEditing;
	public String getMessageID()
	{return messageIDForEditing;}
	private String postPostUrl;
	public String getPostPostUrl()
	{return postPostUrl;}
	
	private String savedPostBody;
	public String getSavedPostBody() {
		if (settings.getBoolean("autoCensorEnable", true))
			return autoCensor(savedPostBody);
		else
			return savedPostBody;
	}
	private String savedPostTitle;
	public String getSavedPostTitle() {
		if (settings.getBoolean("autoCensorEnable", true))
			return autoCensor(savedPostTitle);
		else
			return savedPostTitle;
	}
	
	/** preference object for global settings */
	private static SharedPreferences settings = null;
	public static SharedPreferences getSettingsPref()
	{return settings;}
	
	/** list of accounts (username, password) */
	private static SecurePreferences accounts = null;
	public static SecurePreferences getAccounts()
	{return accounts;}
	
	private LinearLayout titleWrapper;
	private EditText postTitle;
	private EditText postBody;
	
	private TextView titleCounter;
	private TextView bodyCounter;
	
	private Button postButton;
	private Button cancelButton;
	private Button pollButton;
	
	private View pollSep;
	
	private boolean pollUse = false;
	public boolean isUsingPoll() {return pollUse;}
	
	private String pollTitle = EMPTY_STRING;
	public String getPollTitle() {return pollTitle;}
	private String[] pollOptions = new String[10];
	public String[] getPollOptions() {return pollOptions;}
	private int pollMinLevel = -1;
	public String getPollMinLevel() {return Integer.toString(pollMinLevel);}
	
	
	private LinearLayout postWrapper;
	
	private PullToRefreshAttacher contentPTR;
	private ScrollView contentScroller;
	private LinearLayout content;
	private WebView adView;
	
	private ActionBar aBar;
	private MenuItem refreshIcon;
	private MenuItem postIcon;
	private MenuItem addFavIcon;
	private MenuItem remFavIcon;
	private MenuItem searchIcon;
	private MenuItem topicListIcon;

    private String tlUrl;
    
	private enum PostMode {ON_BOARD, ON_TOPIC, NEW_PM};
	private PostMode pMode;
	
	private enum FavMode {ON_BOARD, ON_TOPIC};
	private FavMode fMode;
    
    private TextView title;
    private Button firstPage, prevPage, nextPage, lastPage;
    private String firstPageUrl, prevPageUrl, nextPageUrl, lastPageUrl;
    private NetDesc pageJumperDesc;
    private TextView pageLabel;
    
    private View pageJumperWrapper;
	
	public int getScrollerVertLoc() {
		return contentScroller.getScrollY();}
	
	private final ClickListener cl = new ClickListener();
	
	private static boolean usingLightTheme;
	public static boolean getUsingLightTheme() {return usingLightTheme;}
	private static int accentColor, accentTextColor;
	public static int getAccentColor() {return accentColor;};
	public static int getAccentTextColor() {return accentTextColor;}
	private static StateListDrawable msgHeadSelector;
	public static StateListDrawable getMsgHeadSelector() {return msgHeadSelector;}
	private static StateListDrawable selector;
	public static StateListDrawable getSelector() {return selector;}
	private static boolean isAccentLight;
	public static boolean isAccentLight() {return isAccentLight;}
	
	private static HighlightListDBHelper hlDB;
	public static HighlightListDBHelper getHLDB() {return hlDB;}
	
	private MenuDrawer drawer;
	
	
	
	/**********************************************
	 * START METHODS
	 **********************************************/
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
        
		usingLightTheme = settings.getBoolean("useLightTheme", false);
        if (usingLightTheme) {
        	setTheme(R.style.MyThemes_LightTheme);
        }
        
    	super.onCreate(savedInstanceState);
    	
        setContentView(R.layout.allinonev2);
    	
    	drawer = MenuDrawer.attach(this);
        drawer.setContentView(R.layout.allinonev2);
        drawer.setMenuView(R.layout.drawer);
        
        drawer.setOnDrawerStateChangeListener(new OnDrawerStateChangeListener() {
			
			@Override
			public void onDrawerStateChange(int oldState, int newState) {
				if (newState == MenuDrawer.STATE_CLOSED)
					drawer.findViewById(R.id.dwrScroller).scrollTo(0, 0);
			}
			
			@Override
			public void onDrawerSlide(float openRatio, int offsetPixels) {
				// not needed
			}
		});
        
        if (usingLightTheme)
        	drawer.findViewById(R.id.dwrScroller).setBackgroundResource(android.R.drawable.screen_background_light);
        else
        	drawer.findViewById(R.id.dwrScroller).setBackgroundResource(android.R.drawable.screen_background_dark_transparent);

        
        ((Spinner) drawer.findViewById(R.id.dwrChangeAcc)).setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				if (drawer.isMenuVisible()) {
					if (pos == 0 && Session.getUser() != null) {
						wtl("starting new session from onItemSelected, no login");
						session = new Session(AllInOneV2.this);
						drawer.closeMenu(true);
					}

					else {
						String selUser = parent.getItemAtPosition(pos).toString();
						if (!selUser.equals(Session.getUser())) {
							wtl("starting new session from onItemSelected, logged in");
							session = new Session(AllInOneV2.this, selUser, accounts.getString(selUser));
							drawer.closeMenu(true);
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
        
        ((Button) drawer.findViewById(R.id.dwrBoardJumper)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(true);
				session.get(NetDesc.BOARD_JUMPER, "/boards", null);
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrAMPList)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(true);
				session.get(NetDesc.AMP_LIST, buildAMPLink(), null);
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrTrackedTopics)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(true);
				session.get(NetDesc.TRACKED_TOPICS, "/boards/tracked", null);
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrPMInbox)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(true);
				session.get(NetDesc.PM_INBOX, "/pm/", null);
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrOpenInBrowser)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(false);
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(session.getLastPath()));
				startActivity(browserIntent);
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrHighlightList)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(false);
				startActivity(new Intent(AllInOneV2.this, SettingsHighlightedUsers.class));
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrSettings)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawer.closeMenu(false);
	        	startActivity(new Intent(AllInOneV2.this, SettingsMain.class));
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrODBugRep)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDemandBugReport();
			}
		});
        
        ((Button) drawer.findViewById(R.id.dwrExit)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AllInOneV2.this.finish();
			}
		});
        
        // The drawable that replaces the up indicator in the action bar
        if (usingLightTheme)
        	drawer.setSlideDrawable(R.drawable.ic_drawer_light);
        else
        	drawer.setSlideDrawable(R.drawable.ic_drawer);
        // Whether the previous drawable should be shown
        drawer.setDrawerIndicatorEnabled(true);
        
        if (!settings.contains("defaultAccount")) {
        	// settings need to be set to default
        	PreferenceManager.setDefaultValues(this, R.xml.settingsmain, false);
        	Editor sEditor = settings.edit();
            sEditor.putString("defaultAccount", SettingsMain.NO_DEFAULT_ACCOUNT)
                   .putString("timezone", TimeZone.getDefault().getID())
                   .commit();
        }
        else {
        	if (!settings.contains("accsDoNotNeedConversion")) {
        		// accs need conversion from static SALT to UUID system
        		HashMap<String, String> accs = new HashMap<String, String>();
        		SecurePreferences oldAccs = new SecurePreferences(getApplicationContext(), ACCOUNTS_PREFNAME, SALT_FALLBACK, false);
        		for (String s : oldAccs.getKeys()) {
        			accs.put(s, oldAccs.getString(s));
        		}
        		secureSalt = UUID.randomUUID().toString();
        		settings.edit().putString("secureSalt", secureSalt).commit();
        		SecurePreferences newAccs = new SecurePreferences(getApplicationContext(), ACCOUNTS_PREFNAME, secureSalt, false);
        		for (Map.Entry<String, String> e : accs.entrySet()) {
        			newAccs.put(e.getKey(), e.getValue());
        		}
        	}
        }
        settings.edit().putBoolean("accsDoNotNeedConversion", true).commit();
        
        aBar = getSupportActionBar();
        aBar.setDisplayHomeAsUpEnabled(true);
        aBar.setDisplayShowTitleEnabled(false);
        aBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
    	wtl("logging started from onCreate");
    	
    	wtl("getting accounts");
    	
    	if (settings.contains("secureSalt"))
    		secureSalt = settings.getString("secureSalt", null);
    	else {
    		secureSalt = UUID.randomUUID().toString();
    		settings.edit().putString("secureSalt", secureSalt).commit();
    	}
        accounts = new SecurePreferences(getApplicationContext(), ACCOUNTS_PREFNAME, secureSalt, false);

    	wtl("getting all the views");
        
//        contentPTR = (PullToRefreshScrollView) findViewById(R.id.aioScroller);
//        contentPTR.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
//			@Override
//			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
//				refreshClicked(contentPTR);
//			}
//		});
//        contentScroller = contentPTR.getRefreshableView();
        
        contentPTR = PullToRefreshAttacher.get(this);
        PullToRefreshLayout ptrLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
        ptrLayout.setPullToRefreshAttacher(contentPTR, new OnRefreshListener() {
			@Override
			public void onRefreshStarted(View view) {
				refreshClicked(view);
			}
		});
        
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        contentScroller = (ScrollView) findViewById(R.id.aioScroller);
        contentScroller.addView(content);

        titleWrapper  = (LinearLayout) findViewById(R.id.aioPostTitleWrapper);
        postTitle = (EditText) findViewById(R.id.aioPostTitle);
        postBody = (EditText) findViewById(R.id.aioPostBody);
        titleCounter = (TextView) findViewById(R.id.aioPostTitleCounter);
        bodyCounter = (TextView) findViewById(R.id.aioPostBodyCounter);
        
        title = (TextView) findViewById(R.id.aioTitle);
        title.setSelected(true);
        
        pageJumperWrapper = findViewById(R.id.aioPageJumperWrapper);
        firstPage = (Button) findViewById(R.id.aioFirstPage);
        firstPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				session.get(pageJumperDesc, firstPageUrl, null);
			}
		});
        prevPage = (Button) findViewById(R.id.aioPreviousPage);
        prevPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				session.get(pageJumperDesc, prevPageUrl, null);
			}
		});
        nextPage = (Button) findViewById(R.id.aioNextPage);
        nextPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				session.get(pageJumperDesc, nextPageUrl, null);
			}
		});
        lastPage = (Button) findViewById(R.id.aioLastPage);
        lastPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				session.get(pageJumperDesc, lastPageUrl, null);
			}
		});
        pageLabel = (TextView) findViewById(R.id.aioPageLabel);
        
        postTitle.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String escapedTitle = StringEscapeUtils.escapeHtml4(postTitle.getText().toString());
				titleCounter.setText(escapedTitle.length() + "/80");
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
				String escapedBody = StringEscapeUtils.escapeHtml4(postBody.getText().toString());
				// GFAQs adds 13(!) characters onto bodies when they have a sig, apparently.
				int length = escapedBody.length() + getSig().length() + 13;
				bodyCounter.setText(length + "/4096");
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
        
        postButton = (Button) findViewById(R.id.aioPostDo);
        cancelButton = (Button) findViewById(R.id.aioPostCancel);
        pollButton = (Button) findViewById(R.id.aioPollOptions);
        pollSep = findViewById(R.id.aioPollSep);
        
        postWrapper = (LinearLayout) findViewById(R.id.aioPostWrapper);

    	wtl("creating default sig");
		defaultSig = "Posted with GameRaven *grver*";

    	wtl("getting css directory");
		File cssDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gameraven");
    	if (!cssDirectory.exists()) {
        	wtl("css directory does not exist, creating");
    		cssDirectory.mkdir();
    	}
    	
    	wtl("starting db creation");
    	hlDB = new HighlightListDBHelper(this);
    	
    	wtl("setting check for update flag");
    	needToCheckForUpdate = true;
    	
		wtl("onCreate finishing");
    }
	
	public void disableNavList() {
		drawer.findViewById(R.id.dwrNavWrapper).setVisibility(View.GONE);
	}
	
	public void setNavList(boolean isLoggedIn) {
		drawer.findViewById(R.id.dwrNavWrapper).setVisibility(View.VISIBLE);
		if (isLoggedIn) 
			drawer.findViewById(R.id.dwrLoggedInNav).setVisibility(View.VISIBLE);
		else 
			drawer.findViewById(R.id.dwrLoggedInNav).setVisibility(View.GONE);
	}
	
	@Override
    public boolean onSearchRequested() {
    	if (searchIcon.isVisible())
    		searchIcon.expandActionView();
		return false;
    }
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_MENU) {
	        drawer.toggleMenu();
	        return true;
	    } else {
	        return super.onKeyUp(keyCode, event);
	    }
	}
	
	/** Adds menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        searchIcon = menu.getItem(0);
        topicListIcon = menu.getItem(1);
        addFavIcon = menu.getItem(2);
        remFavIcon = menu.getItem(3);
        postIcon = menu.getItem(4);
        refreshIcon = menu.getItem(5);
        
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchIcon.getActionView();
        if (null != searchView )
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));  
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() 
        {
            public boolean onQueryTextChange(String newText) 
            {
                // just do default
                return false;
            }

            public boolean onQueryTextSubmit(String query) 
            {
      	      HashMap<String, String> data = new HashMap<String, String>();
      	      if (session.getLastDesc() == NetDesc.BOARD) {
      	    	  wtl("searching board for query");
      	    	  data.put("search", query);
          	      session.get(NetDesc.BOARD, session.getLastPathWithoutData(), data);
      	      }
      	      else if (session.getLastDesc() == NetDesc.BOARD_JUMPER || session.getLastDesc() == NetDesc.GAME_SEARCH) {
      	    	  wtl("searching for games");
      	    	  data.put("game", query);
          	      session.get(NetDesc.GAME_SEARCH, "/search/index.html", data);
      	      }
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        
        return true;
    }
    
    /** fires when a menu option is selected */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
        	wtl("toggling drawer");
        	drawer.toggleMenu();
        	return true;
        	
        case R.id.search:
        	searchIcon.expandActionView();
        	return true;
        	
        case R.id.addFav:
        	AlertDialog.Builder afb = new AlertDialog.Builder(this);
        	afb.setNegativeButton("No", null);
        	
        	switch (fMode) {
			case ON_BOARD:
				afb.setTitle("Add Board to Favorites?");
	        	afb.setPositiveButton("Yes", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String addFavUrl = session.getLastPath();
			        	if (addFavUrl.contains("remfav"))
			        		addFavUrl = addFavUrl.replace("remfav", "addfav");
			        	else if (addFavUrl.indexOf('?') != -1)
			        		addFavUrl += "&action=addfav";
			        	else
			        		addFavUrl += "?action=addfav";
			        	
			        	session.get(NetDesc.BOARD, addFavUrl, null);
					}
				});
				break;
			case ON_TOPIC:
				afb.setTitle("Track Topic?");
	        	afb.setPositiveButton("Yes", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String addFavUrl = session.getLastPath();
						int x = addFavUrl.indexOf('#');
						if (x != -1)
							addFavUrl = addFavUrl.substring(0, x);
						
			        	if (addFavUrl.contains("stoptrack"))
			        		addFavUrl = addFavUrl.replace("stoptrack", "tracktopic");
			        	else if (addFavUrl.indexOf('?') != -1)
			        		addFavUrl += "&action=tracktopic";
			        	else
			        		addFavUrl += "?action=tracktopic";
			        	
			        	session.get(NetDesc.TOPIC, addFavUrl, null);
					}
				});
				break;
        	}
        	
        	afb.create().show();
        	
        	return true;
        	
        case R.id.remFav:
        	AlertDialog.Builder rfb = new AlertDialog.Builder(this);
        	rfb.setNegativeButton("No", null);
        	
        	switch (fMode) {
			case ON_BOARD:
				rfb.setTitle("Remove Board from Favorites?");
	        	rfb.setPositiveButton("Yes", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String remFavUrl = session.getLastPath();
			        	if (remFavUrl.contains("addfav"))
			        		remFavUrl = remFavUrl.replace("addfav", "remfav");
			        	else if (remFavUrl.indexOf('?') != -1)
			        		remFavUrl += "&action=remfav";
			        	else
			        		remFavUrl += "?action=remfav";
			        	
			        	session.get(NetDesc.BOARD, remFavUrl, null);
					}
				});
				break;
			case ON_TOPIC:
				rfb.setTitle("Stop Tracking Topic?");
				rfb.setPositiveButton("Yes", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String remFavUrl = session.getLastPath();
						int x = remFavUrl.indexOf('#');
						if (x != -1)
							remFavUrl = remFavUrl.substring(0, x);
						
			        	if (remFavUrl.contains("tracktopic"))
			        		remFavUrl = remFavUrl.replace("tracktopic", "stoptrack");
			        	else if (remFavUrl.indexOf('?') != -1)
			        		remFavUrl += "&action=stoptrack";
			        	else
			        		remFavUrl += "?action=stoptrack";
			        	
			        	session.get(NetDesc.TOPIC, remFavUrl, null);
					}
				});
				break;
        	}
        	
        	rfb.create().show();
        	
        	return true;
        	
        case R.id.topiclist:
        	session.get(NetDesc.BOARD, tlUrl, null);
        	return true;
        	
        case R.id.post:
        	if (pMode == PostMode.ON_BOARD)
        		postOnBoardSetup();
        	else if (pMode == PostMode.ON_TOPIC)
            	postOnTopicSetup();
        	else if (pMode == PostMode.NEW_PM)
        		pmSetup(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        	
        	return true;
        	
        case R.id.refresh:
        	if (session.getLastPath() == null) {
    			if (Session.isLoggedIn()) {
					wtl("starting new session from case R.id.refresh, logged in");
    				session = new Session(this, Session.getUser(), accounts.getString(Session.getUser()));
    			}
    	    	else {
					wtl("starting new session from R.id.refresh, no login");
    	    		session = new Session(this);
    	    	}
    		}
    		else
    			session.refresh();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	@Override
    public void onResume() {
    	wtl("onResume fired");
    	super.onResume();
		
    	contentPTR.setEnabled(settings.getBoolean("enablePTR", false));
		
		accentColor = settings.getInt("accentColor", (getResources().getColor(R.color.holo_blue)));
		float[] hsv = new float[3];
		Color.colorToHSV(accentColor, hsv);
		if (settings.getBoolean("useWhiteAccentText", false)) {
			// color is probably dark
			if (hsv[2] > 0)
				hsv[2] *= 1.2f;
			else
				hsv[2] = 0.2f;
			
			accentTextColor = Color.WHITE;
			isAccentLight = false;
		}
		else {
			// color is probably bright
			hsv[2] *= 0.8f;
			accentTextColor = Color.BLACK;
			isAccentLight = true;
		}
		
		int msgSelectorColor = Color.HSVToColor(hsv);
		
		msgHeadSelector = new StateListDrawable();
		msgHeadSelector.addState(new int[] {android.R.attr.state_focused}, new ColorDrawable(msgSelectorColor));
		msgHeadSelector.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(msgSelectorColor));
		msgHeadSelector.addState(StateSet.WILD_CARD, new ColorDrawable(accentColor));

		selector = new StateListDrawable();
		selector.addState(new int[] {android.R.attr.state_focused}, new ColorDrawable(accentColor));
		selector.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(accentColor));
		selector.addState(StateSet.WILD_CARD, new ColorDrawable(Color.TRANSPARENT));
		

		findViewById(R.id.aioTopSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPJTopSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioFirstPrevSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioNextLastSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPostWrapperSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPostTitleSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPostBodySep).setBackgroundColor(accentColor);
		findViewById(R.id.aioBoldSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioItalicSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioCodeSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioSpoilerSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioCiteSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioHTMLSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPostButtonSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPollSep).setBackgroundColor(accentColor);
		findViewById(R.id.aioPostSep).setBackgroundColor(accentColor);
		drawer.findViewById(R.id.dwrCAHSep).setBackgroundColor(accentColor);
		drawer.findViewById(R.id.dwrNavSep).setBackgroundColor(accentColor);
		drawer.findViewById(R.id.dwrFuncSep).setBackgroundColor(accentColor);
		
		if (session != null) {
			if (settings.getBoolean("reloadOnResume", false)) {
				wtl("session exists, reload on resume is true, refreshing page");
	    		session.refresh();
			}
    	}
		else {
    		String defaultAccount = settings.getString("defaultAccount", SettingsMain.NO_DEFAULT_ACCOUNT);
    		if (accounts.containsKey(defaultAccount)) {
				wtl("starting new session from onResume, logged in");
    			session = new Session(this, defaultAccount, accounts.getString(defaultAccount));
    		}
    		else {
				wtl("starting new session from onResume, no login");
    			session = new Session(this);
    		}
    	}
    	
    	String[] keys = accounts.getKeys();
		
		final String[] usernames = new String[keys.length + 1];
		usernames[0] = "Log Out";
		for (int i = 1; i < usernames.length; i++)
			usernames[i] = keys[i - 1].toString();
		
		int selected = 0;
		for (int x = 1; x < usernames.length; x++)
		{
			if (usernames[x].equals(Session.getUser())) 
				selected = x;
		}
		
		((Spinner) drawer.findViewById(R.id.dwrChangeAcc)).
			setAdapter(new ArrayAdapter<String>(this, R.layout.dropdown_item, usernames));
		
		((Spinner) drawer.findViewById(R.id.dwrChangeAcc)).setSelection(selected);
		
		if (needToCheckForUpdate && isReleaseBuild) {
			needToCheckForUpdate = false;
			wtl("starting update check");
			session.devCheckForUpdate();
		}
		
		title.setSelected(true);
		
		NotifierService.dismissNotif(this);
		wtl("onResume finishing");
    }
	
	public void postError(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg);
		builder.setTitle("There was a problem with your post...");
		builder.setPositiveButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();

		uiCleanup();
	}
	
	public void fourOhError(int errorNum, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		msg = msg.replace("..", ".");
		builder.setMessage(msg);
		builder.setTitle(errorNum + " Error");
		builder.setPositiveButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();

		uiCleanup();
	}
	
	public void timeoutCleanup(NetDesc desc) {
		if (desc != NetDesc.DEV_UPDATE_CHECK) {
			String msg = "timeout msg unset";
			String title = "timeout title unset";
			String posButtonText = "pos button text not set";
			boolean retrySub = false;
			switch (desc) {
			case LOGIN_S1:
			case LOGIN_S2:
				title = "Login Timeout";
				msg = "Login timed out, press retry to try again.";
				posButtonText = "Retry";
				break;
			case POSTMSG_S1:
			case POSTMSG_S2:
			case POSTMSG_S3:
			case POSTTPC_S1:
			case POSTTPC_S2:
			case POSTTPC_S3:
			case QPOSTMSG_S1:
			case QPOSTMSG_S3:
			case QPOSTTPC_S1:
			case QPOSTTPC_S3:
				title = "Post Timeout";
				msg = "Post timed out. Press refresh to check if your post made it through.";
				posButtonText = "Refresh";
				break;
			default:
				retrySub = true;
				title = "Timeout";
				msg = "Connection timed out, press retry to try again.";
				posButtonText = "Retry";
				break;

			}
			final boolean retry = retrySub;
			
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setTitle(title);
			b.setMessage(msg);
			b.setPositiveButton(posButtonText, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (retry)
						session.get(session.getLastAttemptedDesc(), session.getLastAttemptedPath(), null);
					else
						refreshClicked(new View(AllInOneV2.this));
				}
			});
			b.setNegativeButton("Dismiss", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					processContent(session.getLastRes(), session.getLastDesc());
					postExecuteCleanup(session.getLastDesc());
				}
			});
			b.create().show();
		}
	}

	private void uiCleanup() {
		contentPTR.setRefreshing(false);
		if (refreshIcon != null)
			refreshIcon.setVisible(true);
		if (postWrapper.getVisibility() == View.VISIBLE) {
			postButton.setEnabled(true);
			cancelButton.setEnabled(true);
			pollButton.setEnabled(true);
			if (postIcon != null)
				postIcon.setVisible(true);
		}
	}
	
	public void preExecuteSetup(NetDesc desc) {
		wtl("GRAIO dPreES fired --NEL, desc: " + desc.name());
		contentPTR.setRefreshing(true);
		if (refreshIcon != null)
			refreshIcon.setVisible(false);
		if (searchIcon != null)
			searchIcon.setVisible(false);
		if (postIcon != null)
			postIcon.setVisible(false);
		if (addFavIcon != null)
			addFavIcon.setVisible(false);
		if (remFavIcon != null)
			remFavIcon.setVisible(false);
		if (topicListIcon != null)
			topicListIcon.setVisible(false);
		
		if (desc != NetDesc.POSTMSG_S1 && desc != NetDesc.POSTTPC_S1 &&
			desc != NetDesc.QPOSTMSG_S1 && desc != NetDesc.QPOSTTPC_S1 &&
			desc != NetDesc.QEDIT_MSG)
				postCleanup();
	}
	
	private void postCleanup() {
		if (postWrapper.getVisibility() == View.VISIBLE) {
			pageJumperWrapper.setVisibility(View.VISIBLE);
			wtl("postCleanup fired --NEL");
			postWrapper.setVisibility(View.GONE);
			pollButton.setVisibility(View.GONE);
			pollSep.setVisibility(View.GONE);
			postBody.setText(null);
			postTitle.setText(null);
			clearPoll();
			messageIDForEditing = null;
			postPostUrl = null;
			content.requestFocus();
			
			((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).
									hideSoftInputFromWindow(postBody.getWindowToken(), 0);
		}
	}
	
	public void setAMPLinkVisible(boolean visible) {
		if (visible)
			findViewById(R.id.dwrAMPWrapper).setVisibility(View.VISIBLE);
		else
			findViewById(R.id.dwrAMPWrapper).setVisibility(View.GONE);
	}
	
	/********************************************
	 * START HNR
	 * ******************************************/
	
	@SuppressLint("SetJavaScriptEnabled")
	public void processContent(Response res, NetDesc desc) {
		
		wtl("GRAIO hNR fired, desc: " + desc.name());
		
		contentPTR.setEnabled(false);
		contentScroller.scrollTo(0, 0);

		searchIcon.setVisible(false);
		searchIcon.collapseActionView();
		
		postIcon.setVisible(false);
		
		addFavIcon.setVisible(false);
		remFavIcon.setVisible(false);
		
		topicListIcon.setVisible(false);
		
		
		try {
			if (res != null) {
				wtl("res is not null");

				wtl("parsing res");
				Document pRes = res.parse();
				String resUrl = res.url().toString();
				
				if (desc == NetDesc.DEV_UPDATE_CHECK) {
					wtl("GRAIO hNR determined this is update check response");
					
					String version = pRes.getElementById("contentsinner").ownText();
					version = version.trim();
					wtl("latest version is " + version);
					if (!version.equals(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName)) {
						wtl("version does not match current version, showing update dialog");
						showDialog(NEW_VERSION_DIALOG);
					}
					
					wtl("GRAIO hNR finishing via return, desc: " + desc.name());
					return;
				}

				wtl("setting board, topic, message id to null");
				boardID = null;
				topicID = null;
				messageIDForEditing = null;
				
				content.removeAllViews();
				
				Element tbody;
				Element pj = null;
				String headerTitle;
				String firstPage = null;
				String prevPage = null;
				String currPage = "1";
				String pageCount = "1";
				String nextPage = null;
				String lastPage = null;
				
				Element pmInboxLink = pRes.select("div.masthead_user").first().select("a[href=/pm/]").first();
				if (pmInboxLink != null && desc != NetDesc.PM_INBOX) {
					if (!pmInboxLink.text().equals("Inbox")) {
						Toast.makeText(this, "You have unread PM(s)", Toast.LENGTH_SHORT).show();
					}
				}
				
				Element trackedLink = pRes.select("div.masthead_user").first().select("a[href=/boards/tracked]").first();
				if (trackedLink != null && desc != NetDesc.TRACKED_TOPICS) {
					if (!trackedLink.text().equals("Topics")) {
						Toast.makeText(this, "You have unread tracked topic(s)", Toast.LENGTH_SHORT).show();
					}
				}
				
				switch (desc) {
				case BOARD_JUMPER:
				case LOGIN_S2:
					updateHeaderNoJumper("Board Jumper", NetDesc.BOARD_JUMPER);
					content.addView(new HeaderView(this, "Announcements"));
					
					searchIcon.setVisible(true);
					
					processBoards(pRes, true);
					break;
					
				case BOARD_LIST:
					updateHeaderNoJumper(pRes.getElementsByTag("th").get(4).text(), NetDesc.BOARD_LIST);
					processBoards(pRes, true);
					break;
					
				case PM_INBOX:
					tbody = pRes.getElementsByTag("tbody").first();

					headerTitle = Session.getUser() + "'s PM Inbox";
					
					if (tbody != null) {
						pj = pRes.select("ul.paginate").first();
						
						if (pj != null) {
							String pjText = pj.child(0).text();
							if (pjText.contains("Previous"))
								pjText = pj.child(1).text();
							//Page 1 of 5
							int currPageStart = 5;
							int ofIndex = pjText.indexOf(" of ");
							currPage = pjText.substring(currPageStart, ofIndex);
							int pageCountEnd = pjText.length();
							pageCount = pjText.substring(ofIndex + 4, pageCountEnd);
							int currPageNum = Integer.parseInt(currPage);
							int pageCountNum = Integer.parseInt(pageCount);
							
							if (currPageNum > 1) {
								firstPage = "/pm/";
								prevPage = "/pm/?page=" + (currPageNum - 2);
							}
							if (currPageNum != pageCountNum) {
								nextPage = "/pm/?page=" + currPageNum;
								lastPage = "/pm/?page=" + (pageCountNum - 1);
							}
						}
						
						updateHeader(headerTitle, firstPage, prevPage, currPage, 
									 pageCount, nextPage, lastPage, NetDesc.PM_INBOX);
						
						for (Element row : tbody.getElementsByTag("tr")) {
							Elements cells = row.children();
							// [icon] [sender] [subject] [time] [check]
							boolean isOld = true;
							if (cells.get(0).children().first().hasClass("icon-circle"))
								isOld = false;
							String sender = cells.get(1).text();
							Element subjectLinkElem = cells.get(2).children().first();
							String subject = subjectLinkElem.text();
							String link = subjectLinkElem.attr("href");
							String time = cells.get(3).text();
							
							PMView pm = new PMView(this, subject, sender, time, link);
							pm.setOnClickListener(cl);
							
							if (isOld)
								pm.setOld();
							
							content.addView(pm);
						}
					}
					else {
						updateHeaderNoJumper(headerTitle, NetDesc.PM_INBOX);
						content.addView(new HeaderView(this, "You have no private messages at this time."));
					}
					
					postIcon.setVisible(true);
					pMode = PostMode.NEW_PM;
					break;
//					
				case READ_PM:
					headerTitle = pRes.select("h2.title").first().text();
					String pmTitle = headerTitle;
					if (!pmTitle.startsWith("Re: "))
						pmTitle = "Re: " + pmTitle;

					String pmMessage = pRes.select("div.body").first().outerHtml();
					
					Element foot = pRes.select("div.foot").first();
					foot.child(1).remove();
					String pmFoot = foot.outerHtml();
					
					//Sent by: P4wn4g3 on 6/1/2013 2:15:55 PM
					String footText = foot.text();
					
					String sender = footText.substring(9, footText.indexOf(" on "));
					
					updateHeaderNoJumper(pmTitle, NetDesc.READ_PM);
					
					PMDetailView pmd = new PMDetailView(this, sender, pmTitle, pmMessage + pmFoot);
					pmd.setOnClickListener(cl);
					content.addView(pmd);
					break;
					
				case AMP_LIST:
					wtl("GRAIO hNR determined this is an amp response");
					
					tbody = pRes.getElementsByTag("tbody").first();
					
					headerTitle = Session.getUser() + "'s Active Messages";
					
					if (pRes.select("ul.paginate").size() > 1) {
						pj = pRes.select("ul.paginate").get(1);
						if (pj != null && !pj.hasClass("user")
								&& !pj.hasClass("tsort")) {
							int x = 0;
							String pjText = pj.child(x).text();
							while (pjText.contains("First")
									|| pjText.contains("Previous")) {
								x++;
								pjText = pj.child(x).text();
							}
							// Page 2 of 3
							int currPageStart = 5;
							int ofIndex = pjText.indexOf(" of ");
							currPage = pjText.substring(currPageStart, ofIndex);
							int pageCountEnd = pjText.length();
							pageCount = pjText.substring(ofIndex + 4,
									pageCountEnd);
							int currPageNum = Integer.parseInt(currPage);
							int pageCountNum = Integer.parseInt(pageCount);

							String amp = buildAMPLink();
							if (currPageNum > 1) {
								firstPage = amp;
								prevPage = amp + "&page=" + (currPageNum - 2);
							}
							if (currPageNum != pageCountNum) {
								nextPage = amp + "&page=" + currPageNum;
								lastPage = amp + "&page=" + (pageCountNum - 1);
							}
						}
					}
					updateHeader(headerTitle, firstPage, prevPage, currPage, 
							pageCount, nextPage, lastPage, NetDesc.AMP_LIST);
					
					if (!tbody.children().isEmpty()) {
						if (settings.getBoolean("notifsEnable", false) && 
								Session.getUser().equals(settings.getString("defaultAccount", SettingsMain.NO_DEFAULT_ACCOUNT))) {
							Element lPost = pRes.select("td.lastpost").first();
							if (lPost != null) {
								String lTime = lPost.text();
								Date newDate;
								lTime = lTime.replace("Last:", EMPTY_STRING);
								if (lTime.contains("AM") || lTime.contains("PM"))
									newDate = new SimpleDateFormat("MM'/'dd hh':'mmaa", Locale.US).parse(lTime);
								else
									newDate = new SimpleDateFormat("MM'/'dd'/'yyyy", Locale.US).parse(lTime);
								
								long newTime = newDate.getTime();
								long oldTime = settings.getLong("notifsLastPost", 0);
								if (newTime > oldTime) {
									wtl("time is newer");
									settings.edit().putLong("notifsLastPost", newTime).commit();
								}
							}
						}
						
						for (Element row : tbody.children()) {
							// [board] [title] [msg] [last post] [your last post]
							Elements cells = row.children();
							String board = cells.get(0).text();
							Element titleLinkElem = cells.get(1).children()
									.first();
							String title = titleLinkElem.text();
							String link = titleLinkElem.attr("href");
							String mCount = cells.get(2).textNodes().get(0).text().trim();
							Element lPostLinkElem = cells.get(3).children().get(1);
							String lPost = lPostLinkElem.text();
							String lPostLink = lPostLinkElem.attr("href");

							TopicView topic = new TopicView(this, title, board,
									lPost, mCount, link, TopicViewType.NORMAL, 0);

							topic.setOnClickListener(cl);
							topic.setOnLongClickListener(cl);

							LinkButtonView lp = (LinkButtonView) topic.findViewById(R.id.tvLastPostLink);
							lp.setUrlAndType(lPostLink, Type.LAST_POST);
							lp.setOnClickListener(cl);

							content.addView(topic);
						}
					}
					else {
						content.addView(new HeaderView(this, "You have no active messages at this time."));
					}
					
					wtl("amp response block finished");
					break;
					
				case TRACKED_TOPICS:
					headerTitle = Session.getUser() + "'s Tracked Topics";
					updateHeaderNoJumper(headerTitle, desc);
					tbody = pRes.getElementsByTag("tbody").first();
					
					if (tbody != null) {
						for (Element row : tbody.children()) {
							// [remove] [title] [board name] [msgs] [last [pst]
							Elements cells = row.children();
							String removeLink = cells.get(0).child(0)
									.attr("href");
							String topicLink = cells.get(1).child(0)
									.attr("href");
							String topicText = cells.get(1).text();
							String board = cells.get(2).text();
							String msgs = cells.get(3).text();
							String lPostLink = cells.get(4).child(0)
									.attr("href");
							String lPostText = cells.get(4).text();

							TrackedTopicView tt = new TrackedTopicView(this,
									board, topicText, lPostText, msgs,
									topicLink);
							tt.setOnClickListener(cl);

							LinkButtonView st = (LinkButtonView) tt
									.findViewById(R.id.ttStopTracking);
							st.setUrlAndType(removeLink, Type.STOP_TRACK);
							st.setOnClickListener(cl);

							LinkButtonView lp = (LinkButtonView) tt
									.findViewById(R.id.ttLastPostLink);
							lp.setUrlAndType(lPostLink, Type.LAST_POST);
							lp.setOnClickListener(cl);

							content.addView(tt);
						}
					}
					else {
						content.addView(new HeaderView(this, "You have no tracked topics at this time."));
					}
					break;
					
				case BOARD:
					wtl("GRAIO hNR determined this is a board response");
					
					wtl("setting board id");
					boardID = parseBoardID(resUrl);
					
					boolean isSplitList = false;
					if (pRes.getElementsByTag("th").first() != null) {
						if (pRes.getElementsByTag("th").first().text().equals("Board Title")) {
							wtl("is actually a split board list");
							
							updateHeaderNoJumper(pRes.select("h1.page-title").first().text(), NetDesc.BOARD);
							
							processBoards(pRes, false);
							
							isSplitList = true;
						}
					}
					
					if (!isSplitList) {
						String url = resUrl;
						String searchQuery = EMPTY_STRING;
						String searchPJAddition = EMPTY_STRING;
						if (url.contains("search=")) {
							wtl("board search url: " + url);
							searchQuery = url.substring(url.indexOf("search=") + 7);
							int i = searchQuery.indexOf('&');
							if (i != -1)
								searchQuery.replace(searchQuery.substring(i), EMPTY_STRING);
							
							searchPJAddition = "&search=" + searchQuery;
							searchQuery = URLDecoder.decode(searchQuery);
						}
						
						Element headerElem = pRes.getElementsByClass("page-title").first();
						if (headerElem != null)
							headerTitle = headerElem.text();
						else
							headerTitle = "GFAQs Cache Error, Board Title Not Found";
						
						if (searchQuery.length() > 0) 
							headerTitle += " (search: " + searchQuery + ")";
						
						if (pRes.select("ul.paginate").size() > 1) {
							pj = pRes.select("ul.paginate").get(1);
							if (pj != null && !pj.hasClass("user")) {
								int x = 0;
								String pjText = pj.child(x).text();
								while (pjText.contains("First")
										|| pjText.contains("Previous")) {
									x++;
									pjText = pj.child(x).text();
								}
								// Page [dropdown] of 3
								// Page 1 of 3
								int ofIndex = pjText.indexOf(" of ");
								int currPageStart = 5;
								if (pj.getElementsByTag("select").isEmpty())
									currPage = pjText.substring(currPageStart,
											ofIndex);
								else
									currPage = pj
											.select("option[selected=selected]")
											.first().text();

								int pageCountEnd = pjText.length();
								pageCount = pjText.substring(ofIndex + 4,
										pageCountEnd);
								int currPageNum = Integer.parseInt(currPage);
								int pageCountNum = Integer.parseInt(pageCount);

								if (currPageNum > 1) {
									firstPage = "boards/" + boardID + "?page=0"
											+ searchPJAddition;
									prevPage = "boards/" + boardID + "?page="
											+ (currPageNum - 2)
											+ searchPJAddition;
								}
								if (currPageNum != pageCountNum) {
									nextPage = "boards/" + boardID + "?page="
											+ currPageNum + searchPJAddition;
									lastPage = "boards/" + boardID + "?page="
											+ (pageCountNum - 1)
											+ searchPJAddition;
								}
							}
						}
						updateHeader(headerTitle, firstPage, prevPage, currPage, 
								pageCount, nextPage, lastPage, NetDesc.BOARD);
						
						searchIcon.setVisible(true);
						
						if (Session.isLoggedIn()) {
							String favtext = pRes.getElementsByClass("user").first().text().toLowerCase(Locale.US);
							if (favtext.contains("add to favorites")) {
								addFavIcon.setVisible(true);
								fMode = FavMode.ON_BOARD;
							}
							else if (favtext.contains("remove favorite")) {
								remFavIcon.setVisible(true);
								fMode = FavMode.ON_BOARD;
							}

							updatePostingRights(pRes, false);
						}
						
						Element splitList = pRes.select("p:contains(this is a split board)").first();
						if (splitList != null) {
							String splitListLink = splitList.child(0).attr("href");
							BoardView b = new BoardView(this, "This is a Split Board.", 
									"Click here to return to the Split List.", 
									null, null, null, splitListLink, BoardViewType.SPLIT);
							b.setOnClickListener(cl);
							content.addView(b);
						}
						
						Element table = pRes.select("table.board").first();
						if (table != null) {
							
							table.getElementsByTag("col").get(2).remove();
							table.getElementsByTag("th").get(2).remove();
							table.getElementsByTag("col").get(0).remove();
							table.getElementsByTag("th").get(0).remove();
							
							wtl("board row parsing start");
							boolean skipFirst = true;
							Set<String> hlUsers = hlDB.getHighlightedUsers().keySet();
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
									
									TopicViewType type = TopicViewType.NORMAL;
									if (!tImg.endsWith("topic")) {
										if (tImg.endsWith("closed"))
											type = TopicViewType.LOCKED;
										else if (tImg.endsWith("archived"))
											type = TopicViewType.ARCHIVED;
										else if (tImg.endsWith("poll"))
											type = TopicViewType.POLL;
										else if (tImg.endsWith("sticky"))
											type = TopicViewType.PINNED;
									}
									
									int hlColor = 0;
									if (hlUsers.contains(tc.toLowerCase(Locale.US))) {
										HighlightedUser hUser = hlDB.getHighlightedUsers().get(tc.toLowerCase(Locale.US));
										hlColor = hUser.getColor();
										tc += " (" + hUser.getLabel() + ")";
									}
									
									TopicView topic = new TopicView(this, title, tc, lastPost, mCount, tUrl, type, hlColor);
									topic.setOnClickListener(cl);
									LinkButtonView lp = (LinkButtonView) topic.findViewById(R.id.tvLastPostLink);
									lp.setUrlAndType(lpUrl, Type.LAST_POST);
									lp.setOnClickListener(cl);
									
									content.addView(topic);
								}
								else
									skipFirst = false;
							}
							wtl("board row parsing end");
						}
						else {
							content.addView(new HeaderView(this, "There are no topics at this time."));
						}
					}
					
					wtl("board response block finished");
					break;
					
				case TOPIC:
					boardID = parseBoardID(resUrl);
					topicID = parseTopicID(resUrl);

					tlUrl = "boards/" + boardID;
					wtl(tlUrl);
					topicListIcon.setVisible(true);
					
					Element headerElem = pRes.getElementsByClass("title").first();
					if (headerElem != null)
						headerTitle = headerElem.text();
					else
						headerTitle = "GFAQs Cache Error, Title Not Found";
					
					if (headerTitle.equals("Log In to GameFAQs")) {
						headerElem = pRes.getElementsByClass("title").get(1);
						if (headerElem != null)
							headerTitle = headerElem.text();
					}

					if (pRes.select("ul.paginate").size() > 1) {
						pj = pRes.select("ul.paginate").get(1);
						if (pj != null && !pj.hasClass("user")) {
							int x = 0;
							String pjText = pj.child(x).text();
							while (pjText.contains("First")
									|| pjText.contains("Previous")) {
								x++;
								pjText = pj.child(x).text();
							}
							// Page [dropdown] of 3
							// Page 1 of 3
							int ofIndex = pjText.indexOf(" of ");
							int currPageStart = 5;
							if (pj.getElementsByTag("select").isEmpty())
								currPage = pjText.substring(currPageStart,
										ofIndex);
							else
								currPage = pj
										.select("option[selected=selected]")
										.first().text();

							int pageCountEnd = pjText.length();
							pageCount = pjText.substring(ofIndex + 4,
									pageCountEnd);
							int currPageNum = Integer.parseInt(currPage);
							int pageCountNum = Integer.parseInt(pageCount);

							if (currPageNum > 1) {
								firstPage = "boards/" + boardID + "/" + topicID;
								prevPage = "boards/" + boardID + "/" + topicID
										+ "?page=" + (currPageNum - 2);
							}
							if (currPageNum != pageCountNum) {
								nextPage = "boards/" + boardID + "/" + topicID
										+ "?page=" + currPageNum;
								lastPage = "boards/" + boardID + "/" + topicID
										+ "?page=" + (pageCountNum - 1);
							}
						}
					}
					updateHeader(headerTitle, firstPage, prevPage, currPage, 
							pageCount, nextPage, lastPage, NetDesc.TOPIC);
					
					if (Session.isLoggedIn()) {
						String favtext = pRes.getElementsByClass("user").first().text().toLowerCase(Locale.US);
						if (favtext.contains("track topic")) {
							addFavIcon.setVisible(true);
							fMode = FavMode.ON_TOPIC;
						}
						else if (favtext.contains("stop tracking")) {
							remFavIcon.setVisible(true);
							fMode = FavMode.ON_TOPIC;
						}
						
						updatePostingRights(pRes, true);
					}
					
					Elements rows = pRes.select("table.board").first().getElementsByTag("tr");
					int rowCount = rows.size();
					
					MessageView message = null;
					Set<String> hlUsers = hlDB.getHighlightedUsers().keySet();
					for (int x = 0; x < rowCount; x++) {
						Element row = rows.get(x);
						
						String user = null;
						String postNum = null;
						String mID = null;
						String userTitles = EMPTY_STRING;
						String postTimeText = EMPTY_STRING;
						String postTime = EMPTY_STRING;
						Element msgBody = null;
						
						if (row.hasClass("left")) {
							// message poster display set to left of message
							
							Elements authorData = row.getElementsByClass("author_data");
							user = row.getElementsByTag("b").first().text();
							postNum = row.getElementsByTag("a").first().attr("name");
							
							for (int i = 1; i < authorData.size(); i++) {
								Element e = authorData.get(i);
								String t = e.text();
								if (t.startsWith("("))
									userTitles += " " + t;
								
								else if (e.hasClass("tag"))
									userTitles += " (tagged as " + t + ")";
								
								else if (t.startsWith("Posted"))
									postTime = t;
								
								else if (t.equals("message detail"))
									mID = parseMessageID(e.child(0).attr("href"));
							}
							
							msgBody = row.child(1).child(0);
						}
						else {
							// message poster display set to above message
							
							List<TextNode> textNodes = row.child(0).child(0).textNodes();
							Elements elements = row.child(0).child(0).children();
							
							int textNodesSize = textNodes.size();
							for (int y = 0; y < textNodesSize; y++) {
								String text = textNodes.get(y).text();
								if (text.startsWith("Posted"))
									postTimeText = text;
								else if (text.contains("(")) {
									userTitles += " " + text.substring(text.indexOf('('), text.lastIndexOf(')') + 1);
								}
							}
							
							user = elements.get(0).text();
							int anchorCount = row.getElementsByTag("a").size();
							postNum = row.getElementsByTag("a").get((anchorCount > 1 ? 1 : 0)).attr("name");
							int elementsSize = elements.size();
							for (int y = 0; y < elementsSize; y++) {
								Element e = elements.get(y);
								if (e.hasClass("tag"))
									userTitles += " (tagged as " + e.text() + ")";
								
								else if (e.text().equals("message detail"))
									mID = parseMessageID(e.attr("href"));
							}
							//Posted 11/15/2012 11:20:27&nbsp;AM | (edited) [if archived]
							if (postTimeText.contains("(edited)"))
								userTitles +=  " (edited)";
							
							int endPoint = postTimeText.indexOf('|') - 1;
							if (endPoint < 0)
								endPoint = postTimeText.length();
							postTime = postTimeText.substring(0, endPoint);
							
							x++;
							msgBody = rows.get(x).child(0).child(0);
						}
						
						int hlColor = 0;
						if (hlUsers.contains(user.toLowerCase(Locale.US))) {
							HighlightedUser hUser = hlDB
									.getHighlightedUsers().get(
											user.toLowerCase(Locale.US));
							hlColor = hUser.getColor();
							userTitles += " (" + hUser.getLabel() + ")";
						}
						
						message = new MessageView(this, user, userTitles,
								postNum, postTime, msgBody, boardID,
								topicID, mID, hlColor);
						content.addView(message);
					}
					
					final MessageView lastMessage = message;
					if (goToLastPost && !Session.applySavedScroll) {
						contentScroller.post(new Runnable() {
					        @Override
					        public void run() {
					        	contentScroller.smoothScrollTo(0, lastMessage.getTop());
					        }
					    });
					}
					
					break;
					
				case MESSAGE_DETAIL:
					updateHeaderNoJumper("Message Detail", NetDesc.MESSAGE_DETAIL);
					
					boardID = parseBoardID(resUrl);
					topicID = parseTopicID(resUrl);
					
					Elements msgDRows = pRes.getElementsByTag("tr");
					
					String user = msgDRows.first().child(0).child(0).text();
					
					content.addView(new HeaderView(this, "Current Version"));
					
					Element currRow;
					String postTime;
					String mID = parseMessageID(resUrl);
					for (int x = 0; x < msgDRows.size(); x++) {
						if (x == 1)
							content.addView(new HeaderView(this, "Previous Version(s)"));
						else {
							currRow = msgDRows.get(x);
							
							if (currRow.child(0).textNodes().size() > 1)
								postTime = currRow.child(0).textNodes().get(1).text();
							else
								postTime = currRow.child(0).textNodes().get(0).text();
							
							Element body = currRow.child(1);
							MessageView msg = new MessageView(this, user, null, null, postTime, body, boardID, topicID, mID, 0);
							msg.disableTopClick();
							content.addView(msg);
						}
					}
					
					break;
					
				case USER_DETAIL:
					wtl("starting user detail processing");
					tbody = pRes.select("table.board").first().getElementsByTag("tbody").first();
					Log.d("udtb", tbody.outerHtml());
					String name = null;
					String ID = null;
					String level = null;
					String creation = null;
					String lVisit = null;
					String sig = null;
					String karma = null;
					String AMP = null;
					for (Element row : tbody.children()) {
						String label = row.child(0).text().toLowerCase(Locale.US);
						wtl("user detail row label: " + label);
						if (label.equals("user name"))
							name = row.child(1).text();
						else if (label.equals("user id"))
							ID = row.child(1).text();
						else if (label.equals("board user level")) {
							level = row.child(1).html();
							wtl("set level: " + level);
						}
						else if (label.equals("account created"))
							creation = row.child(1).text();
						else if (label.equals("last visit"))
							lVisit = row.child(1).text();
						else if (label.equals("signature"))
							sig = row.child(1).html();
						else if (label.equals("karma"))
							karma = row.child(1).text();
						else if (label.equals("active messages posted"))
							AMP = row.child(1).text();
					}
					
					updateHeaderNoJumper(name + "'s Details", NetDesc.USER_DETAIL);
					
					UserDetailView userDetail = new UserDetailView(this, name, ID, level, creation, lVisit, sig, karma, AMP);
					content.addView(userDetail);
					break;
					
				case GAME_SEARCH:
					wtl("GRAIO hNR determined this is a game search response");

					String url = resUrl;
					wtl("game search url: " + url);
					
					String searchQuery = url.substring(url.indexOf("game=") + 5);
					int i = searchQuery.indexOf("&");
					if (i != -1)
						searchQuery = searchQuery.replace(searchQuery.substring(i), EMPTY_STRING);
					
					int pageIndex = url.indexOf("page=");
					if (pageIndex != -1) {
						currPage = url.substring(pageIndex + 5);
						i = currPage.indexOf("&");
						if (i != -1)
							currPage = currPage.replace(currPage.substring(i), EMPTY_STRING);
					}
					else {
						currPage = "0";
					}

					int currPageNum = Integer.parseInt(currPage);
					
					Element nextPageElem = null;
					
					if (!pRes.getElementsContainingOwnText("Next Page").isEmpty())
						nextPageElem = pRes.getElementsContainingOwnText("Next Page").first();
					
					pageCount = "page count not available";
					if (nextPageElem != null) {
						nextPage = "/search/index.html?game=" + searchQuery + "&page=" + (currPageNum + 1);
					}
					if (currPageNum > 0) {
						prevPage = "/search/index.html?game=" + searchQuery + "&page=" + (currPageNum - 1);
						firstPage = "/search/index.html?game=" + searchQuery + "&page=0";
					}
					
					headerTitle = "Searching games: " + URLDecoder.decode(searchQuery) + EMPTY_STRING;
					
					updateHeader(headerTitle, firstPage, prevPage, Integer.toString(currPageNum + 1), 
								 pageCount, nextPage, lastPage, NetDesc.GAME_SEARCH);
					
					searchIcon.setVisible(true);
					
					Elements gameSearchTables = pRes.select("table.results");
					int tCount = gameSearchTables.size();
					int tCounter = 0;
					if (!gameSearchTables.isEmpty()) {
						for (Element table : gameSearchTables) {
							tCounter++;
							if (tCounter < tCount)
								content.addView(new HeaderView(this, "Best Matches"));
							else
								content.addView(new HeaderView(this, "Good Matches"));
							
							String prevPlatform = EMPTY_STRING;
							
							wtl("board row parsing start");
							for (Element row : table.getElementsByTag("tr")) {
								Elements cells = row.getElementsByTag("td");
								// cells = [platform] [title] [faqs] [codes] [saves] [revs] [mygames] [q&a] [pics] [vids] [board]
								String platform = cells.get(0).text();
								String bName = cells.get(1).text();
								String bUrl = cells.get(10).child(0).attr("href");
								
								if (platform.codePointAt(0) == ('\u00A0')) {
									platform = prevPlatform;
								}
								else {
									prevPlatform = platform;
								}
								
								BoardView bv = new BoardView(this, platform, bName, bUrl);
								bv.setOnClickListener(cl);
								content.addView(bv);
							}
							
							wtl("board row parsing end");
						}
					}
					else {
						content.addView(new HeaderView(this, "No results."));
					}
					
					wtl("game search response block finished");
					break;
					
				default:
					wtl("GRAIO hNR determined response type is unhandled");
					title.setText("Page unhandled - " + resUrl);
					break;
				}
				
				adView = new WebView(this);
		        adView.getSettings().setJavaScriptEnabled(settings.getBoolean("enableJS", true));
		        
				content.addView(adView);
				
				String bgcolor;
				if (usingLightTheme)
		        	bgcolor = "#ffffff";
				else
					bgcolor = "#000000";
				
				StringBuilder adBuilder = new StringBuilder();
				adBuilder.append("<html><body bgcolor=\"" + bgcolor + "\">");
				for (Element e : pRes.getElementsByClass("ad")) {
					adBuilder.append(e.outerHtml());
				}
				adBuilder.append("</body></html>");
				adView.loadDataWithBaseURL(session.getLastPath(), adBuilder.toString(), "text/html", "iso-8859-1", null);

				contentPTR.setEnabled(settings.getBoolean("enablePTR", false));
				
				wtl("applysavedscroll: " + Session.applySavedScroll + ". savedscrollval: " + Session.savedScrollVal);
				if (Session.applySavedScroll) {
					contentScroller.post(new Runnable() {
				        @Override
				        public void run() {
				        	contentScroller.scrollTo(0, Session.savedScrollVal);
				        }
				    });
					Session.applySavedScroll = false;
				}
				
			}
			else {
				wtl("res is null");
				content.addView(new HeaderView(this, "You broke it. Somehow processContent was called with a null response."));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			tryCaught(res.url().toString(), desc.toString(), ExceptionUtils.getStackTrace(e), res.body());
			if (session.canGoBack())
				session.goBack(false);
		}
		catch (StackOverflowError e) {
			e.printStackTrace();
			tryCaught(res.url().toString(), desc.toString(), ExceptionUtils.getStackTrace(e), res.body());
			if (session.canGoBack())
				session.goBack(false);
		}
		
		if (contentPTR.isRefreshing())
			contentPTR.setRefreshComplete();
			
		wtl("GRAIO hNR finishing");
	}
	
	/***********************************
	 * END HNR
	 * *********************************/

	private void processBoards(Document pRes, boolean includeBoardCategories) {
		Elements homeTables = pRes.select("table.board");
		
		boolean skippedFirst = false;
		for (Element row : homeTables.first().getElementsByTag("tr")) {
			if (skippedFirst) {
				if (row.hasClass("head")) {
					content.addView(new HeaderView(this, row.text()));
				}
				else {
					// [title + link] [topics] [msgs] [last post]
					Elements cells = row.children();
					Element titleCell = cells.get(0);
					
					String title = titleCell.child(0).text();
					
					String boardDesc = null;
					if (titleCell.children().size() > 2)
						boardDesc = titleCell.child(2).text();
					
					String link = cells.get(0).children().first().attr("href");
					
					String tCount = null;
					String mCount = null;
					String lPost = null;
					
					BoardViewType bvt;
					
					if (cells.size() > 3) {
						tCount = cells.get(1).text();
						mCount = cells.get(2).text();
						lPost = cells.get(3).text();
						
						bvt = BoardViewType.NORMAL;
					}
					else
						bvt = BoardViewType.SPLIT;
					
					BoardView board = new BoardView(this, title, boardDesc, lPost, tCount, mCount, link, bvt);
					board.setOnClickListener(cl);
					
					content.addView(board);
				}
			}
			else {
				skippedFirst = true;
			}
		}
		
		if (includeBoardCategories) {
			int rowX = 0;
			for (Element row : homeTables.get(1).getElementsByTag("tr")) {
				rowX++;
				if (rowX > 2) {
					Element cell = row.child(0);
					String title = cell.child(0).text();
					String link = cell.child(0).attr("href");
					String boardDesc = cell.child(2).text();
					BoardView board = new BoardView(this, title, boardDesc,
							null, null, null, link, BoardViewType.LIST);
					board.setOnClickListener(cl);
					content.addView(board);
				} else {
					if (rowX == 1) {
						content.addView(new HeaderView(this,
								"Message Board Categories"));
					}
				}
			}
		}
	}
	
	private void updatePostingRights(Document pRes, boolean onTopic) {
		if (onTopic) {
			if (pRes.getElementsByClass("user").first().text().contains("Post New Message")) {
				postIcon.setVisible(true);
				pMode = PostMode.ON_TOPIC;
			}
		}
		else {
			if (pRes.getElementsByClass("user").first().text().contains("New Topic")) {
				postIcon.setVisible(true);
				pMode = PostMode.ON_BOARD;
			}
		}
	}
	
	public void postExecuteCleanup(NetDesc desc) {
		wtl("GRAIO dPostEC --NEL, desc: " + desc.name());
		contentPTR.setRefreshing(false);
		if (refreshIcon != null)
			refreshIcon.setVisible(true);
		if (desc == NetDesc.BOARD || desc == NetDesc.TOPIC)
			postCleanup();
	}
	
	private boolean goToLastPost = false;
	public class ClickListener implements View.OnClickListener, View.OnLongClickListener {
		@Override
		public void onClick(View v) {
			if (BoardView.class.isInstance(v)) {
				BoardView bv = (BoardView) v;
				if (bv.getType() == BoardViewType.LIST) {
					session.get(NetDesc.BOARD_LIST, bv.getUrl(), null);
				}
				else {
					session.get(NetDesc.BOARD, bv.getUrl(), null);
				}
			}
			else if (TopicView.class.isInstance(v)) {
				goToLastPost = false;
				session.get(NetDesc.TOPIC, ((TopicView)v).getUrl(), null);
			}
			else if (TrackedTopicView.class.isInstance(v)) {
				goToLastPost = false;
				session.get(NetDesc.TOPIC, ((TrackedTopicView)v).getUrl(), null);
			}
			else if (LinkButtonView.class.isInstance(v)) {
				if (((LinkButtonView) v).getType() == Type.LAST_POST) {
					goToLastPost = true;
					session.get(NetDesc.TOPIC, ((LinkButtonView) v).getUrl(), null);
				}
				else if (((LinkButtonView) v).getType() == Type.STOP_TRACK) {
					session.get(NetDesc.TRACKED_TOPICS, ((LinkButtonView) v).getUrl(), null);
				}
			}
			else if (PMView.class.isInstance(v)) {
				session.get(NetDesc.READ_PM, ((PMView)v).getUrl(), null);
			}
			else if (PMDetailView.class.isInstance(v)) {
				pmSetup(((PMDetailView)v).getSender(), ((PMDetailView)v).getTitle(), EMPTY_STRING);
			}
		}

		
		@Override
		public boolean onLongClick(View v) {
			String url = ((TopicView)v).getUrl();
			url = url.substring(0, url.lastIndexOf('/'));
			session.get(NetDesc.BOARD, url, null);
			return true;
		}
	}
	
	private void updateHeader(String titleIn, String firstPageIn, String prevPageIn, String currPage, 
							  String pageCount, String nextPageIn, String lastPageIn, NetDesc desc) {
		
		title.setText(titleIn);
		
		if (currPage.equals("-1")) {
			pageJumperWrapper.setVisibility(View.GONE);
		}
		else {
			pageJumperWrapper.setVisibility(View.VISIBLE);
			pageJumperDesc = desc;
			pageLabel.setText(currPage + " / " + pageCount);
			
			if (firstPageIn != null) {
				firstPageUrl = firstPageIn;
				firstPage.setEnabled(true);
			}
			else {
				firstPage.setEnabled(false);
			}
			
			if (prevPageIn != null) {
				prevPageUrl = prevPageIn;
				prevPage.setEnabled(true);
			}
			else {
				prevPage.setEnabled(false);
			}
			
			if (nextPageIn != null) {
				nextPageUrl = nextPageIn;
				nextPage.setEnabled(true);
			}
			else {
				nextPage.setEnabled(false);
			}
			
			if (lastPageIn != null) {
				lastPageUrl = lastPageIn;
				lastPage.setEnabled(true);
			}
			else {
				lastPage.setEnabled(false);
			}
		}
	}
	
	private void updateHeaderNoJumper(String title, NetDesc desc) {
		updateHeader(title, null, null, "-1", "-1", null, null, desc);
	}
	
	private MessageView clickedMsg;
	private String quoteSelection;
	public void messageMenuClicked(MessageView msg) {
		clickedMsg = msg;
		quoteSelection = clickedMsg.getSelection();
		if (quoteSelection != null)
			Toast.makeText(this, "Selected text prepped for quoting.", Toast.LENGTH_SHORT).show();
		
		showDialog(MESSAGE_ACTION_DIALOG);
	}
	
	private void editPostSetup(String msg, String msgID) {
		postBody.setText(msg);
		messageIDForEditing = msgID;
		postOnTopicSetup();
	}
	
	private void quoteSetup(String user, String msg) {
		wtl("quoteSetup fired");
		String quotedMsg = "<cite>" + user + " posted...</cite>\n" +
						   "<quote>" + msg + "</quote>\n\n";
		
		int start = Math.max(postBody.getSelectionStart(), 0);
		int end = Math.max(postBody.getSelectionEnd(), 0);
		postBody.getText().replace(Math.min(start, end), Math.max(start, end), quotedMsg);
		
		if (postWrapper.getVisibility() != View.VISIBLE)
			postOnTopicSetup();
		else
			postBody.setSelection(Math.min(start, end) + quotedMsg.length());
		
		wtl("quoteSetup finishing");
	}
	
	private void postOnTopicSetup() {
		wtl("postOnTopicSetup fired --NEL");
		pageJumperWrapper.setVisibility(View.GONE);
		titleWrapper.setVisibility(View.GONE);
		postWrapper.setVisibility(View.VISIBLE);
		postButton.setEnabled(true);
		cancelButton.setEnabled(true);
		postBody.requestFocus();
		postBody.setSelection(postBody.getText().length());
		postPostUrl = session.getLastPath();
		if (postPostUrl.contains("#"))
			postPostUrl = postPostUrl.substring(0, postPostUrl.indexOf('#'));
	}
	
	private void postOnBoardSetup() {
		wtl("postOnBoardSetup fired --NEL");
		pageJumperWrapper.setVisibility(View.GONE);
		titleWrapper.setVisibility(View.VISIBLE);
		if (Session.getUserLevel() > 29) {
			pollButton.setVisibility(View.VISIBLE);
			pollSep.setVisibility(View.VISIBLE);
		}
		postWrapper.setVisibility(View.VISIBLE);
		postButton.setEnabled(true);
		cancelButton.setEnabled(true);
		postTitle.requestFocus();
		postPostUrl = session.getLastPath();
		if (postPostUrl.contains("#"))
			postPostUrl = postPostUrl.substring(0, postPostUrl.indexOf('#'));
	}
	
	public void postCancel(View view) {
		wtl("postCancel fired --NEL");
		if (settings.getBoolean("confirmPostCancel", false)) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage("Cancel this post?");
			b.setPositiveButton("Yes", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					postCleanup();
				}
			});
			b.setNegativeButton("No", null);
			b.create().show();
		}
		else
			postCleanup();
	}
	
	public void postPollOptions(View view) {
		showDialog(POLL_OPTIONS_DIALOG);
	}
	
	public void postDo(View view) {
		wtl("postDo fired");
		if (settings.getBoolean("confirmPostSubmit", false)) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage("Submit this post?");
			b.setPositiveButton("Yes", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					postSubmit();
				}
			});
			b.setNegativeButton("No", null);
			b.create().show();
		}
		else
			postSubmit();
	}
	
	private void postSubmit() {
		if (titleWrapper.getVisibility() == View.VISIBLE) {
			wtl("posting on a board");
			// posting on a board
			String path = Session.ROOT + "/boards/post.php?board=" + boardID;
			int i = path.indexOf('-');
			path = path.substring(0, i);
			wtl("post path: " + path);
			savedPostBody = postBody.getText().toString();
			wtl("saved post body: " + savedPostBody);
			savedPostTitle = postTitle.getText().toString();
			wtl("saved post title: " + savedPostTitle);
			wtl("sending topic");
			postButton.setEnabled(false);
			pollButton.setEnabled(false);
			cancelButton.setEnabled(false);
			if (pollUse) {
				path += "&poll=1";
				session.get(NetDesc.POSTTPC_S1, path, null);
			}
			else if (Session.getUserLevel() < 30)
				session.get(NetDesc.POSTTPC_S1, path, null);
			else
				session.get(NetDesc.QPOSTTPC_S1, path, null);
		}
		
		else {
			// posting on a topic
			wtl("posting on a topic");
			String path = Session.ROOT + "/boards/post.php?board=" + boardID + "&topic=" + topicID;
			if (messageIDForEditing != null)
				path += "&message=" + messageIDForEditing;
			
			wtl("post path: " + path);
			savedPostBody = postBody.getText().toString();
			wtl("saved post body: " + savedPostBody);
			wtl("sending post");
			postButton.setEnabled(false);
			cancelButton.setEnabled(false);
			if (messageIDForEditing != null)
				session.get(NetDesc.QEDIT_MSG, path, null);
			else if (Session.getUserLevel() < 30)
				session.get(NetDesc.POSTMSG_S1, path, null);
			else
				session.get(NetDesc.QPOSTMSG_S1, path, null);
		}
	}
	
    private String reportCode;
    public String getReportCode() {return reportCode;}
    /** creates dialogs */
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	
    	switch (id) {
    		
    	case NEW_VERSION_DIALOG:
    		dialog = createNewVerDialog();
    		break;
    		
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
		final View v = inflater.inflate(R.layout.polloptions, null);
		b.setView(v);
		b.setCancelable(false);
		
		final EditText[] options = new EditText[10];
		
		final CheckBox poUse = (CheckBox) v.findViewById(R.id.poUse);
		final EditText poTitle = (EditText) v.findViewById(R.id.poTitle);
		options[0] = (EditText) v.findViewById(R.id.po1);
		options[1] = (EditText) v.findViewById(R.id.po2);
		options[2] = (EditText) v.findViewById(R.id.po3);
		options[3] = (EditText) v.findViewById(R.id.po4);
		options[4] = (EditText) v.findViewById(R.id.po5);
		options[5] = (EditText) v.findViewById(R.id.po6);
		options[6] = (EditText) v.findViewById(R.id.po7);
		options[7] = (EditText) v.findViewById(R.id.po8);
		options[8] = (EditText) v.findViewById(R.id.po9);
		options[9] = (EditText) v.findViewById(R.id.po10);
		final Spinner minLevel = (Spinner) v.findViewById(R.id.poMinLevel);
		
		poUse.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				poTitle.setEnabled(isChecked);
				for (int x = 0; x < 10; x++)
					options[x].setEnabled(isChecked);
			}
		});
		
		for (int x = 0; x < 10; x++)
			options[x].setText(pollOptions[x]);
		
		minLevel.setSelection(pollMinLevel);
		poTitle.setText(pollTitle);
		poUse.setChecked(pollUse);
		
		b.setPositiveButton("Save", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				pollUse = poUse.isChecked();
				pollTitle = poTitle.getText().toString();
				pollMinLevel = minLevel.getSelectedItemPosition();
				
				for (int x = 0; x < 10; x++)
					pollOptions[x] = (options[x].getText().toString());
			}
		});
		
		b.setNegativeButton("Cancel", null);
		
		b.setNeutralButton("Clear", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				clearPoll();
			}
		});
		
    	Dialog dialog = b.create();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				removeDialog(POLL_OPTIONS_DIALOG);
			}
		});
		return dialog;
    }
    
    private void clearPoll() {
    	pollUse = false; 
    	pollTitle = EMPTY_STRING; 
    	for (int x = 0; x < 10; x++)
    		pollOptions[x] = EMPTY_STRING; pollMinLevel = -1;
    }

    
	private Dialog createReportMessageDialog() {
		AlertDialog.Builder reportMsgBuilder = new AlertDialog.Builder(this);
		reportMsgBuilder.setTitle("Report Message");
		
		final String[] reportOptions = getResources().getStringArray(R.array.msgReportReasons);
		reportMsgBuilder.setItems(reportOptions, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				reportCode = getResources().getStringArray(R.array.msgReportCodes)[which];
				session.get(NetDesc.MARKMSG_S1, clickedMsg.getMessageDetailLink(), null);
			}
		});
		
		reportMsgBuilder.setNegativeButton("Cancel", null);
		
		Dialog dialog = reportMsgBuilder.create();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				removeDialog(REPORT_MESSAGE_DIALOG);
			}
		});
		return dialog;
	}
	
	private Dialog createMessageActionDialog() {
		AlertDialog.Builder msgActionBuilder = new AlertDialog.Builder(this);
		msgActionBuilder.setTitle("Message Actions");
		
		ArrayList<String> listBuilder = new ArrayList<String>();
		
		if (clickedMsg.isEdited() && clickedMsg.getMessageID() != null)
			listBuilder.add("View Previous Version(s)");
		
		if (Session.isLoggedIn()) {
			if (postIcon.isVisible())
				listBuilder.add("Quote");
			if (Session.getUser().toLowerCase(Locale.US).equals(clickedMsg.getUser().toLowerCase(Locale.US))) {
				if (Session.getUserLevel() > 29 && clickedMsg.isEditable())
					listBuilder.add("Edit");
				if (clickedMsg.getMessageID() != null)
					listBuilder.add("Delete");
			}
			else
				listBuilder.add("Report");
		}
			
		listBuilder.add("Highlight User");
		listBuilder.add("User Details");
		
		final String[] options = new String[listBuilder.size()];
		listBuilder.toArray(options);
		msgActionBuilder.setItems(options, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String selected = options[which];
				if (selected.equals("View Previous Version(s)")) {
					session.get(NetDesc.MESSAGE_DETAIL, clickedMsg.getMessageDetailLink(), null);
				}
				else if (selected.equals("Quote")) {
					String msg = (quoteSelection != null ? quoteSelection : clickedMsg.getMessageForQuoting());
					quoteSetup(clickedMsg.getUser(), msg);
				}
				else if (selected.equals("Edit")) {
					editPostSetup(clickedMsg.getMessageForEditing(), clickedMsg.getMessageID());
				}
				else if (selected.equals("Delete")) {
					session.get(NetDesc.DLTMSG_S1, clickedMsg.getMessageDetailLink(), null);
				}
				
				else if (selected.equals("Report")) {
					showDialog(REPORT_MESSAGE_DIALOG);
				}
				else if (selected.equals("Highlight User")) {
					HighlightedUser user = hlDB.getHighlightedUsers().get(clickedMsg.getUser().toLowerCase(Locale.US));
					HighlightListDBHelper.showHighlightUserDialog(AllInOneV2.this, user, clickedMsg.getUser(), null);
				}
				else if (selected.equals("User Details")) {
					session.get(NetDesc.USER_DETAIL, clickedMsg.getUserDetailLink(), null);
				}
				else {
					Toast.makeText(AllInOneV2.this, "not recognized: " + selected, Toast.LENGTH_SHORT).show();
				}
				
				dialog.dismiss();
			}
		});
		
		msgActionBuilder.setNegativeButton("Cancel", null);
		
		Dialog dialog = msgActionBuilder.create();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				removeDialog(MESSAGE_ACTION_DIALOG);
			}
		});
		return dialog;
	}

    private LinearLayout pmSending;
	private Dialog createSendPMDialog() {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		final View v = inflater.inflate(R.layout.sendpm, null);
		b.setView(v);
		b.setTitle("Send Private Message");
		b.setCancelable(false);
		
		final EditText to = (EditText) v.findViewById(R.id.spTo);
		final EditText subject = (EditText) v.findViewById(R.id.spSubject);
		final EditText message = (EditText) v.findViewById(R.id.spMessage);
		pmSending = (LinearLayout) v.findViewById(R.id.spFootWrapper);
		
		to.setText(savedTo);
		subject.setText(savedSubject);
		message.setText(savedMessage);
		
		b.setPositiveButton("Send", null);
		b.setNegativeButton("Cancel", null);
		
		final AlertDialog d = b.create();
		d.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View v) {
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
									
									session.get(NetDesc.SEND_PM_S1, "/pm/new", null);
									
								}
								else
									Toast.makeText(AllInOneV2.this, "The message can't be empty.", Toast.LENGTH_SHORT).show();
							}
							else
								Toast.makeText(AllInOneV2.this, "The subject can't be empty.", Toast.LENGTH_SHORT).show();
						}
						else
							Toast.makeText(AllInOneV2.this, "The recepient can't be empty.", Toast.LENGTH_SHORT).show();
					}
		        });
			}
		});
  	
		d.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				pmSending = null;
				removeDialog(SEND_PM_DIALOG);
			}
		});
		return d;
	}

	private Dialog createNewVerDialog() {
		AlertDialog.Builder newVersion = new AlertDialog.Builder(this);
		newVersion.setTitle("New Version of GameRaven found!");
		newVersion.setMessage("Would you like to go to the download page for the new version?");
		newVersion.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.evernote.com/shard/s252/sh/b680bb2b-64a1-426d-a98d-6cbfb846a883/75eebb4db64c6e1769dd2d0ace487a88")));
			}
		});
		newVersion.setNegativeButton("No", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dismissDialog(NEW_VERSION_DIALOG);
			}
		});
		return newVersion.create();
	}
	
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
    	
    	savedTo = URLDecoder.decode(savedTo);
    	savedSubject = URLDecoder.decode(savedSubject);
    	savedMessage = URLDecoder.decode(savedMessage);
    	
    	showDialog(SEND_PM_DIALOG);
    }
    
    public void pmCleanup(boolean wasSuccessful, String error) {
    	if (wasSuccessful) {
    		Toast.makeText(this, "PM sent.", Toast.LENGTH_SHORT).show();
        	dismissDialog(SEND_PM_DIALOG);
    	}
    	else {
    		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    		pmSending.setVisibility(View.GONE);
    	}
    }

	
	public void refreshClicked(View view) {
		wtl("refreshClicked fired --NEL");
		view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		if (session.getLastPath() == null) {
			if (Session.isLoggedIn()) {
				wtl("starting new session from refreshClicked, logged in");
				session = new Session(this, Session.getUser(), accounts.getString(Session.getUser()));
			}
	    	else {
				wtl("starting new session from refreshClicked, no login");
	    		session = new Session(this);
	    	}
		}
		else
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
		
		try {
			sig = sig.replace("*grver*", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			sig = sig.replace("*grver*", EMPTY_STRING);
			e.printStackTrace();
		}
		
		return sig;
    }
	
	private static SimpleDateFormat timingFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS z", Locale.US);
    public void wtl(String msg) {
    	if (!isReleaseBuild) {
			msg = msg.replaceAll("\\\\n", "(nl)");
			msg = timingFormat.format(new Date()) + "// " + msg;
			Log.d("logger", msg);
		}
    }
    
    private static final String ON_DEMAND_BUG_REPORT = "On Demand";
    public void onDemandBugReport() {
    	tryCaught(session.getLastPath() + "\nLast Attempted:\n" + session.getLastAttemptedPath(),
    	    	session.getLastDesc() + "\nLast Attempted:\n" + session.getLastAttemptedDesc(),
    			ON_DEMAND_BUG_REPORT,
    			(session.getLastRes() != null ? session.getLastRes().body() : "res is null"));
    }
	
	public void tryCaught(String url, String desc, String stacktrace, String source) {
		String ver;
		try {
			ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			ver = "version not set";
		}
		final String emailMsg = "\n\nVersion:\n" + ver + 
								"\n\nURL:\n" + url + 
								"\n\nDesc:\n" + desc +
								"\n\nStack trace:\n" + stacktrace + 
								"\n\nPage source:\n" + source;
		
    	AlertDialog.Builder b = new AlertDialog.Builder(this);
    	b.setTitle("Send Bug Report");
    	if (stacktrace.equals(ON_DEMAND_BUG_REPORT))
    		b.setMessage("NOTICE! Please include a comment on why you are sending this bug report in the text box " +
    				"below! On demand bug reports do not include any crash information, so we can't determine the " +
    				"bug without your input! Thanks!");
    	else
    		b.setMessage("You've run into a bug! Would you like to email debug information to the developer? The email will contain " + 
    				 "details on the crash itself, the url the server responded with, and the source for the page. " +
    				 "If so, please include a brief comment below on what you were trying to do.");
    	
    	final EditText input = new EditText(this);
    	LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
    	        LinearLayout.LayoutParams.MATCH_PARENT,
    	        LinearLayout.LayoutParams.MATCH_PARENT);
    	input.setLayoutParams(lp);
    	input.setHint("Enter comment here...");
    	b.setView(input);
    	
    	b.setNegativeButton("Cancel", null);
    	b.setPositiveButton("Email to dev", null);
    	
    	final AlertDialog d = b.create();
    	d.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

		            @Override
		            public void onClick(View view) {
		            	if (!input.getText().toString().equals(EMPTY_STRING)) {
			            	Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "ioabsoftware@gmail.com", null));
							i.putExtra(Intent.EXTRA_SUBJECT, "GameRaven Error Report");
							i.putExtra(Intent.EXTRA_TEXT   , "Comment:\n" + input.getText() + emailMsg);
							try {
							    startActivity(Intent.createChooser(i, "Send mail..."));
							} catch (android.content.ActivityNotFoundException ex) {
							    Toast.makeText(AllInOneV2.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
							}
		            	}
		            	else {
		            		input.requestFocus();
		            		Toast.makeText(AllInOneV2.this, "Please include a brief comment in provided text box.", Toast.LENGTH_SHORT).show();
		            	}
		            }
		        });
			}
		});
    	
    	d.show();
    }
	
	private String parseBoardID(String url) {
		wtl("parseBoardID fired");
		// board example: http://www.gamefaqs.com/boards/400-current-events
		String boardUrl = url.substring(Session.ROOT.length() + 8);
		
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

		wtl("boardID: " + boardUrl);
		return boardUrl;
	}
	
	private String parseTopicID(String url) {
		wtl("parseTopicID fired");
		// topic example: http://www.gamefaqs.com/boards/400-current-events/64300205
		String topicUrl = url.substring(url.indexOf('/', Session.ROOT.length() + 8) + 1);
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
		wtl("topicID: " + topicUrl);
		return topicUrl;
	}
	
	private String parseMessageID(String url) {
		wtl("parseMessageID fired");
		String msgID = url.substring(url.lastIndexOf('/') + 1);
		wtl("messageIDForEditing: " + msgID);
		return msgID;
	}
	
    
    @Override
	public void onBackPressed() {
    	if (searchIcon.isActionViewExpanded()) {
    		searchIcon.collapseActionView();
    	}
    	else if (drawer.isMenuVisible()) {
    		drawer.closeMenu(true);
    	}
    	else if (postWrapper.getVisibility() == View.VISIBLE) {
    		postCleanup();
    	}
    	else {
    	    goBack();
    	}
	}

	private void goBack() {
		if (session.canGoBack()) {
			wtl("back pressed, history exists, going back");
			session.goBack(false);
		}
		else {
			wtl("back pressed, no history, exiting app");
			session = null;
		    this.finish();
		}
	}
	
	public static String buildAMPLink() {
		return "/boards/myposts.php?lp=" + settings.getString("ampSortOption", "-1");
	}
	
	private String autoCensor(String text) {
		StringBuilder builder = new StringBuilder(text);
		String textLower = text.toLowerCase(Locale.US);
		for (String word : bannedList)
			censorWord(builder, textLower, word.toLowerCase(Locale.US));
		
		return builder.toString();
	}
	
	private void censorWord(StringBuilder builder, String textLower, String word) {
		int length = word.length();
		String replacement = "";
		
		for (int x = 0; x < length - 1; x++)
			replacement += '*';
		
		while (textLower.contains(word)) {
			int start = textLower.indexOf(word);
			int end = start + length;
			builder.replace(start + 1, end, replacement);
			textLower = textLower.replaceFirst(word, replacement + '*');
		}
	}
	
	public void htmlButtonClicked(View view) {
		String open = ((TextView) view).getText().toString();
		String close = "</" + open.substring(1);
		
		int start = Math.max(postBody.getSelectionStart(), 0);
		int end = Math.max(postBody.getSelectionEnd(), 0);
		
		String insert;
		if (start != end)
			insert = open + postBody.getText().subSequence(start, end) + close;
		else
			insert = open + close;
		
		postBody.getText().replace(Math.min(start, end), Math.max(start, end), insert, 0, insert.length());
	}
	
	private static final String[] bannedList = {
		"***hole",
		"68.13.103",
		"Arse Hole",
		"Arse-hole",
		"Ass hole",
		"Ass****",
		"Ass-hole",
		"Asshole",
		"�^�",
		"Bitch",
		"Bukkake",
		"Cheat Code Central",
		"CheatCC",
		"Clit",
		"Cunt",
		"Dave Allison",
		"David Allison",
		"Dildo",
		"Echo J",
		"Fag",
		"Format C:",
		"FreeFlatScreens.com",
		"FreeIPods.com",
		"Fuck",
		"GFNostalgia",
		"Gook",
		"Jism",
		"Jizm",
		"Jizz",
		"KingOfChaos",
		"Lesbo",
		"LUE2.tk",
		"Mod Files",
		"Mod Pics",
		"ModFiles",
		"ModPics",
		"Nigga",
		"Nigger",
		"Offiz.bei.t-online.de",
		"OutPimp",
		"OutWar.com",
		"PornStarGuru",
		"Pussies",
		"Pussy",
		"RavenBlack.net",
		"Shit",
		"Shiz",
		"SuprNova",
		"Tits",
		"Titties",
		"Titty",
		"UrbanDictionary",
		"Wigga",
		"Wigger",
		"YouDontKnowWhoIAm"};
}
