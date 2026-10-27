package com.android.launcher3.settings;

import static android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED;

import static androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT;

import static com.android.launcher3.BuildConfig.IS_STUDIO_BUILD;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_MULTI_DISPLAY;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_TABLET;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.display.DisplayController;
import com.android.launcher3.display.LauncherDisplayInfo;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.smartspace.SmartspaceEvent;
import com.android.launcher3.util.SafeCloseable;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.SettingsCache;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class SettingsHomescreen extends CollapsingToolbarBaseActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @VisibleForTesting
    static final String DEVELOPER_OPTIONS_KEY = "pref_developer_options";

    public static final String FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode";

    private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";

    public static final String EXTRA_FRAGMENT_ARGS = ":settings:fragment_args";
    public static final String EXTRA_FRAGMENT_HIGHLIGHT_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_FRAGMENT_ROOT_KEY = ARG_PREFERENCE_ROOT;

    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        Intent intent = getIntent();

        if (savedInstanceState == null) {
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            if (args == null) {
                args = new Bundle();
            }

            String highlight = intent.getStringExtra(EXTRA_FRAGMENT_HIGHLIGHT_KEY);
            if (!TextUtils.isEmpty(highlight)) {
                args.putString(EXTRA_FRAGMENT_HIGHLIGHT_KEY, highlight);
            }
            String root = intent.getStringExtra(EXTRA_FRAGMENT_ROOT_KEY);
            if (!TextUtils.isEmpty(root)) {
                args.putString(EXTRA_FRAGMENT_ROOT_KEY, root);
            }

            final FragmentManager fm = getSupportFragmentManager();
            final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(),
                    getString(R.string.home_screen_settings_fragment_name));
            f.setArguments(args);
            fm.beginTransaction().replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, f).commit();
        }
        LauncherPrefs.getPrefs(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LauncherPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LauncherPrefs.SHOW_QUICKSPACE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.USE_SMARTSPACE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.QUICKSPACE_UI_STYLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SMARTSPACE_ENABLED.getSharedPrefKey().equals(key)) {
            LauncherAppState.INSTANCE.executeIfCreated(app -> app.getModel().forceReload("SettingsHomescreen"));
        }
        if (LauncherPrefs.SHOW_HOTSEAT_BG.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_OPACITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_SEARCH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.QSB_STYLE_GOOGLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_THEME.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SEARCH_RADIUS_SIZE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_MUSIC_SEARCH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_OPACITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_HEIGHT.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_STATUS_BAR.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHORT_PARALLAX.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SINGLE_PAGE_CENTER.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DARK_STATUS_BAR.getSharedPrefKey().equals(key)) {
            LauncherAppState.INSTANCE.executeIfCreated(app -> app.setNeedsRestart());
        }
    }

    private boolean startPreference(String fragment, Bundle args, String key) {
        if (getSupportFragmentManager().isStateSaved()) {
            return false;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), fragment);
        if (f instanceof DialogFragment) {
            f.setArguments(args);
            ((DialogFragment) f).show(fm, key);
        } else {
            startActivity(new Intent(this, SettingsHomescreen.class)
                    .putExtra(EXTRA_FRAGMENT_ARGS, args));
        }
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragmentCompat preferenceFragment, Preference pref) {
        return startPreference(pref.getFragment(), pref.getExtras(), pref.getKey());
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        Bundle args = new Bundle();
        args.putString(ARG_PREFERENCE_ROOT, pref.getKey());
        return startPreference(getString(R.string.home_screen_settings_fragment_name), args, pref.getKey());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class HomescreenSettingsFragment extends SettingsBasePreferenceFragment {

        private @Nullable SafeCloseable mSettingCacheSafeCloseable;

        protected boolean mDeveloperOptionsEnabled = false;

        private boolean mRestartOnResume = false;

        private String mHighLightKey;

        private boolean mPreferenceHighlighted = false;

        private static final String KEY_MINUS_ONE = "pref_enable_minus_one";

        private Preference mShowGoogleAppPref;
        private Preference mCalendarPermissionPref;
        private Preference mBluetoothPermissionPref;
        private Preference mNotificationPermissionPref;
        private static final int REQUEST_SMARTSPACE_CALENDAR = 42;
        private static final int REQUEST_SMARTSPACE_BLUETOOTH = 43;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            if (BuildConfig.IS_DEBUG_DEVICE) {
                Uri devUri = Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED);
                SettingsCache settingsCache = SettingsCache.INSTANCE.get(getContext());
                mDeveloperOptionsEnabled = settingsCache.getValue(devUri);
                mSettingCacheSafeCloseable = settingsCache.getListenableRef(devUri).forEach(
                        MAIN_EXECUTOR, (v) -> {
                            if (v != mDeveloperOptionsEnabled) {
                                tryRecreateActivity();
                            }
                            return null;
                        });
            }
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Bundle args = getArguments();
            mHighLightKey = args == null ? null : args.getString(EXTRA_FRAGMENT_HIGHLIGHT_KEY);

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_home_screen_preferences, rootKey);
            addSmartspacePreferences();

            PreferenceScreen screen = getPreferenceScreen();
            initPreferences(screen);

            mShowGoogleAppPref = screen.findPreference(KEY_MINUS_ONE);
            updateIsGoogleAppEnabled();
            updateQsbStylePrefs();

            if (mHighLightKey != null
                    && !isKeyInPreferenceGroup(mHighLightKey, screen)) {
                final PreferenceScreen parentPreferenceScreen =
                        findParentPreference(screen, mHighLightKey);
                if (parentPreferenceScreen != null && getActivity() != null) {
                    if (!TextUtils.isEmpty(parentPreferenceScreen.getTitle())) {
                        getActivity().setTitle(parentPreferenceScreen.getTitle());
                    }
                    setPreferenceScreen(parentPreferenceScreen);
                    return;
                }
            }

            if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }
        }

        private void initPreferences(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (!initPreference(preference)) {
                    group.removePreference(preference);
                } else if (preference instanceof PreferenceGroup) {
                    initPreferences((PreferenceGroup) preference);
                }
            }
        }

        private boolean isKeyInPreferenceGroup(String targetKey, PreferenceGroup parent) {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref.getKey() != null && pref.getKey().equals(targetKey)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private PreferenceScreen findParentPreference(PreferenceScreen parent, String targetKey) {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference pref = parent.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    PreferenceScreen foundKey = findParentPreference((PreferenceScreen) pref,
                            targetKey);
                    if (foundKey != null) {
                        return foundKey;
                    }
                } else if (pref.getKey() != null && pref.getKey().equals(targetKey)) {
                    return parent;
                }
            }
            return null;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            View listView = getListView();
            final int bottomPadding = listView.getPaddingBottom();
            listView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bottomPadding + insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });

            view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        protected boolean initPreference(Preference preference) {
            return true;
        }

        private void updateIsGoogleAppEnabled() {
            if (mShowGoogleAppPref != null) {
                mShowGoogleAppPref.setEnabled(Utilities.isGSAEnabled(getContext()));
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            if (isAdded() && !mPreferenceHighlighted) {
                PreferenceHighlighter highlighter = createHighlighter();
                if (highlighter != null) {
                    getView().postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS);
                    mPreferenceHighlighted = true;
                }
            }
            updateIsGoogleAppEnabled();

            if (mRestartOnResume) {
                recreateActivityNow();
            }
            updateQsbStylePrefs();
            updateQuickspaceClockColorPrefs();
            LauncherPrefs.getPrefs(getContext())
                    .registerOnSharedPreferenceChangeListener(mPrefListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            LauncherPrefs.getPrefs(getContext())
                    .unregisterOnSharedPreferenceChangeListener(mPrefListener);
        }

        private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
                (prefs, key) -> {
                    if (LauncherPrefs.QSB_STYLE_GOOGLE.getSharedPrefKey().equals(key)) {
                        updateQsbStylePrefs();
                    }
                    if (key == null || key.startsWith("pref_smartspace") || key.startsWith("pref_quickspace")) {
                        updateQuickspaceClockColorPrefs();
                    }
                };

        private void updateQsbStylePrefs() {
            boolean hasGoogleApp = Utilities.isGSAEnabled(getContext());
            boolean isGoogleStyle = hasGoogleApp && LauncherPrefs.get(getContext())
                    .get(LauncherPrefs.QSB_STYLE_GOOGLE);

            Preference googleStylePref =
                    findPreference(LauncherPrefs.QSB_STYLE_GOOGLE.getSharedPrefKey());
            if (googleStylePref != null) {
                googleStylePref.setEnabled(hasGoogleApp);
            }

            Preference musicSearchPref =
                    findPreference(LauncherPrefs.DOCK_MUSIC_SEARCH.getSharedPrefKey());
            if (musicSearchPref != null) {
                musicSearchPref.setEnabled(hasGoogleApp);
            }

            // Standard QSB controls — enabled when Google style is unavailable or disabled.
            setQsbPrefEnabled(LauncherPrefs.HOTSEAT_QSB_OPACITY.getSharedPrefKey(), !isGoogleStyle);
            setQsbPrefEnabled(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH.getSharedPrefKey(), !isGoogleStyle);
            setQsbPrefEnabled(LauncherPrefs.SEARCH_RADIUS_SIZE.getSharedPrefKey(), !isGoogleStyle);
            // Google/Pixel QSB controls — enabled only when Google style can actually be used.
            setQsbPrefEnabled(LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY.getSharedPrefKey(), isGoogleStyle);
        }

        private void setQsbPrefEnabled(String key, boolean enabled) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }

        private void addSmartspacePreferences() {
            PreferenceGroup category = findPreference("smartspace_events");
            if (category == null) return;
            Preference info = new Preference(getPreferenceManager().getContext());
            info.setSummary(R.string.smartspace_events_summary);
            info.setSelectable(false);
            info.setIconSpaceReserved(false);
            info.setOrder(-1);
            category.addPreference(info);
            for (SmartspaceEvent event : SmartspaceEvent.values()) {
                SwitchPreferenceCompat pref = new SwitchPreferenceCompat(
                        getPreferenceManager().getContext());
                pref.setKey(event.preference.getSharedPrefKey());
                pref.setTitle(event.title);
                pref.setDefaultValue(true);
                pref.setPersistent(true);
                pref.setIconSpaceReserved(false);
                pref.setOrder(event.ordinal() * 2);
                category.addPreference(pref);
            }
            mCalendarPermissionPref = new Preference(getPreferenceManager().getContext());
            mCalendarPermissionPref.setTitle(R.string.smartspace_calendar_permission);
            mCalendarPermissionPref.setSummary(R.string.smartspace_calendar_permission_summary);
            mCalendarPermissionPref.setIconSpaceReserved(false);
            mCalendarPermissionPref.setOrder(SmartspaceEvent.CALENDAR.ordinal() * 2 + 1);
            mCalendarPermissionPref.setOnPreferenceClickListener(pref -> {
                requestPermissions(new String[] {Manifest.permission.READ_CALENDAR},
                        REQUEST_SMARTSPACE_CALENDAR);
                return true;
            });
            category.addPreference(mCalendarPermissionPref);
            mBluetoothPermissionPref = new Preference(getPreferenceManager().getContext());
            mBluetoothPermissionPref.setTitle(R.string.smartspace_bluetooth_permission);
            mBluetoothPermissionPref.setSummary(R.string.smartspace_bluetooth_permission_summary);
            mBluetoothPermissionPref.setIconSpaceReserved(false);
            mBluetoothPermissionPref.setOrder(SmartspaceEvent.BLUETOOTH.ordinal() * 2 + 1);
            mBluetoothPermissionPref.setOnPreferenceClickListener(pref -> {
                requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_SMARTSPACE_BLUETOOTH);
                return true;
            });
            category.addPreference(mBluetoothPermissionPref);
            mNotificationPermissionPref = new Preference(getPreferenceManager().getContext());
            mNotificationPermissionPref.setTitle(R.string.smartspace_notification_permission);
            mNotificationPermissionPref.setSummary(R.string.smartspace_notification_permission_summary);
            mNotificationPermissionPref.setIconSpaceReserved(false);
            mNotificationPermissionPref.setOrder(SmartspaceEvent.TIMERS.ordinal() * 2 + 1);
            mNotificationPermissionPref.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                new ComponentName(getContext(), NotificationListener.class)
                                        .flattenToString()));
                return true;
            });
            category.addPreference(mNotificationPermissionPref);
            updateQuickspaceClockColorPrefs();
        }

        private void updateQuickspaceClockColorPrefs() {
            if (getContext() == null) return;
            boolean smartspace = LauncherPrefs.USE_SMARTSPACE.get(getContext());
            PreferenceGroup glance = findPreference("at_a_glance_category");
            if (glance != null) {
                for (int i = 0; i < glance.getPreferenceCount(); i++) {
                    Preference pref = glance.getPreference(i);
                    String key = pref.getKey();
                    if (key != null && key.startsWith("pref_quickspace_")) {
                        pref.setVisible(!smartspace);
                    }
                }
            }
            Preference events = findPreference("smartspace_events");
            if (events != null) {
                events.setVisible(smartspace);
                events.setEnabled(smartspace && LauncherPrefs.SHOW_QUICKSPACE.get(getContext()));
            }
            if (mCalendarPermissionPref != null) {
                mCalendarPermissionPref.setVisible(smartspace
                        && LauncherPrefs.SMARTSPACE_CALENDAR.get(getContext())
                        && getContext().checkSelfPermission(Manifest.permission.READ_CALENDAR)
                                != PackageManager.PERMISSION_GRANTED);
            }
            if (mBluetoothPermissionPref != null) {
                mBluetoothPermissionPref.setVisible(smartspace
                        && SmartspaceEvent.BLUETOOTH.isEnabled(getContext())
                        && getContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED);
            }
            if (mNotificationPermissionPref != null) {
                NotificationManager manager = getContext().getSystemService(NotificationManager.class);
                boolean granted = manager != null && manager.isNotificationListenerAccessGranted(
                        new ComponentName(getContext(), NotificationListener.class));
                mNotificationPermissionPref.setVisible(smartspace
                        && SmartspaceEvent.TIMERS.isEnabled(getContext()) && !granted);
            }
            int style;
            try {
                style = Integer.parseInt(
                        LauncherPrefs.get(getContext()).get(LauncherPrefs.QUICKSPACE_UI_STYLE));
            } catch (NumberFormatException e) {
                style = LauncherPrefs.get(getContext()).get(LauncherPrefs.SHOW_QUICKSPACE_ALT)
                        ? 1 : 0;
            }
            boolean isOosClock = style == 2 && !smartspace;
            setQuickspacePrefVisible(
                    LauncherPrefs.QUICKSPACE_CLOCK_SYSTEM_FONT.getSharedPrefKey(), isOosClock);
            setQuickspacePrefVisible(
                    LauncherPrefs.QUICKSPACE_CLOCK_CUSTOM_COLOR.getSharedPrefKey(), isOosClock);
            setQuickspacePrefVisible(
                    LauncherPrefs.QUICKSPACE_CLOCK_COLOR.getSharedPrefKey(), isOosClock);
        }

        private void setQuickspacePrefVisible(String key, boolean visible) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setVisible(visible);
            }
        }

        private void updateSmartspacePermissionPrefs() {
            updateQuickspaceClockColorPrefs();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions,
                int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_SMARTSPACE_CALENDAR
                    || requestCode == REQUEST_SMARTSPACE_BLUETOOTH) {
                updateQuickspaceClockColorPrefs();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mSettingCacheSafeCloseable != null) {
                mSettingCacheSafeCloseable.close();
                mSettingCacheSafeCloseable = null;
            }
        }

        protected void tryRecreateActivity() {
            if (isResumed()) {
                recreateActivityNow();
            } else {
                mRestartOnResume = true;
            }
        }

        private void recreateActivityNow() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }

        private PreferenceHighlighter createHighlighter() {
            if (TextUtils.isEmpty(mHighLightKey)) {
                return null;
            }

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                return null;
            }

            RecyclerView list = getListView();
            PreferencePositionCallback callback = (PreferencePositionCallback) list.getAdapter();
            int position = callback.getPreferenceAdapterPosition(mHighLightKey);
            return position >= 0 ? new PreferenceHighlighter(
                    list, position, screen.findPreference(mHighLightKey))
                    : null;
        }
    }
}
