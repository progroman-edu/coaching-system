package com.chesscoach.main.repository;

import com.chesscoach.main.model.RapidRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RapidRatingRepository extends JpaRepository<RapidRating, Long> {

    Optional<RapidRating> findByTraineeId(Long traineeId);

    void deleteByTraineeId(Long traineeId);

}
