package com.buymore;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final HttpSessionSecurityContextRepository contexts;
    private final String invitation;
    private final String dummyHash;
    AuthController(JdbcTemplate db, PasswordEncoder passwords, HttpSessionSecurityContextRepository contexts, @Value("${buymore.admin-invitation}") String invitation) {
        this.db=db; this.passwords=passwords; this.contexts=contexts; this.invitation=invitation;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public record Registration(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=72) String password, @Pattern(regexp="CUSTOMER|ADMIN") @NotNull String role, String invitationCode) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=72) String password, @NotNull @Pattern(regexp="CUSTOMER|ADMIN") String role) {}
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","ok"); }
    @GetMapping("/auth/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/auth/register") @ResponseStatus(CREATED)
    Map<String,Object> register(@Valid @RequestBody Registration input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(BAD_REQUEST,"Password is too long.");
        if (input.role().equals("ADMIN") && (invitation.isBlank() || input.invitationCode()==null || !MessageDigest.isEqual(invitation.getBytes(StandardCharsets.UTF_8), input.invitationCode().getBytes(StandardCharsets.UTF_8))))
            throw new ResponseStatusException(FORBIDDEN,"A valid admin invitation code is required.");
        db.update("INSERT INTO users(name,email,password_hash,role) VALUES (?,?,?,?)",input.name().trim(),input.email().trim().toLowerCase(Locale.ROOT),passwords.encode(input.password()),input.role());
        return Map.of("ok",true);
    }
    @PostMapping("/auth/login") Map<String,Object> login(@Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
        var rows=db.queryForList("SELECT * FROM users WHERE lower(email)=?", input.email().trim().toLowerCase(Locale.ROOT));
        var user=rows.isEmpty() ? null : rows.getFirst();
        String hash=user==null || user.get("password_hash")==null ? dummyHash : user.get("password_hash").toString();
        boolean matches=passwords.matches(input.password(),hash);
        if (!matches || user==null || user.get("password_hash")==null || !input.role().equals(user.get("role")))
            throw new ResponseStatusException(UNAUTHORIZED,"Email, password, or account type is incorrect.");
        if(request.getSession(false)!=null) request.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.get("id").toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.get("role")))));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context,request,response);
        new HttpSessionCsrfTokenRepository().saveToken(null,request,response);
        return Map.of("user",publicUser(user));
    }
    @GetMapping("/auth/me") Map<String,Object> me(Authentication auth) {
        return publicUser(db.queryForMap("SELECT * FROM users WHERE id=?",Long.parseLong(auth.getName())));
    }
    @PostMapping("/auth/logout") Map<String,Boolean> logout(HttpServletRequest request) {
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext(); return Map.of("ok",true);
    }
    static Map<String,Object> publicUser(Map<String,Object> user) {return Map.of("id",user.get("id"),"name",user.get("name"),"email",user.get("email"),"role",user.get("role"));}
    static boolean isAdmin(Authentication a) {return a!=null && a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN"));}
}
