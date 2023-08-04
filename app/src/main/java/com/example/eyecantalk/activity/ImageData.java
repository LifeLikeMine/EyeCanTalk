package com.example.eyecantalk.activity;

public class ImageData {
    private int id;
    private int imageResId;
    private String imageDescription;

    public ImageData(int id, int imageResId, String imageDescription) {
        this.id = id;
        this.imageResId = imageResId;
        this.imageDescription = imageDescription;
    }

    public int getId() {
        return id;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getImageDescription() {
        return imageDescription;
    }

}
