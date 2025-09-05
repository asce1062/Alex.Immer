package com.asce1062.aleximmer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.webkit.WebView;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native MediaSession service for true Android media experience
 * Provides lockscreen integration, device broadcasting, and native controls
 */
public class MediaSessionService extends Service {
    private static final String TAG = "MediaSessionService";
    private static final String MEDIA_SESSION_TAG = "AlexImmerMediaSession";
    private static final String CHANNEL_ID = "media_session_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Service components
    private final IBinder binder = new MediaSessionBinder();
    private Handler mainHandler;
    private ExecutorService executorService;

    // MediaSession components
    private MediaSessionCompat mediaSession;
    
    // Track last set MediaSession position for comparison logging
    private long lastSetMediaSessionPosition = 0;

    // Callback interface for MainActivity
    private MediaSessionServiceCallback callback;
    
    // WebView reference for direct JavaScript execution
    private WebView webView;

    public interface MediaSessionServiceCallback {
        void onMediaAction(String action);
    }

    public class MediaSessionBinder extends Binder {
        MediaSessionService getService() {
            return MediaSessionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaSessionService created");
        
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
        
        createNotificationChannel();
        initializeMediaSession();
        
        // Start as foreground service immediately with default notification
        startWithDefaultNotification();
    }

    /**
     * Create notification channel for MediaSession notifications
     */
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows currently playing music with native controls");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        
        Log.d(TAG, "MediaSession notification channel created");
    }

    /**
     * Start service with default notification to satisfy foreground service requirements
     */
    private void startWithDefaultNotification() {
        try {
            // Create content intent to open the app
            Intent contentIntent = new Intent(this, MainActivity.class);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(
                    this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);

            // Create default notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle("Music Service")
                    .setContentText("Ready for playback")
                    .setContentIntent(contentPendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
            
            Log.d(TAG, "Started foreground service with default notification");

        } catch (Exception e) {
            Log.e(TAG, "Error starting with default notification", e);
        }
    }

    /**
     * Initialize Android MediaSession for native media experience
     */
    private void initializeMediaSession() {
        try {
            // Create MediaSession
            mediaSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG);
            
            // Set up callback for media button events
            MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
            mediaSession.setCallback(mediaSessionCallback);
            Log.i(TAG, "📱 MediaSession callback registered - ready to receive notification interactions");

            // MediaSession will handle media buttons and transport controls by default

            // Set initial playback state
            PlaybackStateCompat initialState = new PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                .build();
                
            mediaSession.setPlaybackState(initialState);

            Log.d(TAG, "MediaSession initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaSession", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "MediaSessionService onBind() called");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle notification action intents
        if (intent != null && intent.getAction() != null) {
            handleNotificationAction(intent.getAction());
        }
        return START_STICKY;
    }

    public void setCallback(MediaSessionServiceCallback callback) {
        this.callback = callback;
        Log.i(TAG, "📞 MediaSessionService callback set - service is connected and ready");
    }
    
    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    /**
     * Update MediaSession with track metadata and playback state
     */
    public void updateMediaSession(String metadataJson, String playbackStateJson) {
        if (mediaSession == null) {
            Log.e(TAG, "MediaSession not initialized");
            return;
        }

        executorService.execute(() -> {
            try {
                // Parse playback state JSON first
                JSONObject playbackState = new JSONObject(playbackStateJson);
                Log.d(TAG, "Received playback state JSON: " + playbackStateJson);
                PlaybackStateCompat playbackStateCompat = buildPlaybackState(playbackState);
                
                // Parse metadata JSON
                JSONObject metadata = new JSONObject(metadataJson);
                
                // Update MediaSession on main thread
                mainHandler.post(() -> {
                    // Get current state for comparison
                    MediaMetadataCompat currentMetadata = mediaSession.getController() != null ? 
                        mediaSession.getController().getMetadata() : null;
                    PlaybackStateCompat currentState = mediaSession.getController() != null ?
                        mediaSession.getController().getPlaybackState() : null;
                    
                    // Check for meaningful changes (track/album/state) - mirrors AndroidBridge logic
                    String newTrackId = metadata.optString("android.media.metadata.MEDIA_ID", "");
                    String newTrackTitle = metadata.optString("android.media.metadata.TITLE", "");
                    String newTrackArtist = metadata.optString("android.media.metadata.ARTIST", "");
                    String newAlbumTitle = metadata.optString("android.media.metadata.ALBUM", "");
                    
                    String currentTrackId = currentMetadata != null ? 
                        currentMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) : "";
                    String currentTrackTitle = currentMetadata != null ? 
                        currentMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) : "";
                    String currentTrackArtist = currentMetadata != null ? 
                        currentMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) : "";
                    String currentAlbumTitle = currentMetadata != null ? 
                        currentMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM) : "";
                    
                    // Track comparison using multiple identifiers
                    boolean isSameTrack = currentMetadata != null && (
                        (!newTrackId.isEmpty() && !currentTrackId.isEmpty() && newTrackId.equals(currentTrackId)) ||
                        (!newTrackTitle.isEmpty() && !newTrackArtist.isEmpty() && 
                         newTrackTitle.equals(currentTrackTitle) && newTrackArtist.equals(currentTrackArtist))
                    );
                    
                    boolean isStateChange = currentState == null || 
                        currentState.getState() != playbackStateCompat.getState();
                    boolean isTrackChange = !isSameTrack;
                    boolean isAlbumChange = !newAlbumTitle.equals(currentAlbumTitle);
                    
                    // Only update on meaningful changes - mirrors AndroidBridge philosophy
                    if (!isStateChange && !isTrackChange && !isAlbumChange) {
                        Log.d(TAG, "No meaningful changes detected - skipping MediaSession update (position progression handled automatically)");
                        return;
                    }
                    
                    // Log what changes triggered the update
                    if (isStateChange) {
                        Log.d(TAG, "🔔 Play state changed - updating MediaSession: " + 
                            (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING ? "▶️" : "⏸️") +
                            " at position: " + Math.round(playbackStateCompat.getPosition() / 1000.0) + "s");
                    } else {
                        Log.d(TAG, "🔔 Track/Album changed - updating MediaSession: '" + newTrackTitle + "' by '" + newAlbumTitle + "'");
                    }
                    
                    // Set position once and let Android handle progression - mirrors MediaSession API
                    mediaSession.setPlaybackState(playbackStateCompat);
                    
                    // Update metadata only for track/album changes
                    if (isTrackChange || isAlbumChange) {
                        try {
                            MediaMetadataCompat newMetadata = buildMediaMetadata(metadata);
                            mediaSession.setMetadata(newMetadata);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error building metadata", e);
                            // Keep existing metadata on error
                        }
                    }
                    
                    // Update notification with current metadata
                    MediaMetadataCompat notificationMetadata = mediaSession.getController() != null ? 
                        mediaSession.getController().getMetadata() : null;
                    createMediaNotification(notificationMetadata, playbackStateCompat);
                });

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing MediaSession JSON", e);
            }
        });
    }

    /**
     * Build MediaMetadata from JSON
     */
    private MediaMetadataCompat buildMediaMetadata(JSONObject json) throws JSONException {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        
        // Core metadata
        String title = json.optString("android.media.metadata.TITLE", "Unknown Track");
        String artist = json.optString("android.media.metadata.ARTIST", "Unknown Artist");
        String album = json.optString("android.media.metadata.ALBUM", "Unknown Album");
        long duration = json.optLong("android.media.metadata.DURATION", 0);
        String artUri = json.optString("android.media.metadata.ART_URI", "");
        
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, title)
               .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
               .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
               .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, artist)
               .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);

        // Display metadata for rich notifications
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, 
                         json.optString("android.media.metadata.DISPLAY_TITLE", title))
               .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, 
                         json.optString("android.media.metadata.DISPLAY_SUBTITLE", artist))
               .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, 
                         json.optString("android.media.metadata.DISPLAY_DESCRIPTION", album));

        // Additional metadata
        if (json.has("android.media.metadata.GENRE")) {
            builder.putString(MediaMetadata.METADATA_KEY_GENRE, 
                            json.optString("android.media.metadata.GENRE"));
        }
        
        if (json.has("android.media.metadata.YEAR")) {
            builder.putLong(MediaMetadata.METADATA_KEY_YEAR, 
                          json.optLong("android.media.metadata.YEAR"));
        }
        
        if (json.has("android.media.metadata.MEDIA_ID")) {
            builder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, 
                            json.optString("android.media.metadata.MEDIA_ID"));
        }

        // Store data for async artwork loading (but don't reuse builder!)
        final String finalArtUri = artUri;
        final String finalTitle = title;
        
        // Load album artwork asynchronously after initial metadata is set
        if (!artUri.isEmpty()) {
            executorService.execute(() -> {
                Bitmap artwork = loadArtworkFromUrl(finalArtUri);
                if (artwork != null) {
                    // Update MediaSession with artwork on main thread
                    mainHandler.post(() -> {
                        if (mediaSession != null) {
                            // Get current metadata and add artwork without disturbing other fields
                            MediaMetadataCompat currentMetadata = mediaSession.getController().getMetadata();
                            if (currentMetadata != null) {
                                // Create new builder from current metadata to preserve all fields
                                MediaMetadataCompat.Builder newBuilder = new MediaMetadataCompat.Builder(currentMetadata);
                                
                                // Add artwork fields
                                newBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork);
                                newBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork);
                                newBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artwork);
                                newBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, finalArtUri);
                                newBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, finalArtUri);
                                
                                MediaMetadataCompat metadataWithArt = newBuilder.build();
                                mediaSession.setMetadata(metadataWithArt);
                                
                                // Ensure MediaSession is active for lockscreen integration
                                mediaSession.setActive(true);
                                
                                // Also refresh the notification with the new artwork
                                PlaybackStateCompat currentState = mediaSession.getController().getPlaybackState();
                                if (currentState != null) {
                                    createMediaNotification(metadataWithArt, currentState);
                                }
                                
                                Log.d(TAG, "MediaSession updated with album artwork for: " + finalTitle + " (preserving current playback state)");
                            }
                        }
                    });
                }
            });
        }

        return builder.build();
    }

    /**
     * Build PlaybackState from JSON
     */
    private PlaybackStateCompat buildPlaybackState(JSONObject json) throws JSONException {
        int state = json.optInt("state", PlaybackStateCompat.STATE_STOPPED);
        long position = json.optLong("position", 0);
        float playbackSpeed = (float) json.optDouble("playbackSpeed", 1.0);
        long updateTime = json.optLong("updateTime", System.currentTimeMillis());
        long actions = json.optLong("actions", 
            PlaybackStateCompat.ACTION_PLAY |
            PlaybackStateCompat.ACTION_PAUSE |
            PlaybackStateCompat.ACTION_PLAY_PAUSE |
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
            PlaybackStateCompat.ACTION_SEEK_TO
        );

        Log.d(TAG, "Building PlaybackState - Position: " + position + "ms, State: " + state + ", Speed: " + playbackSpeed + ", UpdateTime: " + updateTime);

        // ADD POSITION COMPARISON LOGGING
        logPositionComparison(position);
        
        // Store the position we're setting for future comparisons
        lastSetMediaSessionPosition = position;

        return new PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed, updateTime)
            .setActions(actions)
            .build();
    }


    /**
     * Log position comparison between MediaSession and actual audio (if available)
     */
    private void logPositionComparison(long mediaSessionPosition) {
        try {
            Log.d(TAG, "📱 MediaSession position: " + mediaSessionPosition + "ms (" + (mediaSessionPosition/1000) + "s)");
            Log.d(TAG, "📱 Notification progress bar will show: " + (mediaSessionPosition/1000) + "s");
            
            // Note: We don't have direct access to audio playback position in this service
            // The actual audio is handled by the WebView, but we can log the MediaSession position
            // which is what the notification progress bar will display
            
            if (lastSetMediaSessionPosition > 0) {
                long positionDrift = Math.abs(mediaSessionPosition - lastSetMediaSessionPosition);
                if (positionDrift > 1000) { // More than 1 second difference
                    Log.d(TAG, "📏 Large position change: " + positionDrift + "ms (likely a seek or track change)");
                } else {
                    Log.d(TAG, "📏 Position progression: " + positionDrift + "ms from last update");
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error logging position comparison", e);
        }
    }
    
    /**
     * Get current MediaSession position for comparison logging
     */
    private long getCurrentMediaSessionPosition() {
        if (mediaSession != null && mediaSession.getController() != null) {
            PlaybackStateCompat playbackState = mediaSession.getController().getPlaybackState();
            if (playbackState != null) {
                return playbackState.getPosition();
            }
        }
        return 0;
    }

    /**
     * Load artwork from URL
     */
    private Bitmap loadArtworkFromUrl(String artworkUrl) {
        try {
            URL url = new URL(artworkUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            if (bitmap != null) {
                // Scale for optimal MediaSession display - use larger size for better lockscreen background
                int maxSize = 1024;  // Increased from 512 for better lockscreen quality
                if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                    float ratio = Math.min(
                        (float) maxSize / bitmap.getWidth(),
                        (float) maxSize / bitmap.getHeight());
                    int width = Math.round(bitmap.getWidth() * ratio);
                    int height = Math.round(bitmap.getHeight() * ratio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }
                
                Log.d(TAG, "Album artwork loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return bitmap;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading album artwork: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create foreground notification with MediaSession integration
     * This makes the notification visible in status bar and lockscreen
     */
    private void createMediaNotification(MediaMetadataCompat metadata, PlaybackStateCompat playbackState) {
        try {
            // Get basic info from metadata
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            
            Log.d(TAG, "Creating notification for: " + title + " - Album art available: " + (albumArt != null));
            
            boolean isPlaying = playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            
            // Create content intent to open the app
            Intent contentIntent = new Intent(this, MainActivity.class);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(
                    this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);

            // Create notification with MediaStyle
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(title != null ? title : "Unknown Track")
                    .setContentText(artist != null ? artist : "Unknown Artist")
                    .setSubText(album != null ? album : "Unknown Album")
                    .setContentIntent(contentPendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .setOngoing(isPlaying)
                    .setDeleteIntent(createStopPendingIntent())
                    // MediaStyle connects the notification to the MediaSession
                    .setStyle(new MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2))
                    .addAction(createNotificationAction("previous", "Previous", R.drawable.ic_skip_previous))
                    .addAction(createNotificationAction(
                            isPlaying ? "pause" : "play",
                            isPlaying ? "Pause" : "Play",
                            isPlaying ? R.drawable.ic_pause : R.drawable.ic_play))
                    .addAction(createNotificationAction("next", "Next", R.drawable.ic_skip_next));

            // Set large icon (album artwork or default)
            if (albumArt != null) {
                builder.setLargeIcon(albumArt);
                Log.d(TAG, "Notification created with album artwork: " + albumArt.getWidth() + "x" + albumArt.getHeight());
            } else {
                Log.d(TAG, "Notification created WITHOUT album artwork - artwork may still be loading");
            }

            Notification notification = builder.build();
            
            if (isPlaying) {
                // Start as foreground service when playing
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Started foreground service with notification");
            } else {
                // Show regular notification when paused
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.notify(NOTIFICATION_ID, notification);
                Log.d(TAG, "Updated notification");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating media notification", e);
        }
    }

    /**
     * Create notification action
     */
    private NotificationCompat.Action createNotificationAction(String action, String title, int iconRes) {
        Intent actionIntent = new Intent(this, MediaSessionService.class);
        actionIntent.setAction(action);
        
        PendingIntent actionPendingIntent = PendingIntent.getService(
                this, action.hashCode(), actionIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Action.Builder(iconRes, title, actionPendingIntent).build();
    }

    /**
     * Create stop/delete pending intent
     */
    private PendingIntent createStopPendingIntent() {
        Intent stopIntent = new Intent(this, MediaSessionService.class);
        stopIntent.setAction("stop");
        return PendingIntent.getService(this, "stop".hashCode(), stopIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Set MediaSession active state
     */
    public void setMediaSessionActive(boolean active) {
        if (mediaSession != null) {
            mediaSession.setActive(active);
            Log.d(TAG, "MediaSession active: " + active);
        }
    }

    /**
     * Force position update (for seeks, loop restarts, etc.)
     * Matches website's forcePositionUpdate - simplified clean approach
     * Set position once and let Android handle progression automatically
     */
    public void updatePosition(long positionMs, boolean isPlaying) {
        if (mediaSession == null) {
            Log.e(TAG, "MediaSession not initialized");
            return;
        }

        Log.d(TAG, "🔔 FORCING POSITION UPDATE:");
        Log.d(TAG, "📱 Setting MediaSession to: " + positionMs + "ms (" + Math.round(positionMs / 1000.0) + "s)");
        Log.d(TAG, "📱 Playing state: " + isPlaying);
        Log.d(TAG, "📡 Broadcasting to connected devices");

        mainHandler.post(() -> {
            PlaybackStateCompat currentState = mediaSession.getController() != null ?
                mediaSession.getController().getPlaybackState() : null;
            
            // Create new state with updated position and explicit playing state
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                          positionMs, 
                          isPlaying ? 1.0f : 0.0f, 
                          System.currentTimeMillis());
            
            // Preserve existing actions or set default
            if (currentState != null) {
                stateBuilder.setActions(currentState.getActions());
            } else {
                stateBuilder.setActions(
                    PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                    PlaybackStateCompat.ACTION_SEEK_TO
                );
            }
            
            // Set position once and let Android handle progression
            mediaSession.setPlaybackState(stateBuilder.build());
            
            // Store position and log completion
            lastSetMediaSessionPosition = positionMs;
            Log.d(TAG, "✅ MediaSession position updated - notification progress bar now shows: " + Math.round(positionMs / 1000.0) + "s");
        });
    }

    /**
     * Log periodic position sync check (can be called externally)
     */
    public void logPeriodicPositionSync() {
        if (mediaSession != null && mediaSession.getController() != null) {
            PlaybackStateCompat playbackState = mediaSession.getController().getPlaybackState();
            if (playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                long mediaSessionPosition = playbackState.getPosition();
                long currentTime = System.currentTimeMillis();
                long timeSinceUpdate = currentTime - playbackState.getLastPositionUpdateTime();
                
                // Calculate expected position based on playback speed and time elapsed
                long expectedPosition = playbackState.getPosition() + 
                    (long)(timeSinceUpdate * playbackState.getPlaybackSpeed());
                
                Log.d(TAG, "🔄 PERIODIC POSITION SYNC CHECK:");
                Log.d(TAG, "📱 MediaSession reported: " + (mediaSessionPosition/1000) + "s");
                Log.d(TAG, "📱 Expected position: " + (expectedPosition/1000) + "s");
                Log.d(TAG, "📱 Time since last update: " + timeSinceUpdate + "ms");
                Log.d(TAG, "📱 Notification shows: " + (expectedPosition/1000) + "s (approximately)");
            }
        }
    }

    /**
     * Clear MediaSession
     */
    public void clearMediaSession() {
        if (mediaSession != null) {
            // Set stopped state
            PlaybackStateCompat stoppedState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .build();
                
            mediaSession.setPlaybackState(stoppedState);
            mediaSession.setActive(false);
            
            Log.d(TAG, "MediaSession cleared");
        }
        
        // Clear notification
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancel(NOTIFICATION_ID);
        
        // Stop foreground service
        stopForeground(true);
        
        // Stop the service entirely when cleared
        stopSelf();
        
        Log.d(TAG, "MediaSession notification cleared and service stopping");
    }

    /**
     * MediaSession callback for handling media button events
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "🎵 MediaSession onPlay - callback available: " + (callback != null));
            if (callback != null) {
                callback.onMediaAction("play");
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "MediaSession onPause");
            if (callback != null) {
                callback.onMediaAction("pause");
            }
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "MediaSession onSkipToNext");
            if (callback != null) {
                callback.onMediaAction("next");
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "MediaSession onSkipToPrevious");
            if (callback != null) {
                callback.onMediaAction("previous");
            }
        }

        @Override
        public void onSeekTo(long pos) {
            // Use both Log.d and Log.i to make sure we see this
            Log.i(TAG, "🎯🎯🎯 NOTIFICATION SEEK DETECTED 🎯🎯🎯");
            Log.i(TAG, "📱 User dragged notification progress bar to: " + pos + "ms (" + (pos/1000) + "s)");
            Log.d(TAG, "🎯 SEEK REQUESTED:");
            Log.d(TAG, "📱 Notification seek to: " + pos + "ms (" + (pos/1000) + "s)");
            Log.d(TAG, "📱 Callback available: " + (callback != null));
            
            long beforeSeek = getCurrentMediaSessionPosition();
            Log.d(TAG, "📱 MediaSession before seek: " + beforeSeek + "ms (" + (beforeSeek/1000) + "s)");
            
            // Update our own position immediately for responsive UI
            updatePosition(pos, true);
            
            long afterSeek = getCurrentMediaSessionPosition();
            Log.d(TAG, "📱 MediaSession after seek: " + afterSeek + "ms (" + (afterSeek/1000) + "s)");
            Log.d(TAG, "✅ MediaSession seek completed - notification will show: " + (pos/1000) + "s");
            Log.i(TAG, "✅ Seek processing complete - telling website to seek to " + (pos/1000) + "s");
            
            // Notify website about the seek action
            if (callback != null) {
                callback.onMediaAction("seekto:" + pos);
                Log.d(TAG, "📱 Sent seekto:" + pos + " to website via callback");
            } else {
                Log.w(TAG, "⚠️ No callback available - cannot notify website of seek!");
            }
        }

        @Override
        public void onStop() {
            Log.d(TAG, "MediaSession onStop");
            if (callback != null) {
                callback.onMediaAction("stop");
            }
        }
    }

    /**
     * Handle notification action intents
     */
    private void handleNotificationAction(String action) {
        Log.d(TAG, "Notification action received: " + action);
        
        if (callback != null) {
            callback.onMediaAction(action);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Task removed - stopping all music and processes");
        
        // Tell website to stop audio playback immediately
        if (callback != null) {
            callback.onMediaAction("stop_all_playback");
            Log.d(TAG, "Sent stop_all_playback command to website via callback");
        } else if (webView != null) {
            // Fallback: execute JavaScript directly on WebView
            Log.d(TAG, "Callback unavailable, executing JavaScript directly on WebView");
            if (mainHandler != null) {
                mainHandler.post(() -> {
                    try {
                        String stopScript = "javascript:(function() {" +
                            "console.log('🛑 App terminated - stopping all audio playback (direct)');" +
                            "try {" +
                                "const audioElements = document.querySelectorAll('audio, video');" +
                                "audioElements.forEach(el => { el.pause(); el.currentTime = 0; });" +
                                "if (window.onNotificationAction) {" +
                                    "window.onNotificationAction('stop');" +
                                "}" +
                                "document.dispatchEvent(new CustomEvent('app-terminated', { detail: 'stop_all' }));" +
                                "console.log('✅ All audio playback stopped for app termination (direct)');" +
                            "} catch(e) {" +
                                "console.error('❌ Error stopping playback (direct):', e);" +
                            "}" +
                        "})();";
                        webView.loadUrl(stopScript);
                        Log.d(TAG, "JavaScript executed directly on WebView for app termination");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to execute JavaScript directly", e);
                    }
                });
            }
        } else {
            Log.w(TAG, "No callback or WebView available to stop audio playback");
        }
        
        // Clear MediaSession immediately
        clearMediaSession();
        
        // Stop foreground service
        stopForeground(true);
        
        // Stop the service completely
        stopSelf();
        
        Log.d(TAG, "Service stopped - clean app termination completed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MediaSessionService destroyed");
        
        // Clean up MediaSession
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
}