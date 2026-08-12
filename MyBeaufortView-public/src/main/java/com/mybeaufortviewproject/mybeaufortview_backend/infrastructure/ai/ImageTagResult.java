package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai;

import java.util.List;

public class ImageTagResult {

    private final List<String> tags;
    private final String model;
    private final boolean success;

    public ImageTagResult(List<String> tags, String model, boolean success) {
        this.tags = tags;
        this.model = model;
        this.success = success;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getModel() {
        return model;
    }

    public boolean isSuccess() {
        return success;
    }



}
