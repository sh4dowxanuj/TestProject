package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.Toast;
import android.os.Build;

/**
 * Reading mode utility for better content consumption
 */
public class ReadingModeManager {
    private static final String PREFS_NAME = "reading_mode_prefs";
    private static final String KEY_READING_MODE = "reading_mode_enabled";
    private static final String KEY_FONT_SIZE = "reading_font_size";
    private static final String KEY_BACKGROUND_COLOR = "reading_background";
    
    private static ReadingModeManager instance;
    private final SharedPreferences prefs;
    private final Context context;
    
    public enum BackgroundColor {
        WHITE("#FFFFFF", "#000000"),
        SEPIA("#F4F1E8", "#2F2F2F"),
        DARK("#1E1E1E", "#E0E0E0"),
        BLUE("#E3F2FD", "#1565C0");
        
        private final String backgroundColor;
        private final String textColor;
        
        BackgroundColor(String backgroundColor, String textColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
        
        public String getBackgroundColor() {
            return backgroundColor;
        }
        
        public String getTextColor() {
            return textColor;
        }
    }
    
    private ReadingModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized ReadingModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ReadingModeManager(context);
        }
        return instance;
    }
    
    /**
     * Toggle reading mode
     */
    public void toggleReadingMode(WebView webView) {
        boolean isEnabled = isReadingModeEnabled();
        setReadingModeEnabled(!isEnabled);
        
        if (!isEnabled) {
            enableReadingMode(webView);
        } else {
            disableReadingMode(webView);
        }
    }
    
    /**
     * Enable reading mode
     */
    public void enableReadingMode(WebView webView) {
        if (webView == null) return;
        
        setReadingModeEnabled(true);
        
        String readingModeScript = generateReadingModeScript();
        webView.evaluateJavascript(readingModeScript, null);
        
        Toast.makeText(context, "Reading mode enabled", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Disable reading mode
     */
    public void disableReadingMode(WebView webView) {
        if (webView == null) return;
        
        setReadingModeEnabled(false);
        
        String disableScript = "javascript:(function() {" +
            "var readingDiv = document.getElementById('reading-mode-container');" +
            "if (readingDiv) { readingDiv.remove(); }" +
            "document.body.style.display = 'block';" +
            "})()";
        
        webView.evaluateJavascript(disableScript, null);
        
        Toast.makeText(context, "Reading mode disabled", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Check if reading mode is enabled
     */
    public boolean isReadingModeEnabled() {
        return prefs.getBoolean(KEY_READING_MODE, false);
    }
    
    /**
     * Set reading mode enabled state
     */
    public void setReadingModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_READING_MODE, enabled).apply();
    }
    
    /**
     * Set reading font size
     */
    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply();
    }
    
    /**
     * Get reading font size
     */
    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, 18);
    }
    
    /**
     * Set background color
     */
    public void setBackgroundColor(BackgroundColor color) {
        prefs.edit().putString(KEY_BACKGROUND_COLOR, color.name()).apply();
    }
    
    /**
     * Get background color
     */
    public BackgroundColor getBackgroundColor() {
        String color = prefs.getString(KEY_BACKGROUND_COLOR, BackgroundColor.WHITE.name());
        try {
            return BackgroundColor.valueOf(color);
        } catch (IllegalArgumentException e) {
            return BackgroundColor.WHITE;
        }
    }
    
    /**
     * Generate reading mode JavaScript
     */
    private String generateReadingModeScript() {
        BackgroundColor bgColor = getBackgroundColor();
        int fontSize = getFontSize();
        
        return "javascript:(function() {" +
            "function createReadingMode() {" +
            "  var article = findMainContent();" +
            "  if (!article) return false;" +
            "  " +
            "  var container = document.createElement('div');" +
            "  container.id = 'reading-mode-container';" +
            "  container.style.cssText = '" +
            "    position: fixed;" +
            "    top: 0;" +
            "    left: 0;" +
            "    width: 100%;" +
            "    height: 100%;" +
            "    background-color: " + bgColor.getBackgroundColor() + ";" +
            "    color: " + bgColor.getTextColor() + ";" +
            "    font-size: " + fontSize + "px;" +
            "    font-family: Georgia, serif;" +
            "    line-height: 1.6;" +
            "    padding: 20px;" +
            "    box-sizing: border-box;" +
            "    overflow-y: auto;" +
            "    z-index: 999999;" +
            "  ';" +
            "  " +
            "  var content = document.createElement('div');" +
            "  content.style.cssText = '" +
            "    max-width: 800px;" +
            "    margin: 0 auto;" +
            "    padding: 20px;" +
            "  ';" +
            "  " +
            "  var title = document.querySelector('h1, .title, .headline, [class*=\"title\"]');" +
            "  if (title) {" +
            "    var titleClone = title.cloneNode(true);" +
            "    titleClone.style.cssText = '" +
            "      font-size: " + (fontSize + 8) + "px;" +
            "      font-weight: bold;" +
            "      margin-bottom: 20px;" +
            "      color: " + bgColor.getTextColor() + ";" +
            "    ';" +
            "    content.appendChild(titleClone);" +
            "  }" +
            "  " +
            "  var articleClone = article.cloneNode(true);" +
            "  cleanupContent(articleClone);" +
            "  content.appendChild(articleClone);" +
            "  " +
            "  var closeButton = document.createElement('button');" +
            "  closeButton.innerHTML = '✕';" +
            "  closeButton.style.cssText = '" +
            "    position: fixed;" +
            "    top: 20px;" +
            "    right: 20px;" +
            "    background: " + bgColor.getTextColor() + ";" +
            "    color: " + bgColor.getBackgroundColor() + ";" +
            "    border: none;" +
            "    width: 40px;" +
            "    height: 40px;" +
            "    border-radius: 50%;" +
            "    font-size: 20px;" +
            "    cursor: pointer;" +
            "    z-index: 1000000;" +
            "  ';" +
            "  closeButton.onclick = function() {" +
            "    container.remove();" +
            "    document.body.style.display = 'block';" +
            "  };" +
            "  " +
            "  container.appendChild(content);" +
            "  container.appendChild(closeButton);" +
            "  document.body.appendChild(container);" +
            "  document.body.style.display = 'none';" +
            "  return true;" +
            "}" +
            "" +
            "function findMainContent() {" +
            "  var selectors = [" +
            "    'article'," +
            "    '[role=\"main\"]'," +
            "    '.content'," +
            "    '.article'," +
            "    '.post'," +
            "    '.story'," +
            "    '.entry'," +
            "    '#content'," +
            "    '#main'," +
            "    '.main-content'" +
            "  ];" +
            "  " +
            "  for (var i = 0; i < selectors.length; i++) {" +
            "    var element = document.querySelector(selectors[i]);" +
            "    if (element && element.innerText.length > 200) {" +
            "      return element;" +
            "    }" +
            "  }" +
            "  " +
            "  var paragraphs = document.querySelectorAll('p');" +
            "  if (paragraphs.length > 3) {" +
            "    var container = document.createElement('div');" +
            "    for (var i = 0; i < paragraphs.length; i++) {" +
            "      if (paragraphs[i].innerText.length > 50) {" +
            "        container.appendChild(paragraphs[i].cloneNode(true));" +
            "      }" +
            "    }" +
            "    return container;" +
            "  }" +
            "  " +
            "  return null;" +
            "}" +
            "" +
            "function cleanupContent(element) {" +
            "  var toRemove = element.querySelectorAll('script, style, nav, header, footer, aside, .ad, .advertisement, .sidebar, .social');" +
            "  for (var i = 0; i < toRemove.length; i++) {" +
            "    toRemove[i].remove();" +
            "  }" +
            "  " +
            "  var allElements = element.querySelectorAll('*');" +
            "  for (var i = 0; i < allElements.length; i++) {" +
            "    allElements[i].style.color = '" + bgColor.getTextColor() + "';" +
            "    allElements[i].style.backgroundColor = 'transparent';" +
            "  }" +
            "}" +
            "" +
            "return createReadingMode();" +
            "})()";
    }
    
    /**
     * Check if current page supports reading mode
     */
    public boolean isReadingModeSupported(WebView webView) {
        if (webView == null) return false;
        
        String checkScript = "javascript:(function() {" +
            "var hasArticle = document.querySelector('article') !== null;" +
            "var hasMainContent = document.querySelector('[role=\"main\"], .content, .article, .post') !== null;" +
            "var hasParagraphs = document.querySelectorAll('p').length > 3;" +
            "return hasArticle || hasMainContent || hasParagraphs;" +
            "})()";
        
        webView.evaluateJavascript(checkScript, result -> {
            // Result will be handled in the callback
        });
        
        return true; // Optimistically return true for now
    }
}
