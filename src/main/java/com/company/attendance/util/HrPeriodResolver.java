package com.company.attendance.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HrPeriodResolver {

    public HrPeriod resolve(Integer year, Integer month) {
        if (year == null && month == null) {
            return new HrPeriod(null, null);
        }

        if (year == null) {
            throw new IllegalArgumentException("Year is required when month is provided");
        }

        if (month == null) {
            return new HrPeriod(
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31)
            );
        }

        LocalDate endDate = LocalDate.of(year, month, 20);
        LocalDate startDate = endDate.minusMonths(1).withDayOfMonth(21);
        return new HrPeriod(startDate, endDate);
    }
}
