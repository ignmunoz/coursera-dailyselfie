package com.project.coursera.dailyselfie;

import android.graphics.Bitmap;

/**
 * Created by Ignacio Muñoz on 26-04-2017.
 */

public class SelfieImage {

    private String photoUrl;
    private String extraInfo;
    private Bitmap photoBitMap;

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public Bitmap getPhotoBitMap() {
        return photoBitMap;
    }

    public void setPhotoBitMap(Bitmap photoBitMap) {
        this.photoBitMap = photoBitMap;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }
}
