import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password";
        String hashFromMigration = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        System.out.println("Testing BCrypt hash from migration...");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Hash from migration: " + hashFromMigration);

        boolean matches = encoder.matches(rawPassword, hashFromMigration);
        System.out.println("\nHash matches 'password': " + matches);

        // Generate a new hash for comparison
        String newHash1 = encoder.encode(rawPassword);
        String newHash2 = encoder.encode(rawPassword);
        System.out.println("\nFresh BCrypt hashes for 'password':");
        System.out.println("Hash 1: " + newHash1);
        System.out.println("Hash 2: " + newHash2);

        System.out.println("\nVerifying fresh hashes:");
        System.out.println("Hash 1 matches 'password': " + encoder.matches(rawPassword, newHash1));
        System.out.println("Hash 2 matches 'password': " + encoder.matches(rawPassword, newHash2));
    }
}
