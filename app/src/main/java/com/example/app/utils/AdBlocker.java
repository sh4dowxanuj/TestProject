package com.example.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AdBlocker {
    private static final String TAG = "AdBlocker";
    private static final String PREFS_NAME = "adblock_prefs";
    private static final String KEY_ADBLOCK_ENABLED = "adblock_enabled";
    private static final String KEY_STEALTH_MODE = "stealth_mode";
    
    private final SharedPreferences prefs;
    private final Set<String> blockedDomains;
    private final Set<Pattern> blockedPatterns;
    private final Set<Pattern> scriptPatterns;
    private final ConcurrentHashMap<String, Boolean> domainCache;
    private final ConcurrentHashMap<String, Boolean> urlCache;
    
    // Common ad-serving domains and patterns
    private static final String[] AD_DOMAINS = {
        "googleads.g.doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "facebook.com/tr",
        "facebook.net/en_US/sdk.js",
        "amazon-adsystem.com",
        "adsystem.amazon.com",
        "serving-sys.com",
        "adsystem.amazon.de",
        "adsystem.amazon.co.uk",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "adsystem.amazon.fr",
        "adsystem.amazon.ca",
        "adsystem.amazon.co.jp",
        "adsystem.amazon.it",
        "adsystem.amazon.es",
        "outbrain.com",
        "taboola.com",
        "criteo.com",
        "adsystem.amazon.in",
        "adsystem.amazon.com.br",
        "adsystem.amazon.com.mx",
        "adsystem.amazon.com.au",
        "adsystem.amazon.sg",
        "adsystem.amazon.ae",
        "adsystem.amazon.nl",
        "adsystem.amazon.se",
        "adsystem.amazon.pl",
        "adsystem.amazon.com.tr",
        "adsystem.amazon.sa",
        "adsystem.amazon.eg",
        "doubleclick.net",
        "googletagservices.com",
        "googletagmanager.com",
        "googletag.googlesyndication.com",
        "tpc.googlesyndication.com",
        "cm.g.doubleclick.net",
        "stats.g.doubleclick.net",
        "ad.doubleclick.net",
        "static.doubleclick.net",
        "m.doubleclick.net",
        "mediavisor.doubleclick.net",
        "adadvisor.net",
        "adsystem.googlesyndication.com",
        "pagead2.googlesyndication.com",
        "partner.googleadservices.com",
        "service.google.com",
        "adnxs.com",
        "adsystem.adsystem.com",
        "adsystem.googleadservices.com",
        "adsystem.googletagservices.com",
        "adsystem.googletag.com",
        "adsystem.googlesyndication.com",
        "adsystem.doubleclick.net",
        "adsystem.google.com",
        "adsystem.gstatic.com",
        "adsystem.youtube.com",
        "adsystem.ytimg.com",
        "adform.net",
        "adsystem.adform.net",
        "adnxs.com",
        "adsystem.adnxs.com",
        "adsystem.appnexus.com",
        "adsystem.turn.com",
        "adsystem.nexage.com",
        "adsystem.millennial.com",
        "adsystem.mopub.com",
        "ads.yahoo.com",
        "adsystem.yahoo.com",
        "adsystem.yimg.com",
        "adsystem.flickr.com",
        "adsystem.tumblr.com",
        "adsystem.verizonmedia.com",
        "adsystem.aol.com",
        "adsystem.engadget.com",
        "adsystem.techcrunch.com",
        "adsystem.huffpost.com",
        "adsystem.msn.com",
        "adsystem.live.com",
        "adsystem.bing.com",
        "adsystem.microsoft.com",
        "adsystem.skype.com",
        "adsystem.outlook.com",
        "adsystem.xbox.com",
        "adsystem.windowsphone.com",
        "adsystem.surface.com",
        "adsystem.onedrive.com",
        "adsystem.office.com",
        "adsystem.sharepoint.com",
        "adsystem.teams.com",
        "adsystem.azure.com",
        "adsystem.linkedin.com",
        "adsystem.twitter.com",
        "adsystem.instagram.com",
        "adsystem.whatsapp.com",
        "adsystem.messenger.com",
        "adsystem.pinterest.com",
        "adsystem.snapchat.com",
        "adsystem.tiktok.com",
        "adsystem.reddit.com",
        "adsystem.discord.com",
        "adsystem.telegram.org",
        "adsystem.signal.org",
        "adsystem.wechat.com",
        "adsystem.line.me",
        "adsystem.kakaotalk.com",
        "adsystem.viber.com",
        "adsystem.slack.com",
        "adsystem.zoom.us",
        "adsystem.webex.com",
        "adsystem.gotomeeting.com",
        "adsystem.teamviewer.com",
        "adsystem.anydesk.com",
        "adsystem.logmein.com",
        "adsystem.citrix.com",
        "adsystem.vmware.com",
        "adsystem.parallels.com",
        "adsystem.virtualbox.org",
        "adsystem.docker.com",
        "adsystem.kubernetes.io",
        "adsystem.openshift.com",
        "adsystem.rancher.com",
        "adsystem.nomad.com",
        "adsystem.consul.io",
        "adsystem.vault.io",
        "adsystem.terraform.io",
        "adsystem.packer.io",
        "adsystem.vagrant.com",
        "adsystem.atlas.com",
        "adsystem.boundary.io",
        "adsystem.waypoint.io"
    };
    
    // Patterns for blocking ads
    private static final String[] AD_PATTERNS = {
        ".*\\.ads\\.",
        ".*\\.ad\\.",
        ".*ads.*",
        ".*doubleclick.*",
        ".*googleads.*",
        ".*googlesyndication.*",
        ".*googleadservices.*",
        ".*google-analytics.*",
        ".*facebook.*\\/tr",
        ".*amazon-adsystem.*",
        ".*\\/ads\\/.*",
        ".*\\/ad\\/.*",
        ".*\\/advertisement\\/.*",
        ".*\\/advertising\\/.*",
        ".*\\/adv\\/.*",
        ".*\\/adserver\\/.*",
        ".*\\/adservice\\/.*",
        ".*\\/adsystem\\/.*",
        ".*\\/adnxs\\/.*",
        ".*\\/adform\\/.*",
        ".*\\/adtech\\/.*",
        ".*\\/adroll\\/.*",
        ".*\\/adsense\\/.*",
        ".*\\/adwords\\/.*",
        ".*\\/admob\\/.*",
        ".*\\/adcolony\\/.*",
        ".*\\/unity3d\\/.*",
        ".*\\/unityads\\/.*",
        ".*\\/chartbeat\\/.*",
        ".*\\/quantserve\\/.*",
        ".*\\/scorecardresearch\\/.*",
        ".*\\/comscore\\/.*",
        ".*\\/nielsen\\/.*",
        ".*\\/googletagmanager\\/.*",
        ".*\\/googletagservices\\/.*",
        ".*\\/googletag\\/.*"
    };
    
    // Script patterns for enhanced blocking
    private static final String[] SCRIPT_PATTERNS = {
        ".*analytics.*\\.js",
        ".*tracking.*\\.js",
        ".*gtag.*\\.js",
        ".*gtm.*\\.js",
        ".*facebook.*\\.js",
        ".*twitter.*\\.js",
        ".*linkedin.*\\.js",
        ".*pinterest.*\\.js",
        ".*instagram.*\\.js",
        ".*tiktok.*\\.js",
        ".*snapchat.*\\.js",
        ".*reddit.*\\.js",
        ".*discord.*\\.js",
        ".*telegram.*\\.js",
        ".*signal.*\\.js",
        ".*whatsapp.*\\.js",
        ".*messenger.*\\.js",
        ".*viber.*\\.js",
        ".*line.*\\.js",
        ".*kakaotalk.*\\.js",
        ".*wechat.*\\.js",
        ".*amazon.*\\.js",
        ".*google.*\\.js",
        ".*microsoft.*\\.js",
        ".*yahoo.*\\.js",
        ".*bing.*\\.js",
        ".*yandex.*\\.js",
        ".*baidu.*\\.js",
        ".*naver.*\\.js",
        ".*daum.*\\.js",
        ".*sogou.*\\.js",
        ".*so.*\\.js",
        ".*360.*\\.js",
        ".*qq.*\\.js",
        ".*sina.*\\.js",
        ".*sohu.*\\.js",
        ".*163.*\\.js",
        ".*126.*\\.js",
        ".*139.*\\.js",
        ".*189.*\\.js",
        ".*10086.*\\.js",
        ".*10000.*\\.js",
        ".*10010.*\\.js",
        ".*taobao.*\\.js",
        ".*tmall.*\\.js",
        ".*alibaba.*\\.js",
        ".*alipay.*\\.js",
        ".*wechat.*\\.js",
        ".*qq.*\\.js",
        ".*weibo.*\\.js",
        ".*douyin.*\\.js",
        ".*kuaishou.*\\.js",
        ".*bilibili.*\\.js",
        ".*iqiyi.*\\.js",
        ".*youku.*\\.js",
        ".*tudou.*\\.js",
        ".*56.*\\.js",
        ".*ku6.*\\.js",
        ".*letv.*\\.js",
        ".*pptv.*\\.js",
        ".*sohu.*\\.js",
        ".*163.*\\.js",
        ".*126.*\\.js",
        ".*139.*\\.js",
        ".*189.*\\.js",
        ".*10086.*\\.js",
        ".*10000.*\\.js",
        ".*10010.*\\.js"
    };
    
    public AdBlocker(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        blockedDomains = new HashSet<>();
        blockedPatterns = new HashSet<>();
        scriptPatterns = new HashSet<>();
        domainCache = new ConcurrentHashMap<>();
        urlCache = new ConcurrentHashMap<>();
        
        try {
            // Initialize blocked domains
            for (String domain : AD_DOMAINS) {
                blockedDomains.add(domain.toLowerCase());
            }
            
            // Initialize blocked patterns
            for (String pattern : AD_PATTERNS) {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            }
            
            // Initialize script patterns
            for (String pattern : SCRIPT_PATTERNS) {
                scriptPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            }
            
            Log.d(TAG, "AdBlocker initialized with " + getTotalBlockingRules() + " rules");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdBlocker", e);
        }
    }
    
    public boolean isAdBlockEnabled() {
        return prefs.getBoolean(KEY_ADBLOCK_ENABLED, false); // Default disabled
    }
    
    public void setAdBlockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply();
    }
    
    public boolean isStealthModeEnabled() {
        return prefs.getBoolean(KEY_STEALTH_MODE, false); // Default disabled
    }
    
    public void setStealthModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_STEALTH_MODE, enabled).apply();
    }
    
    public WebResourceResponse shouldBlockRequest(WebResourceRequest request) {
        if (!isAdBlockEnabled() || request == null || request.getUrl() == null) {
            return null; // Don't block if disabled or invalid request
        }
        
        String url = request.getUrl().toString();
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Check cache first for better performance
        Boolean cachedResult = urlCache.get(url);
        if (cachedResult != null) {
            if (cachedResult) {
                incrementBlockedCount();
                return createEmptyResponse();
            }
            return null;
        }
        
        boolean shouldBlock = isBlockedInternal(url);
        
        // Cache the result (but limit cache size)
        if (urlCache.size() < 1000) {
            urlCache.put(url, shouldBlock);
        }
        
        if (shouldBlock) {
            incrementBlockedCount();
            return createEmptyResponse();
        }
        
        return null; // Don't block
    }
    
    private boolean isBlockedInternal(String url) {
        String lowerUrl = url.toLowerCase();
        String host = null;
        
        try {
            URL urlObj = new URL(url);
            host = urlObj.getHost();
            if (host != null) {
                host = host.toLowerCase();
            }
        } catch (Exception e) {
            // If URL parsing fails, still check patterns
        }
        
        // Check blocked domains
        if (host != null) {
            // Check domain cache first
            Boolean cachedDomainResult = domainCache.get(host);
            if (cachedDomainResult != null) {
                return cachedDomainResult;
            }
            
            boolean domainBlocked = false;
            for (String domain : blockedDomains) {
                if (host.contains(domain)) {
                    domainBlocked = true;
                    break;
                }
            }
            
            // Cache domain result
            if (domainCache.size() < 500) {
                domainCache.put(host, domainBlocked);
            }
            
            if (domainBlocked) {
                return true;
            }
        }
        
        // Check blocked patterns using find() instead of matches() for better performance
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(lowerUrl).find()) {
                return true;
            }
        }
        
        // Check script patterns for enhanced blocking
        if (isStealthModeEnabled()) {
            for (Pattern pattern : scriptPatterns) {
                if (pattern.matcher(lowerUrl).find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private WebResourceResponse createEmptyResponse() {
        try {
            return new WebResourceResponse("text/plain", "utf-8", 
                new ByteArrayInputStream("".getBytes("UTF-8")));
        } catch (Exception e) {
            // Fallback to default encoding
            return new WebResourceResponse("text/plain", "utf-8", 
                new ByteArrayInputStream("".getBytes()));
        }
    }
    
    public boolean isBlocked(String url) {
        if (!isAdBlockEnabled() || url == null || url.isEmpty()) {
            return false;
        }
        
        return isBlockedInternal(url);
    }
    
    public int getBlockedCount() {
        return prefs.getInt("blocked_count", 0);
    }
    
    public void incrementBlockedCount() {
        synchronized (this) {
            int count = getBlockedCount();
            prefs.edit().putInt("blocked_count", count + 1).apply();
        }
    }
    
    public void resetBlockedCount() {
        prefs.edit().putInt("blocked_count", 0).apply();
        // Clear caches when resetting
        clearCache();
    }
    
    public void clearCache() {
        domainCache.clear();
        urlCache.clear();
    }
    
    // Check if ad blocker is properly initialized
    public boolean isInitialized() {
        return blockedDomains != null && 
               blockedPatterns != null && 
               scriptPatterns != null &&
               !blockedDomains.isEmpty() &&
               !blockedPatterns.isEmpty() &&
               !scriptPatterns.isEmpty();
    }
    
    // Get total number of blocked domains and patterns
    public int getTotalBlockingRules() {
        return blockedDomains.size() + blockedPatterns.size() + scriptPatterns.size();
    }
    
    // Get statistics
    public String getStatistics() {
        try {
            int blockedCount = getBlockedCount();
            boolean isEnabled = isAdBlockEnabled();
            boolean isStealthEnabled = isStealthModeEnabled();
            
            return String.format("Ad Blocker: %s\nStealth Mode: %s\nBlocked: %d ads", 
                isEnabled ? "ON" : "OFF", 
                isStealthEnabled ? "ON" : "OFF", 
                blockedCount);
        } catch (Exception e) {
            return "Ad Blocker: Error retrieving statistics";
        }
    }
}
