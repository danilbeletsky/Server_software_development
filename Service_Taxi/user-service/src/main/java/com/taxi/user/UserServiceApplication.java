package com.taxi.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/token").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

@Entity
@Table(name = "passengers")
class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String email;
    String phone;
    Instant createdAt = Instant.now();
}

enum DriverStatus {AVAILABLE, BUSY, OFFLINE}

@Entity
@Table(name = "drivers")
class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    String email;
    String phone;
    String licenseNumber;
    @Enumerated(EnumType.STRING)
    DriverStatus status = DriverStatus.AVAILABLE;
    Instant createdAt = Instant.now();
}

interface PassengerRepo extends JpaRepository<Passenger, Long> {}

interface DriverRepo extends JpaRepository<Driver, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.status='AVAILABLE' order by d.id asc")
    List<Driver> findAvailableForUpdate();
}

record PassengerReq(@NotBlank String name, @NotBlank String email, @NotBlank String phone) {}
record DriverReq(@NotBlank String name, @NotBlank String email, @NotBlank String phone, @NotBlank String licenseNumber) {}
record StatusReq(DriverStatus status) {}
record TokenReq(String subject, String role) {}
record TokenResp(String token) {}

@Service
class JwtService {
    private final SecretKey key = Keys.hmacShaKeyFor("very-secret-jwt-key-very-secret-jwt-key-123".getBytes());
    String create(String subject, String role) {
        return Jwts.builder().subject(subject).claim("role", role).signWith(key).compact();
    }
    Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

@Service
class UserDomainService {
    private final PassengerRepo passengerRepo;
    private final DriverRepo driverRepo;
    private final StringRedisTemplate redis;

    UserDomainService(PassengerRepo passengerRepo, DriverRepo driverRepo, StringRedisTemplate redis) {
        this.passengerRepo = passengerRepo;
        this.driverRepo = driverRepo;
        this.redis = redis;
    }

    Passenger savePassenger(PassengerReq req) {
        Passenger p = new Passenger();
        p.name = req.name();
        p.email = req.email();
        p.phone = req.phone();
        return passengerRepo.save(p);
    }

    Driver saveDriver(DriverReq req) {
        Driver d = new Driver();
        d.name = req.name();
        d.email = req.email();
        d.phone = req.phone();
        d.licenseNumber = req.licenseNumber();
        Driver saved = driverRepo.save(d);
        redis.delete("drivers:available");
        return saved;
    }

    Driver setStatus(Long id, DriverStatus status) {
        Driver d = driverRepo.findById(id).orElseThrow();
        d.status = status;
        Driver saved = driverRepo.save(d);
        redis.delete("drivers:available");
        return saved;
    }

    @Transactional
    Optional<Driver> assignDriver() {
        List<Driver> list = driverRepo.findAvailableForUpdate();
        if (list.isEmpty()) return Optional.empty();
        Driver d = list.get(0);
        d.status = DriverStatus.BUSY;
        redis.delete("drivers:available");
        return Optional.of(d);
    }
}

@RestController
class ApiController {
    private final UserDomainService service;
    private final PassengerRepo passengerRepo;
    private final DriverRepo driverRepo;
    private final JwtService jwt;

    ApiController(UserDomainService service, PassengerRepo passengerRepo, DriverRepo driverRepo, JwtService jwt) {
        this.service = service;
        this.passengerRepo = passengerRepo;
        this.driverRepo = driverRepo;
        this.jwt = jwt;
    }

    @PostMapping("/auth/token")
    TokenResp token(@RequestBody TokenReq req) { return new TokenResp(jwt.create(req.subject(), req.role())); }

    @PostMapping("/passengers")
    Passenger createPassenger(@RequestBody PassengerReq req) { return service.savePassenger(req); }

    @GetMapping("/passengers/{id}")
    Passenger getPassenger(@PathVariable Long id) { return passengerRepo.findById(id).orElseThrow(); }

    @GetMapping("/internal/passengers/{id}/exists")
    boolean passengerExists(@PathVariable Long id) { return passengerRepo.existsById(id); }

    @PostMapping("/drivers")
    Driver createDriver(@RequestBody DriverReq req) { return service.saveDriver(req); }

    @GetMapping("/drivers/{id}")
    Driver getDriver(@PathVariable Long id) { return driverRepo.findById(id).orElseThrow(); }

    @PatchMapping("/drivers/{id}/status")
    Driver updateStatus(@PathVariable Long id, @RequestBody StatusReq req) { return service.setStatus(id, req.status()); }

    @PostMapping("/internal/drivers/assign")
    @ResponseStatus(HttpStatus.OK)
    Driver assignDriver() { return service.assignDriver().orElseThrow(() -> new RuntimeException("No available driver")); }
}

@Service
class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    JwtFilter(JwtService jwt) { this.jwt = jwt; }
    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            Claims claims = jwt.parse(h.substring(7));
            var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
