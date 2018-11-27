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

package com.cw.litenotes.define;

import android.app.Activity;
import android.content.Context;

import com.cw.litenotes.R;

/**
 * Created by CW on 2016/6/16.
 * Modified by CW on 2018/11/07
 *
 * build apk file size:
 * 1) prefer w/ assets files: 15,483 KB
 *
 * 2) default w/ assets files: 15,483 KB
 *
 * 3) default w/o assets files: 1,173 KB
 *
 * 4) release: 706 KB
 */
public class Define {

    public static boolean DEBUG_MODE = false;
    public static boolean RELEASE_MODE = !DEBUG_MODE;

    /***************************************************************************
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     ***************************************************************************/
    public static boolean CODE_MODE = RELEASE_MODE; //DEBUG_MODE; //RELEASE_MODE;


    /****************************************************************************
     *
     * Flags for Default tables after App installation:
     * - WITH_DEFAULT_CONTENT
     * - WITH_INITIAL_TABLES
     * Note of flag setting: exclusive
     *
     * With default content
     * - true : un-mark preferred/assets/ line in build.gradle file
     * - false:    mark preferred/assets/ line in build.gradle file
     *
     * android {
     * ...
     *    sourceSets {
     *        main {
     *      // mark: W/O default content
     *      // un-mark: With default content
     *      // Apk file size will increase if assets directory is set at default location (src/main/assets)
     *           assets.srcDirs = ['preferred/assets/']
     *      }
     *    }
     * }
     *
     ************************************************************************************************************/
    /***
     *  With default content by XML file
     */
    public static boolean WITH_DEFAULT_CONTENT = true; //true; //false;
    // by downloaded XML file
    public static boolean DEFAULT_CONTENT_BY_DOWNLOAD = false; //true; //false;
    // by assets XML file
    public static boolean DEFAULT_CONTENT_BY_ASSETS = !DEFAULT_CONTENT_BY_DOWNLOAD;

    /**
     * With initial tables: table count
     * - folder count: 2
     * - page count: 1
     */
    // initial table count
    public static int INITIAL_FOLDERS_COUNT = 2;  // Folder1, Folder2
    public static int INITIAL_PAGES_COUNT = 1;// Page1_1

    /***************************************************************************
     * Enable AdMob at page bottom
     *
     ***************************************************************************/
    public static boolean ENABLE_ADMOB = false; //true; //false;


    // Apply system default for picture path
    public static boolean PICTURE_PATH_BY_SYSTEM_DEFAULT = true;


    // default style
    public static int STYLE_DEFAULT = 1;
    public static int STYLE_PREFER = 2;

    public static String getTabTitle(Context context, Integer Id)
    {
        String title;

        if(Define.WITH_DEFAULT_CONTENT) {
            title = context.getResources().getString(R.string.prefer_page_name).concat(String.valueOf(Id));
        }
        else {
            title = context.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        }
        return title;
    }
}
