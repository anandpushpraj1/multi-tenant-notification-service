package com.example.notification.notification;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType; import java.time.Instant; import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID>{
    Optional<Notification> findByTenantIdAndIdempotencyKey(UUID tenantId,String key);
    Optional<Notification> findByIdAndTenantId(UUID id,UUID tenantId);
    @Query("select distinct n.tenantId from Notification n where n.status in :statuses and (n.nextAttemptAt is null or n.nextAttemptAt <= :now) order by n.tenantId") List<UUID> findEligibleTenantIds(@Param("statuses") Collection<NotificationStatus> statuses,@Param("now") Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.tenantId=:tenantId and n.status in :statuses and (n.nextAttemptAt is null or n.nextAttemptAt <= :now) order by n.createdAt asc") List<Notification> claimCandidates(@Param("tenantId") UUID tenantId,@Param("statuses") Collection<NotificationStatus> statuses,@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);
    @Modifying @Query("update Notification n set n.status=com.example.notification.notification.NotificationStatus.QUEUED,n.nextAttemptAt=:now where n.status=com.example.notification.notification.NotificationStatus.SCHEDULED and n.scheduledAt <= :now") int promoteScheduled(@Param("now") Instant now);
    @Query("select n from Notification n where n.tenantId=:tenantId order by n.createdAt desc") List<Notification> findByTenantId(@Param("tenantId") UUID tenantId);
}
