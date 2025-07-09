package com.example.app;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.DownloadListener;
import android.view.ContextMenu;
import android.view.MenuInflater;

import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app.adapters.SearchSuggestionAdapter;
import com.example.app.database.DatabaseHelper;
import com.example.app.models.BrowserTab;
import com.example.app.models.DownloadItem;
import com.example.app.models.HistoryItem;
import com.example.app.models.SearchEngine;
import com.example.app.models.SearchSuggestion;
import com.example.app.utils.SearchEnginePreferences;
import com.example.app.utils.AdBlocker;
import com.example.app.utils.SearchSuggestionProvider;
import com.example.app.utils.WebDownloader;
import com.example.app.utils.DownloadNotificationManager;
import com.example.app.utils.PerformanceOptimizer;
import com.example.app.utils.PrivateBrowsingManager;
import com.example.app.utils.ReadingModeManager;
import com.example.app.utils.UserAgentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_URL = "https://www.google.com";

    private LinearLayout tabContainer;
    private HorizontalScrollView tabScrollView;
    private EditText urlEditText;
    private ImageButton backButton, forwardButton, refreshButton, homeButton, menuButton;
    private ProgressBar progressBar;
    private FrameLayout webViewContainer;
    
    // Search suggestions components
    private RecyclerView searchSuggestionsRecyclerView;
    private SearchSuggestionAdapter suggestionAdapter;
    private SearchSuggestionProvider suggestionProvider;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 300; // Delay before showing suggestions

    // Download bar components
    private View downloadBar;
    private TextView downloadFileName;
    private ProgressBar downloadProgressBar;
    private TextView downloadStatus;
    private ImageButton downloadCancel;
    private DownloadItem currentDownload;

    private List<BrowserTab> tabs;
    private int currentTabIndex = -1;
    private DatabaseHelper databaseHelper;
    private SearchEnginePreferences searchEnginePrefs;
    private AdBlocker adBlocker;
    private WebDownloader webDownloader;
    private DownloadNotificationManager downloadNotificationManager;
    
    // New utility managers
    private PerformanceOptimizer performanceOptimizer;
    private PrivateBrowsingManager privateBrowsingManager;
    private ReadingModeManager readingModeManager;
    private UserAgentManager userAgentManager;
    
    private boolean isInFullscreenVideo = false;

    // Fullscreen video
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    // Permission request codes
    private static final int PERMISSION_REQUEST_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeDatabase();
        initializeSearchEngine();
        initializeUtilityManagers();
        optimizePerformance();
        setupEventListeners();
        setupBackPressedHandler();
        
        // Create initial tab
        createNewTab(DEFAULT_URL);
        
        // Handle intent URLs
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null) {
            // Handle URL from HistoryActivity or BookmarksActivity
            String urlFromHistory = intent.getStringExtra("url");
            if (urlFromHistory != null) {
                boolean openInNewTab = intent.getBooleanExtra("open_in_new_tab", false);
                if (openInNewTab) {
                    createNewTab(urlFromHistory);
                } else {
                    loadUrl(urlFromHistory);
                }
                return;
            }
            
            // Handle external URLs
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                if (data != null) {
                    String url = data.toString();
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        loadUrl(url);
                    }
                }
            }
        }
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle search suggestions visibility
                if (searchSuggestionsRecyclerView != null && 
                    searchSuggestionsRecyclerView.getVisibility() == View.VISIBLE) {
                    hideSuggestions();
                    return;
                }
                
                // Handle fullscreen video exit
                if (isInFullscreenVideo) {
                    WebView currentWebView = getCurrentWebView();
                    if (currentWebView != null) {
                        // This will trigger onHideCustomView in WebChromeClient
                        currentWebView.evaluateJavascript("document.exitFullscreen();", null);
                    }
                    return;
                }
                
                // Handle regular web navigation
                WebView currentWebView = getCurrentWebView();
                if (currentWebView != null && currentWebView.canGoBack()) {
                    currentWebView.goBack();
                } else {
                    // Exit the app if we can't go back
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle orientation change without recreating activity
        // WebView and all state will be preserved
    }

    private void enableNormalMode() {
        // Normal browsing mode with status bar visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (windowInsetsController != null) {
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars());
                windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void enableFullscreenMode() {
        // Full immersive mode for video content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (windowInsetsController != null) {
                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars() | 
                    WindowInsetsCompat.Type.navigationBars());
                windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void initializeViews() {
        tabContainer = findViewById(R.id.tabContainer);
        tabScrollView = findViewById(R.id.tabScrollView);
        urlEditText = findViewById(R.id.urlEditText);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        homeButton = findViewById(R.id.homeButton);
        menuButton = findViewById(R.id.menuButton);
        progressBar = findViewById(R.id.progressBar);
        webViewContainer = findViewById(R.id.webViewContainer);
        searchSuggestionsRecyclerView = findViewById(R.id.searchSuggestionsRecyclerView);

        // Initialize download bar
        downloadBar = findViewById(R.id.downloadBar);
        downloadFileName = downloadBar.findViewById(R.id.downloadFileName);
        downloadProgressBar = downloadBar.findViewById(R.id.downloadProgress);
        downloadStatus = downloadBar.findViewById(R.id.downloadStatus);
        downloadCancel = downloadBar.findViewById(R.id.downloadCancel);

        tabs = new ArrayList<>();
        
        // Initialize search suggestions
        initializeSearchSuggestions();
    }

    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
        webDownloader = new WebDownloader(this, databaseHelper);
        downloadNotificationManager = new DownloadNotificationManager(this);
        
        // Set up download progress listener
        webDownloader.setProgressListener(new WebDownloader.DownloadProgressListener() {
            private long lastNotificationUpdate = 0;
            private static final long NOTIFICATION_UPDATE_INTERVAL = 1000; // Update notification every 1 second
            
            @Override
            public void onDownloadStarted(DownloadItem item) {
                runOnUiThread(() -> {
                    currentDownload = item;
                    showDownloadBar(item);
                    downloadNotificationManager.showDownloadProgress(item);
                    lastNotificationUpdate = System.currentTimeMillis();
                });
            }

            @Override
            public void onDownloadProgress(DownloadItem item) {
                runOnUiThread(() -> {
                    updateDownloadBar(item);
                    
                    // Throttle notification updates to avoid overwhelming the system
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastNotificationUpdate >= NOTIFICATION_UPDATE_INTERVAL) {
                        downloadNotificationManager.showDownloadProgress(item);
                        lastNotificationUpdate = currentTime;
                    }
                });
            }

            @Override
            public void onDownloadCompleted(DownloadItem item) {
                runOnUiThread(() -> {
                    hideDownloadBar();
                    downloadNotificationManager.showDownloadCompleted(item);
                    Toast.makeText(MainActivity.this, "Download completed: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDownloadFailed(DownloadItem item, String error) {
                runOnUiThread(() -> {
                    hideDownloadBar();
                    downloadNotificationManager.showDownloadFailed(item, error);
                    Toast.makeText(MainActivity.this, "Download failed: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initializeSearchEngine() {
        searchEnginePrefs = new SearchEnginePreferences(this);
        adBlocker = new AdBlocker(this);
        updateUrlBarHint();
    }

    private void initializeSearchSuggestions() {
        suggestionProvider = new SearchSuggestionProvider(this);
        suggestionAdapter = new SearchSuggestionAdapter(new ArrayList<>(), 
            new SearchSuggestionAdapter.OnSuggestionClickListener() {
                @Override
                public void onSuggestionClick(SearchSuggestion suggestion) {
                    hideSuggestionsAndKeyboard();
                    urlEditText.setText(suggestion.getQuery());
                    urlEditText.clearFocus();
                    loadUrl(suggestion.getQuery());
                }
            });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        searchSuggestionsRecyclerView.setLayoutManager(layoutManager);
        searchSuggestionsRecyclerView.setAdapter(suggestionAdapter);
        
        // Optimize RecyclerView performance
        searchSuggestionsRecyclerView.setHasFixedSize(false); // Size can change based on suggestions
        searchSuggestionsRecyclerView.setItemViewCacheSize(20); // Cache more items for better scrolling
        searchSuggestionsRecyclerView.setDrawingCacheEnabled(true);
        searchSuggestionsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void setupEventListeners() {
        // New tab button  
        ImageButton newTabButton = findViewById(R.id.newTabButton);
        if (newTabButton != null) {
            newTabButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewTab(DEFAULT_URL);
                }
            });
        }
        
        // URL EditText
        urlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String text = urlEditText.getText() != null ? urlEditText.getText().toString() : "";
                    hideSuggestionsAndKeyboard();
                    urlEditText.clearFocus();
                    loadUrl(text);
                    return true;
                }
                return false;
            }
        });

        // Add text change listener for search suggestions
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleSearchTextChange(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Hide suggestions when URL bar loses focus
        urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideSuggestions();
                }
            }
        });

        // Hide suggestions when user taps outside URL bar or suggestions
        findViewById(R.id.webViewContainer).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (searchSuggestionsRecyclerView != null && 
                        searchSuggestionsRecyclerView.getVisibility() == View.VISIBLE) {
                        hideSuggestionsAndKeyboard();
                        urlEditText.clearFocus();
                    }
                }
                return false; // Allow other touch events to be processed
            }
        });
        
        // Hide suggestions when user taps on tab container
        tabContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (searchSuggestionsRecyclerView != null && 
                        searchSuggestionsRecyclerView.getVisibility() == View.VISIBLE) {
                        hideSuggestionsAndKeyboard();
                        urlEditText.clearFocus();
                    }
                }
                return false; // Allow other touch events to be processed
            }
        });
        
        // Hide suggestions when user taps on navigation bar (but not on URL bar)
        findViewById(R.id.navigationBar).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (searchSuggestionsRecyclerView != null && 
                        searchSuggestionsRecyclerView.getVisibility() == View.VISIBLE) {
                        // Check if the touch is not on the URL EditText
                        int[] urlLocation = new int[2];
                        urlEditText.getLocationOnScreen(urlLocation);
                        float touchX = event.getRawX();
                        float touchY = event.getRawY();
                        
                        if (touchX < urlLocation[0] || touchX > urlLocation[0] + urlEditText.getWidth() ||
                            touchY < urlLocation[1] || touchY > urlLocation[1] + urlEditText.getHeight()) {
                            hideSuggestionsAndKeyboard();
                            urlEditText.clearFocus();
                        }
                    }
                }
                return false; // Allow other touch events to be processed
            }
        });

        // Hide suggestions when user taps on the root container
        findViewById(android.R.id.content).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (searchSuggestionsRecyclerView != null && 
                        searchSuggestionsRecyclerView.getVisibility() == View.VISIBLE) {
                        // Check if the touch is not on the search suggestions or URL bar
                        int[] suggestionLocation = new int[2];
                        int[] urlLocation = new int[2];
                        
                        searchSuggestionsRecyclerView.getLocationOnScreen(suggestionLocation);
                        urlEditText.getLocationOnScreen(urlLocation);
                        
                        float touchX = event.getRawX();
                        float touchY = event.getRawY();
                        
                        boolean touchOnSuggestions = touchX >= suggestionLocation[0] && 
                                                   touchX <= suggestionLocation[0] + searchSuggestionsRecyclerView.getWidth() &&
                                                   touchY >= suggestionLocation[1] && 
                                                   touchY <= suggestionLocation[1] + searchSuggestionsRecyclerView.getHeight();
                        
                        boolean touchOnUrlBar = touchX >= urlLocation[0] && 
                                              touchX <= urlLocation[0] + urlEditText.getWidth() &&
                                              touchY >= urlLocation[1] && 
                                              touchY <= urlLocation[1] + urlEditText.getHeight();
                        
                        if (!touchOnSuggestions && !touchOnUrlBar) {
                            hideSuggestionsAndKeyboard();
                            urlEditText.clearFocus();
                        }
                    }
                }
                return false; // Allow other touch events to be processed
            }
        });

        // Navigation buttons
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = getCurrentWebView();
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = getCurrentWebView();
                if (webView != null && webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = getCurrentWebView();
                if (webView != null) {
                    webView.reload();
                }
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl(DEFAULT_URL);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });
    }

    private void createNewTab(String url) {
        try {
            BrowserTab tab = new BrowserTab("New Tab", url);
            WebView webView = createWebView();
            if (webView == null) return; // Safety check
            
            tab.setWebView(webView);
            tabs.add(tab);

            // Create tab UI
            View tabView = createTabView(tab, tabs.size() - 1);
            if (tabView != null && tabContainer != null) {
                tabContainer.addView(tabView);
            }

            // Add new tab button if this is the first tab or it doesn't exist
            addNewTabButton();

            // Switch to new tab
            switchToTab(tabs.size() - 1);
            
            // Sync tab container and scroll to show the new tab
            syncTabContainer();
            scrollToActiveTab();
            
            // Load URL safely
            if (webView != null && url != null && !url.isEmpty()) {
                webView.loadUrl(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the error gracefully, maybe show a toast
            if (this != null && !isDestroyed()) {
                Toast.makeText(this, "Error creating new tab", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private WebView createWebView() {
        try {
            WebView webView = new WebView(this);
            
            // Apply performance optimizations
            if (performanceOptimizer != null) {
                performanceOptimizer.optimizeWebView(webView);
            }
            
            // Apply private browsing settings
            if (privateBrowsingManager != null) {
                privateBrowsingManager.configureWebViewForPrivateMode(webView);
            }
            
            // Apply user agent settings
            if (userAgentManager != null) {
                userAgentManager.applyUserAgent(webView);
            }
            
            // Basic WebView settings (in addition to optimizations)
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            
            // Enable fullscreen video support
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            
            // Set download listener
            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, 
                                          String mimeType, long contentLength) {
                    handleDownload(url, userAgent, contentDisposition, mimeType);
                }
            });
            
            // Register for context menu (for image download)
            registerForContextMenu(webView);
            
            // Note: setAllowFileAccessFromFileURLs and setAllowUniversalAccessFromFileURLs 
            // are deprecated for security reasons. Only enable if absolutely necessary for your use case.
            // Consider using a more secure approach like serving files from assets or resources
            
            // These are disabled by default on API 16+ for security reasons
            // Only enable if your app specifically needs file URL access
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            // Note: setPluginState has been deprecated as plugins are no longer supported

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Handle intent:// URLs
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                            return true;
                        }
                        
                        // Try to get fallback URL
                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl);
                            return true;
                        }
                        
                        // If no fallback, try to open the app store
                        String packageName = intent.getPackage();
                        if (packageName != null) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, 
                                Uri.parse("market://details?id=" + packageName));
                            if (marketIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(marketIntent);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // If all fails, just ignore the URL
                        return true;
                    }
                    return true;
                }
                
                // Handle other special schemes
                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("sms:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return true;
                    }
                }
                
                // Let WebView handle normal URLs
                return false;
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Check if the request should be blocked by ad blocker
                try {
                    if (adBlocker != null && adBlocker.isInitialized()) {
                        WebResourceResponse blockedResponse = adBlocker.shouldBlockRequest(request);
                        if (blockedResponse != null) {
                            // The ad blocker already incremented the blocked count
                            return blockedResponse;
                        }
                    }
                } catch (Exception e) {
                    // Log error but don't crash - continue with normal request
                    e.printStackTrace();
                }
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                if (urlEditText != null && url != null) {
                    urlEditText.setText(url);
                }
                updateNavigationButtons();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                updateNavigationButtons();
                
                // Update tab title
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    BrowserTab currentTab = getCurrentTab();
                    if (currentTab != null) {
                        currentTab.setTitle(title);
                        updateTabTitle(currentTabIndex);
                    }
                    
                    // Add to history (only if not in private mode)
                    if (databaseHelper != null && url != null && 
                        privateBrowsingManager != null && privateBrowsingManager.shouldSaveHistory()) {
                        HistoryItem historyItem = new HistoryItem(title, url);
                        databaseHelper.addHistoryItem(historyItem);
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                WebView currentWebView = getCurrentWebView();
                if (currentWebView == view && title != null) {
                    BrowserTab currentTab = getCurrentTab();
                    if (currentTab != null) {
                        currentTab.setTitle(title);
                        updateTabTitle(currentTabIndex);
                    }
                }
            }
            
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Handle fullscreen video
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                
                customView = view;
                customViewCallback = callback;
                isInFullscreenVideo = true;
                
                // Hide browser UI elements
                if (tabScrollView != null) tabScrollView.setVisibility(View.GONE);
                findViewById(R.id.newTabButton).setVisibility(View.GONE);
                findViewById(R.id.navigationBar).setVisibility(View.GONE);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                // Make fullscreen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsControllerCompat windowInsetsController = 
                        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                    if (windowInsetsController != null) {
                        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars() | 
                            WindowInsetsCompat.Type.navigationBars());
                        windowInsetsController.setSystemBarsBehavior(
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                }
                
                // Add custom view to container
                webViewContainer.addView(customView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
            
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                
                // Remove custom view
                webViewContainer.removeView(customView);
                customView = null;
                isInFullscreenVideo = false;
                
                // Show browser UI elements
                if (tabScrollView != null) tabScrollView.setVisibility(View.VISIBLE);
                findViewById(R.id.newTabButton).setVisibility(View.VISIBLE);
                findViewById(R.id.navigationBar).setVisibility(View.VISIBLE);
                
                // Restore normal browsing mode (with status bar)
                enableNormalMode();
                
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }
        });

        return webView;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private View createTabView(BrowserTab tab, int index) {
        View tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, tabContainer, false);
        TextView tabTitle = tabView.findViewById(R.id.tabTitle);
        ImageButton closeTab = tabView.findViewById(R.id.closeTab);

        if (tabTitle != null && tab != null) {
            tabTitle.setText(tab.getTitle());
        }
        
        // Store the tab reference in the view's tag to avoid index issues
        tabView.setTag(tab);
        
        tabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab clickedTab = (BrowserTab) v.getTag();
                int tabIndex = tabs.indexOf(clickedTab);
                if (tabIndex >= 0) {
                    switchToTab(tabIndex);
                }
            }
        });
        
        if (closeTab != null) {
            closeTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserTab clickedTab = (BrowserTab) tabView.getTag();
                    int tabIndex = tabs.indexOf(clickedTab);
                    if (tabIndex >= 0) {
                        closeTab(tabIndex);
                    }
                }
            });
        }

        return tabView;
    }

    private void addNewTabButton() {
        ImageButton newTabButton = findViewById(R.id.newTabButton);
        if (newTabButton != null) {
            newTabButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewTab(DEFAULT_URL);
                }
            });
        }
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size() || webViewContainer == null) return;

        // Hide current WebView
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            BrowserTab currentTab = tabs.get(currentTabIndex);
            WebView currentWebView = currentTab.getWebView();
            if (currentWebView != null && currentWebView.getParent() == webViewContainer) {
                webViewContainer.removeView(currentWebView);
            }
            currentTab.setSelected(false);
        }

        // Show new WebView
        currentTabIndex = index;
        BrowserTab newCurrentTab = tabs.get(currentTabIndex);
        if (newCurrentTab != null) {
            newCurrentTab.setSelected(true);
            
            WebView webView = newCurrentTab.getWebView();
            if (webView != null && webView.getParent() != webViewContainer) {
                webViewContainer.addView(webView);
            }

            // Update UI safely
            if (urlEditText != null) {
                urlEditText.setText(newCurrentTab.getUrl());
            }
            updateNavigationButtons();
            updateTabSelection();
            scrollToActiveTab();
        }
    }

    private void closeTab(int index) {
        if (tabs.size() <= 1) return; // Don't close the last tab

        BrowserTab tabToClose = tabs.get(index);
        WebView webViewToClose = tabToClose.getWebView();
        
        // Remove WebView from container if it's currently displayed
        if (webViewToClose.getParent() == webViewContainer) {
            webViewContainer.removeView(webViewToClose);
        }
        
        // Clean up WebView with performance optimizer
        if (performanceOptimizer != null) {
            performanceOptimizer.cleanupWebView(webViewToClose);
        }
        
        // Destroy WebView
        webViewToClose.destroy();
        
        // Remove tab from list and UI
        tabs.remove(index);
        tabContainer.removeViewAt(index);

        // Adjust current tab index
        if (currentTabIndex == index) {
            if (index > 0) {
                switchToTab(index - 1);
            } else if (tabs.size() > 0) {
                switchToTab(0);
            }
        } else if (currentTabIndex > index) {
            currentTabIndex--;
        }
        
        // Update tab selection after closing
        updateTabSelection();
    }

    private void updateTabTitle(int index) {
        if (index >= 0 && index < tabs.size() && index < tabContainer.getChildCount()) {
            BrowserTab tab = tabs.get(index);
            View tabView = tabContainer.getChildAt(index);
            
            // Verify this is actually a tab view (not the new tab button)
            if (tabView.getTag() instanceof BrowserTab) {
                TextView tabTitle = tabView.findViewById(R.id.tabTitle);
                if (tabTitle != null) {
                    tabTitle.setText(tab.getTitle());
                }
            }
        }
    }

    private void updateTabSelection() {
        for (int i = 0; i < tabs.size() && i < tabContainer.getChildCount(); i++) {
            View tabView = tabContainer.getChildAt(i);
            
            // Verify this is actually a tab view (not the new tab button)
            if (tabView.getTag() instanceof BrowserTab) {
                TextView tabTitle = tabView.findViewById(R.id.tabTitle);
                tabView.setSelected(i == currentTabIndex);
                
                // Update text color based on selection state
                if (tabTitle != null) {
                    if (i == currentTabIndex) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            tabTitle.setTextColor(getResources().getColor(R.color.tab_text_selected_dark, getTheme()));
                        } else {
                            tabTitle.setTextColor(getResources().getColor(R.color.tab_text_selected_dark));
                        }
                    } else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            tabTitle.setTextColor(getResources().getColor(R.color.tab_text_normal_dark, getTheme()));
                        } else {
                            tabTitle.setTextColor(getResources().getColor(R.color.tab_text_normal_dark));
                        }
                    }
                }
            }
        }
    }
    
    private void scrollToActiveTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabContainer.getChildCount()) {
            View activeTab = tabContainer.getChildAt(currentTabIndex);
            if (activeTab != null && tabScrollView != null) {
                // Calculate the position to scroll to center the active tab
                int scrollTo = activeTab.getLeft() - (tabScrollView.getWidth() / 2) + (activeTab.getWidth() / 2);
                tabScrollView.smoothScrollTo(Math.max(0, scrollTo), 0);
            }
        }
    }

    private void updateNavigationButtons() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null) {
            backButton.setEnabled(currentWebView.canGoBack());
            forwardButton.setEnabled(currentWebView.canGoForward());
        } else {
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
        }
    }

    private void loadUrl(String input) {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView == null) return;

        String url;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            SearchEngine selectedEngine = searchEnginePrefs.getSelectedSearchEngine();
            url = selectedEngine.getSearchUrl() + input.replace(" ", "+");
        }

        getCurrentTab().setUrl(url);
        currentWebView.loadUrl(url);
    }

    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        
        // Update bookmark icon
        WebView currentWebView = getCurrentWebView();
        String currentUrl = currentWebView != null ? currentWebView.getUrl() : "";
        boolean isBookmarked = databaseHelper.isBookmarked(currentUrl);
        MenuItem bookmarkItem = popup.getMenu().findItem(R.id.action_bookmark);
        bookmarkItem.setTitle(isBookmarked ? "Remove Bookmark" : "Add Bookmark");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.action_new_tab) {
                    createNewTab(DEFAULT_URL);
                    return true;
                } else if (itemId == R.id.action_private_tab) {
                    togglePrivateMode();
                    return true;
                } else if (itemId == R.id.action_bookmark) {
                    toggleBookmark();
                    return true;
                } else if (itemId == R.id.action_bookmarks) {
                    startActivity(new Intent(MainActivity.this, BookmarksActivity.class));
                    return true;
                } else if (itemId == R.id.action_history) {
                    startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                    return true;
                } else if (itemId == R.id.action_downloads) {
                    startActivity(new Intent(MainActivity.this, DownloadsActivity.class));
                    return true;
                } else if (itemId == R.id.action_reading_mode) {
                    toggleReadingMode();
                    return true;
                } else if (itemId == R.id.action_user_agent) {
                    showUserAgentDialog();
                    return true;
                } else if (itemId == R.id.action_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                } else if (itemId == R.id.action_share) {
                    shareCurrentPage();
                    return true;
                } else if (itemId == R.id.action_performance) {
                    showPerformanceStats();
                    return true;
                }
                return false;
            }
        });
        
        popup.show();
    }

    private void toggleBookmark() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView == null) return;

        String url = currentWebView.getUrl();
        String title = currentWebView.getTitle();
        
        if (url == null) return;

        if (databaseHelper.isBookmarked(url)) {
            // Remove bookmark
            // Note: This is simplified - in a real app you'd need to get the bookmark ID
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
        } else {
            // Add bookmark
            databaseHelper.addBookmark(new com.example.app.models.Bookmark(title, url));
            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentPage() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null && currentWebView.getUrl() != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentWebView.getUrl());
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentWebView.getTitle());
            startActivity(Intent.createChooser(shareIntent, "Share page"));
        }
    }

    private void togglePrivateMode() {
        if (privateBrowsingManager != null) {
            boolean isPrivate = privateBrowsingManager.isPrivateModeEnabled();
            if (isPrivate) {
                privateBrowsingManager.disablePrivateMode();
                Toast.makeText(this, "Private mode disabled", Toast.LENGTH_SHORT).show();
            } else {
                privateBrowsingManager.enablePrivateMode();
                Toast.makeText(this, "Private mode enabled", Toast.LENGTH_SHORT).show();
            }
            
            // Update all existing tabs
            for (BrowserTab tab : tabs) {
                if (tab.getWebView() != null) {
                    privateBrowsingManager.configureWebViewForPrivateMode(tab.getWebView());
                }
            }
        }
    }
    
    private void toggleReadingMode() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null && readingModeManager != null) {
            readingModeManager.toggleReadingMode(currentWebView);
        }
    }
    
    private void showUserAgentDialog() {
        if (userAgentManager == null) return;
        
        UserAgentManager.UserAgentType[] types = userAgentManager.getAllUserAgentTypes();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }
        
        UserAgentManager.UserAgentType currentType = userAgentManager.getUserAgentType();
        int selectedIndex = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == currentType) {
                selectedIndex = i;
                break;
            }
        }
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Select User Agent")
            .setSingleChoiceItems(typeNames, selectedIndex, (dialog, which) -> {
                userAgentManager.setUserAgentType(types[which]);
                
                // Apply to all tabs
                for (BrowserTab tab : tabs) {
                    if (tab.getWebView() != null) {
                        userAgentManager.applyUserAgent(tab.getWebView());
                    }
                }
                
                Toast.makeText(this, "User agent changed to " + types[which].getDisplayName(), 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showPerformanceStats() {
        if (performanceOptimizer != null) {
            PerformanceOptimizer.MemoryStats stats = performanceOptimizer.getMemoryStats();
            
            String message = "Memory Usage: " + stats.getFormattedUsage() + "\n" +
                           "Active WebViews: " + stats.activeWebViews + "\n" +
                           "Tabs: " + tabs.size();
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("Performance Stats")
                .setMessage(message)
                .setPositiveButton("Clear Cache", (dialog, which) -> {
                    performanceOptimizer.clearImageCache();
                    performanceOptimizer.clearWebViewCache();
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
        }
    }
    
    private void toggleAdBlocker() {
        if (adBlocker != null) {
            boolean isEnabled = adBlocker.isAdBlockEnabled();
            adBlocker.setAdBlockEnabled(!isEnabled);
            Toast.makeText(this, "Ad Blocker " + (isEnabled ? "disabled" : "enabled"), Toast.LENGTH_SHORT).show();
        }
    }

    private BrowserTab getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup WebViews to prevent memory leaks
        if (tabs != null) {
            for (BrowserTab tab : tabs) {
                if (tab.getWebView() != null) {
                    tab.getWebView().destroy();
                }
            }
            tabs.clear();
        }
        
        // Cleanup search suggestion provider
        if (suggestionProvider != null) {
            suggestionProvider.cleanup();
            suggestionProvider = null;
        }
        
        // Cleanup handlers
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
        
        // Cleanup database
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }
        
        // Cleanup downloaders
        if (webDownloader != null) {
            webDownloader.cleanup();
            webDownloader = null;
        }
        
        // Cleanup notification manager
        if (downloadNotificationManager != null) {
            downloadNotificationManager = null;
        }
        
        // Cleanup ad blocker
        if (adBlocker != null) {
            adBlocker = null;
        }
        
        // Cleanup utility managers
        if (performanceOptimizer != null) {
            performanceOptimizer.clearImageCache();
            performanceOptimizer.clearWebViewCache();
        }
        
        if (privateBrowsingManager != null) {
            privateBrowsingManager.clearPrivateData();
            privateBrowsingManager = null;
        }
        
        if (readingModeManager != null) {
            readingModeManager = null;
        }
        
        if (userAgentManager != null) {
            userAgentManager = null;
        }
    }

    private void updateUrlBarHint() {
        if (searchEnginePrefs != null && urlEditText != null) {
            SearchEngine selectedEngine = searchEnginePrefs.getSelectedSearchEngine();
            String hint = "Search " + selectedEngine.getName() + " or enter URL";
            urlEditText.setHint(hint);
        }
    }

    private void hideSuggestions() {
        if (searchSuggestionsRecyclerView != null) {
            searchSuggestionsRecyclerView.setVisibility(View.GONE);
        }
        // Cancel any pending search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        // Cancel any pending network requests
        cancelPendingSearchRequests();
    }

    private void hideSuggestionsAndKeyboard() {
        hideSuggestions();
        
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && urlEditText != null) {
            imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
        }
    }

    private void showSuggestions(List<SearchSuggestion> suggestions) {
        if (suggestions != null && !suggestions.isEmpty() && urlEditText.hasFocus()) {
            suggestionAdapter.updateSuggestions(suggestions);
            searchSuggestionsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            hideSuggestions();
        }
    }

    private void handleSearchTextChange(String query) {
        // Cancel previous search if pending
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (query.trim().isEmpty()) {
            hideSuggestions();
            return;
        }

        // Create new search runnable with delay to avoid too many requests
        searchRunnable = new Runnable() {
            @Override
            public void run() {
                if (suggestionProvider != null) {
                    suggestionProvider.getSuggestions(query, new SearchSuggestionProvider.SuggestionCallback() {
                        @Override
                        public void onSuggestionsReady(List<SearchSuggestion> suggestions) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSuggestions(suggestions);
                                }
                            });
                        }
                    });
                }
            }
        };

        // Post with delay
        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
    }

    private void showDownloadBar(DownloadItem item) {
        if (downloadBar != null) {
            downloadBar.setVisibility(View.VISIBLE);
            downloadFileName.setText(item.getTitle());
            downloadProgressBar.setProgress(0);
            downloadStatus.setText("Starting download...");
            
            downloadCancel.setOnClickListener(v -> {
                if (currentDownload != null) {
                    webDownloader.cancelDownload(currentDownload.getId());
                    hideDownloadBar();
                    Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateDownloadBar(DownloadItem item) {
        if (downloadBar != null && downloadBar.getVisibility() == View.VISIBLE && currentDownload != null && 
            currentDownload.getId() == item.getId()) {
            
            int progress = item.getProgress();
            downloadProgressBar.setProgress(progress);
            
            if (item.getFileSize() > 0) {
                String statusText = progress + "% - " + formatFileSize(item.getDownloadedSize()) + 
                                  " of " + formatFileSize(item.getFileSize());
                downloadStatus.setText(statusText);
            } else {
                downloadStatus.setText(progress + "% - " + formatFileSize(item.getDownloadedSize()));
            }
        }
    }

    private void hideDownloadBar() {
        if (downloadBar != null) {
            downloadBar.setVisibility(View.GONE);
        }
        currentDownload = null;
    }

    private void syncTabContainer() {
        // Update tab container and sync selection
        updateTabSelection();
        scrollToActiveTab();
    }

    private void handleDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        if (hasStoragePermission()) {
            webDownloader.startDownload(url, userAgent, contentDisposition, mimeType);
        } else {
            requestStoragePermission();
        }
    }

    private void cancelPendingSearchRequests() {
        if (suggestionProvider != null) {
            suggestionProvider.cancelPendingRequests();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_STORAGE);
    }

    private WebView getCurrentWebView() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex).getWebView();
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide search suggestions when activity is paused
        hideSuggestionsAndKeyboard();
        // Cancel any pending network requests to prevent memory leaks
        cancelPendingSearchRequests();
    }
    
    private void initializeUtilityManagers() {
        performanceOptimizer = PerformanceOptimizer.getInstance(this);
        privateBrowsingManager = PrivateBrowsingManager.getInstance(this);
        readingModeManager = ReadingModeManager.getInstance(this);
        userAgentManager = UserAgentManager.getInstance(this);
    }
    
    private void optimizePerformance() {
        // Preload common domains
        performanceOptimizer.preloadDomain("google.com");
        performanceOptimizer.preloadDomain("github.com");
        performanceOptimizer.preloadDomain("stackoverflow.com");
        
        // Clean up old cache periodically
        performanceOptimizer.cleanupPreloadCache();
    }
}

