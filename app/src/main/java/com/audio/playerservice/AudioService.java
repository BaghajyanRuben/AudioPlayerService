package com.audio.playerservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioService extends IntentService {
	private static int NOTIFICATION_ID = 123;
	private static String AUDIO_PLAYER_SERVICE = "audio.player.service";


	public static final String ACTION_PLAY = "action_play";
	public static final String ACTION_PAUSE = "action_pause";
	public static final String ACTION_NEXT = "action_next";
	public static final String ACTION_PREVIOUS = "action_previous";
	public static final String ACTION_STOP = "action_stop";

	private MediaPlayer mMediaPlayer;
	private MediaSessionManager mManager;
	private MediaSession mSession;
	private MediaController mController;

	private int audioIndex = 0;

	private List<String> audio = new ArrayList<>();
	private int trackCurrentIndex = 0;

	public AudioService() {
		super(AUDIO_PLAYER_SERVICE);
	}


	@Override
	public void onCreate() {
		super.onCreate();
		loadMediaFiles();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (mMediaPlayer == null) {
			initMediaSessions();
		}

		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (intent == null || intent.getAction() == null)
			return;

		String action = intent.getAction();

		if (action.equalsIgnoreCase(ACTION_PLAY)) {
			mController.getTransportControls().play();
		} else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
			mController.getTransportControls().pause();
		} else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
			mController.getTransportControls().skipToPrevious();
		} else if (action.equalsIgnoreCase(ACTION_NEXT)) {
			mController.getTransportControls().skipToNext();
		} else if (action.equalsIgnoreCase(ACTION_STOP)) {
			stopSelf();
		}
	}

	private Notification.Action generateAction(int icon, String title, String intentAction) {
		Intent intent = new Intent(getApplicationContext(), AudioService.class);
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
		return new Notification.Action.Builder(icon, title, pendingIntent).build();
	}

	private void buildNotification(Notification.Action action) {
		Notification.MediaStyle style = new Notification.MediaStyle();

		Intent intent = new Intent(getApplicationContext(), AudioService.class);
		intent.setAction(ACTION_STOP);

		PendingIntent pendingIntent
				= PendingIntent.getService(getApplicationContext(),
				1,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_audiotrack)
				.setContentTitle("Media Title")
				.setContentText("Media Artist")
				.setDeleteIntent(pendingIntent)
				.setOngoing(true)
				.setStyle(style);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel mChannel = notificationManager.getNotificationChannel("audio.player");
			if (mChannel == null) {
				mChannel = new NotificationChannel("audio.player", "audio player", importance);
				mChannel.setDescription("Media track");
				notificationManager.createNotificationChannel(mChannel);
			}

			builder.setChannelId("audio.player");

		}

		builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
		builder.addAction(action);
		builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
		style.setShowActionsInCompactView(0, 1, 2);

		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}


	private void initMediaSessions() {
		mMediaPlayer = new MediaPlayer();

		mSession = new MediaSession(getApplicationContext(), "simple player session");
		mController = new MediaController(getApplicationContext(), mSession.getSessionToken());

		mSession.setCallback(new MediaSession.Callback() {
								 @Override
								 public void onPlay() {
									 super.onPlay();
									 Log.w(AUDIO_PLAYER_SERVICE, "onPlay");
									 initPlayer();
									 buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
								 }

								 @Override
								 public void onPause() {
									 super.onPause();
									 Log.w(AUDIO_PLAYER_SERVICE, "onPause");
									 mMediaPlayer.pause();
									 trackCurrentIndex = mMediaPlayer.getCurrentPosition();
									 buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
								 }

								 @Override
								 public void onSkipToNext() {
									 super.onSkipToNext();
									 trackCurrentIndex = 0;

									 audioIndex++;
									 if (audioIndex >= audio.size()) {
										 audioIndex = 0;
									 }
									 initPlayer();
									 Log.w(AUDIO_PLAYER_SERVICE, "onSkipToNext");
									 //Change media here
									 buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
								 }

								 @Override
								 public void onSkipToPrevious() {
									 super.onSkipToPrevious();
									 trackCurrentIndex = 0;
									 audioIndex--;
									 if (audioIndex < 0) {
										 audioIndex = audio.size() - 1;
									 }
									 initPlayer();
									 Log.w(AUDIO_PLAYER_SERVICE, "onSkipToPrevious");
									 //Change media here
									 buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
								 }

								 @Override
								 public void onFastForward() {
									 super.onFastForward();
									 Log.w(AUDIO_PLAYER_SERVICE, "onFastForward");
									 //Manipulate current media here
								 }

								 @Override
								 public void onRewind() {
									 super.onRewind();
									 Log.w(AUDIO_PLAYER_SERVICE, "onRewind");
									 //Manipulate current media here
								 }

								 @Override
								 public void onStop() {
									 super.onStop();
									 Log.w(AUDIO_PLAYER_SERVICE, "onStop");
									 //Stop media player here

								 }

								 @Override
								 public void onSeekTo(long pos) {
									 super.onSeekTo(pos);
								 }

								 @Override
								 public void onSetRating(@NonNull Rating rating) {
									 super.onSetRating(rating);
								 }
							 }
		);
	}

	private void stop() {
		NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}

	@Override
	public void onDestroy() {
		if (mSession != null) {
			mSession.release();
		}


		stop();
		super.onDestroy();
	}


	private void initPlayer() {
		mMediaPlayer.reset();

		try {

			if (audio.isEmpty()) {
				return;
			}

			mMediaPlayer.setDataSource(audio.get(audioIndex));
			mMediaPlayer.prepare();
			mMediaPlayer.seekTo(trackCurrentIndex);
			mMediaPlayer.start();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	String[] mCursorCols = new String[]{
			"audio._id AS _id", // index must match IDCOLIDX below
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.DATA,
			MediaStore.Audio.Media.MIME_TYPE,
			MediaStore.Audio.Media.ALBUM_ID,
			MediaStore.Audio.Media.ARTIST_ID,
			MediaStore.Audio.Media.DURATION
	};

	private void loadMediaFiles() {
		Uri MUSIC_URL = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		Cursor mCursor = getContentResolver().query(MUSIC_URL,
				mCursorCols,
				"duration > 10000",
				null,
				null);


		mCursor.moveToFirst();
		while (mCursor.moveToNext()) {
			int dataColumn = mCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
			String data = mCursor.getString(dataColumn);
			audio.add(data);
		}
	}

}
