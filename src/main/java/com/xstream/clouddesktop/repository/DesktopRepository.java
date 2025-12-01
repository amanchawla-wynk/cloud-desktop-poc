package com.xstream.clouddesktop.repository;

import com.xstream.clouddesktop.model.Desktop;
import com.xstream.clouddesktop.model.DesktopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesktopRepository extends JpaRepository<Desktop, Long> {

    Optional<Desktop> findByUserId(String userId);

    Optional<Desktop> findByUserIdAndStatusNot(String userId, DesktopStatus status);

    List<Desktop> findByUserIdAndStatus(String userId, DesktopStatus status);

    Optional<Desktop> findByVmId(Integer vmId);

    Optional<Desktop> findByConnectionId(String connectionId);

    List<Desktop> findAllByStatus(DesktopStatus status);

    List<Desktop> findAllByStatusIn(List<DesktopStatus> statuses);

    List<Desktop> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<Desktop> findAllByExpiresAtBeforeAndStatusNot(Instant time, DesktopStatus status);

    @Modifying
    @Query("UPDATE Desktop d SET d.status = :status, d.updatedAt = :updatedAt WHERE d.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") DesktopStatus status,
            @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("UPDATE Desktop d SET d.status = :status, d.errorMessage = :errorMessage, d.updatedAt = :updatedAt WHERE d.id = :id")
    void updateStatusAndError(@Param("id") Long id, @Param("status") DesktopStatus status,
            @Param("errorMessage") String errorMessage, @Param("updatedAt") Instant updatedAt);
}
