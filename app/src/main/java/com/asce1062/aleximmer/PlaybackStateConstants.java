package com.asce1062.aleximmer;

import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Constants for MediaSession PlaybackState that match the website's AndroidBridge
 * These map directly to the PlaybackStateConstants used in AndroidBridgeService.ts
 */
public class PlaybackStateConstants {
    
    // Playback States
    public static final int STATE_NONE = PlaybackStateCompat.STATE_NONE;
    public static final int STATE_STOPPED = PlaybackStateCompat.STATE_STOPPED;
    public static final int STATE_PAUSED = PlaybackStateCompat.STATE_PAUSED;
    public static final int STATE_PLAYING = PlaybackStateCompat.STATE_PLAYING;
    public static final int STATE_FAST_FORWARDING = PlaybackStateCompat.STATE_FAST_FORWARDING;
    public static final int STATE_REWINDING = PlaybackStateCompat.STATE_REWINDING;
    public static final int STATE_BUFFERING = PlaybackStateCompat.STATE_BUFFERING;
    public static final int STATE_ERROR = PlaybackStateCompat.STATE_ERROR;
    public static final int STATE_CONNECTING = PlaybackStateCompat.STATE_CONNECTING;
    public static final int STATE_SKIPPING_TO_PREVIOUS = PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
    public static final int STATE_SKIPPING_TO_NEXT = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
    public static final int STATE_SKIPPING_TO_QUEUE_ITEM = PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;

    // Playback Actions
    public static final long ACTION_STOP = PlaybackStateCompat.ACTION_STOP;
    public static final long ACTION_PAUSE = PlaybackStateCompat.ACTION_PAUSE;
    public static final long ACTION_PLAY = PlaybackStateCompat.ACTION_PLAY;
    public static final long ACTION_REWIND = PlaybackStateCompat.ACTION_REWIND;
    public static final long ACTION_SKIP_TO_PREVIOUS = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
    public static final long ACTION_SKIP_TO_NEXT = PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
    public static final long ACTION_FAST_FORWARD = PlaybackStateCompat.ACTION_FAST_FORWARD;
    public static final long ACTION_SET_RATING = PlaybackStateCompat.ACTION_SET_RATING;
    public static final long ACTION_SEEK_TO = PlaybackStateCompat.ACTION_SEEK_TO;
    public static final long ACTION_PLAY_PAUSE = PlaybackStateCompat.ACTION_PLAY_PAUSE;
    public static final long ACTION_PLAY_FROM_MEDIA_ID = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
    public static final long ACTION_PLAY_FROM_SEARCH = PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
    public static final long ACTION_SKIP_TO_QUEUE_ITEM = PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
    public static final long ACTION_PLAY_FROM_URI = PlaybackStateCompat.ACTION_PLAY_FROM_URI;
    public static final long ACTION_PREPARE = PlaybackStateCompat.ACTION_PREPARE;
    public static final long ACTION_PREPARE_FROM_MEDIA_ID = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID;
    public static final long ACTION_PREPARE_FROM_SEARCH = PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH;
    public static final long ACTION_PREPARE_FROM_URI = PlaybackStateCompat.ACTION_PREPARE_FROM_URI;
    public static final long ACTION_SET_REPEAT_MODE = PlaybackStateCompat.ACTION_SET_REPEAT_MODE;
    public static final long ACTION_SET_SHUFFLE_MODE = PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE;
    public static final long ACTION_SET_CAPTIONING_ENABLED = PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;

    // Repeat modes
    public static final int REPEAT_MODE_NONE = PlaybackStateCompat.REPEAT_MODE_NONE;
    public static final int REPEAT_MODE_ONE = PlaybackStateCompat.REPEAT_MODE_ONE;
    public static final int REPEAT_MODE_ALL = PlaybackStateCompat.REPEAT_MODE_ALL;
    public static final int REPEAT_MODE_GROUP = PlaybackStateCompat.REPEAT_MODE_GROUP;

    // Shuffle modes  
    public static final int SHUFFLE_MODE_NONE = PlaybackStateCompat.SHUFFLE_MODE_NONE;
    public static final int SHUFFLE_MODE_ALL = PlaybackStateCompat.SHUFFLE_MODE_ALL;
    public static final int SHUFFLE_MODE_GROUP = PlaybackStateCompat.SHUFFLE_MODE_GROUP;
}