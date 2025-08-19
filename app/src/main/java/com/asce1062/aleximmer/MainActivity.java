package com.asce1062.aleximmer;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
            // Dark theme - light icons on dark background + dark topography
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
            // Light theme - dark icons on light background + light topography
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
            // Light theme - dark icons on light background
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
            // Dark theme - light icons on dark background
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

        // Custom WebViewClient to handle navigation
        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Keep navigation within the WebView for our domain
                if (url.contains("asce1062.github.io")) {
                    // Return false to let WebView handle the URL loading
                    return false;
                }
                // For external links, open in browser
                // Return true to prevent WebView from loading external URLs
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Set up theme listener for website theme toggle detection
                if (myWeb != null) {
                    myWeb.postDelayed(() -> injectThemeListener(), 1000);
                }
            }
        });

        // Load your website
        myWeb.loadUrl("https://asce1062.github.io/");
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle theme changes
        configureSystemBars();
        if (myWeb != null) {
            // Keep WebView background transparent to show topography pattern
            myWeb.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void setupBackPressedDispatcher() {
        // Modern AndroidX approach for handling back button
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
