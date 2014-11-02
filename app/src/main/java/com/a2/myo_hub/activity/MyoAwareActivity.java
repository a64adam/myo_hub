package com.a2.myo_hub.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.a2.myo_hub.R;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;


public class MyoAwareActivity extends Activity {

    private static final String TAG = MyoAwareActivity.class.getName();

    private static final boolean DEBUG = true;

    private static final String MYO_DEVICE_KEY = "myo_device_key";

    private SharedPreferences sharedPreferences;

    private final Hub hub = Hub.getInstance();
    private final DeviceListener listener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            if (!DEBUG) {
                sharedPreferences.edit().putString(MYO_DEVICE_KEY, myo.getMacAddress()).commit();
            }

            populateCard(myo);

            toggleCards();
            toggleSuccessView();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleSuccessView();
                }
            }, 2000);
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            if (!DEBUG) {
                sharedPreferences.edit().remove(MYO_DEVICE_KEY);
            }

            toggleCards();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher_layout);

        if (!hub.init(this)) {
            finish();
            return;
        }

        setOutline();

        sharedPreferences = getSharedPreferences(TAG, MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        hub.addListener(listener);

        invalidatePrompt();
    }

    @Override
    protected void onPause() {
        super.onPause();

        hub.removeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.launcher_menu, menu);
        return true;
    }

    private void setOutline() {
        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
                outline.setOval(0, 0, size, size);
            }
        };

        findViewById(R.id.fab).setOutlineProvider(viewOutlineProvider);
        findViewById(R.id.fab_complete).setOutlineProvider(viewOutlineProvider);
    }

    public void launchNotificationAccess(View v) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
    }

    public void addMyo(View v) {
        if (findViewById(R.id.myo).getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Myo already connected!", Toast.LENGTH_SHORT).show();
            return;
        }

        toggleProgress();

        if (DEBUG) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    listener.onConnect(null, 0);
                }
            }, 2000);
        } else {
            hub.pairWithAnyMyo();
        }
    }

    public void removeMyo(View v) {
        if (DEBUG) {
            listener.onDisconnect(null, 0);
        } else {
            if (sharedPreferences.contains(MYO_DEVICE_KEY)) {
                hub.unpair(sharedPreferences.getString(MYO_DEVICE_KEY, ""));
            }
        }
    }

    private void toggleSuccessView() {
        final View successView = findViewById(R.id.fab_complete);

        int cx = (successView.getLeft() + successView.getRight()) / 2;
        int cy = (successView.getTop() + successView.getBottom()) / 2;
        float radius = successView.getWidth();

        if (successView.getVisibility() == View.INVISIBLE) {
            Log.d(TAG, "Toggling success FAB ON");
            toggleProgress();

            successView.setVisibility(View.VISIBLE);
            Animator reveal = ViewAnimationUtils.createCircularReveal(successView, cx, cy, 0, radius);
            reveal.setDuration(500).start();
        } else {
            Log.d(TAG, "Toggling success FAB OFF");
            Animator reveal = ViewAnimationUtils.createCircularReveal(successView,
                    cx, cy, radius, 0);
            reveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG, "Success FAB set to INVISIBLE");
                    successView.setVisibility(View.INVISIBLE);
                }
            });
            reveal.start();
        }
    }

    private void toggleProgress() {
        final View progress = findViewById(R.id.progress);

        if (progress.getVisibility() == View.INVISIBLE) {
            Log.d(TAG, "Toggling progress ON");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
            progress.setVisibility(View.VISIBLE);
            progress.animate().alpha(1.0f).start();
        } else {
            Log.d(TAG, "Toggling progress OFF");
            progress.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG, "Progress set to INVISIBLE");
                    progress.setVisibility(View.INVISIBLE);
                    progress.animate().setListener(null);
                }
            }).start();
        }
    }

    private void invalidatePrompt() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();

        if (enabledNotificationListeners.contains(packageName)) {
            findViewById(R.id.prompt).setVisibility(View.GONE);
        } else {
            findViewById(R.id.prompt).setVisibility(View.VISIBLE);
        }
    }

    private void toggleCards() {
        final View empty = findViewById(R.id.empty);
        final View myoCard = findViewById(R.id.myo);

        if (empty.getVisibility() == View.VISIBLE) {
            performAnimation(R.anim.slide_out, empty.getId());
        } else {
            empty.setVisibility(View.VISIBLE);
            performAnimation(R.anim.slide_up, empty.getId());
        }

        if (myoCard.getVisibility() == View.GONE) {
            myoCard.setVisibility(View.VISIBLE);
            performAnimation(R.anim.slide_up, myoCard.getId());

        } else {
            performAnimation(R.anim.slide_out, myoCard.getId());
        }
    }

    private void performAnimation(final int animId, int resId) {
        final View view = findViewById(resId);

        final Animation anim = AnimationUtils.loadAnimation(this, animId);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animId == R.anim.slide_out) {
                    view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        view.startAnimation(anim);
    }

    private void populateCard(Myo myo) {
        if (myo != null) {
            ((TextView) findViewById(R.id.myo_name)).setText(myo.getName());
            ((TextView) findViewById(R.id.myo_mac)).setText(myo.getMacAddress());
            ((TextView) findViewById(R.id.myo_status)).setText(myo.isConnected() ? "Connected" : "Disconnected");
        }
    }
}
