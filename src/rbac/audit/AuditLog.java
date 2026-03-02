package rbac.audit;

import rbac.util.DateUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
    }

    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String action, String performer, String target, String details) {
        entries.add(new AuditEntry(
                DateUtils.getCurrentDateTime(),
                action,
                performer,
                target,
                details
        ));
    }

    public List<AuditEntry> getAll() {
        return List.copyOf(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equalsIgnoreCase(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }
        for (AuditEntry e : entries) {
            System.out.printf("[%s] %-15s by %-10s target=%s details=%s%n",
                    e.timestamp(), e.action(), e.performer(), e.target(), e.details());
        }
    }

    public void saveToFile(String filename) {
        try (FileWriter fw = new FileWriter(filename, true)) {
            for (AuditEntry e : entries) {
                fw.write(String.format("%s;%s;%s;%s;%s%n",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details()));
            }
        } catch (IOException ex) {
            System.out.println("Failed to save audit log: " + ex.getMessage());
        }
    }
}

