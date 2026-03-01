package com.chesscoach.main.repository;

import com.chesscoach.main.model.Notification;
import com.chesscoach.main.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTraineeIdAndReadFalseOrderByCreatedAtDesc(Long traineeId);

    List<Notification> findByTypeAndReadFalseOrderByScheduledAtAsc(NotificationType type);

    List<Notification> findByScheduledAtLessThanEqualAndSentAtIsNullOrderByScheduledAtAsc(OffsetDateTime scheduledAt);
}
