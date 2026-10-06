package com.buymore;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DemoUsersTest {

    @Test void seedsOneAdminAndOneCustomerWithUsableCredentials() {
        var users = DemoUsersSeeder.accounts();
        assertEquals(2, users.size(), "an admin and a customer test account are required");
        var roles = users.stream().map(DemoUsersSeeder.Account::role).toList();
        assertTrue(roles.contains("ADMIN"), "admin test account missing");
        assertTrue(roles.contains("CUSTOMER"), "customer test account missing");
        for (var user : users) {
            assertTrue(user.email().matches("^[^@]+@[^@]+\\.[^@]+$"), "invalid email: " + user.email());
            assertTrue(user.password().length() >= 8, "password too short for the login form: " + user.email());
            assertNotNull(user.name());
        }
    }

    @Test void insertsOnlyAccountsThatAreNotThereYet() {
        var db = mock(JdbcTemplate.class);
        when(db.queryForList(anyString())).thenReturn(List.of(Map.of("email", "customer@buymore.test")));
        DemoUsersSeeder.sync(db, password -> "$2a$10$hash$" + password);
        verify(db, times(1)).update(anyString(), any(), any(), any(), any());
    }

    @Test void insertsNothingWhenBothAccountsExist() {
        var db = mock(JdbcTemplate.class);
        when(db.queryForList(anyString())).thenReturn(
            List.of(Map.of("email", "customer@buymore.test"), Map.of("email", "admin@buymore.test")));
        DemoUsersSeeder.sync(db, password -> "$2a$10$hash$" + password);
        verify(db, never()).update(anyString(), any(), any(), any(), any());
    }
}
