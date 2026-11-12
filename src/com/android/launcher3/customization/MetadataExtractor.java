/*
 * Copyright (C) 2020 Shade Launcher
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.customization;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.android.launcher3.R;

/** Reads the version, last update time and install source of a package. */
class MetadataExtractor {
    private final Context mContext;
    private final PackageManager mPm;

    @Nullable private String mSourcePkg;
    @Nullable private Intent mMarketIntent;

    private long mLastUpdate = 0;
    @Nullable private String mVersionName;
    private long mVersionCode = 0;

    MetadataExtractor(Context context, String pkg) {
        mContext = context;
        mPm = context.getPackageManager();

        try {
            InstallSourceInfo installSource = mPm.getInstallSourceInfo(pkg);
            mSourcePkg = installSource.getInstallingPackageName();
            if (!TextUtils.isEmpty(mSourcePkg)) {
                mMarketIntent = new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("market://details?id=" + pkg))
                        .setPackage(mSourcePkg)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException e) {
            // Unknown package, show it as installed by the system.
        }

        try {
            PackageInfo pi = mPm.getPackageInfo(pkg, 0);
            mLastUpdate = pi.lastUpdateTime;
            mVersionName = pi.versionName;
            mVersionCode = pi.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            // Leave the defaults.
        }
    }

    CharSequence getSource() {
        if (TextUtils.isEmpty(mSourcePkg)) {
            return mContext.getString(R.string.app_info_source_system);
        }
        try {
            ApplicationInfo ai = mPm.getApplicationInfo(mSourcePkg, 0);
            return ai.loadLabel(mPm);
        } catch (PackageManager.NameNotFoundException e) {
            return mSourcePkg;
        }
    }

    CharSequence getLastUpdate() {
        if (mLastUpdate <= 0) {
            return mContext.getString(R.string.app_info_unknown);
        }
        return DateUtils.formatDateTime(mContext, mLastUpdate,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE);
    }

    String getVersionName() {
        return TextUtils.isEmpty(mVersionName)
                ? mContext.getString(R.string.app_info_unknown) : mVersionName;
    }

    long getVersionCode() {
        return mVersionCode;
    }

    @Nullable
    Intent getMarketIntent() {
        return mMarketIntent;
    }
}
