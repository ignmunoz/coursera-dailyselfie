package com.project.coursera.dailyselfie;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class SelfieActivity extends AppCompatActivity {

    private GridView grid;
    private List<String> listOfImgs;
    private String[] FilePathStrings;
    private String[] FileNameStrings;
    private Uri mCapturedImageURI;
    private ImageListAdapter mAdapter;
    private TextView mEmptyTextView;

    private static final String CAMERA_DIR = "/DailySelfie";

    private boolean isAnotherActivity = false;
    private boolean isThumbnailClicked = false;
    private boolean isCameraActivityRunning = false;

    private String mCurrentPhotoPath = null;

    private AlarmManager mAlarmManager;
    Intent mNotificationReceiverIntent;
    PendingIntent mNotificationReceiverPendingIntent;
    private static final long INITIAL_ALARM_DELAY = 2 * 10 * 1000L;

    private final int CAMERA_CAPTURE = 1;
    private static final int REQUEST_FOR_STORAGE = 1111;

    //private static final String LOG_TAG = "LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);

        isStoragePermissionGranted();

        //createDirIfNotExists(CAMERA_DIR);

        // Get the AlarmManager Service
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Create an Intent to broadcast to the AlarmNotificationReceiver
        mNotificationReceiverIntent = new Intent(SelfieActivity.this,
                SelfieNotificationReceiver.class);

        // Create an PendingIntent that holds the NotificationReceiverIntent
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(
                SelfieActivity.this, 0, mNotificationReceiverIntent, PendingIntent.FLAG_ONE_SHOT);

        grid = (GridView) findViewById(R.id.gridviewimg);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                getSelfieGallery();

                //Create an Intent to start the ImageViewActivity
                Intent intent = new Intent(SelfieActivity.this,
                        ImageViewActivity.class);

                intent.putExtra("filepath", FilePathStrings);
                intent.putExtra("filename", FileNameStrings);

                // Pass click position
                intent.putExtra("position", position);

                isAnotherActivity = true;
                isThumbnailClicked = true;

                // Start the ImageViewActivity
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_camera) {
            dispatchTakePictureIntent();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPhotoGrid();
        cancelNotificationAlarm();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isAnotherActivity = false;
        setPhotoGrid();
        cancelNotificationAlarm();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isAnotherActivity){
            grid.invalidate();
            setNotificationAlarm();
        } else{
            if(isThumbnailClicked) {
                String path = this.getCurrentPhotoPath();
                if (isCameraActivityRunning && path != null) {
                    new File(path).delete();
                    grid.invalidate();
                    mAdapter.clear();
                    mAdapter = new ImageListAdapter(this, listOfImgs);
                    grid.setAdapter(mAdapter);

                    resolveEmptyText();
                    mAdapter.notifyDataSetChanged();
                    isThumbnailClicked = false;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                requestCode == CAMERA_CAPTURE ) {
            addPhotoToGallery();
            listOfImgs = RetrieveCapturedImagePath();
            isCameraActivityRunning = false;

            if(listOfImgs != null){
                grid.invalidate();
                mAdapter.clear();
                mAdapter = new ImageListAdapter(this, listOfImgs);
                grid.setAdapter(mAdapter);
            }

            resolveEmptyText();
            mAdapter.notifyDataSetChanged();

        } else {
            String path = this.getCurrentPhotoPath();
            new File(path).delete();
            grid.invalidate();
            mAdapter.clear();
            mAdapter = new ImageListAdapter(this, listOfImgs);
            grid.setAdapter(mAdapter);

            resolveEmptyText();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_FOR_STORAGE : {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    createDirIfNotExists(CAMERA_DIR);
                    setPhotoGrid();
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void setPhotoGrid(){
        listOfImgs = null;
        listOfImgs = RetrieveCapturedImagePath();

        mAdapter = new ImageListAdapter(this, listOfImgs);
        grid.setAdapter(mAdapter);
        mEmptyTextView = (TextView) findViewById(R.id.empty);

        resolveEmptyText();
    }

    private List<String> RetrieveCapturedImagePath() {
        List<String> tFileList = new ArrayList<>();
        File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES + CAMERA_DIR).toString());
//            if (!f.mkdirs()) {
//                Log.e(LOG_TAG, "Directory not created");
//            }
        if (f.exists()) {
            File[] files = f.listFiles();
            if (files != null){
                Arrays.sort(files);

                for (File file : files) {
                    if (file.isDirectory())
                        continue;
                    tFileList.add(file.getPath());
                }
            }
        }
        return tFileList;
    }

    private String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    private void setCurrentPhotoPath(String mCurrentPhotoPath) {
        this.mCurrentPhotoPath = mCurrentPhotoPath;
    }

   public Uri getCapturedImageURI() {
       return mCapturedImageURI;
   }

    private void setCapturedImageURI(Uri mCapturedImageURI) {
        this.mCapturedImageURI = mCapturedImageURI;
    }

    // Get the directory to store our images in, and create it if it doesn't already exist
    private File getImageStorageDirectory(Context context) throws IOException, SecurityException  {
        if (!isStoragePermissionGranted()) {
            throw new SecurityException("App cannot run without WRITE_EXTERNAL_STORAGE permission.");
        }
        File externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File imageDir = new File(externalStorageDir, context.getResources().getString(R.string.app_name));

        // Throw if the image directory doesn't already exist and cannot be created
        if (!imageDir.mkdir() && !imageDir.isDirectory())
            throw new IOException("Failed to create image storage directory: " + imageDir.getPath());

        return imageDir;
    }

    private File createImageFile(Context context) throws IOException, SecurityException {
        if (!isStoragePermissionGranted()) {
            throw new SecurityException("App cannot run without WRITE_EXTERNAL_STORAGE permission.");
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getImageStorageDirectory(context);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //this.setCurrentPhotoPath("file:" + image.getAbsolutePath());
        return image;
    }

    private void dispatchTakePictureIntent() {
        // Check if there is a camera.
        Context context = getApplicationContext();
        PackageManager packageManager = context.getPackageManager();

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Log.e("App-Hardware", "This device does not have a camera.");
            return;
        }
        else{
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            isAnotherActivity = true;
            this.setCurrentPhotoPath(null);

            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(this.getPackageManager()) != null){
                // Create the File where the photo should go.
                try {
                    File photoFile = createImageFile(context);
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri fileUri = Uri.fromFile(photoFile);
                        this.setCapturedImageURI(fileUri);
                        this.setCurrentPhotoPath(fileUri.getPath());
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        grantURIPermissionsForIntent(this, takePictureIntent, this.getCapturedImageURI());
                        startActivityForResult(takePictureIntent, CAMERA_CAPTURE);
                    }
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e("App-Photo", "There was a problem saving the photo.");
                }
            }
        }

    }

    private void addPhotoToGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(this.getCurrentPhotoPath());
            final Uri contentUri = Uri.fromFile(f);
            scanIntent.setData(contentUri);
            this.sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + CAMERA_DIR)));
            sendBroadcast(intent);
        }
    }

    private void getSelfieGallery(){
        File storageDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES + CAMERA_DIR);

        if (storageDir != null && storageDir.isDirectory()) {
            File[] listFile = storageDir.listFiles();

            if (listFile == null){
                FilePathStrings = new String[0];
                FileNameStrings = new String[0];
            } else{
                // Create a String array for FilePathStrings
                FilePathStrings = new String[listFile.length];
                FileNameStrings = new String[listFile.length];

                for (int i = 0; i < listFile.length; i++) {
                    // Get the path of the image file
                    FilePathStrings[i] = listFile[i].getAbsolutePath();
                    FileNameStrings[i] = listFile[i].getName();
                }
            }
        }
    }

    private void createDirIfNotExists(String path) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(this, getString(R.string.not_save), Toast.LENGTH_LONG).show();
            }
            else{
                file.mkdirs();
            }
        }
    }

    private void setNotificationAlarm(){
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INITIAL_ALARM_DELAY,
                mNotificationReceiverPendingIntent);

        Log.e("Alarm", "Notification will send two minutes from now");
    }

    private void cancelNotificationAlarm() {
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(SelfieActivity.this, 0, mNotificationReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(mNotificationReceiverPendingIntent);

        Log.e("Alarm", "Notification has been cancel!");
    }

    private void resolveEmptyText(){
        if(mAdapter.isEmpty()){
            setEmptyText();
            mEmptyTextView.setVisibility(View.VISIBLE);

        } else {
            mEmptyTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void setEmptyText() {
        mEmptyTextView.setText("No Photos!");
    }

    private boolean isStoragePermissionGranted(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_FOR_STORAGE);
                return false;
            }
        } else {
            return true;
        }
    }

    static void grantURIPermissionsForIntent(Context context, Intent takePictureIntent, Uri photoURI) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName,
                    photoURI,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}