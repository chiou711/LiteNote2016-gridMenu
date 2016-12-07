package com.cw.litenote.util.audio;

import java.io.File;
import java.util.Locale;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.main.Page;
import com.cw.litenote.R;
import com.cw.litenote.main.TabsHost;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;

public class UtilAudio {
	
	// Stop audio media player and audio handler
    public static void stopAudioPlayer()
    {
    	System.out.println("UtilAudio / _stopAudioPlayer");
        if(AudioPlayer.mMediaPlayer != null)
    	{
			if(AudioPlayer.mMediaPlayer.isPlaying())
				AudioPlayer.mMediaPlayer.pause();
    		AudioPlayer.mMediaPlayer.release();
    		AudioPlayer.mMediaPlayer = null;
    		AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunOneTimeMode); 
    		AudioPlayer.mAudioHandler.removeCallbacks(AudioPlayer.mRunContinueMode); 
    		AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
    	}
    }
    
    public static void stopAudioIfNeeded()
    {
		if( (AudioPlayer.mMediaPlayer != null)    &&
			(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)&&
			(TabsHost.mNow_pageId == MainAct.mPlaying_pageId)&&
			(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP)      )
		{
			UtilAudio.stopAudioPlayer();
			AudioPlayer.mAudioIndex = 0;
			AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
			if(MainAct.mSubMenuItemAudio != null)
				MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
			Page.mItemAdapter.notifyDataSetChanged(); // disable focus
		}     	
    }
    
    // update footer audio state
    public static void updateFooterAudioState(ImageView footerAudioImage, TextView textView)
    {
    	System.out.println("UtilAudio/ _updateFooterAudioState / AudioPlayer.mPlayerState = " + AudioPlayer.mPlayerState);
		textView.setBackgroundColor(ColorSet.color_black);
		if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
		{
//			footerTextView.setEnabled(true);
			textView.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
			textView.setSelected(true);
//			footerAudioImage.setImageResource(R.drawable.ic_audio_selected); //highlight
			footerAudioImage.setImageResource(R.drawable.ic_media_pause);
		}
		else if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) ||
				 (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)    )
		{
//			footerTextView.setEnabled(false); // gray out effect if no setting text color
			textView.setSelected(false);
			textView.setTextColor(ColorSet.getPauseColor(MainAct.mAct));
//			footerAudioImage.setImageResource(R.drawable.ic_lock_ringer_on);
			footerAudioImage.setImageResource(R.drawable.ic_media_play);
			AudioPlayer.scrollHighlightAudioItemToVisible();
		}
    }
    
    // check if file has audio extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasAudioExtension(File file)
    {
    	boolean hasAudio = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
       		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
       		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
       		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
       		fn.endsWith("wav") || fn.endsWith("wma")
    		) 
	    	hasAudio = true;
	    
    	return hasAudio;
    }
    
    // check if string has audio extension
    public static boolean hasAudioExtension(String string)
    {
    	boolean hasAudio = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
	           		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
	           		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
	           		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
	           		fn.endsWith("wav") || fn.endsWith("wma")
	        		) 
	    		hasAudio = true;
    	}
    	return hasAudio;
    }     
    
    public static boolean mIsCalledWhilePlayingAudio;
    // for Pause audio player when incoming call
    // http://stackoverflow.com/questions/5610464/stopping-starting-music-on-incoming-calls
    public static PhoneStateListener phoneStateListener = new PhoneStateListener() 
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) 
        {
            if ( (state == TelephonyManager.CALL_STATE_RINGING) || 
                 (state == TelephonyManager.CALL_STATE_OFFHOOK )   ) 
            {
                //Incoming call or Call out: Pause music
            	System.out.println("Incoming call:");
            	if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
            	{
            		AudioPlayer.manageAudioState(MainAct.mAct);
            		mIsCalledWhilePlayingAudio = true;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_IDLE) 
            {
                //Not in call: Play music
            	System.out.println("Not in call:");
            	if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) && 
            		mIsCalledWhilePlayingAudio )	
            	{
            		AudioPlayer.manageAudioState(MainAct.mAct); // pause => play
            		mIsCalledWhilePlayingAudio = false;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_OFFHOOK) 
            {
                //A call is dialing, active or on hold
            	System.out.println("A call is dialing, active or on hold:");
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    
    
}
