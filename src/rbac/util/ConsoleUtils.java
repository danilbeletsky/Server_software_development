package rbac.util;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public final class ConsoleUtils {
    private ConsoleUtils() {
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + ": ");
            String value = scanner.nextLine();
            value = ValidationUtils.normalizeString(value);
            if (!required || !value.isEmpty()) {
                return value;
            }
            System.out.println("Value is required.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " [" + min + "-" + max + "]: ");
            String line = scanner.nextLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value < min || value > max) {
                    System.out.println("Value must be between " + min + " and " + max + ".");
                } else {
                    return value;
                }
            } catch (NumberFormatException | InputMismatchException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if ("yes".equals(answer) || "y".equals(answer)) {
                return true;
            }
            if ("no".equals(answer) || "n".equals(answer)) {
                return false;
            }
            System.out.println("Please answer yes or no.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options list is empty");
        }
        while (true) {
            System.out.println(message + ":");
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ") " + options.get(i));
            }
            int index = promptInt(scanner, "Choose option", 1, options.size());
            return options.get(index - 1);
        }
    }
}

