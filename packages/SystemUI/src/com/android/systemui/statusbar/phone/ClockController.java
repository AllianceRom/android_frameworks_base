package com.android.systemui.statusbar.phone;

import com.android.systemui.FontSizeUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.graphics.Rect;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Clock;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import android.graphics.Typeface;

/**
 * To control your...clock
 */
public class ClockController {

    public static final int STYLE_HIDE_CLOCK    = 0;
    public static final int STYLE_CLOCK_RIGHT   = 1;
    public static final int STYLE_CLOCK_CENTER  = 2;
    public static final int STYLE_CLOCK_LEFT    = 3;

    public static final int DEFAULT_ICON_TINT = Color.WHITE;
    public static final int FONT_NORMAL = 0;

    private final IconMerger mNotificationIcons;
    private final Context mContext;
    private final SettingsObserver mSettingsObserver;
    private Clock mRightClock, mLeftClock;
    public static Clock mCenterClock, mActiveClock;

    private int mClockPosition;
    private int mAmPmStyle;
    private int mClockDateStyle;
    private int mClockDateDisplay;
    private int mClockDatePosition;
    private int mClockFontStyle = FONT_NORMAL;
    private int mIconTint;
    private final Rect mTintArea = new Rect();
    private final Handler handler = new Handler();
    TimerTask second;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_AM_PM),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE_CASE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_DATE_FORMAT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_POSITION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_COLOR),
                    false, this, UserHandle.USER_ALL);
	    resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_DATE_POSITION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
		    Settings.System.STATUSBAR_CLOCK_FONT), 
                    false, mSettingsObserver);
            updateSettings();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
		
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateSettings();
        }
    }

    public ClockController(View statusBar, IconMerger notificationIcons, Handler handler) {
        mRightClock = (Clock) statusBar.findViewById(R.id.clock);
        mCenterClock = (Clock) statusBar.findViewById(R.id.center_clock);
        mLeftClock = (Clock) statusBar.findViewById(R.id.left_clock);
        mNotificationIcons = notificationIcons;
        mContext = statusBar.getContext();				
	mIconTint = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_COLOR, Color.WHITE);
        mActiveClock = mRightClock;
	setTextColor(mIconTint);
        mSettingsObserver = new SettingsObserver(handler);
        mSettingsObserver.observe();
    }

    private Clock getClockForCurrentLocation() {
        Clock clockForAlignment;
        switch (mClockPosition) {
            case STYLE_CLOCK_CENTER:
                clockForAlignment = mCenterClock;
                break;
            case STYLE_CLOCK_LEFT:
                clockForAlignment = mLeftClock;
                break;
            case STYLE_CLOCK_RIGHT:
            case STYLE_HIDE_CLOCK:
            default:
                clockForAlignment = mRightClock;
                break;
        }
        return clockForAlignment;
    }

    private void updateActiveClock() {
        mActiveClock.setVisibility(View.GONE);
        if (mClockPosition == STYLE_HIDE_CLOCK) {
            return;
        }

        mActiveClock = getClockForCurrentLocation();
        mActiveClock.getFontStyle(mClockFontStyle);
        mActiveClock.setVisibility(View.VISIBLE);
        mActiveClock.setAmPmStyle(mAmPmStyle);
        mActiveClock.setClockDateDisplay(mClockDateDisplay);
        mActiveClock.setClockDateStyle(mClockDateStyle);
	mActiveClock.setClockDatePosition(mClockDatePosition);
	mIconTint = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_COLOR, Color.WHITE);
        setClockAndDateStatus();
        setTextColor(mIconTint);
        updateFontSize();
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mAmPmStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AM_PM, Clock.AM_PM_STYLE_GONE,
                UserHandle.USER_CURRENT);
        mClockPosition = Settings.System.getIntForUser(resolver, 
		Settings.System.STATUS_BAR_CLOCK_POSITION, STYLE_CLOCK_RIGHT,
		UserHandle.USER_CURRENT);
        mClockDateDisplay = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_DATE, Clock.CLOCK_DATE_DISPLAY_GONE,
                UserHandle.USER_CURRENT);
        mClockDateStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_DATE_CASE, Clock.CLOCK_DATE_STYLE_REGULAR,
                UserHandle.USER_CURRENT);
        mClockDatePosition = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_DATE_POSITION, 0);
        mClockFontStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_FONT, FONT_NORMAL);
        mIconTint = Settings.System.getInt(resolver, 
		Settings.System.STATUS_BAR_CLOCK_COLOR, Color.WHITE);

        second = new TimerTask()
        {
            @Override
            public void run()
             {
                Runnable updater = new Runnable()
                  {
                   public void run()
                   {
                       updateActiveClock();
                   }
                  };
                handler.post(updater);
             }
        };
        Timer timer = new Timer();
        timer.schedule(second, 0, 1001);
		
        setTextColor(mIconTint);
        updateActiveClock();
    }

    private void setClockAndDateStatus() {
        if (mNotificationIcons != null) {
            mNotificationIcons.setClockAndDateStatus(mClockPosition);
        }
    }

    public void setVisibility(boolean visible) {
        if (mActiveClock != null) {
            mActiveClock.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setTintArea(Rect tintArea) {
        if (tintArea == null) {
            mTintArea.setEmpty();
        } else {
            mTintArea.set(tintArea);
        }
        applyClockTint();
    }

    public void setTextColor(int iconTint) {
        mIconTint = iconTint;
        if (mActiveClock != null) {
            mActiveClock.setTextColor(iconTint);
        }
        applyClockTint();
    }

    public void updateFontSize() {
        if (mActiveClock != null) {
            FontSizeUtils.updateFontSize(mActiveClock, R.dimen.status_bar_clock_size);
            mActiveClock.setPaddingRelative(
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.status_bar_clock_starting_padding),
                    0,
                    mContext.getResources().getDimensionPixelSize(
                        R.dimen.status_bar_clock_end_padding),
                    0);
        }
    }

    private void applyClockTint() {
        StatusBarIconController.getTint(mTintArea, mActiveClock, mIconTint);
    }
}
