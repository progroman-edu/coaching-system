package com.chesscoach.main.repository;

import com.chesscoach.main.model.BlitzRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlitzRatingRepository extends JpaRepository<BlitzRating, Long> {

    Optional<BlitzRating> findByTraineeId(Long traineeId);

    void deleteByTraineeId(Long traineeId);

}
