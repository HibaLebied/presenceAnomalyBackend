package com.company.attendance.entity.source;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class HrAnomalyViewId implements Serializable {

    @NotBlank
    @Column(name = "MATRICULE", nullable = false, length = 15)
    private String matricule;

    @NotNull
    @Column(name = "DT_POINTAGE", nullable = false)
    private LocalDateTime dtPointage;
}
