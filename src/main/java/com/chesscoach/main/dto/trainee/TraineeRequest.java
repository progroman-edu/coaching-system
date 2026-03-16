// This DTO defines request payload fields for Trainee operations.
package com.chesscoach.main.dto.trainee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}

