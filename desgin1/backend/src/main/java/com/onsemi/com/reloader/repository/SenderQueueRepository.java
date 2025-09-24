package com.onsemi.com.reloader.repository;

import com.onsemi.com.reloader.entity.SenderQueueEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SenderQueueRepository extends JpaRepository<SenderQueueEntry, Long> {
    long countByStatus(String status);

    // Portable JPA methods using Pageable for batching
    List<SenderQueueEntry> findByStatusOrderByCreatedAt(String status, Pageable pageable);

    List<SenderQueueEntry> findBySenderIdAndStatusOrderByCreatedAt(Integer senderId, String status, Pageable pageable);
}