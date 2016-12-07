package com.cw.litenote.note;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.video.UtilVideo;
import com.cw.litenote.util.video.VideoPlayer;
import com.cw.litenote.util.CustomWebView;
import com.cw.litenote.util.Util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Note_UI
{
    public static boolean showSeekBarProgress;
    public static int videoFileLength_inMilliSeconds;
    static int videoView_progress;
    private static int mPosition;
    private ViewPager pager;
    private FragmentActivity act;

    public Note_UI(FragmentActivity activity, ViewPager viewPager, int position)
    {

        System.out.println("Note_UI / constructor");
        pager = viewPager;
        act = activity;
        mPosition = position;

        DB_page db_page = new DB_page(act,Util.getPref_lastTimeView_page_tableId(act));
        String pictureUri = db_page.getNotePictureUri(position,true);
        String linkUri = db_page.getNoteLinkUri(position,true);

        String tagStr = "current"+ position +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);

        if((pictureGroup != null))
        {
            setPictureView_listeners(act, pager, pictureUri, linkUri, pictureGroup);

            TextView picView_title;
            TextView picView_footer;
            Button picView_back_button;
            Button picView_viewMode_button;
            TextView videoView_currPosition;
            SeekBar videoView_seekBar;
            TextView videoView_fileLength;

            picView_title = (TextView) (pictureGroup.findViewById(R.id.image_title));
            picView_footer = (TextView) (pictureGroup.findViewById(R.id.image_footer));

            picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
            picView_viewMode_button = (Button) (pictureGroup.findViewById(R.id.image_view_mode));

            videoView_currPosition = (TextView) (pictureGroup.findViewById(R.id.video_current_pos));
            videoView_seekBar = (SeekBar)(pictureGroup.findViewById(R.id.video_seek_bar));
            videoView_fileLength = (TextView) (pictureGroup.findViewById(R.id.video_file_length));

            if(Note.isPictureMode())
                picView_back_button.setVisibility(View.VISIBLE);
            else
                picView_back_button.setVisibility(View.GONE);

            String pictureName;
            if(!Util.isEmptyString(pictureUri))
                pictureName = Util.getDisplayNameByUriString(pictureUri, act);
            else if(Util.isYouTubeLink(linkUri))
                pictureName = linkUri;
            else
                pictureName = "";

            if(!Util.isEmptyString(pictureName))
            {
                picView_title.setVisibility(View.VISIBLE);
                picView_title.setText(pictureName);
            }
            else
                picView_title.setVisibility(View.INVISIBLE);

            if(Note.isPictureMode()) {
                picView_footer.setVisibility(View.VISIBLE);
                picView_footer.setText((pager.getCurrentItem()+1) +
                        "/" + pager.getAdapter().getCount());
            }
            else
                picView_footer.setVisibility(View.GONE);

            if(UtilVideo.hasVideoExtension(pictureUri, act))
            {
                if(!UtilVideo.hasMediaControlWidget)
                    showVideoPlayButtonState(position);
                else
                    showPicViewUI_previous_next(false,0);
            }

            // set image view buttons (View Mode, Previous, Next) visibility
            if(Note.isPictureMode() )
            {
                picView_viewMode_button.setVisibility(View.VISIBLE);

                // show previous/next buttons for image, not for video
                if(!UtilVideo.hasMediaControlWidget )
                    showPicViewUI_previous_next(true,position);
                else if(UtilVideo.mVideoView == null) // for image
                    showPicViewUI_previous_next(true,position);
            }
            else
            {
                showPicViewUI_previous_next(false,0);
                picView_viewMode_button.setVisibility(View.GONE);
            }

            // show seek bar for video only
            if(!UtilVideo.hasMediaControlWidget)
            {
                if(UtilVideo.hasVideoExtension(pictureUri,act))
                {
                    MediaPlayer mp = MediaPlayer.create(act,
                            Uri.parse(pictureUri));

                    if(mp!= null) {
                        videoFileLength_inMilliSeconds = mp.getDuration();
                        mp.release();
                    }

                    primaryVideoSeekBarProgressUpdater(pager,UtilVideo.mPlayVideoPosition,pictureUri);
                }
                else
                {
                    videoView_currPosition.setVisibility(View.GONE);
                    videoView_seekBar.setVisibility(View.GONE);
                    videoView_fileLength.setVisibility(View.GONE);
                }
            }

            showSeekBarProgress = true;
        }
    } //Note_view_UI constructor

    static boolean isWithinDelay;

    private static String pictureString;

	private void setPictureView_listeners(final FragmentActivity act,final ViewPager pager,
                                         final String strPicture, final String linkUri, ViewGroup viewGroup)
	{
        Button picView_back_button;
        Button picView_viewMode_button;
        Button picView_previous_button;
        Button picView_next_button;

        Button mVideoPlayButton;
        final TextView videoView_currPosition;
        SeekBar videoView_seekBar;

        mVideoPlayButton = (Button) (viewGroup.findViewById(R.id.video_view_play_video));
        // Set video play button listener
        if(UtilVideo.hasVideoExtension(strPicture, act))
        {
            mVideoPlayButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    System.out.println("Note_UI / setPictureView_listeners / getVideoState() = " + UtilVideo.getVideoState());

                    if( (AudioPlayer.mMediaPlayer != null) &&
						AudioPlayer.mMediaPlayer.isPlaying() &&
						(UtilVideo.getVideoState() != UtilVideo.VIDEO_AT_PLAY) )
                    {
                        // Dialog: confirm to disable audio or not
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        builder.setTitle(R.string.title_playing_audio)
                               .setMessage(R.string.message_continue_or_stop)
                               .setPositiveButton(R.string.btn_Stop,
                                       new DialogInterface.OnClickListener(){
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                AudioPlayer.stopAudio();
                                                UtilVideo.changeVideoState();
                                                UtilVideo.playOrPauseVideo(pager,strPicture);
                                            }})
                               .setNegativeButton(R.string.btn_Continue,
                                       new DialogInterface.OnClickListener(){
                                           @Override
                                           public void onClick(DialogInterface dialog, int which) {
                                               UtilVideo.changeVideoState();
                                               UtilVideo.playOrPauseVideo(pager,strPicture);
                                           }})
                                .show();
                    }
                    else
                    {
                        UtilVideo.changeVideoState();
                        UtilVideo.playOrPauseVideo(pager,strPicture);
                    }
                }
            });
        }
        else if(Util.isEmptyString(strPicture) &&
                Util.isYouTubeLink(linkUri)       )
        {
            mVideoPlayButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play, 0, 0, 0);
            mVideoPlayButton.setVisibility(View.VISIBLE);
            // set listener for running YouTube
            mVideoPlayButton.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View view)
                {
                    System.out.println("Note_UI / _setPictureView_listeners / onClick to play YouTube / linkUri = " + linkUri);
                    openLink_YouTube(linkUri);
                }
            });
        }

		picView_back_button = (Button) (viewGroup.findViewById(R.id.image_view_back));
        picView_viewMode_button = (Button) (viewGroup.findViewById(R.id.image_view_mode));
        picView_previous_button = (Button) (viewGroup.findViewById(R.id.image_view_previous));
        picView_next_button = (Button) (viewGroup.findViewById(R.id.image_view_next));
        
        videoView_currPosition = (TextView) (viewGroup.findViewById(R.id.video_current_pos));
        videoView_seekBar = (SeekBar)(viewGroup.findViewById(R.id.video_seek_bar));

        // view mode 
    	// picture only
	  	if(Note.isPictureMode())
	  	{
			// image: view back
	  		picView_back_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back /*android.R.drawable.ic_menu_revert*/, 0, 0, 0);
			// click to finish Note_view_pager
	  		picView_back_button.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) 
	            {
    		        // remove current link web view
					int position = Note.mCurrentPosition;
					String tagPictureStr = "current"+ position +"pictureView";
					ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagPictureStr);
					if(pictureGroup != null)
					{
						CustomWebView linkWebView = ((CustomWebView) pictureGroup.findViewById(R.id.link_web_view));
						CustomWebView.pauseWebView(linkWebView);
						CustomWebView.blankWebView(linkWebView);
					}
	    	    	
	    	    	// set not full screen
	    	    	Util.setNotFullScreen(act);
	    	    	
        			// back to view all mode
	        		Note.setViewAllMode();
					Note.setOutline(act);
	            }
	        });   
			
			// image: view mode
	  		picView_viewMode_button.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_view, 0, 0, 0);
			// click to select view mode 
	  		picView_viewMode_button.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) 
	            {
                    TextView audio_title_text_view = (TextView) act.findViewById(R.id.pager_audio_title);
                    audio_title_text_view.setSelected(false);
	            	
                    isWithinDelay = false;

	            	//Creating the instance of PopupMenu  
	                PopupMenu popup = new PopupMenu(act, view);
	                
	                //Inflating the Popup using xml file  
	                popup.getMenuInflater().inflate(R.menu.pop_up_menu, popup.getMenu()); 
	                
	                //registering popup with OnMenuItemClickListener  
	                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() 
	                {  
		                public boolean onMenuItemClick(MenuItem item) 
		                {  
		                	
		                	switch (item.getItemId()) {
		                    case R.id.view_all:
		                 		 Note.setViewAllMode();
								 Note.setOutline(act);
		                         break;
		                    case R.id.view_picture:
		                 		 Note.setPictureMode();
								 Note.setOutline(act);
		                          break;
		                    case R.id.view_text:
		                 		 Note.setTextMode();
								 Note.setOutline(act);
		                    	 break;
		                     }		                	
		                	 return true;  
		                }  
	                });  
	                
	                popup.setOnDismissListener(new PopupMenu.OnDismissListener() 
	                {
						@Override
						public void onDismiss(PopupMenu menu) 
						{
                            TextView audio_title_text_view = (TextView) act.findViewById(R.id.pager_audio_title);
							if(AudioPlayer.mMediaPlayer != null) 
							{
								if(AudioPlayer.mMediaPlayer.isPlaying()) {
									Note.showAudioName(act);
									audio_title_text_view.setSelected(true);
								}
							}
							else
								audio_title_text_view.setSelected(false);

		    				delay_pagerUI_all_off(0,strPicture, Note.mCurrentPosition);
						}
					} );
	                
	                popup.show();//showing pop up menu, will show status bar
	            }
	        });       			
			
			// image: previous button
	  		picView_previous_button.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_previous, 0, 0, 0);
			// click to previous 
	  		picView_previous_button.setOnClickListener(new View.OnClickListener() 
	        {
	            public void onClick(View view) {
	            	// since onPageChanged is not called fast enough, add stop functions below
	            	Note.mCurrentPosition--;
	            	pager.setCurrentItem(pager.getCurrentItem() - 1);
	            }
	        });   
        
			// image: next button
	  		picView_next_button.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_next, 0, 0, 0);
			// click to next 
	  		picView_next_button.setOnClickListener(new View.OnClickListener()
	        {
	            public void onClick(View view) {
	            	// since onPageChanged is not called fast enough, add stop functions below
	            	Note.mCurrentPosition++;
	            	pager.setCurrentItem(pager.getCurrentItem() + 1);
	            }
	        }); 
	  	}
	  	
	  	// video view: apply media control customization or not
	  	if(Note.isPictureMode()|| Note.isViewAllMode())
	  	{
			if(!UtilVideo.hasMediaControlWidget)
			{
				// set video seek bar listener
				videoView_seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
				{
					// onStartTrackingTouch
					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
                    {
						System.out.println("Note_UI / _onStartTrackingTouch");
						if( (UtilVideo.mVideoPlayer == null)  && (UtilVideo.mVideoView != null))
						{
							if(Build.VERSION.SDK_INT >= 16)
								UtilVideo.mVideoView.setBackground(null);
							else
								UtilVideo.mVideoView.setBackgroundDrawable(null);
							
							UtilVideo.mVideoView.setVisibility(View.VISIBLE);
							UtilVideo.mVideoPlayer = new VideoPlayer(act,pager,strPicture);
							UtilVideo.mVideoView.seekTo(UtilVideo.mPlayVideoPosition);
						}
					}
					
					// onProgressChanged
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
					{
						System.out.println("Note_UI / _onProgressChanged");
						if(fromUser)
						{	
							// show progress change
					    	int currentPos = videoFileLength_inMilliSeconds*progress/(seekBar.getMax()+1);
					    	// update current play time
					     	videoView_currPosition.setText(Util.getTimeFormatString(currentPos));
					     	
					     	//add below to keep showing seek bar
					     	if(Note.isPictureMode())
					     		showPicViewUI_previous_next(true,mPosition);
					        showSeekBarProgress = true;
					    	delay_pagerUI_all_off(3005,strPicture, Note.mCurrentPosition); // for 3 seconds, _onProgressChanged
						}
					}
					
					// onStopTrackingTouch
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) 
					{
						System.out.println("Note_UI / _onStopTrackingTouch");
						if( UtilVideo.mVideoView != null  )
						{
							int mPlayVideoPosition = (int) (((float)(videoFileLength_inMilliSeconds / 100)) * seekBar.getProgress());
							if(UtilVideo.mVideoPlayer != null)
								UtilVideo.mVideoView.seekTo(mPlayVideoPosition);
						}
					}	
				});
			}
	  	}
	}

	public void delay_pagerUI_all_off(long delayTimeMilliSec,String pictureStr,int position)
	{
		System.out.println("Note_UI / _delay_pagerUI_all_off / delayTimeMilliSec = " + delayTimeMilliSec);
        pictureString = pictureStr;
        long timeMilliSec = System.currentTimeMillis() + delayTimeMilliSec;
        if(timeMilliSec == 0)
        {
            // no delay
            setUI_all_off(pictureStr,position);
        }
        else {
            // with delay
            isWithinDelay = true; // only set true here
            handler.postDelayed(runnable,delayTimeMilliSec);
        }
	}


    Handler handler = new Handler();
    Runnable runnable = new Runnable()
    {
        public void run()
        {
            if(!isWithinDelay)
                return;

//            System.out.println("Note_UI / _runnable ");
            if(pager != null)
            {
                int position =  pager.getCurrentItem();
                String tagImageStr = "current"+ position +"pictureView";
                System.out.println("Note_UI / _runnable / position = " + position);
                ViewGroup imageGroup = (ViewGroup) pager.findViewWithTag(tagImageStr);

                if(imageGroup != null)
                {
                    // to distinguish image and video, does not show video play icon
                    // only when video is playing
                    if(!Util.isEmptyString(pictureString))
                        setUI_all_off(pictureString,position);
                }

                showSeekBarProgress = false;
            }
        }
    };

	private void setUI_top_off(String pictureStr,int position)
	{
        String tagStr = "current"+ position +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);
//        System.out.println("Note_UI / _showPictureViewUI / tagStr = " + tagStr);

        TextView picView_title;
        TextView picView_footer;
        Button picView_back_button;
        Button picView_viewMode_button;
        Button picView_previous_button = null;
        Button picView_next_button = null;

        if((pictureGroup != null))
        {
            picView_title = (TextView) (pictureGroup.findViewById(R.id.image_title));
            picView_footer = (TextView) (pictureGroup.findViewById(R.id.image_footer));

            picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
            picView_viewMode_button = (Button) (pictureGroup.findViewById(R.id.image_view_mode));
            picView_previous_button = (Button) (pictureGroup.findViewById(R.id.image_view_previous));
            picView_next_button = (Button) (pictureGroup.findViewById(R.id.image_view_next));
            picView_title.setVisibility(View.GONE);
            picView_footer.setVisibility(View.GONE);

            picView_back_button.setVisibility(View.GONE);
            picView_viewMode_button.setVisibility(View.GONE);
        }

		if((!UtilVideo.hasMediaControlWidget) && UtilVideo.hasVideoExtension(pictureStr, act))
			updateVideoPlayButtonState(pager,position);

		if(Note.isPictureMode() && UtilImage.hasImageExtension(pictureStr, act))
		{
            if(picView_previous_button != null) {
                picView_previous_button.setVisibility(View.GONE);
                picView_next_button.setVisibility(View.GONE);
            }
		}		
	}
	
	private void setUI_all_off(String pictureStr,int position)
	{

        System.out.println("Note_UI / _setUI_all_off / position = " + position );

        setUI_top_off(pictureStr,position);

        String tagStr = "current"+ position +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);

        Button picView_previous_button;
        Button picView_next_button;

        TextView videoView_currPosition;
        SeekBar videoView_seekBar;
        TextView videoView_fileLength;

        if((pictureGroup != null))
        {
            picView_previous_button = (Button) (pictureGroup.findViewById(R.id.image_view_previous));
            picView_next_button = (Button) (pictureGroup.findViewById(R.id.image_view_next));

            videoView_currPosition = (TextView) (pictureGroup.findViewById(R.id.video_current_pos));
            videoView_seekBar = (SeekBar) (pictureGroup.findViewById(R.id.video_seek_bar));
            videoView_fileLength = (TextView) (pictureGroup.findViewById(R.id.video_file_length));

            if((!UtilVideo.hasMediaControlWidget) && UtilVideo.hasVideoExtension(pictureStr, act)) {
                picView_previous_button.setVisibility(View.GONE); //??? how to avoid this flash?
                picView_next_button.setVisibility(View.GONE);
            }

            videoView_currPosition.setVisibility(View.GONE);
            videoView_seekBar.setVisibility(View.GONE);
            videoView_fileLength.setVisibility(View.GONE);
        }

        isWithinDelay = false;
	}
	


    private void showPicViewUI_previous_next(boolean show,int position) {
        String tagStr = "current" + position + "pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);
//        System.out.println("Note_UI / _showPictureViewUI / tagStr = " + tagStr);

        Button picView_previous_button;
        Button picView_next_button;

        if (pictureGroup != null) {
            picView_previous_button = (Button) (pictureGroup.findViewById(R.id.image_view_previous));
            picView_next_button = (Button) (pictureGroup.findViewById(R.id.image_view_next));

            if (show) {
                picView_previous_button.setVisibility(View.VISIBLE);
                picView_previous_button.setEnabled(position == 0);
                picView_previous_button.setAlpha(position == 0 ? 0.1f : 1f);

                picView_next_button.setVisibility(View.VISIBLE);
                picView_next_button.setAlpha(position == (Note.mPagerAdapter.getCount() - 1) ? 0.1f : 1f);
                picView_next_button.setEnabled(position == (Note.mPagerAdapter.getCount() - 1));
            } else {
                picView_previous_button.setVisibility(View.GONE);
                picView_next_button.setVisibility(View.GONE);
            }
        }
    }


    /**
     *  Video UI
     */

    // Set video UI
	public void setVideoViewUI(String pictureUri,int position)
	{
		System.out.println("Note_UI / _setVideoViewUI");
		if(UtilVideo.hasVideoExtension(pictureUri, act))
			showVideoPlayButtonState(position);
	}

	// show video play button state
	private void showVideoPlayButtonState(int position)
	{
        String tagStr = "current"+ position +"pictureView";
        ViewGroup pictureGroup = (ViewGroup) pager.findViewWithTag(tagStr);
        System.out.println("Note_UI / _showPictureViewUI / tagStr = " + tagStr);

        Button mVideoPlayButton = null;

        if(pictureGroup != null)
            mVideoPlayButton = (Button) (pictureGroup.findViewById(R.id.video_view_play_video));

		Button btn = mVideoPlayButton;
		// show video play button icon
		if(btn != null)
		{
            int state = UtilVideo.getVideoState();
			System.out.println("Note_UI / _showVideoPlayButtonState / getVideoState() = " + state);

			if(state == UtilVideo.VIDEO_AT_PLAY)
				btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause, 0, 0, 0);
			else if((state == UtilVideo.VIDEO_AT_PAUSE) || (state == UtilVideo.VIDEO_AT_STOP))
				btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play, 0, 0, 0);

			btn.setVisibility(View.VISIBLE);
		}
	}

    private static ViewGroup getPictureGroup(int position,ViewPager pager)
    {
        String tagStr = "current"+ position +"pictureView";
        ViewGroup viewGroup = (ViewGroup) pager.findViewWithTag(tagStr);//??? how to remove static of mPager
        System.out.println("Note_UI / _getPictureGroup / tagStr = " + tagStr);
        return  viewGroup;
    }

    // update video play button state
    public static void updateVideoPlayButtonState(ViewPager pager,int position)
    {
        ViewGroup pictureGroup = getPictureGroup(position,pager);
        Button mVideoPlayButton = null;

        if(pictureGroup != null)
            mVideoPlayButton = (Button) (pictureGroup.findViewById(R.id.video_view_play_video));

    	Button btn = mVideoPlayButton;

    	if(btn == null)
    		return;

        // show video play button icon
        int state = UtilVideo.getVideoState();
        if(state == UtilVideo.VIDEO_AT_PLAY)
        	btn.setVisibility(View.GONE);
        else if((state == UtilVideo.VIDEO_AT_PAUSE) || (state == UtilVideo.VIDEO_AT_STOP))
        {
        	btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play, 0, 0, 0);
        	btn.setVisibility(View.VISIBLE);
        }
    }

	public static void primaryVideoSeekBarProgressUpdater(ViewPager pager,int currentPos,String pictureUri)
   	{
	   	if( (UtilVideo.mVideoView == null))
	   		return;

        ViewGroup pictureGroup = getPictureGroup(currentPos,pager);

        TextView videoView_currPosition = null;
        SeekBar videoView_seekBar = null;
        TextView videoView_fileLength = null;

        if(pictureGroup != null)
        {
            videoView_currPosition = (TextView) (pictureGroup.findViewById(R.id.video_current_pos));
            videoView_seekBar = (SeekBar) (pictureGroup.findViewById(R.id.video_seek_bar));
            videoView_fileLength = (TextView) (pictureGroup.findViewById(R.id.video_file_length));
        }

	    // show current play position
	   	if(videoView_currPosition != null)
	   	{
	   		videoView_currPosition.setText(Util.getTimeFormatString(currentPos));
	   		videoView_currPosition.setVisibility(View.VISIBLE);
	   	}

		// show file length
		if(!Util.isEmptyString(pictureUri))
		{
			// set file length
			if(videoView_fileLength != null)
			{
				videoView_fileLength.setText(Util.getTimeFormatString(videoFileLength_inMilliSeconds));
				videoView_fileLength.setVisibility(View.VISIBLE);
			}
		}

	   	// show seek bar progress
	   	if(videoView_seekBar != null)
	   	{
	   		videoView_seekBar.setVisibility(View.VISIBLE);
	   		videoView_seekBar.setMax(99);
	   		videoView_progress = (int)(((float)currentPos/videoFileLength_inMilliSeconds)*100);
	   		videoView_seekBar.setProgress(videoView_progress);
	   	}
    }


    //
    // Open link of YouTube
    //
    // Due to "AdWords or copyright" server limitation, for some URI,
    // "video is not available" message could show up.
    // At this case, one solution is to switch current mobile website to desktop website by browser setting.
    // So, base on URI key words to decide "YouTube App" or "browser" launch.
    private void openLink_YouTube(String linkUri)
    {
        // by YouTube App
        if(linkUri.contains("youtu.be"))
        {
            // stop audio and video if playing
            Note.stopAV();

            String id = Util.getYoutubeId(linkUri);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + id));
            act.startActivity(intent);
        }
        // by Chrome browser
        else if(linkUri.contains("youtube.com"))
        {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUri));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setPackage("com.android.chrome");

            try
            {
                act.startActivity(i);
            }
            catch (ActivityNotFoundException e)
            {
                // Chrome is probably not installed
                // Try with the default browser
                i.setPackage(null);
                act.startActivity(i);
            }
        }
    }
}
