package com.company.attendance.detection.rule;

import com.company.attendance.detection.model.AnomalyCode;
import com.company.attendance.detection.model.DetectionContext;
import com.company.attendance.detection.model.DetectionResult;

public abstract class AbstractOvertimeRule extends AbstractAttendanceRule {

    private static final int FIFTY_MINUTES_IN_SECONDS = 50 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public DetectionResult detect(DetectionContext context) {
        if (hasDeclaredOvertime(context)) {
            return DetectionResult.noAnomaly();
        }

        if (isOvertimeNotDeclared(context)) {
            return DetectionResult.detected(AnomalyCode.HEURES_SUPPLEMENTAIRES);
        }

        return DetectionResult.noAnomaly();
    }

    private boolean hasDeclaredOvertime(DetectionContext context) {
        return context.attendanceRecord().getDebutHsup() != null;
    }

    private boolean isOvertimeNotDeclared(DetectionContext context) {
        if (Boolean.TRUE.equals(context.attendanceRecord().getImpair())) {
            return false;
        }

        String planning = context.attendanceRecord().getPlanning();
        Integer entry = firstEntry(context.attendanceRecord());
        Integer exit = lastExit(context.attendanceRecord());

        if ("HD".equals(planning)) {
            return entry != null
                    && exit != null
                    && normalizeExitAfterEntry(exit, entry) - entry >= FIFTY_MINUTES_IN_SECONDS;
        }

        ShiftWindow shiftWindow = shiftWindowFor(planning);
        if (shiftWindow == null) {
            return false;
        }

        return startsMoreThanFiftyMinutesBeforeShift(entry, shiftWindow.startSeconds(), shiftWindow.crossesMidnight())
                || endsMoreThanFiftyMinutesAfterShift(
                entry,
                exit,
                shiftWindow.endSeconds(),
                shiftWindow.crossesMidnight()
        );
    }

    private boolean startsMoreThanFiftyMinutesBeforeShift(
            Integer entry,
            int shiftStartSeconds,
            boolean crossesMidnight
    ) {
        if (entry == null) {
            return false;
        }

        int normalizedEntry = normalizeForOvernightShift(entry, crossesMidnight);

        return normalizedEntry < shiftStartSeconds
                && shiftStartSeconds - normalizedEntry >= FIFTY_MINUTES_IN_SECONDS;
    }

    private boolean endsMoreThanFiftyMinutesAfterShift(
            Integer entry,
            Integer exit,
            int shiftEndSeconds,
            boolean crossesMidnight
    ) {
        if (exit == null) {
            return false;
        }

        int normalizedExit = normalizeExitForComparison(entry, exit, crossesMidnight);
        int normalizedShiftEnd = normalizeForOvernightShift(shiftEndSeconds, crossesMidnight);

        return normalizedExit > normalizedShiftEnd
                && normalizedExit - normalizedShiftEnd >= FIFTY_MINUTES_IN_SECONDS;
    }

    private int normalizeForOvernightShift(int seconds, boolean crossesMidnight) {
        return crossesMidnight && seconds < SECONDS_PER_DAY / 2
                ? seconds + SECONDS_PER_DAY
                : seconds;
    }

    private int normalizeExitForComparison(Integer entry, int exit, boolean crossesMidnight) {
        int normalizedExit = normalizeForOvernightShift(exit, crossesMidnight);
        if (entry == null) {
            return normalizedExit;
        }

        int normalizedEntry = normalizeForOvernightShift(entry, crossesMidnight);
        return normalizeExitAfterEntry(normalizedExit, normalizedEntry);
    }

    private int normalizeExitAfterEntry(int exit, int entry) {
        return exit < entry ? exit + SECONDS_PER_DAY : exit;
    }

    private ShiftWindow shiftWindowFor(String planning) {
        return switch (planning) {
            case "SH3" -> new ShiftWindow(15 * 3600, 23 * 3600, false);
            case "SH3B" -> new ShiftWindow(14 * 3600, 22 * 3600, false);
            case "SH9" -> new ShiftWindow(23 * 3600, 7 * 3600, true);
            case "SH9B" -> new ShiftWindow(22 * 3600, 6 * 3600, true);
            case "SH1" -> new ShiftWindow(7 * 3600, 15 * 3600, false);
            case "SH1B" -> new ShiftWindow(6 * 3600, 14 * 3600, false);
            default -> null;
        };
    }

    private record ShiftWindow(int startSeconds, int endSeconds, boolean crossesMidnight) {
    }
}
