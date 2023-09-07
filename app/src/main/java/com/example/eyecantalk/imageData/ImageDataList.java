package com.example.eyecantalk.imageData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageDataList {
    private List<Integer> imageDataList;
    private String uuid;

    public ImageDataList(String uuid, List<Integer> imageDataList) {
        this.uuid = uuid;
        this.imageDataList = imageDataList;
    }

    public List<Integer> getImageDataList() {
        return imageDataList;
    }
}
