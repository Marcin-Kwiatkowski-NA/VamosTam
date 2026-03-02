package com.vamigo.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReminderTrackingRepository extends JpaRepository<ReviewReminderTracking, Long> {

    boolean existsByBookingIdAndUserIdAndType(Long bookingId, Long userId, ReviewReminderTracking.ReminderType type);
}
