package rbac.core;

import rbac.system.RBACSystem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> commandDescriptions = new LinkedHashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command cmd = commands.get(commandName);
        if (cmd == null) {
            System.out.println("Unknown command: " + commandName);
            return;
        }
        cmd.execute(scanner, system);
    }

    public void printHelp() {
        System.out.println("Available commands:");
        commandDescriptions.forEach((name, desc) ->
                System.out.printf("  %-20s - %s%n", name, desc));
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }
        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0];
        executeCommand(commandName, scanner, system);
    }
}

