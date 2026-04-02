package com.omnicharge.recharge.entity;

import com.omnicharge.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recharges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Recharge extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rechargeId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false)
    private Long operatorId;

    @Column(nullable = false)
    private String operatorName;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer planValidityDays;

    @Column(nullable = false)
    private LocalDate planExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RechargeStatus status;

    @Column(length = 500)
    private String failureReason;

    private String transactionId;
}
