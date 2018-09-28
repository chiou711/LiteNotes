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

package com.cw.litenotes.operation.audio;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.litenotes.R;
import com.cw.litenotes.main.MainAct;
import com.cw.litenotes.tabs.AudioUi_page;
import com.cw.litenotes.tabs.TabsHost;
import com.cw.litenotes.util.Util;
import com.cw.litenotes.util.audio.UtilAudio;

import java.util.Locale;

public class AudioPlayer_page
{
	private static final String TAG = "AUDIO_PLAYER"; // error logging tag
	private static final int DURATION_1S = 1000; // 1 seconds per slide
    private static Audio_manager mAudioManager; // slide show being played
	private static int mPlaybackTime; // time in miniSeconds from which media should play
	private static int mAudio_tryTimes; // use to avoid useless looping in Continue mode
    private AppCompatActivity act;
    private Async_audioUrlVerify mAudioUrlVerifyTask;
	private AudioUi_page audioUi_page;
    public static Handler mAudioHandler;

	public AudioPlayer_page(AppCompatActivity act, AudioUi_page audioUi_page){
		this.act = act;
		this.audioUi_page = audioUi_page;

		System.out.println("AudioPlayer_page / constructor ");
		// start a new handler
        mAudioHandler = new Handler();
	}

    /**
     * prepare audio info
     */
    public static void prepareAudioInfo()
    {
        mAudioManager = new Audio_manager();
        mAudioManager.updateAudioInfo();
    }

	/**
     *  Run audio state
     */
    public void runAudioState()
	{
	   	System.out.println("AudioPlayer_page / _runAudioState ");
	   	// if media player is null, set new fragment
		if(BackgroundAudioService.mMediaPlayer == null)//for first
		{
		 	// show toast if Audio file is not found or No selection of audio file
			if( (Audio_manager.getAudioFilesCount() == 0) &&
				(Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)        )
			{
				Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			}
			else
			{
				mPlaybackTime = 0;
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);
				mAudio_tryTimes = 0;

				//for 1st play
				audioUrl_page = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);
				while (Util.isEmptyString(audioUrl_page) || !UtilAudio.hasAudioExtension(audioUrl_page)) {
                    Audio_manager.mAudioPos++;
                    audioUrl_page = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

                    if(Audio_manager.mAudioPos >= TabsHost.getCurrentPage().getNotesCountInPage(MainAct.mAct))
                        break;
				}

				if(!Util.isEmptyString(audioUrl_page) && UtilAudio.hasAudioExtension(audioUrl_page) ) {
                    startNewAudio();

                    MediaControllerCompat.getMediaController(MainAct.mAct)
                            .getTransportControls()
                            .playFromUri(Uri.parse(audioUrl_page), null);

                    MediaControllerCompat.getMediaController(MainAct.mAct).getTransportControls().play();
                }
                else
                {
                    Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
                }
			}
		}
		else
		{
			// from play to pause
			if(BackgroundAudioService.mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer_page / _runAudioState / play -> pause");
				BackgroundAudioService.mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(page_runnable);
                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PAUSE);

                //for pause
                MediaControllerCompat.getMediaController(MainAct.mAct).getTransportControls().pause();
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer_page / _runAudioState / pause -> play");
                mAudio_tryTimes = 0;

                if( (mAudioHandler != null) &&
			        (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE))
					mAudioHandler.post(page_runnable);

                Audio_manager.setPlayerState(Audio_manager.PLAYER_AT_PLAY);

                //for play
                MediaControllerCompat.getMediaController(MainAct.mAct).getTransportControls().play();
			}
		}
	}


	// set list view footer audio control
	private void showAudioPanel(AppCompatActivity act,boolean enable)
	{
//		System.out.println("AudioPlayer_page / _showAudioPanel / enable = " + enable);

		View audio_panel = act.findViewById(R.id.audio_panel);
        if(audio_panel != null) {
            TextView audio_panel_title_textView = (TextView) audio_panel.findViewById(R.id.audio_panel_title);
            SeekBar seekBarProgress = (SeekBar) audio_panel.findViewById(R.id.audioPanel_seek_bar);

            // show audio panel
            if (enable) {
                audio_panel.setVisibility(View.VISIBLE);
                audio_panel_title_textView.setVisibility(View.VISIBLE);

                // set footer message with audio name
                String audioStr = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);
                audio_panel_title_textView.setText(Util.getDisplayNameByUriString(audioStr, act));

                // show audio playing item number
                TextView audioPanel_audio_number = (TextView) audio_panel.findViewById(R.id.audioPanel_audio_number);
                String message = act.getResources().getString(R.string.menu_button_play) +
                        "#" +
                        (Audio_manager.mAudioPos +1);
                audioPanel_audio_number.setText(message);

                seekBarProgress.setVisibility(View.VISIBLE);
            } else {
                audio_panel.setVisibility(View.GONE);
            }
        }
	}

	private boolean isAudioPanelOn()
    {
        View audio_panel = act.findViewById(R.id.audio_panel);
        boolean isOn = false;
        if(audio_panel != null)
            isOn = (audio_panel.getVisibility() == View.VISIBLE);
        return isOn;
    }

    /**
     * Continue mode runnable
     */
	private String audioUrl_page;
	public Runnable page_runnable = new Runnable()
	{   @Override
		public void run()
		{
//			System.out.println("AudioPlayer_page / _page_runnable");
            if(!Audio_manager.isRunnableOn_page)
            {
                stopHandler();
                stopAsyncTask();

                if((audioUi_page != null) &&
                   (Audio_manager.getPlayerState() == Audio_manager.PLAYER_AT_STOP))
                    showAudioPanel(act,false);
                return;
            }

	   		if( Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 1 )
	   		{
                // for incoming call case and after Key protection
                if(!isAudioPanelOn())
                    showAudioPanel(act,true);

                if(!BackgroundAudioService.mIsPrepared)
	   			{
		    		// check if audio file exists or not
   					audioUrl_page = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

					if(!Async_audioUrlVerify.mIsOkUrl)
					{
						mAudio_tryTimes++;
						nextAudio_player();
					}
					else
   					{
                        // set listeners
                        if(BackgroundAudioService.mMediaPlayer != null)
                            setMediaPlayerListeners();

	   					AudioUi_page.mProgress = 0;

						// for network stream buffer change
                        if(BackgroundAudioService.mMediaPlayer != null) {
                            BackgroundAudioService.mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
                                @Override
                                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                                    if (TabsHost.getCurrentPage().seekBarProgress != null)
                                        TabsHost.getCurrentPage().seekBarProgress.setSecondaryProgress(percent);
                                }
                            });
                        }

                        showAudioPanel(act,true);
   					}
	   			}
	   			else
	   			{
//	   				// keep looping, do not set post() here, it will affect slide show timing
	   				if(mAudio_tryTimes < Audio_manager.getAudioFilesCount())
	   				{
						// update page audio seek bar
						if(audioUi_page != null)
	   						update_audioPanel_progress(audioUi_page);

						if(mAudio_tryTimes == 0)
							mAudioHandler.postDelayed(page_runnable,DURATION_1S);
						else
							mAudioHandler.postDelayed(page_runnable,DURATION_1S/10);
	   				}
	   			}
	   		}
	   		else if( (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0 ) )// for non-audio item
	   		{
//	   			System.out.println("AudioPlayer_page / page_runnable / for non-audio item");
				nextAudio_player();

				TabsHost.audioPlayer_page.scrollHighlightAudioItemToVisible(TabsHost.getCurrentPage().recyclerView);
				TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();

			}
		}
	};	

	private void stopHandler()
    {
        if(mAudioHandler != null) {
            mAudioHandler.removeCallbacks(page_runnable);
            mAudioHandler = null;
        }
    }

    private void stopAsyncTask()
    {
        // stop async task
        // make sure progress dialog will disappear
        if( (mAudioUrlVerifyTask!= null) &&
            (!mAudioUrlVerifyTask.isCancelled()) )
        {
            mAudioUrlVerifyTask.cancel(true);

            if( (mAudioUrlVerifyTask.mUrlVerifyDialog != null) &&
                    mAudioUrlVerifyTask.mUrlVerifyDialog.isShowing()	)
            {
                mAudioUrlVerifyTask.mUrlVerifyDialog.dismiss();
            }

            if( (mAudioUrlVerifyTask.mAsyncTaskAudioPrepare != null) &&
                    (mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog != null) &&
                    mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.isShowing()	)
            {
                mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.dismiss();
            }
        }

    }


    public static int media_file_length;
    /**
     * Set audio player listeners
     */
	private void setMediaPlayerListeners()
	{
		System.out.println("AudioPlayer_page / _setAudioPlayerListeners");

			// On Completion listener
			BackgroundAudioService.mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
			{	@Override
				public void onCompletion(MediaPlayer mp) 
				{
					System.out.println("AudioPlayer_page / _setAudioPlayerListeners / _onCompletion");
					
					if(BackgroundAudioService.mMediaPlayer != null)
						BackgroundAudioService.mMediaPlayer.release();
	
					BackgroundAudioService.mMediaPlayer = null;
					mPlaybackTime = 0;

					// get next index
					if(Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
					{
                        nextAudio_player();
						TabsHost.audioPlayer_page.scrollHighlightAudioItemToVisible(TabsHost.getCurrentPage().recyclerView);
						TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
					}
				}
			});
			
			// - on prepared listener
			BackgroundAudioService.mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
			{	@Override
				public void onPrepared(MediaPlayer mp)
				{
					System.out.println("AudioPlayer_page / _setAudioPlayerListeners / _onPrepared");

					BackgroundAudioService.mIsPrepared = true;

					if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
					{
                        showAudioPanel(act,true);

						// media file length
						media_file_length = BackgroundAudioService.mMediaPlayer.getDuration(); // gets the song length in milliseconds from URL
                        System.out.println("AudioPlayer_page / _setAudioPlayerListeners / media_file_length = " + media_file_length);

						// set footer message: media name
						if (!Util.isEmptyString(audioUrl_page))
						{
                            // set seek bar progress
                            if(audioUi_page != null)
                                update_audioPanel_progress(audioUi_page);

							TextView audioPanel_file_length = (TextView) act.findViewById(R.id.audioPanel_file_length);
							// show audio file length of playing
							int fileHour = Math.round((float)(media_file_length / 1000 / 60 / 60));
							int fileMin = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
							int fileSec = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
                            if(audioPanel_file_length != null) {
                                audioPanel_file_length.setText(String.format(Locale.US, "%2d", fileHour) + ":" +
                                        String.format(Locale.US, "%02d", fileMin) + ":" +
                                        String.format(Locale.US, "%02d", fileSec));
                            }

                            scrollHighlightAudioItemToVisible(TabsHost.getCurrentPage().recyclerView);
							TabsHost.getCurrentPage().itemAdapter.notifyDataSetChanged();
                        }


						BackgroundAudioService.mMediaPlayer.seekTo(mPlaybackTime);

						// add for calling runnable
						if (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)
							mAudioHandler.postDelayed(page_runnable, Util.oneSecond / 4);
					}
				}
			});
			
			// - on error listener
			BackgroundAudioService.mMediaPlayer.setOnErrorListener(new OnErrorListener()
			{	@Override
				public boolean onError(MediaPlayer mp,int what,int extra) 
				{
					// more than one error when playing an index 
					System.out.println("AudioPlayer_page / _setAudioPlayerListeners / _onError / what = " + what + " , extra = " + extra);
					return false;
				}
			});
	}

    private static final int UNBOUNDED = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
	/**
	* Scroll highlight audio item to visible position
	*
	* At the following conditions
	* 	1) click audio item of list view (this highlight is not good for user expectation, so cancel this condition now)
	* 	2) click previous/next item in audio controller
	* 	3) change tab to playing tab
	* 	4) back from key protect off
	* 	5) if seeker bar reaches the end
	* In order to view audio highlight item, playing(highlighted) audio item can be auto scrolled to top,
	* unless it is at the end page of list view, there is no need to scroll.
	*/
	public void scrollHighlightAudioItemToVisible(RecyclerView recyclerView)
	{
//		System.out.println("AudioPlayer_page / _scrollHighlightAudioItemToVisible");

        LinearLayoutManager layoutMgr = ((LinearLayoutManager) recyclerView.getLayoutManager());
		// version limitation: _scrollListBy
		// NoteFragment.drag_listView.scrollListBy(firstVisibleIndex_top);
		if(Build.VERSION.SDK_INT < 19)
			return;

		int pos;
		int itemHeight = 50;//init
		int dividerHeight;
		int firstVisible_note_pos;
		View v;

        pos = layoutMgr.findFirstVisibleItemPosition();
//			System.out.println("---------------- pos = " + pos);

		View childView;
		if(recyclerView.getAdapter() != null) {
            childView = layoutMgr.findViewByPosition(pos);

            // avoid exception: audio playing and doing checked notes operation at non-playing page
            if(childView == null)
            	return;

			childView.measure(UNBOUNDED, UNBOUNDED);
			itemHeight = childView.getMeasuredHeight();
//                System.out.println("---------------- itemHeight = " + itemHeight);
		}

//		dividerHeight = recyclerView.getDividerHeight();//todo temp
        dividerHeight = 0;
//			System.out.println("---------------- dividerHeight = " + dividerHeight);

        firstVisible_note_pos = layoutMgr.findFirstVisibleItemPosition();

//		System.out.println("---------------- firstVisible_note_pos = " + firstVisible_note_pos);

		v = recyclerView.getChildAt(0);

		int firstVisibleNote_top = (v == null) ? 0 : v.getTop();
//			System.out.println("---------------- firstVisibleNote_top = " + firstVisibleNote_top);

//		System.out.println("---------------- Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);

		if(firstVisibleNote_top < 0)
		{
            // restore index and top position
            recyclerView.scrollBy(0,firstVisibleNote_top);
//				System.out.println("----- scroll backwards by firstVisibleNote_top " + firstVisibleNote_top);
		}

		boolean noScroll = false;
		// base on Audio_manager.mAudioPos to scroll
		if(firstVisible_note_pos != Audio_manager.mAudioPos)
		{
			while ((firstVisible_note_pos != Audio_manager.mAudioPos) && (!noScroll))
			{
				int offset = itemHeight + dividerHeight;
				// scroll forwards
				if (firstVisible_note_pos > Audio_manager.mAudioPos)
				{
                    recyclerView.scrollBy(0,-offset);
//						System.out.println("-----scroll forwards (to top)" + (-offset));
				}
				// scroll backwards
				else if (firstVisible_note_pos < Audio_manager.mAudioPos)
				{
					// when real item height could be larger than visible item height, so
					// scroll twice here in odder to do scroll successfully, otherwise scroll could fail
                    recyclerView.scrollBy(0,offset/2);
                    recyclerView.scrollBy(0,offset/2);
//					System.out.println("-----scroll backwards (to bottom) " + offset);
				}

//					System.out.println("---------------- firstVisible_note_pos = " + firstVisible_note_pos);
//					System.out.println("---------------- Page.drag_listView.getFirstVisiblePosition() = " + listView.getFirstVisiblePosition());
				if(firstVisible_note_pos == layoutMgr.findFirstVisibleItemPosition())
					noScroll = true;
				else {
					// update first visible index
					firstVisible_note_pos = layoutMgr.findFirstVisibleItemPosition();
				}
			}

			// do v scroll
			TabsHost.store_listView_vScroll(recyclerView);
			TabsHost.resume_listView_vScroll(recyclerView);
		}
	}

    /**
     * Start new audio
     */
    private void startNewAudio()
    {
        System.out.println("AudioPlayer_page / _startNewAudio / Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);

        // remove call backs to make sure next toast will appear soon
        if(mAudioHandler != null)
            mAudioHandler.removeCallbacks(page_runnable);
        mAudioHandler = null;
        mAudioHandler = new Handler();

        Audio_manager.isRunnableOn_page = true;
        Audio_manager.isRunnableOn_note = false;
        BackgroundAudioService.mMediaPlayer = null;

        // verify audio URL
        Async_audioUrlVerify.mIsOkUrl = false;

        if( (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE) &&
            (Audio_manager.getCheckedAudio(Audio_manager.mAudioPos) == 0)          )
        {
            mAudioHandler.postDelayed(page_runnable,Util.oneSecond/4);
        }
        else
        {
            mAudioUrlVerifyTask = new Async_audioUrlVerify(act, mAudioManager.getAudioStringAt(Audio_manager.mAudioPos));
            mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");

            while(!Async_audioUrlVerify.mIsOkUrl)
            {
                //wait for Url verification
                try {
                    Thread.sleep(Util.oneSecond/20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // prepare audio
            if(Async_audioUrlVerify.mIsOkUrl)
            {
                // launch handler
                if( (Audio_manager.getPlayerState() != Audio_manager.PLAYER_AT_STOP) &&
                        (Audio_manager.getAudioPlayMode() == Audio_manager.PAGE_PLAY_MODE)   )
                {
                    mAudioHandler.postDelayed(page_runnable, Util.oneSecond / 4);
                }

                // during audio Preparing
                Async_audioPrepare mAsyncTaskAudioPrepare = new Async_audioPrepare(act);
                mAsyncTaskAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Preparing to play ...");
            }
        }

    }

    /**
     * Play next audio at AudioPlayer_page
     */
    private void nextAudio_player()
    {
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
        System.out.println("AudioPlayer_page / _playNextAudio");
        if(BackgroundAudioService.mMediaPlayer != null)
        {
            BackgroundAudioService.mMediaPlayer.release();
            BackgroundAudioService.mMediaPlayer = null;
        }
        mPlaybackTime = 0;

        // new audio index
        Audio_manager.mAudioPos++;

		if(Audio_manager.mAudioPos >= Audio_manager.getPlayingPage_notesCount())
            Audio_manager.mAudioPos = 0; //back to first index

        // check try times,had tried or not tried yet, anyway the audio file is found
        System.out.println("AudioPlayer_page / check mTryTimes = " + mAudio_tryTimes);
        if(mAudio_tryTimes < Audio_manager.getAudioFilesCount() )
        {
			audioUrl_page = Audio_manager.getAudioStringAt(Audio_manager.mAudioPos);

			startNewAudio();

			if(!Util.isEmptyString(audioUrl_page))
			{
				MediaControllerCompat.getMediaController(MainAct.mAct)
						.getTransportControls()
						.playFromUri(Uri.parse(audioUrl_page), null);
				MediaControllerCompat.getMediaController(MainAct.mAct).getTransportControls().play();
			}

        }
        else // try enough times: still no audio file is found
        {
            Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();

            // do not show highlight
            if(MainAct.mSubMenuItemAudio != null)
                MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);

            // stop media player
            Audio_manager.stopAudioPlayer();
        }
        System.out.println("AudioPlayer_page / _playNextAudio / Audio_manager.mAudioPos = " + Audio_manager.mAudioPos);
    }

    private void update_audioPanel_progress(AudioUi_page audioUi_page)
    {
//        if(!listView.isShown())
//            return;

//		System.out.println("AudioPlayer_page / _update_audioPanel_progress");

        // get current playing position
        int currentPos = 0;
        if(BackgroundAudioService.mMediaPlayer != null)
            currentPos = BackgroundAudioService.mMediaPlayer.getCurrentPosition();

        int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
        int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

        // set current playing time
        audioUi_page.audioPanel_curr_pos.setText(String.format(Locale.US,"%2d", curHour)+":" +
                String.format(Locale.US,"%02d", curMin)+":" +
                String.format(Locale.US,"%02d", curSec) );//??? why affect audio title?

        // set current progress
        AudioUi_page.mProgress = (int)(((float)currentPos/ media_file_length)*100);
        audioUi_page.seekBarProgress.setProgress(AudioUi_page.mProgress); // This math construction give a percentage of "was playing"/"song length"
    }
}