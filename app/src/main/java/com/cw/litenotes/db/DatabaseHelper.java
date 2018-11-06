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

package com.cw.litenotes.db;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.litenotes.R;
import com.cw.litenotes.main.MainAct;
import com.cw.litenotes.define.Define;

// Data Base Helper 
class DatabaseHelper extends SQLiteOpenHelper
{  
    static final String DB_NAME = "litenote.db";
    private static int DB_VERSION = 1;
    
    DatabaseHelper(Context context)
    {  
        super(context, DB_NAME , null, DB_VERSION);
    }

    @Override
    //Called when the database is created ONLY for the first time.
    public void onCreate(SQLiteDatabase sqlDb)
    {   
    	String tableCreated;
    	String DB_CREATE;
    	
    	// WritableDatabase(i.e. sqlDb) is created
    	DB_drawer.mSqlDb = sqlDb;
		DB_folder.mSqlDb = sqlDb; // add for DB_drawer.insertFolderTable below
		DB_page.mSqlDb = sqlDb; // add for DB_folder.insertPageTable below

    	System.out.println("DatabaseHelper / _onCreate");

		// Create Drawer table
		tableCreated = DB_drawer.DB_DRAWER_TABLE_NAME;
		DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
				DB_drawer.KEY_FOLDER_ID + " INTEGER PRIMARY KEY," +
				DB_drawer.KEY_FOLDER_TABLE_ID + " INTEGER," +
				DB_drawer.KEY_FOLDER_TITLE + " TEXT," +
				DB_drawer.KEY_FOLDER_CREATED + " INTEGER);";
		sqlDb.execSQL(DB_CREATE);

		if(Define.WITH_DEFAULT_CONTENT || Define.WITH_INITIAL_PAGE_FOR_NEW_FOLDER)
		{
			for(int i = 1; i<= Define.INITIAL_FOLDERS_COUNT; i++)
			{
				/**
				 * Create
                 * default content tables
                 *           or
                 * initial folder tables
				 */
				System.out.println("DatabaseHelper / _onCreate / will insert folder table " + i);
				DB_drawer dB_drawer = new DB_drawer(MainAct.mAct);
				String folderTitle = MainAct.mAct.getResources().getString(R.string.default_folder_name).concat(String.valueOf(i));
				dB_drawer.insertFolder(i, folderTitle, false); // Note: must set false for DB creation stage
				dB_drawer.insertFolderTable(dB_drawer, i, false);

                /**
                 *  Create initial page tables
                 */
                if(Define.WITH_INITIAL_PAGE_FOR_NEW_FOLDER)
                {
                    // page tables
                    for(int j = 1; j<= Define.INITIAL_PAGES_COUNT_FOR_NEW_FOLDER; j++)
                    {
                        System.out.println("DatabaseHelper / _onCreate / will insert page table " + j);
                        DB_folder db_folder = new DB_folder(MainAct.mAct,i);
                        db_folder.insertPageTable(db_folder, i, j, false);

                        String DB_FOLDER_TABLE_PREFIX = "Folder";
                        String folder_table = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(i));
                        db_folder.insertPage(sqlDb,
                                             folder_table,
                                             Define.getTabTitle(MainAct.mAct,1),
                                             1,
                                             Define.STYLE_DEFAULT);//Define.STYLE_PREFER
                        //db_folder.insertPage(sqlDb,folder_table,"N2",2,1);
                    }
                }//if(Define.WITH_INITIAL_PAGE_FOR_NEW_FOLDER)
			}
		}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    { //how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//        System.out.println("DatabaseHelper / _onUpgrade DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
    
    @Override
    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
    { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//        System.out.println("DatabaseHelper / _onDowngrade / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }

}
