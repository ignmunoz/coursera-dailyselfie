package com.project.coursera.dailyselfie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

class ImageListAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> mSelfie;
    private static final int WIDTH = 250;
    private static final int HEIGHT = 250;
    private static final int PADDING = 8;

    public ImageListAdapter(Context c, List<String> selfie) {
        context = c;
        mSelfie = selfie;
    }

    // Return the number of items in the Adapter
    @Override
    public int getCount() {
        if(mSelfie != null)
            return mSelfie.size();
        else return 0;
    }

    @Override
    public Object getItem(int position) {
        return mSelfie.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        BitmapFactory.Options bfOptions = new BitmapFactory.Options();
        bfOptions.inDither = false; //Disable Dithering mode
        bfOptions.inTempStorage = new byte[16 * 1024];

        // if convertView's not recycled, initialize some attributes
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(WIDTH, HEIGHT));
            imageView.setPadding(PADDING, PADDING, PADDING, PADDING);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        FileInputStream fs = null;
        Bitmap bm;

        try {
            fs = new FileInputStream(new File(mSelfie.get(position)));
            bm = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);
            imageView.setImageBitmap(bm);
            imageView.setId(position);
            imageView.setLayoutParams(new GridView.LayoutParams(200, 160));
            bfOptions.inBitmap = bm;
        } catch (IOException e) {
            e.printStackTrace();

        } finally{
            if(fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return imageView;
    }


    public void clear() { mSelfie.clear(); }
}
