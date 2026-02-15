package rbac;

import rbac.assignment.*;
import rbac.permission.Permission;
import rbac.role.Role;
import rbac.user.User;

import java.util.*;

public class Main {

    private static final Map<String, User> users = new HashMap<>();
    private static final Map<String, Role> roles = new HashMap<>();
    private static final List<RoleAssignment> assignments = new ArrayList<>();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {

            System.out.println("\n=== RBAC MENU ===");
            System.out.println("1 - Создание пользователя");
            System.out.println("2 - Создание роли");
            System.out.println("3 - Добавить разрешение к роли");
            System.out.println("4 - Назначить роль пользователю");
            System.out.println("5 - Показать роли");
            System.out.println("6 - Результат");
            System.out.println("0 - Выход");
            System.out.print("Choose option: ");

            String input = scanner.nextLine();

            switch (input) {

                case "1" -> {
                    try {
                        System.out.print("Имя: ");
                        String username = scanner.nextLine();

                        System.out.print("Полное имя: ");
                        String fullName = scanner.nextLine();

                        System.out.print("Email: ");
                        String email = scanner.nextLine();

                        User user = User.validate(username, fullName, email);
                        users.put(username, user);

                        System.out.println("Пользователь создан: " + user.format());

                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }

                case "2" -> {
                    try {
                        System.out.print("Имя Роли: ");
                        String roleName = scanner.nextLine();

                        System.out.print("Описание: ");
                        String desc = scanner.nextLine();

                        Role role = new Role(roleName, desc);
                        roles.put(roleName, role);

                        System.out.println("Роль создана.");

                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }

                case "3" -> {
                    System.out.print("Имя Роли: ");
                    String roleName = scanner.nextLine();

                    Role role = roles.get(roleName);
                    if (role == null) {
                        System.out.println("Роль не найдена.");
                        break;
                    }

                    System.out.print("Имя разрешение: ");
                    String permName = scanner.nextLine();

                    System.out.print("Право: ");
                    String resource = scanner.nextLine();

                    System.out.print("Описание: ");
                    String desc = scanner.nextLine();

                    Permission permission =
                            new Permission(permName, resource, desc);

                    role.addPermission(permission);

                    System.out.println("Разрешение создано.");
                }

                case "4" -> {
                    System.out.print("Имя: ");
                    String username = scanner.nextLine();

                    System.out.print("Имя роли: ");
                    String roleName = scanner.nextLine();

                    User user = users.get(username);
                    Role role = roles.get(roleName);

                    if (user == null || role == null) {
                        System.out.println("Пользователь или роль не найдена.");
                        break;
                    }

AssignmentMetadata meta =
        AssignmentMetadata.now("Система", "Назначение вручную");

PermanentAssignment pa =
        new PermanentAssignment(user, role, meta);

                    assignments.add(pa);

                    System.out.println("Роль назначена.");
                }

                        case "5" -> {
                        if (roles.isEmpty()) {
        System.out.println("Роли не соданы.");
                    } else {
                            roles.values().forEach(r -> {
        System.out.println(r.format());
        });
        }
        }

        case "6" -> {
        if (assignments.isEmpty()) {
        System.out.println("Не рузльтатов.");
                    } else {
                            assignments.forEach(a ->
        System.out.println(
                                        ((AbstractRoleAssignment) a).summary()
                                ));
                                        }
                                        }

                                        case "0" -> {
running = false;
        System.out.println("Выход...");
                }

default -> System.out.println("Неверное значение");
            }
                    }

                    scanner.close();
    }
            }