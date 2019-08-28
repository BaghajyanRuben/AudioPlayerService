package com.audio.playerservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

	private static final int PERMISSION_REQ_CODE = 1234;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
					startAudioService();
				} else {
					ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQ_CODE);
				}

			}
		});

		findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				stopAudioService();
			}
		});

	}

	private void startAudioService(){
		Intent intent = new Intent(MainActivity.this, AudioService.class);
		intent.setAction(AudioService.ACTION_PLAY);
		startService(intent);
	}

	private void stopAudioService(){
		Intent intent = new Intent(MainActivity.this, AudioService.class);
		intent.setAction(AudioService.ACTION_STOP);
		startService(intent);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSION_REQ_CODE && permissions.length != 0 && grantResults.length != 0){
			startAudioService();
		}
	}
}
