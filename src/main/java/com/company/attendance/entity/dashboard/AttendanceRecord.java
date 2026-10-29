package com.company.attendance.entity.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(
        name = "attendance_records",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attendance_records_source_record_id", columnNames = "source_record_id"),
                @UniqueConstraint(name = "uq_attendance_records_matricule_date", columnNames = {"MATRICULE", "DT_POINTAGE"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "source_record_id", nullable = false, length = 100)
    private String sourceRecordId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "MATRICULE", nullable = false, length = 50)
    private String matricule;

    @NotBlank
    @Size(max = 200)
    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Size(max = 200)
    @Column(name = "superieur", length = 200)
    private String superieur;

    @NotNull
    @Column(name = "DT_POINTAGE", nullable = false)
    private LocalDate dtPointage;

    @NotNull
    @Min(1)
    @Max(2)
    @Column(name = "HIERARCHIE", nullable = false)
    private Integer hierarchie;

    @Size(max = 3)
    @Column(name = "TYPE_JOUR", length = 1)
    private String typeJour;

    @Size(max = 10)
    @Column(name = "PLANNING", length = 10)
    private String planning;

    @Min(0)
    @Max(86399)
    @Column(name = "E_1")
    private Integer e1;

    @Min(0)
    @Max(86399)
    @Column(name = "S_1")
    private Integer s1;

    @Min(0)
    @Max(86399)
    @Column(name = "E_2")
    private Integer e2;

    @Min(0)
    @Max(86399)
    @Column(name = "S_2")
    private Integer s2;

    @Min(0)
    @Max(86399)
    @Column(name = "E_3")
    private Integer e3;

    @Min(0)
    @Max(86399)
    @Column(name = "S_3")
    private Integer s3;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "NbjTMOI", nullable = false, precision = 10, scale = 2)
    private BigDecimal nbjTmoi;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "NbhTMOD", nullable = false, precision = 10, scale = 2)
    private BigDecimal nbhTmod;

    @Size(max = 90)
    @Column(name = "type_absense", length = 90)
    private String typeAbsense;

    @Min(0)
    @Max(86399)
    @Column(name = "debut_HSUP")
    private Integer debutHsup;

    @NotNull
    @Column(name = "impair", nullable = false)
    private Boolean impair;

    @NotNull
    @Column(name = "annee", nullable = false)
    private Integer annee;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "mois", nullable = false)
    private Integer mois;

    @Size(max = 255)
    @Column(name = "DEPARTEMENT", length = 255)
    private String departement;

    @NotNull
    @Column(name = "updated_at_source", nullable = false)
    private LocalDateTime updatedAtSource;

    @NotNull
    @Column(name = "synchronized_at", nullable = false)
    private LocalDateTime synchronizedAt;

    @OneToOne(mappedBy = "attendanceRecord", fetch = FetchType.LAZY)
    private Anomaly anomaly;
}
