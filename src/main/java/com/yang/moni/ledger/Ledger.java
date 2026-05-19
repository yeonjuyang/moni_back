package com.yang.moni.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "ledger_name", nullable = false)
    private String ledgerName;

    @Column(name = "ledger_type", nullable = false)
    private String ledgerType;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "invite_code")
    private String inviteCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        active = true;
    }

    public void updateName(String name) {
        this.ledgerName = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInviteCode(String code) {
        this.inviteCode = code;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
