package com.company.attendance.entity.source;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Immutable
@Table(name = "hr_anomaly_view", schema = "dbo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HrAnomalyView {

    @Valid
    @EmbeddedId
    private HrAnomalyViewId id;

    @Size(max = 61)
    @Column(name = "nom", length = 61)
    private String nom;

    @Size(max = 77)
    @Column(name = "superieur", length = 77)
    private String superieur;

    @Size(max = 10)
    @Column(name = "HIERARCHIE", length = 10)
    private String hierarchie;

    @Size(max = 3)
    @Column(name = "TYPE_JOUR", length = 3)
    private String typeJour;

    @Size(max = 8)
    @Column(name = "PLANNING", length = 8)
    private String planning;

    @Column(name = "E_1")
    private Integer e1;

    @Column(name = "S_1")
    private Integer s1;

    @Column(name = "E_2")
    private Integer e2;

    @Column(name = "S_2")
    private Integer s2;

    @Column(name = "E_3")
    private Integer e3;

    @Column(name = "S_3")
    private Integer s3;

    @Column(name = "NbjTMOI")
    private Double nbjTmoi;

    @Column(name = "NbhTMOD")
    private Double nbhTmod;

    @Size(max = 90)
    @Column(name = "type_absense", length = 90)
    private String typeAbsense;

    @Column(name = "debut_HSUP")
    private Integer debutHsup;

    @Column(name = "impair")
    private Double impair;

    @Column(name = "annee")
    private Double annee;

    @Column(name = "mois")
    private Double mois;

    @Column(name = "DEPARTEMENT")
    private String departement;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getMatricule() {
        return id.getMatricule();
    }

    public LocalDate getDtPointage() {
        return id.getDtPointage().toLocalDate();
    }

    public LocalDateTime getDtPointageDateTime() {
        return id.getDtPointage();
    }
}
