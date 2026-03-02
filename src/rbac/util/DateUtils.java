package rbac.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateUtils {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME);
    }

    public static boolean isBefore(String date1, String date2) {
        return LocalDate.parse(date1, DATE).isBefore(LocalDate.parse(date2, DATE));
    }

    public static boolean isAfter(String date1, String date2) {
        return LocalDate.parse(date1, DATE).isAfter(LocalDate.parse(date2, DATE));
    }

    public static String addDays(String date, int days) {
        LocalDate d = LocalDate.parse(date, DATE);
        return d.plusDays(days).format(DATE);
    }

    public static String formatRelativeTime(String date) {
        LocalDate target = LocalDate.parse(date, DATE);
        LocalDate today = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(today, target);
        if (diff == 0) {
            return "today";
        } else if (diff > 0) {
            return "in " + diff + " days";
        } else {
            return Math.abs(diff) + " days ago";
        }
    }
}

