package com.yang.moni.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger")
@Getter
@NoArgsConstructor
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
}
