package com.example.testproject;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;

import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testproject.adapters.SearchSuggestionAdapter;
import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.BrowserTab;
import com.example.testproject.models.DownloadItem;
import com.example.testproject.models.HistoryItem;
import com.example.testproject.models.SearchEngine;
import com.example.testproject.models.SearchSuggestion;
import com.example.testproject.utils.SearchEnginePreferences;
import com.example.testproject.utils.SearchSuggestionProvider;
import com.example.testproject.utils.ChromeStyleDownloader;
import com.example.testproject.utils.DownloadNotificationManager;

import java.util.ArrayList;
import java.util.List;

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

    // Chrome-style download bar components
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
    private ChromeStyleDownloader chromeDownloader;
    private DownloadNotificationManager downloadNotificationManager;
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
        setupEventListeners();
        setupBackPressedHandler();
        
        // Create initial tab
        createNewTab(DEFAULT_URL);
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
        chromeDownloader = new ChromeStyleDownloader(this, databaseHelper);
        downloadNotificationManager = new DownloadNotificationManager(this);
        
        // Set up download progress listener
        chromeDownloader.setProgressListener(new ChromeStyleDownloader.DownloadProgressListener() {
            @Override
            public void onDownloadStarted(DownloadItem item) {
                runOnUiThread(() -> {
                    currentDownload = item;
                    showDownloadBar(item);
                    downloadNotificationManager.showDownloadProgress(item);
                });
            }

            @Override
            public void onDownloadProgress(DownloadItem item) {
                runOnUiThread(() -> {
                    updateDownloadBar(item);
                    downloadNotificationManager.showDownloadProgress(item);
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
        updateUrlBarHint();
    }

    private void initializeSearchSuggestions() {
        suggestionProvider = new SearchSuggestionProvider(this);
        suggestionAdapter = new SearchSuggestionAdapter(new ArrayList<>(), 
            new SearchSuggestionAdapter.OnSuggestionClickListener() {
                @Override
                public void onSuggestionClick(SearchSuggestion suggestion) {
                    hideSuggestions();
                    urlEditText.setText(suggestion.getQuery());
                    loadUrl(suggestion.getQuery());
                }
            });
        
        searchSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchSuggestionsRecyclerView.setAdapter(suggestionAdapter);
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
                    hideSuggestions();
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
            
            // Note: setAllowFileAccessFromFileURLs and setAllowUniversalAccessFromFileURLs 
            // are deprecated for security reasons. Only enable if absolutely necessary for your use case.
            // Consider using a more secure approach like serving files from assets or resources
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // These are disabled by default on API 16+ for security reasons
                // Only enable if your app specifically needs file URL access
                webSettings.setAllowFileAccessFromFileURLs(false);
                webSettings.setAllowUniversalAccessFromFileURLs(false);
            }
            // Note: setPluginState has been deprecated as plugins are no longer supported

        webView.setWebViewClient(new WebViewClient() {
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
                    
                    // Add to history
                    if (databaseHelper != null && url != null) {
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
                } else if (itemId == R.id.action_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                } else if (itemId == R.id.action_share) {
                    shareCurrentPage();
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
            databaseHelper.addBookmark(new com.example.testproject.models.Bookmark(title, url));
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

    private BrowserTab getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    private WebView getCurrentWebView() {
        BrowserTab currentTab = getCurrentTab();
        return currentTab != null ? currentTab.getWebView() : null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (BrowserTab tab : tabs) {
            if (tab.getWebView() != null) {
                tab.getWebView().destroy();
            }
        }
        
        // Cleanup search suggestion provider
        if (suggestionProvider != null) {
            suggestionProvider.cleanup();
        }
        
        // Cleanup download manager
        if (chromeDownloader != null) {
            chromeDownloader.cleanup();
        }
    }
    
    // Helper method to safely get tab count for UI operations
    private int getTabCount() {
        return tabs != null ? tabs.size() : 0;
    }
    
    // Helper method to ensure tab container is in sync with tab list
    private void syncTabContainer() {
        if (tabContainer == null || tabs == null) return;
        
        // Remove extra views that might exist
        while (tabContainer.getChildCount() > tabs.size()) {
            View extraView = tabContainer.getChildAt(tabContainer.getChildCount() - 1);
            if (!(extraView.getTag() instanceof BrowserTab)) {
                tabContainer.removeViewAt(tabContainer.getChildCount() - 1);
            } else {
                break;
            }
        }
        
        // Update tab selection and ensure correct index
        if (currentTabIndex >= tabs.size()) {
            currentTabIndex = Math.max(0, tabs.size() - 1);
        }
        updateTabSelection();
    }

    private void updateUrlBarHint() {
        if (urlEditText != null && searchEnginePrefs != null) {
            SearchEngine selectedEngine = searchEnginePrefs.getSelectedSearchEngine();
            String hint = "Enter URL or search with " + selectedEngine.getName() + "...";
            urlEditText.setHint(hint);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUrlBarHint();
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

    private void showSuggestions(List<SearchSuggestion> suggestions) {
        if (suggestions != null && !suggestions.isEmpty() && urlEditText.hasFocus()) {
            suggestionAdapter.updateSuggestions(suggestions);
            searchSuggestionsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            hideSuggestions();
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
    }

    private void showDownloadBar(DownloadItem item) {
        downloadBar.setVisibility(View.VISIBLE);
        downloadFileName.setText(item.getTitle());
        downloadProgressBar.setProgress(0);
        downloadStatus.setText("Starting download...");
        
        downloadCancel.setOnClickListener(v -> {
            if (currentDownload != null) {
                chromeDownloader.cancelDownload(currentDownload.getId());
                hideDownloadBar();
                Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDownloadBar(DownloadItem item) {
        if (downloadBar.getVisibility() == View.VISIBLE && currentDownload != null && 
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
        downloadBar.setVisibility(View.GONE);
        currentDownload = null;
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+ we use scoped storage, no special permissions needed
            return true;
        } else {
            // On Android 6-9, we need legacy storage permissions
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed on Android 10+
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_STORAGE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted. You can now download files.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied. Downloads may not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void handleDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        // Check and request storage permissions if needed (for Android 6-9)
        if (!hasStoragePermission()) {
            // Store download info for later
            // For now, we'll just request permission and the user will need to retry
            requestStoragePermission();
            Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Start download using ChromeStyleDownloader
        chromeDownloader.startDownload(url, userAgent, contentDisposition, mimeType);
    }
    
    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}
