/*
 * Copyright (C) 2018 CW Chiu
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

package com.cw.litenotes.util;
/**
 * This file provides simple End User License Agreement
 * It shows a simple dialog with the license text, and two buttons.
 * If user clicks on 'cancel' button, app closes and user will not be granted access to app.
 * If user clicks on 'accept' button, app access is allowed and this choice is saved in preferences
 * so next time this will not show, until next upgrade.
 */
 
import com.cw.litenotes.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import static android.os.Build.VERSION_CODES.M;

public class EULA_dlg {

    private String EULA_PREFIX = "appEULA";
    private Activity mAct;
    SharedPreferences prefs;
    String eulaKey;

    public EULA_dlg(Activity context) {
        mAct = context;
    }
 
    private PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = mAct.getPackageManager().getPackageInfo(
                    mAct.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }
 
    public void show() {
        System.out.println("EULA_dlg / _show");
        PackageInfo versionInfo = getPackageInfo();
        prefs= PreferenceManager.getDefaultSharedPreferences(mAct);
        // The eulaKey changes every time you increment the version number in
        // the AndroidManifest.xml
        eulaKey = EULA_PREFIX + versionInfo.versionCode;

        boolean bAlreadyAccepted = prefs.getBoolean(eulaKey, false);
        
        if (bAlreadyAccepted == false) {
 
            // EULA title
            String title = mAct.getString(R.string.app_name) +
            			   " v" + 
            			   versionInfo.versionName;
 
            // EULA text
            String message = mAct.getString(R.string.EULA_string);
 
            // Disable orientation changes, to prevent parent activity
            // re-initialization
//            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
 
            AlertDialog.Builder builder = new AlertDialog.Builder(mAct)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.accept,
                            new Dialog.OnClickListener() {
 
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    applyPreference();

                                    // check permission first time, request all necessary permissions
                                    if(Build.VERSION.SDK_INT >= M)//API23
                                    {
                                        int permissionCamera = ActivityCompat.checkSelfPermission(mAct, Manifest.permission.CAMERA);
                                        if(permissionCamera != PackageManager.PERMISSION_GRANTED)
                                        {
                                            ActivityCompat.requestPermissions(mAct,
                                                                new String[]{Manifest.permission.CAMERA,
                                                                             Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                             Manifest.permission.READ_EXTERNAL_STORAGE,
                                                                             Manifest.permission.READ_PHONE_STATE
                                                                                              },
                                                                Util.PERMISSIONS_REQUEST_ALL);
                                        }
                                    }

                                    // Close dialog
                                    dialogInterface.dismiss();

                                    // Enable orientation changes based on
                                    // device's sensor
//                                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new Dialog.OnClickListener() {
 
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // Close the activity as they have declined
                                    // the EULA
                                    mAct.finish();
//                                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                                }
 
                            });
            builder.create().show();
        }
    }

    void applyPreference()
    {
        // Mark this version as read.
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(eulaKey, true);
        editor.apply();
    }
}