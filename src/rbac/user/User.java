package rbac.user;

import java.util.regex.Pattern;

public record User(String username, String fullname, String email) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public static User validate(String username, String fullname, String email) {
    if (isBlank(username)){
        throw  new IllegalArgumentException("Username пользователя не должно быть пустым!");
    }
    if (!USERNAME_PATTERN.matcher(username).matches()){
        throw new IllegalArgumentException("Username должен быть от 3 до 20 символов!");
    }
    if (isBlank(fullname)){
        throw new IllegalArgumentException("Имя не должно быть пустым!");
    }
    if (isBlank(email) || !email.contains("@") || !email.substring(email.indexOf("@")).contains(".")){
        throw new IllegalArgumentException("Email неправильного формата!");
    }
    return new User(username, fullname, email);
    }

    public String format(){
        return "%s (%s) <%s>".formatted(username, fullname, email);
    }


        private static boolean isBlank(String s){
            return s == null || s.isBlank();

    }
}
