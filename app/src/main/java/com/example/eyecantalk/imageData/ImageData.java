package com.example.eyecantalk.imageData;

import android.graphics.drawable.Drawable;

public class ImageData {
    private int id;
    private Drawable imageResId;
    private String imageDescription;

    public ImageData(int id, Drawable imageResId, String imageDescription) {
        this.id = id;
        this.imageResId = imageResId;
        this.imageDescription = imageDescription;
    }

    public int getId() {
        return id;
    }

    public Drawable getImageResId() {
        return imageResId;
    }

    public String getImageDescription() {
        return imageDescription;
    }
}
