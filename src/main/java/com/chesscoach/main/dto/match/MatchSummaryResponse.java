// This file contains project logic for MatchSummaryResponse.
package com.chesscoach.main.dto.match;

import java.time.LocalDate;

public class MatchSummaryResponse {
    private Long matchId;
    private LocalDate scheduledDate;
    private String format;
    private String whitePlayer;
    private String blackPlayer;
    private String result;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(String whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(String blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}

