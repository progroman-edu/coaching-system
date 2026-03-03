// This file contains project logic for Notification.
package com.chesscoach.main.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_trainee", columnList = "trainee_id"),
        @Index(name = "idx_notifications_type", columnList = "type"),
        @Index(name = "idx_notifications_is_read", columnList = "is_read")
    }
)
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id")
    private Trainee trainee;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private Boolean read = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trainee getTrainee() {
        return trainee;
    }

    public void setTrainee(Trainee trainee) {
        this.trainee = trainee;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }
}

