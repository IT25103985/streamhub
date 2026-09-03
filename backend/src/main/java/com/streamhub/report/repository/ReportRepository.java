package com.streamhub.report.repository;

import com.streamhub.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
//repository querying reported content ordered by creation date
public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findByReporter_UserId(UUID reporterId, Pageable pageable);
    
    Page<Report> findByStatus(String status, Pageable pageable);
    
    Page<Report> findByReportableType(String reportableType, Pageable pageable);
    
    Page<Report> findByStatusAndReportableType(String status, String reportableType, Pageable pageable);
    
    boolean existsByReporter_UserIdAndReportableTypeAndReportableIdAndStatus(UUID reporterId, String reportableType, UUID reportableId, String status);
}
