package com.example.eyecantalk.activity;

public class ImageData {
    private int id;
    private String imageUrl;
    private String imageDescription;

    public ImageData(int id, String imageUrl, String imageDescription) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.imageDescription = imageDescription;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    // Getters and setters
}
