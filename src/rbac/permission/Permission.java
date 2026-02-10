package rbac.permission;

import java.util.Locale;

public record Permission(String name, String resource, String description) {
    public Permission{
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Имя разрешения не должно быть пустым!");
        }
        if (name.contains(" ")){
            throw new IllegalArgumentException("Разрешение не должно содержать проблемы!");
        }
        name = name.toUpperCase();

        if (resource == null || resource.isBlank()){
            throw new IllegalArgumentException("Ресурс не должен быть пустым!");
        }
        resource = resource.toUpperCase();

        if (description == null || description.isBlank()){
            throw new IllegalArgumentException("Описание не дожно быть пустым");
        }


    }

    public String format () {
        return "%s on %s: %s".formatted(name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattera){
        boolean nameOk = namePattern == null || name.contains(namePattern.toUpperCase());
        boolean resOk = resourcePattera == null || resource.contains(resourcePattera.toLowerCase());
        return  nameOk && resOk;
    }
}
