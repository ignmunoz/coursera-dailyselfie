package com.project.coursera.dailyselfie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageViewActivity extends Activity {

    private static final int WIDTH = 250;
    private static final int HEIGHT = 250;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_selfie);

        ImageView mPicture = (ImageView) findViewById(R.id.activity_open_file_picture);

        // Get the Intent used to start this Activity
        Intent intent = getIntent();

        int position = intent.getExtras().getInt("position");

        // Get String arrays FilePathStrings & FileNameStrings
        String[] filepath = intent.getStringArrayExtra("filepath");
        String[] filename = intent.getStringArrayExtra("filename");

        Bitmap bmp = BitmapFactory.decodeFile(filepath[position]);
        Toast.makeText (this, filename[position], Toast.LENGTH_SHORT).show();

        // Set the decoded bitmap into ImageView
        mPicture.setImageBitmap(bmp);
    }
}
