package com.example.eyecantalk.imageData;

public class ImageData {
    private String uuid;
    private int id;
    private int imageResId;
    private String imageDescription;

    public ImageData(String uuid, int id, int imageResId, String imageDescription) {
        this.uuid = uuid;
        this.id = id;
        this.imageResId = imageResId;
        this.imageDescription = imageDescription;
    }

    public String getUuid() {
        return uuid;
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
