// This file contains project logic for RatingTrendPointResponse.
package com.chesscoach.main.dto.analytics;

import java.time.OffsetDateTime;

public class RatingTrendPointResponse {
    private OffsetDateTime timestamp;
    private Integer rating;
    private Integer change;

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getChange() {
        return change;
    }

    public void setChange(Integer change) {
        this.change = change;
    }
}

