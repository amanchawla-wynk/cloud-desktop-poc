package com.xstream.clouddesktop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "desktops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Desktop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    private Integer vmId;

    private String vmIpAddress;

    private String connectionId;

    @Column(length = 1024)
    private String connectionUrl;

    private String protocol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesktopStatus status;

    private String statusMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesktopPlan plan;

    private Integer cpuCores;

    private Integer memoryMb;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant lastAccessedAt;

    private Instant expiresAt;

    @Column(length = 2048)
    private String errorMessage;
}
