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

package com.cw.litenote.note_common;

import java.util.Date;

import com.cw.litenote.db.DB_folder;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.util.image.TouchImageView;
import com.cw.litenote.util.image.UtilImage_bitmapLoader;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.preferences.Pref;
import com.cw.litenote.util.uil.UilCommon;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Note_common {

	public TextView audioTextView;

	public ImageView picImageView;
	public String pictureUriInDB;
	public String drawingUriInDB;
	public String audioUriInDB;
	public String oriPictureUri;
	public String currPictureUri;
	public String currAudioUri;

	public String oriAudioUri;
	public String oriDrawingUri;
	public String oriLinkUri;

	public EditText linkEditText;
	public EditText titleEditText;
	public EditText bodyEditText;
	public String oriTitle;
	public String oriBody;

	public Long noteId;
	public Long oriCreatedTime;
	public Long oriMarking;

	public boolean bRollBackData;
	public boolean bRemovePictureUri = false;
	public boolean bRemoveAudioUri = false;
	public boolean bEditPicture = false;

    private DB_page dB_page;
    Activity act;
    int style;
    ProgressBar progressBar;
    ProgressBar progressBarExpand;
	TouchImageView enlargedImage;

	public Note_common(Activity act,DB_page _db, Long noteId,String strTitle, String pictureUri, String audioUri, String drawingUri, String linkUri, String strBody, Long createdTime)
    {
    	this.act = act;
    	this.noteId = noteId;
    			
    	oriTitle = strTitle;
	    oriBody = strBody;
	    oriPictureUri = pictureUri;
	    oriAudioUri = audioUri;
	    oriDrawingUri = drawingUri;
	    oriLinkUri = linkUri;
	    
	    oriCreatedTime = createdTime;
	    currPictureUri = pictureUri;
	    currAudioUri = audioUri;
	    
	    dB_page = _db;//Page.mDb_page;
	    
	    oriMarking = dB_page.getNoteMarking_byId(noteId);
		
	    bRollBackData = false;
		bEditPicture = true;
		bShowEnlargedImage = false;
    }

	public Note_common(Activity act)
    {
    	this.act = act;
		dB_page = new DB_page(act, TabsHost.getCurrentPageTableId());
    }

	public void UI_init()
    {

		UI_init_text();

    	audioTextView = (TextView) act.findViewById(R.id.edit_audio);
    	linkEditText = (EditText) act.findViewById(R.id.edit_link);
        picImageView = (ImageView) act.findViewById(R.id.edit_picture);

        progressBar = (ProgressBar) act.findViewById(R.id.edit_progress_bar);
        progressBarExpand = (ProgressBar) act.findViewById(R.id.edit_progress_bar_expand);

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		enlargedImage = (TouchImageView)act.findViewById(R.id.expanded_image);

		//set audio color
//		audioTextView.setTextColor(Util.mText_ColorArray[style]);
//		audioTextView.setBackgroundColor(Util.mBG_ColorArray[style]);

		//set link color
		if(linkEditText != null)
		{
			linkEditText.setTextColor(ColorSet.mText_ColorArray[style]);
			linkEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		}

		picImageView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);

		// set thumb nail listener
        picImageView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view) {
            	if(bShowEnlargedImage == true)
            	{
            		closeEnlargedImage();
            		// show soft input
//            		if (act.getCurrentFocus() != null)
//            		    imm.showSoftInput(act.getCurrentFocus(), 0);
            	}
            	else
                {
            		// hide soft input
            		if (act.getCurrentFocus() != null)
            			imm.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);

                	System.out.println("Note_common / pictureUriInDB = " + pictureUriInDB);
                	if( (!Util.isEmptyString(pictureUriInDB)) ||
						(!Util.isEmptyString(drawingUriInDB))   )
                	{
                		bRemovePictureUri = false;
                		System.out.println("picImageView.setOnClickListener / pictureUriInDB = " + pictureUriInDB);

                		// check if pictureUri has scheme
                		if(Util.isUriExisted(pictureUriInDB, act) ||
                           Util.isUriExisted(drawingUriInDB, act)	)
                		{
	                		if(Uri.parse(pictureUriInDB).isAbsolute())
	                		{
//	                			int style =  Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());
	                			new UtilImage_bitmapLoader(enlargedImage,
                                                           pictureUriInDB,
                                                           progressBarExpand,
//	                					                   (style % 2 == 1 ?
//                                                            UilCommon.optionsForRounded_light:
//                                                            UilCommon.optionsForRounded_dark),
                                                           UilCommon.optionsForFadeIn,
                                                           act);
	                			bShowEnlargedImage = true;
	                		}
                            else if(Uri.parse(drawingUriInDB).isAbsolute())
                            {
//	                			int style =  Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());
                                new UtilImage_bitmapLoader(enlargedImage,
                                        drawingUriInDB,
                                        progressBarExpand,
//	                					                   (style % 2 == 1 ?
//                                                            UilCommon.optionsForRounded_light:
//                                                            UilCommon.optionsForRounded_dark),
                                        UilCommon.optionsForFadeIn,
                                        act);
                                bShowEnlargedImage = true;
                            }
	                		else
	                		{
	                			System.out.println("pictureUriInDB is not Uri format");
	                		}
                		}
                		else
                			Toast.makeText(act,R.string.file_not_found,Toast.LENGTH_SHORT).show();
                	}
                	else
            			Toast.makeText(act,R.string.file_is_not_created,Toast.LENGTH_SHORT).show();

				}
            }
        });

		// set thumb nail long click listener
        picImageView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view) {
            	if(bEditPicture) {
					if(!Util.isEmptyString(pictureUriInDB) )
						openSetPictureDialog();
					else if(!Util.isEmptyString(drawingUriInDB))
					{
						Intent i = new Intent(act, Note_drawing.class);
						i.putExtra("drawing_id",noteId);
						i.putExtra("drawing_mode",Util.DRAWING_EDIT);
						act.startActivityForResult(i,Util.DRAWING_EDIT);
					}
				}
                return false;
            }
        });
    }

	public void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText = (EditText) act.findViewById(R.id.edit_title);
		bodyEditText = (EditText) act.findViewById(R.id.edit_body);
		linkEditText = (EditText) act.findViewById(R.id.edit_link);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set body color
		bodyEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		bodyEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set link color
		linkEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		linkEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

    // set image close listener
	public void setCloseImageListeners(EditText editText)
    {
    	editText.setOnClickListener(new OnClickListener()
    	{   @Override
			public void onClick(View v) 
			{
				if(bShowEnlargedImage == true)
					closeEnlargedImage();
			}
		});
    	
    	editText.setOnFocusChangeListener(new OnFocusChangeListener() 
    	{   @Override
            public void onFocusChange(View v, boolean hasFocus) 
    		{
    				if(bShowEnlargedImage == true)
    					closeEnlargedImage();
            } 
    	});   
    }


	public boolean bShowEnlargedImage;
	public void closeEnlargedImage()
    {
    	System.out.println("closeExpandImage");
		enlargedImage.setVisibility(View.GONE);
		bShowEnlargedImage = false;
    }

	public void openSetPictureDialog()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setTitle(R.string.edit_note_set_picture_dlg_title)
			   .setMessage(currPictureUri)
			   .setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						bRemovePictureUri = false; // reset
						// For selecting local gallery
//						Intent intent = new Intent(act, PictureGridAct.class);
//						intent.putExtra("gallery", false);
//						act.startActivityForResult(intent, Util.ACTIVITY_SELECT_PICTURE);
						
						// select global
						final String[] items = new String[]{act.getResources().getText(R.string.note_ready_image).toString(),
															act.getResources().getText(R.string.note_ready_video).toString()};
					    AlertDialog.Builder builder = new AlertDialog.Builder(act);
					   
					    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
					    {
							@Override
							public void onClick(DialogInterface dialog, int which) 
							{
								String mediaType = null;
								if(which ==0)
									mediaType = "image/*";
								else if(which ==1)
									mediaType = "video/*";
								
								System.out.println("Note_common / _openSetPictureDialog / mediaType = " + mediaType);
								act.startActivityForResult(Util.chooseMediaIntentByType(act, mediaType),
				   						Util.CHOOSER_SET_PICTURE);	
								//end
								dialog.dismiss();
							}
					    };
					    builder.setTitle(R.string.edit_note_set_picture_dlg_title)
							   .setSingleChoiceItems(items, -1, listener)
							   .setNegativeButton(R.string.btn_Cancel, null)
							   .show();
					}
				})					
			   .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{// cancel
					}
				});

				if(!pictureUriInDB.isEmpty())
				{
					builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							//just delete picture file name
							currPictureUri = "";
							oriPictureUri = "";
					    	removePictureStringFromCurrentEditNote(noteId);
					    	populateFields_all(noteId);
					    	bRemovePictureUri = true;
						}
					});
				}
		
		Dialog dialog = builder.create();
		dialog.show();
    }

	public void deleteNote(Long rowId)
    {
    	System.out.println("Note_common / _deleteNote");
        // for Add new note (noteId is null first), but decide to cancel
        if(rowId != null)
        	dB_page.deleteNote(rowId,true);
    }
    
    // populate text fields
	public void populateFields_text(Long rowId)
	{
		if (rowId != null) {
			// title
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			titleEditText.setText(strTitleEdit);
			titleEditText.setSelection(strTitleEdit.length());

			// body
			String strBodyEdit = dB_page.getNoteBody_byId(rowId);
			bodyEditText.setText(strBodyEdit);
			bodyEditText.setSelection(strBodyEdit.length());

			// link
			String strLinkEdit = dB_page.getNoteLinkUri_byId(rowId);
			linkEditText.setText(strLinkEdit);
			linkEditText.setSelection(strLinkEdit.length());
		}
        else
        {
            // renew title
            String strBlank = "";
            titleEditText.setText(strBlank);
            titleEditText.setSelection(strBlank.length());
            titleEditText.requestFocus();

            // renew body
            bodyEditText.setText(strBlank);
            bodyEditText.setSelection(strBlank.length());

			// renew link
			linkEditText.setText(strBlank);
			linkEditText.setSelection(strBlank.length());
        }
	}

    // populate all fields
	public void populateFields_all(Long rowId)
    {
    	if (rowId != null) 
    	{
			populateFields_text(rowId);

    		// for picture block
			pictureUriInDB = dB_page.getNotePictureUri_byId(rowId);
			drawingUriInDB = dB_page.getNoteDrawingUri_byId(rowId);
			System.out.println("populateFields_all / mPictureFileNameInDB = " + pictureUriInDB);
    		
			// load bitmap to image view
			if( (!Util.isEmptyString(pictureUriInDB)) || (!Util.isEmptyString(drawingUriInDB)) )
			{
				int style =  Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());

				String thumbUri = "";
				if(!Util.isEmptyString(pictureUriInDB) )
					thumbUri = pictureUriInDB;
				else if(!Util.isEmptyString(drawingUriInDB))
					thumbUri = drawingUriInDB;

				new UtilImage_bitmapLoader(picImageView,
						                   thumbUri, progressBar,
//    					                   (style % 2 == 1 ?
//                                            UilCommon.optionsForRounded_light:
//                                            UilCommon.optionsForRounded_dark),
                                           UilCommon.optionsForFadeIn,
                                           act);
			}
			else
			{
	    		picImageView.setImageResource(style %2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);
			}
			
			// set listeners for closing image view 
	    	if(!Util.isEmptyString(pictureUriInDB))
	    	{
	    		setCloseImageListeners(linkEditText);
	    		setCloseImageListeners(titleEditText);
	    		setCloseImageListeners(bodyEditText);
	    	}			
	    	
    		// audio
			audioUriInDB = dB_page.getNoteAudioUri_byId(rowId);
        	if(!Util.isEmptyString(audioUriInDB))
    		{
    			String audio_name = audioUriInDB;
				System.out.println("populateFields_all / set audio name / audio_name = " + audio_name);
				audioTextView.setText(act.getResources().getText(R.string.note_audio) + ": " + audio_name);
    		}
        	else
				audioTextView.setText("");
        		
    		// link
			String strLinkEdit = dB_page.getNoteLink_byId(rowId);
            linkEditText.setText(strLinkEdit);
            linkEditText.setSelection(strLinkEdit.length());

            // title        	
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			final String curLinkStr = linkEditText.getText().toString();
			if( Util.isEmptyString(strTitleEdit) &&
				Util.isEmptyString(titleEditText.getText().toString()) )
			{
				if(Util.isYouTubeLink(curLinkStr) )
				{
					final String hint = Util.getYouTubeTitle(curLinkStr);

					titleEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
								titleEditText.setHint(Html.fromHtml("<small style=\"text-color: gray;\"><i>" +
																	  hint +
																	  "</i></small>") );
                            }
                        }
                    });

					titleEditText.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
						        ((EditText) v).setText(hint);
                                ((EditText) v).setSelection(hint.length());
							return false;
						}
					});
				}
				else if(curLinkStr.startsWith("http"))
				{
					Util.setHttpTitle(curLinkStr, act, titleEditText);
				}
			}
        }
    	else
    	{
            // renew link
			String strLinkEdit = "";
			if(linkEditText != null)
			{
	            linkEditText.setText(strLinkEdit);
	            linkEditText.setSelection(strLinkEdit.length());
	            linkEditText.requestFocus();
			}
    	}
    }

	public boolean isLinkUriModified()
    {
    	return !oriLinkUri.equals(linkEditText.getText().toString());
    }

	public boolean isTitleModified()
    {
    	return !oriTitle.equals(titleEditText.getText().toString());
    }

	public boolean isPictureModified()
    {
    	return !oriPictureUri.equals(pictureUriInDB);
    }

	public boolean isAudioModified()
    {
    	if(oriAudioUri == null)
    		return false;
    	else
    		return !oriAudioUri.equals(audioUriInDB);
    }

	public boolean isBodyModified()
    {
    	return !oriBody.equals(bodyEditText.getText().toString());
    }

	public boolean isNoteModified()
    {
    	boolean bModified = false;
//		System.out.println("Note_common / _isNoteModified / isTitleModified() = " + isTitleModified());
//		System.out.println("Note_common / _isNoteModified / isPictureModified() = " + isPictureModified());
//		System.out.println("Note_common / _isNoteModified / isAudioModified() = " + isAudioModified());
//		System.out.println("Note_common / _isNoteModified / isBodyModified() = " + isBodyModified());
//		System.out.println("Note_common / _isNoteModified / bRemovePictureUri = " + bRemovePictureUri);
//		System.out.println("Note_common / _isNoteModified / bRemoveAudioUri = " + bRemoveAudioUri);
    	if( isTitleModified() ||
    		isPictureModified() ||
    		isAudioModified() ||
    		isBodyModified() ||
    		isLinkUriModified() ||
    		bRemovePictureUri ||
    		bRemoveAudioUri)
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }

	public boolean isTextAdded()
    {
    	boolean bEdit = false;
    	String curTitle = titleEditText.getText().toString();
		String curBody = bodyEditText.getText().toString();
		String curLink = linkEditText.getText().toString();

    	if(!Util.isEmptyString(curTitle)||
		   !Util.isEmptyString(curBody) ||
		   !Util.isEmptyString(curLink)   )
       	{
    		bEdit = true;
       	}
       	
    	return bEdit;
    }

	public Long saveStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri)
	{
		String linkUri = "";
		if(linkEditText != null)
			linkUri = linkEditText.getText().toString();
    	String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();

        if(enSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!title.isEmpty()) ||
	        		(!body.isEmpty()) ||
	        		(!pictureUri.isEmpty()) || 
	        		(!audioUri.isEmpty()) ||
	        		(!linkUri.isEmpty())            )
	        	{
	        		// insert
	        		System.out.println("Note_common / _saveStateInDB / insert");
	        		rowId = dB_page.insertNote(title, pictureUri, audioUri, drawingUri, linkUri, body, 0, (long) 0);// add new note, get return row Id
	        	}
        		currPictureUri = pictureUri; // update file name
        		currAudioUri = audioUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date(); 
	        	if( !Util.isEmptyString(title) || 
	        		!Util.isEmptyString(body) ||
	        		!Util.isEmptyString(pictureUri) ||
	        		!Util.isEmptyString(audioUri) ||
	        		!Util.isEmptyString(linkUri)       )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_common / _saveStateInDB / update: roll back");
			        	linkUri = oriLinkUri;
	        			title = oriTitle;
	        			body = oriBody;
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, title, pictureUri, audioUri, drawingUri, linkUri, body, oriMarking, time,true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_common / _saveStateInDB / update new");
						System.out.println("--- rowId = " + rowId);
						System.out.println("--- oriMarking = " + oriMarking);
						System.out.println("--- audioUri = " + audioUri);

                        long marking;
                        if(null == oriMarking)
                            marking = 0;
                        else
                            marking = oriMarking;

//						long marking = (!audioUri.isEmpty())?1:oriMarking;
                        boolean isOK;
	        			isOK = dB_page.updateNote(rowId, title, pictureUri, audioUri, drawingUri, linkUri, body,
												marking, now.getTime(),true); // update note
	        			System.out.println("--- isOK = " + isOK);
	        		}
	        		currPictureUri = pictureUri;
	        		currAudioUri = audioUri;
	        	}
	        	else if( Util.isEmptyString(title) &&
	        			 Util.isEmptyString(body) &&
 						 Util.isEmptyString(pictureUri) &&
						 Util.isEmptyString(drawingUri) &&
			        	 Util.isEmptyString(audioUri) &&
			        	 Util.isEmptyString(linkUri)         )
	        	{
	        		// delete
	        		System.out.println("Note_common / _saveStateInDB / delete");
	        		deleteNote(rowId);
	        	}
	        }
        }
        
		return rowId;
	}

	public Long savePictureStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri, String linkUri)
	{
		boolean mEnSaveDb = enSaveDb;
        if(mEnSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( !pictureUri.isEmpty())
	        	{
	        		// insert
					String name = Util.getDisplayNameByUriString(pictureUri, act);
	        		System.out.println("Note_common / _savePictureStateInDB / insert");
	        		rowId = dB_page.insertNote(name, pictureUri, audioUri, drawingUri, linkUri, "", 1, (long) 0);// add new note, get return row Id
	        	}
        		currPictureUri = pictureUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date(); 
	        	if( !pictureUri.isEmpty())
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_common / _savePictureStateInDB / update: roll back");
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, "", pictureUri, audioUri, drawingUri, linkUri, "", oriMarking, time, true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_common / _savePictureStateInDB / update new");
	        			dB_page.updateNote(rowId, "", pictureUri, audioUri, drawingUri, linkUri, "", 1, now.getTime(), true); // update note
	        		}
	        		currPictureUri = pictureUri; // update file name
	        	}
	        	else if(pictureUri.isEmpty())
	        	{
	        		// delete
	        		System.out.println("Note_common / _savePictureStateInDB / delete");
	        		deleteNote(rowId);
	        	}
	        }
        }
        
		return rowId;
	}
	
	// for confirmation condition
	public void removePictureStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
    				   "",
				oriAudioUri,
				oriDrawingUri,
				oriLinkUri,
				oriBody,
				oriMarking,
				oriCreatedTime, true );
	}

	public void removePictureStringFromCurrentEditNote(Long rowId) {
        String linkUri = linkEditText.getText().toString();
        String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();
        
    	dB_page.updateNote(rowId,
    				   title,
    				   "",
				oriAudioUri,
				oriDrawingUri,
    				   linkUri,
    				   body,
				oriMarking,
				oriCreatedTime, true );
	}

	public void removeAudioStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
				oriPictureUri,
    				   "",
				oriDrawingUri,
				oriLinkUri,
				oriBody,
				oriMarking,
				oriCreatedTime, true );
	}

	public void removeAudioStringFromCurrentEditNote(Long rowId) {
        String linkUri = linkEditText.getText().toString();
        String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();
        dB_page.updateNote(rowId,
    				   title,
				oriPictureUri,
    				   "",
				oriDrawingUri,
    				   linkUri,
    				   body,
				oriMarking,
				oriCreatedTime, true );
	}

	public void removeLinkUriFromCurrentEditNote(Long rowId) {
        String title = titleEditText.getText().toString();
        String body = bodyEditText.getText().toString();
        dB_page.updateNote(rowId,
    				   title,
				oriPictureUri,
				oriAudioUri,
				oriDrawingUri,
    				   "",
    				   body,
				oriMarking,
				oriCreatedTime, true );
	}

	public int getCount()
	{
		int noteCount = dB_page.getNotesCount(true);
		return noteCount;
	}
	
	// for audio
	Long insertAudioToDB(String audioUri)
	{
		Long rowId = null;
       	if( !Util.isEmptyString(audioUri))
    	{
    		// insert
    		System.out.println("Note_common / _insertAudioToDB / insert");
    		// set marking to 1 for default
    		rowId = dB_page.insertNote("", "", audioUri, "", "", "", 1, (long) 0);// add new note, get return row Id
    	}
		return rowId;
	}
	
}