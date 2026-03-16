// This file defines normalized Chess.com rating data returned by integration endpoints.
package com.chesscoach.main.dto.chesscom;

public class ChessComRatingResponse {
    private String username;
    private Integer rapid;
    private Integer blitz;
    private Integer bullet;
    private Integer puzzles;
    private Integer puzzleRush;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRapid() {
        return rapid;
    }

    public void setRapid(Integer rapid) {
        this.rapid = rapid;
    }

    public Integer getBlitz() {
        return blitz;
    }

    public void setBlitz(Integer blitz) {
        this.blitz = blitz;
    }

    public Integer getBullet() {
        return bullet;
    }

    public void setBullet(Integer bullet) {
        this.bullet = bullet;
    }

    public Integer getPuzzleRush() {
        return puzzleRush;
    }

    public void setPuzzleRush(Integer puzzleRush) {
        this.puzzleRush = puzzleRush;
    }

    public Integer getPuzzles() {
        return puzzles;
    }

    public void setPuzzles(Integer puzzles) {
        this.puzzles = puzzles;
    }
}
