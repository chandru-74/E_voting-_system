import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        String rawPassword = "AdminAccess@2026";  // ← your chosen password
        String hash = new BCryptPasswordEncoder().encode(rawPassword);
        System.out.println(hash);
    }
}