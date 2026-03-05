// This file contains project logic for TraineeRequest.
package com.chesscoach.main.dto.trainee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TraineeRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @Min(4)
    @Max(100)
    private Integer age;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotBlank
    @Size(max = 50)
    private String gradeLevel;

    @NotBlank
    @Size(max = 100)
    private String courseStrand;

    private Integer ranking;

    private String photoPath;

    @NotBlank
    @Size(max = 80)
    private String chessUsername;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getCourseStrand() {
        return courseStrand;
    }

    public void setCourseStrand(String courseStrand) {
        this.courseStrand = courseStrand;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getChessUsername() {
        return chessUsername;
    }

    public void setChessUsername(String chessUsername) {
        this.chessUsername = chessUsername;
    }
}

