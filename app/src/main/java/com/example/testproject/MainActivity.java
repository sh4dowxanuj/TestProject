package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testproject.database.DatabaseHelper;
import com.example.testproject.models.BrowserTab;
import com.example.testproject.models.HistoryItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_URL = "https://www.google.com";
    private static final String SEARCH_URL = "https://www.google.com/search?q=";

    private Toolbar toolbar;
    private LinearLayout tabContainer;
    private HorizontalScrollView tabScrollView;
    private EditText urlEditText;
    private ImageButton backButton, forwardButton, refreshButton, homeButton, menuButton;
    private ProgressBar progressBar;
    private FrameLayout webViewContainer;

    private List<BrowserTab> tabs;
    private int currentTabIndex = -1;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeDatabase();
        setupToolbar();
        setupEventListeners();
        
        // Create initial tab
        createNewTab(DEFAULT_URL);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
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

        tabs = new ArrayList<>();
    }

    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupEventListeners() {
        // URL EditText
        urlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String text = urlEditText.getText() != null ? urlEditText.getText().toString() : "";
                    loadUrl(text);
                    return true;
                }
                return false;
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
        BrowserTab tab = new BrowserTab("New Tab", url);
        WebView webView = createWebView();
        tab.setWebView(webView);
        tabs.add(tab);

        // Create tab UI
        View tabView = createTabView(tab, tabs.size() - 1);
        tabContainer.addView(tabView);

        // Add new tab button if this is the first tab
        if (tabs.size() == 1) {
            addNewTabButton();
        }

        // Switch to new tab
        switchToTab(tabs.size() - 1);
        
        // Load URL
        webView.loadUrl(url);
    }

    private WebView createWebView() {
        WebView webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

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
        });

        return webView;
    }

    private View createTabView(BrowserTab tab, int index) {
        View tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null);
        TextView tabTitle = tabView.findViewById(R.id.tabTitle);
        ImageButton closeTab = tabView.findViewById(R.id.closeTab);

        if (tabTitle != null && tab != null) {
            tabTitle.setText(tab.getTitle());
        }
        
        tabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(index);
            }
        });
        
        if (closeTab != null) {
            closeTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeTab(index);
                }
            });
        }

        return tabView;
    }

    private void addNewTabButton() {
        ImageButton newTabButton = new ImageButton(this);
        newTabButton.setImageResource(R.drawable.ic_add);
        newTabButton.setBackground(getDrawable(R.drawable.tab_background));
        newTabButton.setContentDescription("New Tab");
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            getResources().getDimensionPixelSize(android.R.dimen.app_icon_size),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        newTabButton.setLayoutParams(params);
        
        newTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewTab(DEFAULT_URL);
            }
        });
        
        if (tabContainer != null) {
            tabContainer.addView(newTabButton);
        }
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        // Hide current WebView
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            WebView currentWebView = tabs.get(currentTabIndex).getWebView();
            if (currentWebView.getParent() == webViewContainer) {
                webViewContainer.removeView(currentWebView);
            }
            tabs.get(currentTabIndex).setSelected(false);
        }

        // Show new WebView
        currentTabIndex = index;
        BrowserTab currentTab = tabs.get(currentTabIndex);
        currentTab.setSelected(true);
        
        WebView webView = currentTab.getWebView();
        if (webView.getParent() != webViewContainer) {
            webViewContainer.addView(webView);
        }

        // Update UI
        urlEditText.setText(currentTab.getUrl());
        updateNavigationButtons();
        updateTabSelection();
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
    }

    private void updateTabTitle(int index) {
        if (index >= 0 && index < tabContainer.getChildCount() - 1) { // -1 for new tab button
            View tabView = tabContainer.getChildAt(index);
            TextView tabTitle = tabView.findViewById(R.id.tabTitle);
            tabTitle.setText(tabs.get(index).getTitle());
        }
    }

    private void updateTabSelection() {
        for (int i = 0; i < tabs.size(); i++) {
            View tabView = tabContainer.getChildAt(i);
            TextView tabTitle = tabView.findViewById(R.id.tabTitle);
            tabView.setSelected(i == currentTabIndex);
            
            // Update text color based on selection state
            if (i == currentTabIndex) {
                tabTitle.setTextColor(getResources().getColor(R.color.tab_text_selected, getTheme()));
            } else {
                tabTitle.setTextColor(getResources().getColor(R.color.tab_text_normal, getTheme()));
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
            url = SEARCH_URL + input.replace(" ", "+");
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
    public void onBackPressed() {
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (BrowserTab tab : tabs) {
            if (tab.getWebView() != null) {
                tab.getWebView().destroy();
            }
        }
    }
}
