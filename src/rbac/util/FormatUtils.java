package rbac.util;

import java.util.List;

public final class FormatUtils {
    private FormatUtils() {
    }

    public static String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String padLeft(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() + text.length() < length) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String formatHeader(String text) {
        String line = repeat("-", text.length() + 4);
        return line + System.lineSeparator()
                + "| " + text + " |" + System.lineSeparator()
                + line;
    }

    public static String formatBox(String text) {
        String[] lines = text.split("\\R");
        int max = 0;
        for (String line : lines) {
            if (line.length() > max) {
                max = line.length();
            }
        }
        String border = "+" + repeat("-", max + 2) + "+";
        StringBuilder sb = new StringBuilder(border).append(System.lineSeparator());
        for (String line : lines) {
            sb.append("| ").append(padRight(line, max)).append(" |").append(System.lineSeparator());
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i] != null ? row[i].length() : 0);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(border(widths)).append(System.lineSeparator());
        sb.append(row(headers, widths)).append(System.lineSeparator());
        sb.append(border(widths)).append(System.lineSeparator());
        for (String[] row : rows) {
            sb.append(row(row, widths)).append(System.lineSeparator());
        }
        sb.append(border(widths));
        return sb.toString();
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append(repeat("-", w + 2)).append("+");
        }
        return sb.toString();
    }

    private static String row(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String v = i < values.length && values[i] != null ? values[i] : "";
            sb.append(" ").append(padRight(v, widths[i])).append(" |");
        }
        return sb.toString();
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}

