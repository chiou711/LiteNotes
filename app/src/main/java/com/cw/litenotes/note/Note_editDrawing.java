// Note_editDrawing.java
// Draws View which changes color in response to user touches.
package com.cw.litenotes.note;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.cw.litenotes.R;
import com.cw.litenotes.db.DB_page;
import com.cw.litenotes.note_add.Note_drawingView;
import com.cw.litenotes.page.Page_recycler;
import com.cw.litenotes.util.Util;
import com.cw.litenotes.util.preferences.Pref;

import java.util.concurrent.atomic.AtomicBoolean;

public class Note_editDrawing extends Activity
{
   private Note_drawingView drawingView; // drawing View
   private SensorManager sensorManager; // monitors accelerometer
   private float acceleration; // acceleration
   private float currentAcceleration; // current acceleration
   private float lastAcceleration; // last acceleration
   private AtomicBoolean dialogIsVisible = new AtomicBoolean(); // false

   // create menu ids for each menu option 
   private static final int COLOR_MENU_ID = Menu.FIRST;
   private static final int WIDTH_MENU_ID = Menu.FIRST + 1;
   private static final int ERASE_MENU_ID = Menu.FIRST + 2;
   private static final int CLEAR_MENU_ID = Menu.FIRST + 3;
   private static final int SAVE_MENU_ID = Menu.FIRST + 4;
   
   // value used to determine whether user shook the device to erase
   private static final int ACCELERATION_THRESHOLD = 15000;
   
   // variable that refers to a Choose Color or Choose Line Width dialog
   private Dialog currentDialog;

   private DB_page dB;
   Long noteId;
   String selectedDrawingUri;
   String drawingUriInDB;

   // called when this Activity is loaded
   @Override
   protected void onCreate(Bundle savedInstanceState) 
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.drawing_main); // inflate the layout

      // get reference to the DoodleView
      drawingView = findViewById(R.id.doodleView);
      
      // initialize acceleration values
      acceleration = 0.00f; 
      currentAcceleration = SensorManager.GRAVITY_EARTH;    
      lastAcceleration = SensorManager.GRAVITY_EARTH;    
     
      enableAccelerometerListening(); // listen for shake

      drawingUriInDB = "";
      selectedDrawingUri = "";

      // get row Id from saved instance
      noteId = (savedInstanceState == null) ? null :
              (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);

      // get audio Uri in DB if instance is not null
      dB = new DB_page(this, Pref.getPref_focusView_page_tableId(this));
      if(savedInstanceState != null)
      {
         System.out.println("Note_addAudio / noteId =  " + noteId);
         if(noteId != null)
            drawingUriInDB = dB.getNoteAudioUri_byId(noteId);
      }

      getActionBar().setTitle("Drawing");

   } // end method onCreate

   // when app is sent to the background, stop listening for sensor events
   @Override
   protected void onPause()
   {
      super.onPause();
      disableAccelerometerListening(); // don't listen for shake 
   } // end method onPause

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
   }

   // enable listening for accelerometer events
   private void enableAccelerometerListening()
   {
      // initialize the SensorManager
      sensorManager = 
         (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      sensorManager.registerListener(sensorEventListener, 
         sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
         SensorManager.SENSOR_DELAY_NORMAL);
   } // end method enableAccelerometerListening
   
   // disable listening for accelerometer events
   private void disableAccelerometerListening()
   {
      // stop listening for sensor events
      if (sensorManager != null) 
      {         
         sensorManager.unregisterListener(
            sensorEventListener, 
            sensorManager.getDefaultSensor(
               SensorManager.SENSOR_ACCELEROMETER));         
         sensorManager = null;     
      } // end if
   } // end method disableAccelerometerListening

   // event handler for accelerometer events
   private SensorEventListener sensorEventListener = 
      new SensorEventListener()
      {
         // use accelerometer to determine whether user shook device 
         @Override
         public void onSensorChanged(SensorEvent event)
         {  
            // ensure that other dialogs are not displayed
            if (!dialogIsVisible.get())
            {
               // get x, y, and z values for the SensorEvent
               float x = event.values[0];
               float y = event.values[1];
               float z = event.values[2];
      
               // save previous acceleration value
               lastAcceleration = currentAcceleration;
      
               // calculate the current acceleration
               currentAcceleration = x * x + y * y + z * z;
      
               // calculate the change in acceleration
               acceleration = currentAcceleration * 
                  (currentAcceleration - lastAcceleration);
      
               // if the acceleration is above a certain threshold
               if (acceleration > ACCELERATION_THRESHOLD)
               {
                  // create a new AlertDialog Builder
                  AlertDialog.Builder builder = 
                     new AlertDialog.Builder(Note_editDrawing.this);
                
                  // set the AlertDialog's message 
                  builder.setMessage(R.string.message_erase);
                  builder.setCancelable(true);
                
                  // add Erase Button
                  builder.setPositiveButton(R.string.button_erase,
                     new DialogInterface.OnClickListener() 
                     {
                        public void onClick(DialogInterface dialog, int id) 
                        {
                          dialogIsVisible.set(false);
                          drawingView.clear(); // clear the screen
                        } // end method onClick
                     } // end anonymous inner class
                  ); // end call to setPositiveButton
               
                  // add Cancel Button
                  builder.setNegativeButton(R.string.button_cancel,
                     new DialogInterface.OnClickListener() 
                     {
                        public void onClick(DialogInterface dialog, int id) 
                        {
                          dialogIsVisible.set(false);
                          dialog.cancel(); // dismiss the dialog
                        } // end method onClick
                     } // end anonymous inner class
                  ); // end call to setNegativeButton
      
                  dialogIsVisible.set(true); // dialog is on the screen
                  builder.show(); // display the dialog
               } // end if
            } // end if
         } // end method onSensorChanged
      
         // required method of interface SensorEventListener
         @Override
         public void onAccuracyChanged(Sensor sensor, int accuracy)
         {
         } // end method onAccuracyChanged
      }; // end anonymous inner class 
   
   // displays configuration options in menu
   @Override
   public boolean onCreateOptionsMenu(Menu menu) 
   {
      super.onCreateOptionsMenu(menu); // call super's method

      // add options to menu
//      menu.add(Menu.NONE, COLOR_MENU_ID, Menu.NONE,
//         R.string.menuitem_color);


      menu.add(0, COLOR_MENU_ID, 0, R.string.menuitem_color)
	      .setIcon(android.R.drawable.ic_menu_edit)
	      .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

//      menu.add(Menu.NONE, WIDTH_MENU_ID, Menu.NONE,
//         R.string.menuitem_line_width);

       menu.add(0, WIDTH_MENU_ID, 1, R.string.menuitem_line_width)
           .setIcon(android.R.drawable.ic_menu_sort_by_size)
           .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

      menu.add(Menu.NONE, ERASE_MENU_ID, Menu.NONE, 
         R.string.menuitem_erase);
      menu.add(Menu.NONE, CLEAR_MENU_ID, Menu.NONE, 
         R.string.menuitem_clear);
      menu.add(Menu.NONE, SAVE_MENU_ID, Menu.NONE, 
         R.string.menuitem_save_image);

      return true; // options menu creation was handled
   } // end onCreateOptionsMenu

   // handle choice from options menu
   @Override
   public boolean onOptionsItemSelected(MenuItem item) 
   {
      // switch based on the MenuItem id
      switch (item.getItemId()) 
      {
         case COLOR_MENU_ID:
            showColorDialog(); // display color selection dialog
            return true; // consume the menu event
         case WIDTH_MENU_ID:
            showLineWidthDialog(); // display line thickness dialog
            return true; // consume the menu event
         case ERASE_MENU_ID:
            drawingView.setDrawingColor(Color.WHITE); // line color white
            return true; // consume the menu event
         case CLEAR_MENU_ID:
            drawingView.clear(); // clear drawingView
            return true; // consume the menu event
         case SAVE_MENU_ID:
            //todo Add storage permission
            String uriStr = drawingView.saveImage(this); // save the current images

            dB = new DB_page(this, Pref.getPref_focusView_page_tableId(this));

            String scheme = Uri.parse(uriStr).getScheme();
               // add single file
               if( scheme.equalsIgnoreCase("file") ||
                   scheme.equalsIgnoreCase("content") )
               {
                  // check if content scheme points to local file
                  if(scheme.equalsIgnoreCase("content"))
                  {
                     String realPath = Util.getLocalRealPathByUri(this, Uri.parse(uriStr));

                     if(realPath != null)
                        uriStr = "file://".concat(realPath);
                  }

                  noteId = null; // set null for Insert

                  if( !Util.isEmptyString(uriStr))
                  {
                     // insert
                     // set marking to 1 for default
                     noteId = dB.insertNote("", "", "", uriStr, "", "", 1, (long) 0);// add new note, get return row Id
                  }

                  if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
                      dB.getNotesCount(true) > 0 )
                  {
                     Page_recycler.swap(Page_recycler.mDb_page);
                  }

                  if(!Util.isEmptyString(uriStr))
                  {
                     String drawingName = Util.getDisplayNameByUriString(uriStr, this);
                     Util.showSavedFileToast(drawingName,this);
                  }
               }

               return true; // consume the menu event
      } // end switch

      return super.onOptionsItemSelected(item); // call super's method
   } // end method onOptionsItemSelected
     
   // display a dialog for selecting color
   private void showColorDialog()
   {
      // create the dialog and inflate its content
      currentDialog = new Dialog(this);
      currentDialog.setContentView(R.layout.drawing_color_dialog);
      currentDialog.setTitle(R.string.title_color_dialog);
      currentDialog.setCancelable(true);
      
      // get the color SeekBars and set their onChange listeners
      final SeekBar alphaSeekBar = 
         (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
      final SeekBar redSeekBar = 
         (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
      final SeekBar greenSeekBar = 
         (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
      final SeekBar blueSeekBar = 
         (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);

      // register SeekBar event listeners
      alphaSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
      redSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
      greenSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
      blueSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged);
     
      // use current drawing color to set SeekBar values
      final int color = drawingView.getDrawingColor();
      alphaSeekBar.setProgress(Color.alpha(color));
      redSeekBar.setProgress(Color.red(color));
      greenSeekBar.setProgress(Color.green(color));
      blueSeekBar.setProgress(Color.blue(color));        
      
      // set the Set Color Button's onClickListener
      Button setColorButton = (Button) currentDialog.findViewById(
         R.id.setColorButton);
      setColorButton.setOnClickListener(setColorButtonListener);
 
      dialogIsVisible.set(true); // dialog is on the screen
      currentDialog.show(); // show the dialog
   } // end method showColorDialog
   
   // OnSeekBarChangeListener for the SeekBars in the color dialog
   private OnSeekBarChangeListener colorSeekBarChanged = 
     new OnSeekBarChangeListener() 
   {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress,
         boolean fromUser) 
      {
         // get the SeekBars and the colorView LinearLayout
         SeekBar alphaSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
         SeekBar redSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
         SeekBar greenSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
         SeekBar blueSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);
         View colorView = 
            (View) currentDialog.findViewById(R.id.colorView);

         // display the current color
         colorView.setBackgroundColor(Color.argb(
            alphaSeekBar.getProgress(), redSeekBar.getProgress(), 
            greenSeekBar.getProgress(), blueSeekBar.getProgress()));
      } // end method onProgressChanged
      
      // required method of interface OnSeekBarChangeListener
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) 
      {
      } // end method onStartTrackingTouch
      
      // required method of interface OnSeekBarChangeListener
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) 
      {
      } // end method onStopTrackingTouch
   }; // end colorSeekBarChanged
   
   // OnClickListener for the color dialog's Set Color Button
   private OnClickListener setColorButtonListener = new OnClickListener() 
   {
      @Override
      public void onClick(View v) 
      {
         // get the color SeekBars
         SeekBar alphaSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.alphaSeekBar);
         SeekBar redSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
         SeekBar greenSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.greenSeekBar);
         SeekBar blueSeekBar = 
            (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);

         // set the line color
         drawingView.setDrawingColor(Color.argb(
            alphaSeekBar.getProgress(), redSeekBar.getProgress(), 
            greenSeekBar.getProgress(), blueSeekBar.getProgress()));
         dialogIsVisible.set(false); // dialog is not on the screen
         currentDialog.dismiss(); // hide the dialog
         currentDialog = null; // dialog no longer needed
      } // end method onClick
   }; // end setColorButtonListener
   
   // display a dialog for setting the line width
   private void showLineWidthDialog()
   {
      // create the dialog and inflate its content
      currentDialog = new Dialog(this);
      currentDialog.setContentView(R.layout.drawing_width_dialog);
      currentDialog.setTitle(R.string.title_line_width_dialog);
      currentDialog.setCancelable(true);
      
      // get widthSeekBar and configure it
      SeekBar widthSeekBar = 
         (SeekBar) currentDialog.findViewById(R.id.widthSeekBar);
      widthSeekBar.setOnSeekBarChangeListener(widthSeekBarChanged);
      widthSeekBar.setProgress(drawingView.getLineWidth());
       
      // set the Set Line Width Button's onClickListener
      Button setLineWidthButton = 
         (Button) currentDialog.findViewById(R.id.widthDialogDoneButton);
      setLineWidthButton.setOnClickListener(setLineWidthButtonListener);
      
      dialogIsVisible.set(true); // dialog is on the screen
      currentDialog.show(); // show the dialog      
   } // end method showLineWidthDialog

   // OnSeekBarChangeListener for the SeekBar in the width dialog
   private OnSeekBarChangeListener widthSeekBarChanged = 
      new OnSeekBarChangeListener() 
      {
         Bitmap bitmap = Bitmap.createBitmap( // create Bitmap
            400, 100, Bitmap.Config.ARGB_8888);
         Canvas canvas = new Canvas(bitmap); // associate with Canvas
         
         @Override
         public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) 
         {  
            // get the ImageView
            ImageView widthImageView = (ImageView) 
               currentDialog.findViewById(R.id.widthImageView);
            
            // configure a Paint object for the current SeekBar value
            Paint p = new Paint();
            p.setColor(drawingView.getDrawingColor());
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(progress);
            
            // erase the bitmap and redraw the line
            bitmap.eraseColor(Color.WHITE);
            canvas.drawLine(30, 50, 370, 50, p);
            widthImageView.setImageBitmap(bitmap);
         } // end method onProgressChanged
   
         // required method of interface OnSeekBarChangeListener
         @Override
         public void onStartTrackingTouch(SeekBar seekBar) 
         {
         } // end method onStartTrackingTouch
   
         // required method of interface OnSeekBarChangeListener
         @Override
         public void onStopTrackingTouch(SeekBar seekBar) 
         {
         } // end method onStopTrackingTouch
      }; // end widthSeekBarChanged

   // OnClickListener for the line width dialog's Set Line Width Button
   private OnClickListener setLineWidthButtonListener = 
      new OnClickListener() 
      {
         @Override
         public void onClick(View v) 
         {
            // get the color SeekBars
            SeekBar widthSeekBar = 
               (SeekBar) currentDialog.findViewById(R.id.widthSeekBar);
   
            // set the line color
            drawingView.setLineWidth(widthSeekBar.getProgress());
            dialogIsVisible.set(false); // dialog is not on the screen
            currentDialog.dismiss(); // hide the dialog
            currentDialog = null; // dialog no longer needed
         } // end method onClick
      }; // end setColorButtonListener
} // end class Note_editDrawing


/**************************************************************************
 * (C) Copyright 1992-2012 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 **************************************************************************/
