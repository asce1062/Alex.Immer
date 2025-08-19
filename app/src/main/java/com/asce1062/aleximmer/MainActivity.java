package com.asce1062.aleximmer;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.WindowInsetsController;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    WebView myWeb;

    // JavaScript Interface for website-to-Android communication
    public class WebAppInterface {
        @JavascriptInterface
        public void updateTheme(String theme) {
            Log.d("MainActivity", "Website theme changed to: " + theme);

            // Update complete theme (status bar + topography background) on UI thread
            runOnUiThread(() -> {
                boolean isDark = theme.equals("dark");
                updateStatusBarForWebsiteTheme(isDark);
            });
        }

        @JavascriptInterface
        public void notifyThemeReady() {
            Log.d("MainActivity", "Website theme system is ready");
        }

        @JavascriptInterface
        public void downloadBase64(String base64Data, String fileName) {
            Log.d("MainActivity", "Received base64 download request for: " + fileName);
            runOnUiThread(() -> saveBase64ToFile(base64Data, fileName, null));
        }

        @JavascriptInterface
        public void downloadBase64WithAlbum(String base64Data, String fileName, String albumName) {
            Log.d("MainActivity", "Received base64 download request for: " + fileName + " (Album: " + albumName + ")");
            runOnUiThread(() -> saveBase64ToFile(base64Data, fileName, albumName));
        }

        @JavascriptInterface
        public void downloadError(String errorMessage) {
            Log.e("MainActivity", "Download error from JavaScript: " + errorMessage);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download failed: " + errorMessage, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the correct theme before content view
        applyThemeBasedOnSystemSettings();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Configure system bars AFTER setContentView to ensure window is ready
        configureSystemBars();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply top padding to avoid WebView offset issues
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        setupWebView();
        setupBackPressedDispatcher();
    }

    private void updateStatusBarForWebsiteTheme(boolean isDarkTheme) {
        WindowInsetsController windowInsetsController = getWindow().getInsetsController();

        Log.d("MainActivity", "Updating theme for website toggle - Dark: " + isDarkTheme);

        if (isDarkTheme) {
            // Dark theme
            // Light icons on dark background + dark topography
            int darkStatusColor = getColor(R.color.status_bar_dark);
            int darkNavColor = getColor(R.color.navigation_bar_dark);

            getWindow().setStatusBarColor(darkStatusColor);
            getWindow().setNavigationBarColor(darkNavColor);

            // Update background to dark topography pattern
            getWindow().setBackgroundDrawableResource(R.drawable.topography_background_dark);

            if (windowInsetsController != null) {
                windowInsetsController.setSystemBarsAppearance(
                    0, // Clear light appearance flags
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            // Light theme 
            // Dark icons on light background + light topography
            int lightStatusColor = getColor(R.color.status_bar_light);
            int lightNavColor = getColor(R.color.navigation_bar_light);

            getWindow().setStatusBarColor(lightStatusColor);
            getWindow().setNavigationBarColor(lightNavColor);

            // Update background to light topography pattern
            getWindow().setBackgroundDrawableResource(R.drawable.topography_background_light);

            if (windowInsetsController != null) {
                windowInsetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        }
    }

    private void applyThemeBasedOnSystemSettings() {
        // Check if dark mode is enabled
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                // Dark theme is already the default in themes.xml
                break;
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // Let the DayNight theme handle this automatically
                break;
        }
    }

    private void configureSystemBars() {
        WindowInsetsController windowInsetsController = getWindow().getInsetsController();

        // Check if we're in light mode to adjust colors and appearance
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isLightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_NO;

        Log.d("MainActivity", "Configuring system bars - Light mode: " + isLightMode);

        if (isLightMode) {
            // Light theme
            // Dark icons on light background
            int lightStatusColor = getColor(R.color.status_bar_light);
            int lightNavColor = getColor(R.color.navigation_bar_light);

            Log.d("MainActivity", "Light theme colors - Status: " + Integer.toHexString(lightStatusColor) +
                  ", Nav: " + Integer.toHexString(lightNavColor));

            getWindow().setStatusBarColor(lightStatusColor);
            getWindow().setNavigationBarColor(lightNavColor);

            // Force light appearance for system bars
            if (windowInsetsController != null) {
                windowInsetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
                Log.d("MainActivity", "Applied light appearance to system bars");
            }
        } else {
            // Dark theme
            // Light icons on dark background
            int darkStatusColor = getColor(R.color.status_bar_dark);
            int darkNavColor = getColor(R.color.navigation_bar_dark);

            Log.d("MainActivity", "Dark theme colors - Status: " + Integer.toHexString(darkStatusColor) +
                  ", Nav: " + Integer.toHexString(darkNavColor));

            getWindow().setStatusBarColor(darkStatusColor);
            getWindow().setNavigationBarColor(darkNavColor);

            // Clear light appearance flags for dark theme
            if (windowInsetsController != null) {
                windowInsetsController.setSystemBarsAppearance(
                    0, // Clear all light appearance flags
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
                Log.d("MainActivity", "Applied dark appearance to system bars");
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        myWeb = findViewById(R.id.myWeb);
        WebSettings webSettings = myWeb.getSettings();

        // Essential WebView settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Basic settings only
        // Let website handle responsive behavior naturally
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);

        // Add JavaScript interface for website-to-Android communication
        myWeb.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setForceDark(WebSettings.FORCE_DARK_OFF);

        // Set background to transparent to show the topography pattern behind
        myWeb.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // Set up download listener for file downloads
        myWeb.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                Log.d("MainActivity", "Download started for: " + url);
                downloadFile(url, userAgent, contentDisposition, mimeType);
            }
        });

        // Custom WebViewClient to handle navigation
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle mailto links
                // Open with email app chooser
                if (url.startsWith("mailto:")) {
                    try {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(Uri.parse(url));
                        startActivity(Intent.createChooser(emailIntent, "Send email"));
                        return true; // Prevent WebView from handling
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error opening email app: " + e.getMessage());
                        return true;
                    }
                }

                // Handle tel links
                // Open with phone app
                if (url.startsWith("tel:")) {
                    try {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                        dialIntent.setData(Uri.parse(url));
                        startActivity(dialIntent);
                        return true; // Prevent WebView from handling
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error opening phone app: " + e.getMessage());
                        return true;
                    }
                }

                // Keep navigation within the WebView for our domain
                if (url.contains("asce1062.github.io")) {
                    // Return false to let WebView handle the URL loading
                    return false;
                }

                // Handle external HTTP/HTTPS links
                // Open in browser
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                        return true; // Prevent WebView from handling
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error opening browser: " + e.getMessage());
                        return true;
                    }
                }

                // For any other schemes, try to handle with appropriate app
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Log.e("MainActivity", "Error handling URL: " + url + " - " + e.getMessage());
                    return true; // Prevent WebView from handling unsupported URLs
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Sync system theme with website and set up listener
                if (myWeb != null) {
                    myWeb.postDelayed(() -> syncSystemThemeToWebsite(), 500);
                    myWeb.postDelayed(() -> injectThemeListener(), 1000);
                    myWeb.postDelayed(() -> injectBlobInterceptor(), 1500);
                }
            }
        });

        // Load our website
        myWeb.loadUrl("https://asce1062.github.io/");
    }

    private void syncSystemThemeToWebsite() {
        // System and website theme sync
        // Update website theme classes without layout modifications
        String themeScript = "javascript:(function() {" +
            "const isDarkMode = " + isDarkModeEnabled() + ";" +
            "const html = document.documentElement;" +
            "if (isDarkMode) {" +
                "html.classList.add('dark');" +
                "html.setAttribute('data-theme', 'dark');" +
            "} else {" +
                "html.classList.remove('dark');" +
                "html.setAttribute('data-theme', 'light');" +
            "}" +
            "console.log('Android system theme synced to website:', isDarkMode ? 'dark' : 'light');" +
            "})();";

        if (myWeb != null) {
            myWeb.loadUrl(themeScript);
        }
    }

    private void injectThemeListener() {
        // JavaScript to listen for website theme toggle and notify Android
        String themeListenerScript = "javascript:(function() {" +
            "console.log('Setting up theme listener for Android');" +

            // Function to notify Android of theme changes
            "function notifyAndroidTheme() {" +
                "const isDark = document.documentElement.classList.contains('dark');" +
                "const theme = isDark ? 'dark' : 'light';" +
                "console.log('Notifying Android of theme change:', theme);" +
                "if (window.AndroidInterface) {" +
                    "window.AndroidInterface.updateTheme(theme);" +
                "}" +
            "}" +

            // Set up MutationObserver to watch for class changes on html element
            "const observer = new MutationObserver(function(mutations) {" +
                "mutations.forEach(function(mutation) {" +
                    "if (mutation.type === 'attributes' && mutation.attributeName === 'class') {" +
                        "notifyAndroidTheme();" +
                    "}" +
                "});" +
            "});" +

            // Start observing
            "observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });" +

            // Also listen for data-theme attribute changes
            "const themeObserver = new MutationObserver(function(mutations) {" +
                "mutations.forEach(function(mutation) {" +
                    "if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {" +
                        "notifyAndroidTheme();" +
                    "}" +
                "});" +
            "});" +

            "themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });" +

            // Initial notification
            "notifyAndroidTheme();" +

            // Let Android know the theme system is ready
            "if (window.AndroidInterface) {" +
                "window.AndroidInterface.notifyThemeReady();" +
            "}" +

            "console.log('Theme listener setup complete');" +
            "})();";

        if (myWeb != null) {
            myWeb.loadUrl(themeListenerScript);
        }
    }

    private boolean isDarkModeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void downloadFile(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            // Generate filename from URL or content disposition
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

            Log.d("MainActivity", "Starting download: " + fileName + " from " + url);

            // Handle blob URLs by converting them to downloadable format
            if (url.startsWith("blob:")) {
                Log.d("MainActivity", "Detected blob URL, converting to downloadable format");
                convertBlobToDownload(url, fileName);
                return;
            }

            // Create download request for regular HTTP/HTTPS URLs
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            // Set user agent if provided
            if (userAgent != null && !userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            // Add headers for our domain
            // Handle authentication/CORS if needed
            if (url.contains("asce1062.github.io")) {
                request.addRequestHeader("Referer", "https://asce1062.github.io/");
            }

            // Configure download settings
            request.setDescription("Downloading " + fileName);
            request.setTitle(fileName);
            // Files in Downloads are automatically scanned by MediaStore on API 29+
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Set destination to Downloads folder
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            // Get download manager and enqueue download
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                long downloadId = downloadManager.enqueue(request);
                Toast.makeText(this, "Downloading " + fileName, Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Download enqueued with ID: " + downloadId + " - " + fileName);
            } else {
                Toast.makeText(this, "Download failed - Download manager not available", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Download manager is null");
            }

        } catch (SecurityException e) {
            Toast.makeText(this, "Download failed - Permission denied", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Download security error: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Download error: " + e.getMessage());
        }
    }

    private void injectBlobInterceptor() {
        // JavaScript to intercept blob creation and store blob data for downloads
        String blobInterceptorScript = "javascript:(function() {" +
            "console.log('Setting up blob interceptor for Android downloads');" +

            // Store original URL.createObjectURL and fetch
            "const originalCreateObjectURL = URL.createObjectURL;" +
            "const originalFetch = window.fetch;" +
            "const blobStore = new Map();" +
            "const urlToFilename = new Map();" +
            "let lastFetchedUrl = null;" +
            "const downloadedTracks = new Set();" +
            "let zipGenerationContext = { isAlbum: false, albumName: null, trackCount: 0 };" +

            // Override fetch to capture the original filename from successful audio fetches
            "window.fetch = function(...args) {" +
                "const url = args[0];" +
                "return originalFetch.apply(this, args).then(response => {" +
                    "if (response.ok && url && (url.includes('.mp3') || url.includes('.flac') || url.includes('.wav') || url.includes('.zip'))) {" +
                        "lastFetchedUrl = url;" +
                        "console.log('Audio fetch completed for:', url);" +

                        // Track downloaded tracks for ZIP generation
                        "if (url.includes('/audio/') && (url.includes('.mp3') || url.includes('.flac') || url.includes('.wav'))) {" +
                            "downloadedTracks.add(url);" +

                            // Extract album name from URL pattern /audio/{albumName}/track.mp3
                            "const albumMatch = url.match(/\\/audio\\/([^/]+)\\//); " +
                            "if (albumMatch) {" +
                                "const currentAlbum = albumMatch[1];" +
                                "if (!zipGenerationContext.albumName) {" +
                                    "zipGenerationContext.albumName = currentAlbum;" +
                                    "zipGenerationContext.isAlbum = true;" +
                                "} else if (zipGenerationContext.albumName !== currentAlbum) {" +
                                    // Multiple albums = custom selection
                                    "zipGenerationContext.isAlbum = false;" +
                                "}" +
                                "zipGenerationContext.trackCount = downloadedTracks.size;" +
                                "console.log('ZIP context updated:', zipGenerationContext);" +
                            "}" +
                        "}" +
                    "}" +
                    "return response;" +
                "});" +
            "};" +

            // Override URL.createObjectURL to store blob data and associate with filename
            "URL.createObjectURL = function(object) {" +
                "const url = originalCreateObjectURL.call(this, object);" +
                "if (object instanceof Blob) {" +
                    "console.log('Blob created with URL:', url, 'Size:', object.size);" +
                    "blobStore.set(url, object);" +

                    // Handle ZIP files with intelligent naming. Only treat as ZIP if:
                    // 1. Explicitly ZIP MIME type, OR
                    // 2. Multiple tracks in download context
                    "const isLikelyZip = object.type === 'application/zip' || " +
                        "zipGenerationContext.trackCount > 1;" +
                    "if (isLikelyZip) {" +
                        "let zipFilename;" +
                        "if (zipGenerationContext.isAlbum && zipGenerationContext.albumName) {" +
                            "zipFilename = zipGenerationContext.albumName.replace(/[^a-zA-Z0-9\\s-_]/g, '_') + '.zip';" +
                            "console.log('Album ZIP detected, using filename:', zipFilename);" +
                        "} else if (zipGenerationContext.trackCount > 0) {" +
                            "const today = new Date().toISOString().split('T')[0];" +
                            "zipFilename = 'alex_immer_selected_tracks_' + today + '.zip';" +
                            "console.log('Custom selection ZIP detected, using filename:', zipFilename);" +
                        "} else {" +
                            "const today = new Date().toISOString().split('T')[0];" +
                            "zipFilename = 'alex_immer_download_' + today + '.zip';" +
                        "}" +
                        "urlToFilename.set(url, zipFilename);" +
                        "console.log('ZIP blob associated with filename:', zipFilename);" +

                        // Reset context after ZIP creation
                        "downloadedTracks.clear();" +
                        "zipGenerationContext = { isAlbum: false, albumName: null, trackCount: 0 };" +
                    "}" +
                    // Handle individual audio files
                    "else if (object.type.startsWith('audio/')) {" + // Audio MIME type
                        "let filename = null;" +
                        "if (lastFetchedUrl && (lastFetchedUrl.includes('.mp3') || lastFetchedUrl.includes('.flac') || lastFetchedUrl.includes('.wav'))) {" +
                            "filename = lastFetchedUrl.split('/').pop();" +
                            "lastFetchedUrl = null;" + // Reset after use
                        "} else {" +
                            // Fallback: look for the most recent audio download in our tracked downloads
                            "const audioTracks = Array.from(downloadedTracks).filter(url => " +
                                "url.includes('.mp3') || url.includes('.flac') || url.includes('.wav'));" +
                            "if (audioTracks.length > 0) {" +
                                // Get the most recent one (last in the array)
                                "const mostRecentTrack = audioTracks[audioTracks.length - 1];" +
                                "filename = mostRecentTrack.split('/').pop();" +
                                "console.log('Using fallback filename from recent downloads:', filename);" +
                            "}" +
                        "}" +
                        "if (filename) {" +
                            "urlToFilename.set(url, filename);" +
                            "console.log('Audio blob associated with filename:', filename);" +
                        "} else {" +
                            "console.log('Audio blob created but no filename found - will use blob UUID');" +
                        "}" +
                    "}" +
                "}" +
                "return url;" +
            "};" +

            // Add helper function to download blob by URL
            "window.downloadBlobByUrl = function(blobUrl, fallbackFileName) {" +
                "console.log('Attempting to download blob:', blobUrl);" +
                "const blob = blobStore.get(blobUrl);" +
                "if (blob) {" +
                    "const actualFileName = urlToFilename.get(blobUrl) || fallbackFileName;" +
                    "console.log('Found stored blob, using filename:', actualFileName);" +
                    "let albumName = null;" +
                    "if (actualFileName && (actualFileName.includes('.mp3') || actualFileName.includes('.flac') || actualFileName.includes('.wav'))) {" +
                        "for (const trackedUrl of downloadedTracks) {" +
                            "if (trackedUrl.endsWith(actualFileName)) {" +
                                "const albumMatch = trackedUrl.match(/\\/audio\\/([^/]+)\\//); " +
                                "if (albumMatch) {" +
                                    "albumName = albumMatch[1];" +
                                    "console.log('Extracted album name for audio file:', albumName);" +
                                    "break;" +
                                "}" +
                            "}" +
                        "}" +
                    "}" +

                    "const reader = new FileReader();" +
                    "reader.onload = function() {" +
                        "const base64Data = reader.result.split(',')[1];" +
                        "console.log('Blob converted to base64, size:', base64Data.length);" +
                        "if (window.AndroidInterface) {" +
                            "if (albumName) {" +
                                "window.AndroidInterface.downloadBase64WithAlbum(base64Data, actualFileName, albumName);" +
                            "} else {" +
                                "window.AndroidInterface.downloadBase64(base64Data, actualFileName);" +
                            "}" +
                        "}" +
                    "};" +
                    "reader.onerror = function() {" +
                        "console.error('FileReader error');" +
                        "if (window.AndroidInterface) {" +
                            "window.AndroidInterface.downloadError('FileReader failed');" +
                        "}" +
                    "};" +
                    "reader.readAsDataURL(blob);" +
                "} else {" +
                    "console.error('Blob not found in store for URL:', blobUrl);" +
                    "if (window.AndroidInterface) {" +
                        "window.AndroidInterface.downloadError('Blob not found in store');" +
                    "}" +
                "}" +
            "};" +

            "console.log('Blob interceptor setup complete');" +
            "})();";

        if (myWeb != null) {
            myWeb.loadUrl(blobInterceptorScript);
        }
    }

    private void convertBlobToDownload(String blobUrl, String fileName) {
        // Try interceptor first, then fallback to fetch approach
        String blobDownloadScript = "javascript:(function() {" +
            "console.log('Converting blob URL to download:', '" + blobUrl + "');" +

            // Try interceptor method first
            "if (typeof window.downloadBlobByUrl === 'function') {" +
                "console.log('Using blob interceptor method');" +
                "window.downloadBlobByUrl('" + blobUrl + "', '" + fileName + "');" +
                "return;" +
            "}" +

            "console.log('Blob interceptor not available, using fetch method');" +

            // Try immediate fetch with timeout
            "const controller = new AbortController();" +
            "const timeoutId = setTimeout(() => controller.abort(), 10000);" + // 10 second timeout

            "fetch('" + blobUrl + "', { signal: controller.signal })" +
                ".then(response => {" +
                    "clearTimeout(timeoutId);" +
                    "if (!response.ok) {" +
                        "throw new Error('Response not OK: ' + response.status);" +
                    "}" +
                    "return response.blob();" +
                "})" +
                ".then(blob => {" +
                    "console.log('Blob fetched successfully, size:', blob.size);" +
                    "return new Promise((resolve, reject) => {" +
                        "const reader = new FileReader();" +
                        "reader.onload = () => resolve(reader.result);" +
                        "reader.onerror = () => reject(new Error('FileReader failed'));" +
                        "reader.readAsDataURL(blob);" +
                    "});" +
                "})" +
                ".then(dataUrl => {" +
                    "const base64Data = dataUrl.split(',')[1];" +
                    "console.log('Blob converted to base64, size:', base64Data.length);" +
                    "if (window.AndroidInterface) {" +
                        "window.AndroidInterface.downloadBase64(base64Data, '" + fileName + "');" +
                    "} else {" +
                        "console.error('AndroidInterface not available');" +
                    "}" +
                "})" +
                ".catch(error => {" +
                    "clearTimeout(timeoutId);" +
                    "console.error('Error converting blob:', error);" +
                    "if (window.AndroidInterface) {" +
                        "window.AndroidInterface.downloadError('Blob conversion failed: ' + error.message);" +
                    "}" +
                "});" +
            "})();";

        if (myWeb != null) {
            myWeb.loadUrl(blobDownloadScript);
        }
    }

    private void saveBase64ToFile(String base64Data, String fileName, String albumName) {
        try {
            // Decode base64 data
            byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);

            File targetDir;
            File file;

            // Determine if this is an audio file and has album info
            boolean isAudioFile = fileName.toLowerCase().endsWith(".mp3") ||
                                fileName.toLowerCase().endsWith(".flac") ||
                                fileName.toLowerCase().endsWith(".wav");

            Log.d("MainActivity", "File analysis - isAudioFile: " + isAudioFile + ", albumName: '" + albumName + "', fileName: " + fileName);

            if (isAudioFile && albumName != null && !albumName.trim().isEmpty()) {
                // Save audio files to Music/{AlbumName}/ directory
                File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

                // Sanitize album name for filesystem
                String sanitizedAlbumName = albumName.replaceAll("[^a-zA-Z0-9\\s\\-_.]", "_").trim();
                targetDir = new File(musicDir, sanitizedAlbumName);

                if (!targetDir.exists()) {
                    boolean created = targetDir.mkdirs();
                    if (!created) {
                        throw new IOException("Failed to create Music/" + sanitizedAlbumName + " directory");
                    }
                    Log.d("MainActivity", "Created album directory: " + targetDir.getAbsolutePath());
                }

                file = new File(targetDir, fileName);
                Log.d("MainActivity", "Saving audio file to Music album directory: " + file.getAbsolutePath());
            } else {
                // Save non-audio files or files without album info to Downloads
                targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!targetDir.exists()) {
                    boolean created = targetDir.mkdirs();
                    if (!created) {
                        throw new IOException("Failed to create Downloads directory");
                    }
                }
                file = new File(targetDir, fileName);
                Log.d("MainActivity", "Saving file to Downloads: " + file.getAbsolutePath());
            }

            // Check if file already exists for appropriate messaging
            boolean fileExists = file.exists();

            // Write data to file (this will overwrite if exists)
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(decodedData);
                fos.flush();
            }

            // Show appropriate success message
            if (fileExists) {
                Toast.makeText(this, "Updated " + fileName, Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Successfully updated existing file: " + file.getAbsolutePath());
            } else {
                Toast.makeText(this, "Downloaded " + fileName, Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Successfully saved new file: " + file.getAbsolutePath());
            }

            // Register file with MediaStore for modern Android versions
            addFileToMediaStore(file, fileName, albumName, isAudioFile);

        } catch (IOException e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Error saving base64 file: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Unexpected error saving file: " + e.getMessage());
        }
    }

    private void addFileToMediaStore(File file, String fileName, String albumName, boolean isAudioFile) {
        try {
            ContentResolver contentResolver = getContentResolver();
            ContentValues values = new ContentValues();

            // Determine MIME type based on file extension
            String mimeType = "application/octet-stream"; // Default
            if (fileName.toLowerCase().endsWith(".mp3")) {
                mimeType = "audio/mpeg";
            } else if (fileName.toLowerCase().endsWith(".flac")) {
                mimeType = "audio/flac";
            } else if (fileName.toLowerCase().endsWith(".wav")) {
                mimeType = "audio/wav";
            } else if (fileName.toLowerCase().endsWith(".zip")) {
                mimeType = "application/zip";
            }

            // Set file metadata
            // DATA field is deprecated
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.SIZE, file.length());

            if (isAudioFile && albumName != null && !albumName.trim().isEmpty()) {
                // Registered audio files in Music/{Album}/ directory with MediaStore
                String sanitizedAlbumName = albumName.replaceAll("[^a-zA-Z0-9\\s\\-_.]", "_").trim();
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/" + sanitizedAlbumName);
                values.put(MediaStore.Audio.Media.IS_DOWNLOAD, 1);
                values.put(MediaStore.Audio.Media.ALBUM, albumName); // Set album metadata

                Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                // Insert into MediaStore
                Uri uri = contentResolver.insert(collection, values);
                if (uri != null) {
                    Log.d("MainActivity", "Audio file registered with MediaStore: " + uri);
                } else {
                    Log.w("MainActivity", "Failed to register audio file with MediaStore");
                }
            } else if (!mimeType.startsWith("audio/")) {
                // Non-audio files in Downloads folder
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

                // Insert into MediaStore
                Uri uri = contentResolver.insert(collection, values);
                if (uri != null) {
                    Log.d("MainActivity", "File registered with MediaStore: " + uri);
                } else {
                    Log.w("MainActivity", "Failed to register file with MediaStore");
                }
            } else {
                Log.d("MainActivity", "Skipping MediaStore registration for audio file without album info");
            }

        } catch (Exception e) {
            // Don't show user error as file was still downloaded successfully
            Log.e("MainActivity", "Error adding file to MediaStore: " + e.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle theme changes
        configureSystemBars();
        if (myWeb != null) {
            // Keep WebView background transparent to show topography pattern
            myWeb.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            // Sync system theme change with website
            myWeb.postDelayed(this::syncSystemThemeToWebsite, 100);
        }
    }

    private void setupBackPressedDispatcher() {
        // Handle back button
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle back button for WebView navigation
                if (myWeb != null && myWeb.canGoBack()) {
                    myWeb.goBack();
                } else {
                    // Let the system handle the back press (exit app)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
