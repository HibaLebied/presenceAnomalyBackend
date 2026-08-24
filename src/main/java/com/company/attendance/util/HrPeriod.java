package com.company.attendance.util;

import java.time.LocalDate;

public record HrPeriod(
        LocalDate startDate,
        LocalDate endDate
) {
}
