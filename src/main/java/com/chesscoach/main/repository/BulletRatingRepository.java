package com.chesscoach.main.repository;

import com.chesscoach.main.model.BulletRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BulletRatingRepository extends JpaRepository<BulletRating, Long> {

    Optional<BulletRating> findByTraineeId(Long traineeId);

    void deleteByTraineeId(Long traineeId);

}
