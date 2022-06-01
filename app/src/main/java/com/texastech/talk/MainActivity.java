package com.texastech.talk;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.texastech.talk.database.AppDatabase;
import com.texastech.talk.database.Mood;
import com.texastech.talk.database.MoodDao;
import com.texastech.talk.database.Music;
import com.texastech.talk.database.MusicDao;
import com.texastech.talk.database.Resources;
import com.texastech.talk.database.ResourcesDao;
import com.texastech.talk.intro.IntroActivity;
import com.texastech.talk.notification.AlarmReceiver;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    /**
     * This is the core, single activity that runs throughout the lifetime of
     * the application. The rest of the UI consists of fragments embedded
     * into the UI for this activity using the Navigation component or
     * AlertDialogs and other Android components for requesting for input.
     */
    public static final String QUERY_MOOD_PARAMETER = "MainActivity.QueryMood";
    public static final String NOTIFICATION_CHANNEL_ID = "MainActivity.NotificationChan";

    private int mCurrentMood = 1;
    private int mCurrentMoodIntensity = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        /**
         * Runs when the activity is first launched. For more information about
         * the Android activity lifecycle, please refer to https://bit.ly/2q7i3eK.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPrefs.getBoolean(IntroActivity.LAUNCHED_APP_BEFORE, false)) {
            Intent intent = new Intent(this, IntroActivity.class);
            finish();
            startActivity(intent);
            setupResourcesDatabase();
            setupMusicDatabase();
        }

        registerNotificationChannel();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        /**
         * This callback is called when the app is being restored after being paused.
         * This could be due to one of two reasons: the user opened the app from
         * the app icon or by clicking a notification from Talk.
         */
        super.onResume();

        // TODO: Remove, this is for demo purposes
        boolean resumingFromNotification = getIntent().getBooleanExtra(QUERY_MOOD_PARAMETER, false);
        if (resumingFromNotification) {
            showCurrentMoodDialog();
        } else {
            // Show notification if opening the app
            showNotification();
        }
    }

    void registerNotificationChannel() {
        /**
         * Registers a notification channel which is required to post notifications
         * to the user. This is done repeatedly whenever the app is started but
         * there is not problem with calling .createNotificationChannel repeatedly.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "DailyNotification", importance);
            channel.setDescription("Talk.Notifications");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    void setupBottomNavigation() {
        /**
         * Links the BottomNavigationView element to the NavController that controls
         * the NavHost containing all the top-level UI fragments. The NavController
         * is then used to switch between UIs/fragments.
         */
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    void setupResourcesDatabase() {
        /**
         * Sets up the Resources database with all the articles.
         */
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        ResourcesDao resDao = database.resourcesDao();

        // Depressed = 1, Sad = 2, Angry = 3, Scared = 4, Moderate = 5, Happy = 6
        final int MoodDepressed = 1;
        final int MoodSad = 2;
        final int MoodAngry = 3;
        final int MoodScared = 4;
        final int MoodModerate = 5;
        final int MoodHappy = 6;

        // Create the list of all the resources
        Resources[] allResources = {
                // Depressed
                new Resources("Coping with depression", "When you’re depressed, you can’t just will yourself to “snap out of it.” But these tips can help put you on the road to recovery.", "https://www.helpguide.org/articles/depression/coping-with-depression.htm", MoodDepressed),
                new Resources("What is depression?", "Depression is a disorder that is evidenced by excessive sadness, loss of interest in enjoyable things, and low motivation.", "https://thiswayup.org.au/how-do-you-feel/sad/", MoodDepressed),
                new Resources("Cat", "Watch this video.", "https://www.youtube.com/watch?v=xbs7FT7dXYc", MoodDepressed),
                new Resources("Depression Symptoms and Warning Signs", "Do you think you might be depressed? Here are some of the signs and symptoms to look for—and tips for getting the help you need.", "https://www.helpguide.org/articles/depression/depression-symptoms-and-warning-signs.htm", MoodDepressed),
                new Resources("Suicide Helpline", "Please refer this in case of emergency", "https://indianhelpline.com/SUICIDE-HELPLINE/", MoodDepressed),

                // Sad
                new Resources("Alone in the crowd - How loneliness affects the mind and body", "Watch this video about being lonely.", "https://www.youtube.com/watch?v=R8A7JodFx4s", MoodSad),
                new Resources("Am I Depressed or Just Really Sad?", "People often think they’re depressed when they’re sad, or sad when they’re depressed.", "https://www.vice.com/en_us/article/9kzqa7/am-i-depressed-difference-sadness-depression", MoodSad),
                new Resources("Why am I sad all the time?", "Ever felt sad or stressed for no apparent reason?", "https://au.reachout.com/articles/why-am-i-sad-all-the-time", MoodSad),
                new Resources("How do I know if I'm sad or depressed?", "If you're afraid that your depressed, there are many things you can do to help figure it out.", "https://www.7cups.com/qa-depression-3/how-do-i-know-if-im-sad-or-depressed-650/", MoodSad),

                // Angry
                new Resources("Anger Management", "Is your temper hijacking your life? These tips and techniques can help you get anger under control and express your feelings in healthier ways.", "https://www.helpguide.org/articles/relationships-communication/anger-management.htm", MoodAngry),
                new Resources("Controlling anger before it controls you", "We all know what anger is, and we've all felt it: whether as a fleeting annoyance or as full-fledged rage.", "https://www.apa.org/topics/anger/control", MoodAngry),
                new Resources("I'm Angry", "Watch this video.", "https://www.youtube.com/watch?v=vyMx7s9cThU", MoodAngry),
                new Resources("Why Am I So Angry?", "Anger can be a force for good. But ongoing, intense anger is neither helpful nor healthy. Here's how to get a grip.", "https://www.webmd.com/mental-health/features/why-am-i-so-angry#1", MoodAngry),
                new Resources("5 Minutes Anger Management Meditation", "Meditation for anger management", "https://youtu.be/LNengFfaVGE", MoodAngry),

                // Scared
                new Resources("Phobias and Irrational Fears", "Is a phobia keeping you from doing things you’d like to do? Learn how to recognize, treat, and overcome the problem.", "https://www.helpguide.org/articles/anxiety/phobias-and-irrational-fears.htm", MoodScared),
                new Resources("I'm Scared", "The fact that you feel scared about these intrusive thought means that you need to see a psychotherapist.", "https://www.mentalhelp.net/advice/i-m-scared/", MoodScared),
                new Resources("Jeremy Zucker - Scared (Lyrics)", "Listen to song about loneliness.", "https://www.youtube.com/watch?v=iyEUvUcMHgE", MoodScared),
                new Resources("How To Stop Being So Goddamn Scared All The Time", "So, you're scared. Let's finally talk about that, shall we?", "https://ittybiz.com/how-to-stop-being-scared/", MoodScared),

                // Moderate
                new Resources("5 Steps To Avoid Complacency", "Remember the fire in the belly you felt on the way to achieving a goal?", "https://thetobincompany.com/5-steps-to-avoid-complacency/", MoodModerate),
                new Resources("How to be human: what it means to feel normal", "Leah Reich was one of the first internet advice columnists", "https://www.theverge.com/2017/2/5/14514224/how-to-be-human-depression-anxiety-feeling-normal", MoodModerate),
                new Resources("NEVER GET COMFORTABLE - Best Motivational Video", "Motivate yourself with this video", "https://www.youtube.com/watch?v=2o8fmUlHAyk", MoodModerate),
                new Resources("10 Best Things To Do With Your Free Time", "Watch this video about using your free time", "https://www.youtube.com/watch?v=afoAXho6EHs", MoodModerate),

                // Happy
                new Resources("Feeling Happy and Being Happy Aren't the Same", "Can you be wrong about whether you are happy?", "https://www.psychologytoday.com/us/blog/am-i-right/201310/feeling-happy-and-being-happy-arent-the-same", MoodHappy),
                new Resources("How to feel happier, according to neuroscientists and psychologists", "Researchers have known for decades that certain activities make us feel better, and they're just beginning to understand what happens in the brain to boost our mood.", "https://www.businessinsider.com/how-feel-happy-happier-better-2017-7", MoodHappy),
                new Resources("Pharrell Williams - Happy", "Listen to Pharrell sing about being Happy!", "https://www.youtube.com/watch?v=ZbZSe6N_BXs", MoodHappy),
                new Resources("The Science of Happiness: What Actually Makes Us Happy", "We all want to be happy. Period. In fact, I would argue that nearly everything we do, whether it’s working, marrying, running, or even filing our taxes is done with an overarching purpose: To feel happier.", "https://medium.com/@MaxWeigand/the-science-of-happiness-what-actually-makes-us-happy-78edcc9bdd58", MoodHappy),
                
        };

        resDao.insertAll(allResources);

        Toast.makeText(this, "Resource database loaded", Toast.LENGTH_LONG).show();
    }
    void setupMusicDatabase() {
        /**
         * Sets up the Music database with all the articles.
         */
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        MusicDao musicDao = database.MusicDao();

        // Depressed = 1, Sad = 2, Angry = 3, Scared = 4, Moderate = 5, Happy = 6
        final int MoodDepressed = 1;
        final int MoodSad = 2;
        final int MoodAngry = 3;
        final int MoodScared = 4;
        final int MoodModerate = 5;
        final int MoodHappy = 6;

        // Create the list of all the resources
        Music[] songs = {
                // Depressed
                new Music("Breathe Me", "Sia", "https://open.spotify.com/track/7jqzZyJJLrpkRFYGpkqSK6?si=ecdb4bbc58584e78", MoodDepressed),
                new Music("Day ’N’ Nite", "Kid Cudi", "https://open.spotify.com/track/4ywmyUAQ0WAdNHXpoWWqfv?si=a1313ecd7a7f4a74", MoodDepressed),
                new Music("Fade to Black", "Metallica", "https://open.spotify.com/track/5nekfiTN45vlxG0eNJQQye?si=9505e6e621d842e6", MoodDepressed),
                new Music("Zero", "Imagine Dragons", "https://open.spotify.com/track/2bzitsPcImYC6DZWvvLCQi?si=b7568413a3154e34", MoodDepressed),
                new Music("Car Radio", "Twenty One Pilots", "https://open.spotify.com/track/5cbpoIu3YjoOwbBDGUEp3P?si=b2f4ea63d6544acb", MoodDepressed),

                // Sad
                new Music("Moral of the Story (feat. Niall Horan", "Ashe, Niall Horan", "spotify:track:5v6qYImm5k36GHlFxdEZyv?context=spotify%3Aartist%3A6P5NO5hzJbuOqSdyPB7SJM", MoodSad),
                new Music("Numb Little Bug", "Em Beihold", "https://open.spotify.com/track/3o9kpgkIcffx0iSwxhuNI2?si=68ba1e654e7b45cd", MoodSad),
                new Music("Runaway", "AURORA", "https://open.spotify.com/track/1v1oIWf2Xgh54kIWuKsDf6?si=266eea8bdc1542f2", MoodSad),
                new Music("See You Again (feat. Charlie Puth)", "Wiz Khalifa, Charlie Puth", "https://open.spotify.com/track/2JzZzZUQj3Qff7wapcbKjc?si=f5c88d9823f54be8", MoodSad),
                new Music("In The End - Mellen Gi Remix", "Linkin Park, Tommee Profitt, Fleurie, Mellen Gi", "https://open.spotify.com/track/5rAUZy2eDdegBxUVYxePK2?si=7e2ac2ab829e4d52", MoodSad),

                // Angry
                new Music("In the End", "Linkin Park", "https://open.spotify.com/track/60a0Rd6pjrkxjPbaKzXjfq?si=90b5d742ba1142ff", MoodAngry),
                new Music("We Are Never Ever Getting Back Together", "Taylor Swift", "https://open.spotify.com/track/5YqltLsjdqFtvqE7Nrysvs?si=94476b397ac54cc7", MoodAngry),
                new Music("So What", "Pink", "https://open.spotify.com/track/0JiY190vktuhSGN6aqJdrt?si=6ab983c68d404e39", MoodAngry),
                new Music("First Of The Year (Equinox)", "Skrillex", "https://open.spotify.com/track/5i7fZq3chLyCHo3VeB6goD?si=01ee22c43eeb4a9a", MoodAngry),
                new Music("Smells Like Teen Spirit", "Nirvana", "https://open.spotify.com/track/4CeeEOM32jQcH3eN9Q2dGj?si=5087a890ff224338", MoodAngry),

                // Scared
                new Music("Not Afraid", "Eminem", "https://open.spotify.com/track/7Ie9W94M7OjPoZVV216Xus?si=9cbf5a919edd4bd9", MoodScared),
                new Music("Stressed Out", "Twenty One Pilots", "https://open.spotify.com/track/3CRDbSIZ4r5MsZ0YwxuEkn?si=e_W4iKs3Qyy5lhNgF5_cyA&utm_source=whatsapp", MoodScared),
                new Music("Let It Go", "Idina Menzel", "https://open.spotify.com/track/0qcr5FMsEO85NAQjrlDRKo?si=3b7de5ff360c4e6f", MoodScared),
                new Music("Inner Demons", "Julia Brennan", "https://open.spotify.com/track/2OjmXOwfH7XG3oBzuv54Dw?si=b83e868d46c94929", MoodScared),
                new Music("Demons", "Imagine Dragons", "https://open.spotify.com/track/5qaEfEh1AtSdrdrByCP7qR?si=abee4062d39944c0", MoodScared),

                // Moderate
                new Music("One Day", "Tata McRae", "https://open.spotify.com/track/27r2uANqwK7XbsiAZnzf9e?si=S73p6iHfTjKmJSEv81bcXw&utm_source=whatsapp", MoodModerate),
                new Music("Work From Home", "Fifth Harmony", "https://open.spotify.com/track/4tCtwWceOPWzenK2HAIJSb?si=992df2c96b664381", MoodModerate),
                new Music("All Of Me", "John Legend", "https://open.spotify.com/track/3U4isOIWM3VvDubwSI3y7a?si=68171bf9bf024f6d", MoodModerate),
                new Music("Take Me Home, Country Roads", "John Denver", "https://open.spotify.com/track/1YYhDizHx7PnDhAhko6cDS?si=b794b011f3ed426d", MoodModerate),
                new Music("Sunflower", "Post Malone, Swae Lee", "https://open.spotify.com/track/3KkXRkHbMCARz0aVfEt68P?si=d12bb19f93aa4371", MoodModerate),

                // Happy
                new Music("Sunday Best", "Surfaces", "https://open.spotify.com/track/1Cv1YLb4q0RzL6pybtaMLo?si=AUvGrH0nTe-89rp-mY2UBQ&utm_source=whatsapp", MoodHappy),
                new Music("Summer Of '69", "Bryan Adams", "https://open.spotify.com/track/0GONea6G2XdnHWjNZd6zt3?si=396e9ba7f7cd4e6f", MoodHappy),
                new Music("All Star", "Smash Mouth", "https://open.spotify.com/track/3cfOd4CMv2snFaKAnMdnvK?si=d6ac0f8d0dd34735", MoodHappy),
                new Music("Uptown Funk", "Mark Ronson, Bruno Mars", "https://open.spotify.com/track/32OlwWuMpZ6b0aN2RZOeMS?si=2062592133514112", MoodHappy),
                new Music("Counting Stars", "One Republic", "https://open.spotify.com/track/2tpWsVSb9UEmDRxAl1zhX1?si=e4c7868a9c6747bd", MoodHappy),

        };

        musicDao.insertAll(songs);

        Toast.makeText(this, "Resource database loaded", Toast.LENGTH_LONG).show();
    }
    void showCurrentMoodDialog() {
        /**
         * Shows the user a dialog that asks them for their current mood then
         * stores the result inside of an instance variable.
         */
        CharSequence moods[] = {
                // Depressed = 1, Sad = 2, Angry = 3, Scared = 4, Moderate = 5, Happy = 6
                "Depressed", "Sad", "Angry", "Scared", "Moderate", "Happy"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("How are you feeling?");
        builder.setSingleChoiceItems(moods, 0, new MoodDialogChoiceListener());
        builder.setPositiveButton("Next", new MoodDialogListener());
        builder.show();
    }

    void showMoodIntensityDialog() {
        /**
         * Shows the dialog that asks the user the intensity of the mood
         */
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialog);
        SeekBar seekBar = new SeekBar(MainActivity.this);
        seekBar.setMax(4);
        seekBar.setOnSeekBarChangeListener(new MoodIntensityDialogSeekListener());

        builder.setTitle("How intense is this feeling?");
        builder.setView(seekBar);
        builder.setPositiveButton("Save", new MoodIntesityDialogListener());

        builder.show();
    }

    void saveMoodToDatabase() {
        /**
         * Saves the current mood state to the SQLite database.
         */
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        MoodDao moodDao = database.moodDao();

        // Get the last entered date
        List<Mood> allMoods = moodDao.getAll();
        int numberOfMoods = allMoods.size();
        int lastEnteredDate = 0;
        if (numberOfMoods > 0) {
             lastEnteredDate = allMoods.get(numberOfMoods - 1).date;
        }

        // Create the new Mood
        Mood currentMood = new Mood(lastEnteredDate + 1, mCurrentMood, mCurrentMoodIntensity);
        moodDao.insert(currentMood);

        // Ask the user (again)
        showNotification();
    }

    void showNotification() {
        /**
         * Sets the alarm to display a notification in the notification bar asking the user to hit
         * the notification so that they get prompted to enter their mood. The notification is
         * shown 3 seconds after requested for demo purposes.
         * TODO: Remove.
         */
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr != null) {
            alarmMgr.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 2 * 1000,
                    pendingIntent
            );
        }
    }

    class MoodDialogListener implements DialogInterface.OnClickListener {
        /**
         * Listeners for the click event when the user chooses the mood
         * that they're in and hits the submit button.
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            showMoodIntensityDialog();
        }
    }

    class MoodDialogChoiceListener implements DialogInterface.OnClickListener {
        /**
         * Listens for the event in which the user chooses a different mood
         * from the multiple choice menu.
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Offset the choice because it goes from 0-5
            mCurrentMood = which + 1;
        }
    }

    class MoodIntesityDialogListener implements DialogInterface.OnClickListener {
        /**
         * Listens for the event in which the user chooses a mood intensity
         * using the SeekBar then hits the submit button.
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            saveMoodToDatabase();
            Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_LONG).show();
        }
    }

    class MoodIntensityDialogSeekListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Listens for updates on the SeekBar used to get the user's current mood.
         * The data is stored which is then used to save to the local SQLite database.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // Progress goes from 0-5 but we use 1-6
            mCurrentMoodIntensity = progress + 1;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

}