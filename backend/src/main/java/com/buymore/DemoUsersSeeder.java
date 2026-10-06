package com.buymore;

import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Keeps a working admin and customer test account in the demo database. */
@Component
public class DemoUsersSeeder implements ApplicationRunner {
    public record Account(String name, String email, String password, String role) {}

    static List<Account> accounts() {
        return List.of(
            new Account("Test Customer", "customer@buymore.test", "Customer-demo-42", "CUSTOMER"),
            new Account("Preview Admin", "admin@buymore.test", "Admin-demo-42", "ADMIN"));
    }

    final JdbcTemplate db; final PasswordEncoder encoder; final boolean enabled;
    DemoUsersSeeder(JdbcTemplate db, PasswordEncoder encoder, @Value("${buymore.seed-demo-users:true}") boolean enabled) {
        this.db = db; this.encoder = encoder; this.enabled = enabled;
    }

    @Override public void run(ApplicationArguments args) {
        if (!enabled) return;
        sync(db, encoder::encode);
    }

    static void sync(JdbcTemplate db, Function<String, String> hash) {
        var existing = new HashSet<String>();
        for (var row : db.queryForList("SELECT email FROM users")) {
            Object email = row.get("email");
            if (email != null) existing.add(email.toString().toLowerCase(Locale.ROOT));
        }
        for (var account : accounts()) {
            if (existing.contains(account.email())) continue;
            db.update("INSERT INTO users(name,email,password_hash,role) VALUES (?,?,?,?)",
                account.name(), account.email(), hash.apply(account.password()), account.role());
        }
    }
}
