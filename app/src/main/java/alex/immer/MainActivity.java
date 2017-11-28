package alex.immer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.peculiargames.andmodplug.MODResourcePlayer;

public class MainActivity extends AppCompatActivity {
    // tempo slider sets tempo instead of modifying it from -50 to +50
    public static final boolean TEMPO_OVERRIDE = true;

    //
    // prefix for Log output
    private final static String LOGPREFIX = "ANDRESMODPLAYER";

    //
    // for saving song number and position in song at onPause() time (to Preferences)
    public static final String SHARED_PREFS_NAME = "MRPlayerPrefs";
    public static final String PREFS_SONGNUM = "SongNum";
    public static final String PREFS_SONGPATTERN = "SongPattern";

    private SharedPreferences mConfig;


    // default song to start with
    public static final int DEFAULT_SONG = 0;

    private MODResourcePlayer resplayer = null;

    // UI
    private TextView modnameText;
    private TextView numtracksText;
    private TextView ig;

    private ImageButton nextsongButton;
    private ImageButton prevsongbutton;
    private ImageButton playpauseButton;
    private ImageView art;
    private ImageView imageView;

    private SeekBar temposeekbar;
    private int tempo = 0;

    private boolean mPlaying;

    private int currmod;

    private final int[] MODlist = {
            R.raw.leet,
            R.raw.abarenboshogun,
            R.raw.amityville,
            R.raw.anticipation,
            R.raw.anticipationn,
            R.raw.backatitagain,
            R.raw.blownfuse,
            R.raw.bootcampriot,
            R.raw.botenanna,
            R.raw.bubblebobble,
            R.raw.chaos,
            R.raw.chimai,
            R.raw.chimairemix,
            R.raw.coldburns,
            R.raw.contra,
            R.raw.dance,
            R.raw.delta,
            R.raw.descendantsofthesun,
            R.raw.dissociation,
            R.raw.embrace,
            R.raw.eminemia,
            R.raw.escapeartist,
            R.raw.eutuiubire,
            R.raw.excite,
            R.raw.fackdaheyter,
            R.raw.fantasy,
            R.raw.forjessica,
            R.raw.fourthwave,
            R.raw.ghosttown,
            R.raw.godom,
            R.raw.greenberetloader,
            R.raw.happyholidays,
            R.raw.happynewyear,
            R.raw.imthedevil,
            R.raw.inmymind,
            R.raw.intro,
            R.raw.itmighthavebeen,
            R.raw.itsalright,
            R.raw.itslllove,
            R.raw.keepmoving,
            R.raw.laserbeams,
            R.raw.lastninja,
            R.raw.lastone,
            R.raw.life,
            R.raw.lightspeed,
            R.raw.lightspeed2,
            R.raw.lovedemo,
            R.raw.loveorig,
            R.raw.mauve,
            R.raw.moveyourbody,
            R.raw.nothingelsematters,
            R.raw.omicron,
            R.raw.oneforyou,
            R.raw.outro,
            R.raw.paradiserain,
            R.raw.playground,
            R.raw.prey,
            R.raw.rambopartii,
            R.raw.samolonging,
            R.raw.secondwave,
            R.raw.shadowgate,
            R.raw.smallandtiny,
            R.raw.sodorrah,
            R.raw.spacemelody,
            R.raw.strangerthings,
            R.raw.sunvsmooneclipsed,
            R.raw.sysop,
            R.raw.thedaytheylanded,
            R.raw.theeaglehaslanded,
            R.raw.thepeak,
            R.raw.thirdwave,
            R.raw.unknownsoldier,
            R.raw.virtualdreams,
            R.raw.whenyesterdayistoday,
            R.raw.withoutyou,
            R.raw.you
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // *****************************************
        // SET UP the user interface
        // *****************************************

        SetupUserInterface();


        currmod = 0;

        // try out a mod file
        resplayer = new MODResourcePlayer(this);
        resplayer.LoadMODResource(MODlist[currmod]);


        // display the name and number of tracks
        modnameText.setText("" + resplayer.getModName());
        numtracksText.setText(resplayer.getNumChannels() + " channels " + resplayer.getRate() + "Hz");

        tempo = resplayer.getSongDefaultTempo();
        Log.v(LOGPREFIX, "Song's default tempo is " + tempo);
        temposeekbar.setProgress((int) (((tempo - 32) * 100.0f) / (255 - 32)));

        // start up the music (well, start the thread, at least...)
        resplayer.start();

        mPlaying = true;

        Log.v(LOGPREFIX, "onCreate()");
    }
    // pause the Activity and MOD player
    @Override
    protected void onPause() {
        super.onPause();

        //
        // save song number and current pattern so we can resume from there...
        SharedPreferences.Editor prefs = getSharedPreferences(SHARED_PREFS_NAME, 0).edit();
        prefs.putInt(PREFS_SONGNUM, currmod);
        prefs.putInt(PREFS_SONGPATTERN, resplayer.getCurrentPattern());
        prefs.commit();

        resplayer.StopAndClose();
        resplayer = null;

        Log.v(LOGPREFIX, "onPause()");

    }
    // resume the ModPlayer Activity
    @Override
    protected void onResume() {
        super.onResume();
        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        final Animation animRotate = AnimationUtils.loadAnimation(this, R.anim.anim_rotate);

        if (resplayer == null) {

            //
            // restore song number and current pattern so we can resume from there...
            mConfig = getSharedPreferences(SHARED_PREFS_NAME, 0);
            currmod = mConfig.getInt(PREFS_SONGNUM, 0);
            int pattern = mConfig.getInt(PREFS_SONGPATTERN, 0);

            // get a new player thread with this mod file data
            resplayer = new MODResourcePlayer(this);
            resplayer.LoadMODResource(MODlist[currmod]);

            resplayer.setCurrentPattern(pattern);


            // display the name and number of tracks
            modnameText.setText(""+resplayer.getModName());
            numtracksText.setText(resplayer.getNumChannels()+" channels "+resplayer.getRate()+"Hz");
            tempo = resplayer.getSongDefaultTempo();
            Log.v(LOGPREFIX, "Song's default tempo is "+tempo);
            temposeekbar.setProgress((int) (((tempo - 32) * 100.0f) / (255 - 32)));

            // start up the music (well, start the thread, at least...)
            resplayer.start();

        }
        else {
            //resplayer.setPatternChangeRunnable(sHandler, sPatternChanged);
            //resplayer.repeatPattern(1);
            resplayer.UnPausePlay();
        }
        art.startAnimation(animAlpha);
        playpauseButton.startAnimation(animRotate);
        playpauseButton.setImageResource(R.drawable.ic_pause_);

        Log.v(LOGPREFIX, "onResume()");
    }
    //User Interface!
    private void SetupUserInterface() {

        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);

        //
        // get handles to the volume slider and text user interface
        // widgets
        //

        temposeekbar = (SeekBar) findViewById(R.id.tempo);
        if (TEMPO_OVERRIDE)
            temposeekbar.setProgress((int)(tempo));
        else
            temposeekbar.setProgress((int)(tempo+50));

        // set up the tempo slider
        temposeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar arg0, int temp, boolean arg2) {
                if (TEMPO_OVERRIDE)
                    tempo = ((int) ((temp / 100.0f) * (255 - 32))) + 32;
                else
                    tempo = temp - 50;
                // arbitrarily decided to have setVolume() take an integer from 0-255... see PlayerThread.java
                if (TEMPO_OVERRIDE)
                    resplayer.setTempo(tempo);
                else
                    resplayer.modifyTempo(tempo);
                //temposeekbar.setProgress((int)(tempo*100));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //imageclick
        art = (ImageView)findViewById(R.id.art);
        art.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                v.startAnimation(animAlpha);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://soundcloud.com/aleximmer"));
                startActivity(intent);

            }
        });

        //imageclick
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                v.startAnimation(animAlpha);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.instagram.com/alex.immer/"));
                startActivity(intent);

            }
        });

        //textclick
        ig = (TextView) findViewById(R.id.ig);
        ig.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                v.startAnimation(animAlpha);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.instagram.com/alex.immer/"));
                startActivity(intent);

            }
        });

        // get handles to the mod name, num tracks text and for the next song button
        modnameText = (TextView) findViewById(R.id.modnametext);
        numtracksText = (TextView) findViewById(R.id.numtrackstext);
        nextsongButton = (ImageButton) findViewById(R.id.nextsongbutton);
        nextsongButton.setImageResource(R.drawable.ic_skip_next);
        //nextsongButton.setBackgroundResource(R.drawable.graybutton);
        nextsongButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.startAnimation(animAlpha);
                // Perform action on clicks
                NextMOD();
            }
        });

        // get handles to the mod name, num tracks text and for the next song button
        modnameText = (TextView) findViewById(R.id.modnametext);
        numtracksText = (TextView) findViewById(R.id.numtrackstext);
        prevsongbutton = (ImageButton) findViewById(R.id.prevsongbutton);
        prevsongbutton.setImageResource(R.drawable.ic_skip_previous);
        //nextsongButton.setBackgroundResource(R.drawable.graybutton);
        prevsongbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.startAnimation(animAlpha);
                // Perform action on clicks
                PrevMOD();
            }
        });

        playpauseButton = (ImageButton) findViewById(R.id.playpausebutton);
        //playpauseButton.setBackgroundResource(R.drawable.graybutton);
        playpauseButton.setVisibility(View.VISIBLE);
        playpauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.startAnimation(animAlpha);
                // Perform action on clicks
                TogglePlayPause();
            }
        });

    }
    //
    // play the next MOD file in our list
    //
    public void NextMOD() {
        resplayer.PausePlay();

        // bump up to next mod file
        currmod++;
        if (currmod >= MODlist.length) currmod = 0;

        // load it into the player
        resplayer.LoadMODResource(MODlist[currmod]);

        // show its name and number of tracks
        modnameText.setText(""+resplayer.getModName());
        numtracksText.setText(resplayer.getNumChannels() + " channels " + resplayer.getRate() + "Hz");
        tempo = resplayer.getSongDefaultTempo();
        Log.v(LOGPREFIX, "Song's default tempo is " + tempo);
        temposeekbar.setProgress((int) (((tempo - 32) * 100.0f) / (255 - 32)));

        // enjoy
        //resplayer.repeatPattern(1);
        resplayer.UnPausePlay();
    }

    //
    // play the next MOD file in our list
    //
    public void PrevMOD() {
        resplayer.PausePlay();
        // bump up to previous mod file
        currmod--;
        //this is static. Current number of indexes is 76.
        if (currmod <= MODlist.length-76) currmod = 0;

        // load it into the player
        resplayer.LoadMODResource(MODlist[currmod]);

        // show its name and number of tracks
        modnameText.setText(""+resplayer.getModName());
        numtracksText.setText(resplayer.getNumChannels()+" channels "+resplayer.getRate()+"Hz");
        tempo = resplayer.getSongDefaultTempo();
        Log.v(LOGPREFIX, "Song's default tempo is " + tempo);
        temposeekbar.setProgress((int) (((tempo - 32) * 100.0f) / (255 - 32)));

        // enjoy
        //resplayer.repeatPattern(1);
        resplayer.UnPausePlay();
    }

    //
    // Toggle Play/Pause
    //
    public void TogglePlayPause() {
        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        mPlaying = !mPlaying;

        if (mPlaying) {
            resplayer.UnPausePlay();
            playpauseButton.startAnimation(animAlpha);
            playpauseButton.setImageResource(R.drawable.ic_pause_);
        }
        else {
            resplayer.PausePlay();
            playpauseButton.startAnimation(animAlpha);
            playpauseButton.setImageResource(R.drawable.ic_play_);
        }
    }
}
