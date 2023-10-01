package com.example.eyecantalk.imageData;

import java.util.List;

public class AacResponse {
    private List<Integer> aac_id;

    @Override
    public String toString() {
        return "AacResponse{" +
                "aac_id=" + aac_id +
                '}';
    }

    public List<Integer> getAacId() {
        return aac_id;
    }
}
