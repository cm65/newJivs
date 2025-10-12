package com.jivs.platform.domain.businessobject;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the lifecycle of a business object
 */
@Entity
@Table(name = "business_object_lifecycles")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BusinessObjectLifecycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_object_id", nullable = false)
    private BusinessObject businessObject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleState currentState;

    @Enumerated(EnumType.STRING)
    private LifecycleState previousState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleEventType event;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "user_id")
    private Long userId;

    @OneToMany(mappedBy = "lifecycle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventTime DESC")
    private List<LifecycleEvent> events = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime stateChangedAt;

    private String stateChangedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusinessObject getBusinessObject() {
        return businessObject;
    }

    public void setBusinessObject(BusinessObject businessObject) {
        this.businessObject = businessObject;
    }

    public LifecycleState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(LifecycleState currentState) {
        this.currentState = currentState;
    }

    public LifecycleState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(LifecycleState previousState) {
        this.previousState = previousState;
    }

    public LifecycleEventType getEvent() {
        return event;
    }

    public void setEvent(LifecycleEventType event) {
        this.event = event;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<LifecycleEvent> getEvents() {
        return events;
    }

    public void setEvents(List<LifecycleEvent> events) {
        this.events = events;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStateChangedAt() {
        return stateChangedAt;
    }

    public void setStateChangedAt(LocalDateTime stateChangedAt) {
        this.stateChangedAt = stateChangedAt;
    }

    public String getStateChangedBy() {
        return stateChangedBy;
    }

    public void setStateChangedBy(String stateChangedBy) {
        this.stateChangedBy = stateChangedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
