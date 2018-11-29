    package com.cw.litenotes.note;

    import android.app.Activity;
    import android.app.Dialog;
    import android.content.pm.ActivityInfo;
    import android.graphics.Bitmap;
    import android.graphics.Canvas;
    import android.graphics.Color;
    import android.graphics.Paint;
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
    import com.cw.litenotes.note_common.Note_drawingView;
    import com.cw.litenotes.tabs.TabsHost;

    import java.util.Date;

    public class Note_editDrawing extends Activity
    {
        private Note_drawingView drawingView; // drawing View

        // create menu ids for each menu option
        private static final int COLOR_MENU_ID = Menu.FIRST;
        private static final int WIDTH_MENU_ID = Menu.FIRST + 1;
        private static final int ERASE_MENU_ID = Menu.FIRST + 2;
        private static final int CLEAR_MENU_ID = Menu.FIRST + 3;
        private static final int UPDATE_MENU_ID = Menu.FIRST + 4;

        // variable that refers to a Choose Color or Choose Line Width dialog
        private Dialog currentDialog;

        private DB_page dB;
        public static Long noteId;
        String selectedDrawingUri;
        public static String drawingUriInDB;
        long id;

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
             super.onCreate(savedInstanceState);

             Bundle extras = getIntent().getExtras();
             drawingUriInDB = extras.getString("drawing_uri");
             id = extras.getLong("drawing_id");
             dB = new DB_page(this, TabsHost.getCurrentPageTableId());

             // edit drawing mode
             Note_drawingView.drawing_mode = Note_drawingView.EDIT_MODE;

             // get reference to the DoodleView
             setContentView(R.layout.drawing_main); // inflate the layout
             drawingView = findViewById(R.id.doodleView);

             selectedDrawingUri = "";

             getActionBar().setTitle(R.string.edit_drawing);
        }

        @Override
        protected void onResume() {
            super.onResume();
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        @Override
        protected void onPause(){
            super.onPause();
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu)
        {
            super.onCreateOptionsMenu(menu); // call super's method

            menu.add(0, COLOR_MENU_ID, 0, R.string.menuitem_color)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

            menu.add(0, WIDTH_MENU_ID, 1, R.string.menuitem_line_width)
                .setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

            menu.add(Menu.NONE, ERASE_MENU_ID, Menu.NONE,
            R.string.menuitem_erase);
            menu.add(Menu.NONE, CLEAR_MENU_ID, Menu.NONE,
            R.string.menuitem_clear);
            menu.add(Menu.NONE, UPDATE_MENU_ID, Menu.NONE,
            R.string.menuitem_update_image);

            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            // switch based on the MenuItem id
            switch (item.getItemId())
            {
                case COLOR_MENU_ID:
                    showColorDialog(); // display color selection dialog
                    return true;

                case WIDTH_MENU_ID:
                    showLineWidthDialog(); // display line thickness dialog
                    return true;

                case ERASE_MENU_ID:
                    drawingView.setDrawingColor(Color.WHITE); // line color white
                    return true;

                case CLEAR_MENU_ID:
                    drawingView.clear(); // clear drawingView
                    return true;

                case UPDATE_MENU_ID:
                    drawingView.updateImage(); // save the current images
                    Date now = new Date();
                    dB.updateNote(id,
                            dB.getNoteTitle_byId(id),
                            dB.getNotePictureUri_byId(id),
                            dB.getNoteAudioUri_byId(id),
                            drawingUriInDB,
                            dB.getNoteLinkUri_byId(id),
                            dB.getNoteBody_byId(id),
                            dB.getNoteMarking_byId(id),
                            now.getTime(),
                            true);// add new note, get return row Id
                    return true;
            }

            return super.onOptionsItemSelected(item); // call super's method
        }

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

            currentDialog.show();
        }

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
            }

            // required method of interface OnSeekBarChangeListener
            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            // required method of interface OnSeekBarChangeListener
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        };

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
                currentDialog.dismiss(); // hide the dialog
                currentDialog = null; // dialog no longer needed
            }
        };

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

            currentDialog.show(); // show the dialog
        }

        // OnSeekBarChangeListener for the SeekBar in the width dialog
        private OnSeekBarChangeListener widthSeekBarChanged = new OnSeekBarChangeListener() {
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
            }

            // required method of interface OnSeekBarChangeListener
            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            // required method of interface OnSeekBarChangeListener
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        };

        // OnClickListener for the line width dialog's Set Line Width Button
        private OnClickListener setLineWidthButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v)
            {
               // get the color SeekBars
               SeekBar widthSeekBar =
                  (SeekBar) currentDialog.findViewById(R.id.widthSeekBar);

               // set the line color
               drawingView.setLineWidth(widthSeekBar.getProgress());
               currentDialog.dismiss(); // hide the dialog
               currentDialog = null; // dialog no longer needed
            }
        };
   }
