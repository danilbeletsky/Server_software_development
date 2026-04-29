package com.taxi.trip;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class TripServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }

    @Bean
    Queue notificationQueue() { return new Queue("notification.tasks", true); }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    RestClient restClient() {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(3000);
        return RestClient.builder().requestFactory(f).build();
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

enum TripStatus {CREATED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED}

@Entity
@Table(name = "trips")
class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long passengerId;
    Long driverId;
    @Enumerated(EnumType.STRING)
    TripStatus status = TripStatus.CREATED;
    String origin;
    String destination;
    BigDecimal price;
    Integer rating;
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();
}

interface TripRepo extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    @Query("select count(t) from Trip t where t.createdAt >= ?1")
    long countSince(Instant from);
    @Query("select avg(t.price) from Trip t where t.createdAt >= ?1")
    BigDecimal avgPriceSince(Instant from);
}

record CreateTripReq(Long passenger_id, @NotBlank String origin, @NotBlank String destination, Double distance) {}
record UpdateStatusReq(TripStatus status) {}
record RateReq(@Min(1) @Max(5) Integer stars) {}
record TokenReq(String subject, String role) {}
record TokenResp(String token) {}
record DriverDto(Long id, String name, String email, String phone, String licenseNumber, String status) {}
record NotificationMsg(Long tripId, String recipientType, Long recipientId, String message) {}
record StatsResp(long tripsToday, BigDecimal averagePriceToday) {}

@Service
class JwtService {
    private final SecretKey key = Keys.hmacShaKeyFor("very-secret-jwt-key-very-secret-jwt-key-123".getBytes());
    String create(String subject, String role) { return Jwts.builder().subject(subject).claim("role", role).signWith(key).compact(); }
    Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}

@Service
class TripDomainService {
    private final TripRepo tripRepo;
    private final RestClient rest;
    private final RabbitTemplate rabbit;
    private final JwtService jwt;
    private final BigDecimal tariff = new BigDecimal("15.0");

    TripDomainService(TripRepo tripRepo, RestClient rest, RabbitTemplate rabbit, JwtService jwt) {
        this.tripRepo = tripRepo;
        this.rest = rest;
        this.rabbit = rabbit;
        this.jwt = jwt;
    }

    @Transactional
    Trip create(CreateTripReq req) {
        String token = jwt.create("trip-service", "SERVICE");
        Boolean exists = rest.get().uri("http://localhost:8081/internal/passengers/" + req.passenger_id() + "/exists")
                .header("Authorization", "Bearer " + token).retrieve().body(Boolean.class);
        if (exists == null || !exists) throw new RuntimeException("Passenger not found");
        DriverDto driver = rest.post().uri("http://localhost:8081/internal/drivers/assign")
                .header("Authorization", "Bearer " + token).retrieve().body(DriverDto.class);
        if (driver == null) throw new RuntimeException("No available driver");
        Trip t = new Trip();
        t.passengerId = req.passenger_id();
        t.driverId = driver.id();
        t.origin = req.origin();
        t.destination = req.destination();
        double distance = req.distance() == null ? 5.0 : req.distance();
        t.price = BigDecimal.valueOf(distance).multiply(tariff);
        t.status = TripStatus.ACCEPTED;
        Trip saved = tripRepo.save(t);
        rabbit.convertAndSend("notification.tasks", new NotificationMsg(saved.id, "PASSENGER", saved.passengerId, "Trip accepted"));
        rabbit.convertAndSend("notification.tasks", new NotificationMsg(saved.id, "DRIVER", saved.driverId, "New trip assigned"));
        return saved;
    }

    @Transactional
    Trip updateStatus(Long id, TripStatus status) {
        Trip t = tripRepo.findById(id).orElseThrow();
        t.status = status;
        t.updatedAt = Instant.now();
        Trip saved = tripRepo.save(t);
        if (status == TripStatus.COMPLETED) {
            String token = jwt.create("trip-service", "SERVICE");
            rest.patch().uri("http://localhost:8081/drivers/" + t.driverId + "/status")
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("status", "AVAILABLE"))
                    .retrieve().toBodilessEntity();
        }
        rabbit.convertAndSend("notification.tasks", new NotificationMsg(saved.id, "PASSENGER", saved.passengerId, "Trip status " + status));
        return saved;
    }
}

@RestController
class ApiController {
    private final TripDomainService service;
    private final TripRepo tripRepo;
    private final JwtService jwt;
    ApiController(TripDomainService service, TripRepo tripRepo, JwtService jwt) {
        this.service = service; this.tripRepo = tripRepo; this.jwt = jwt;
    }
    @PostMapping("/auth/token")
    TokenResp token(@RequestBody TokenReq req) { return new TokenResp(jwt.create(req.subject(), req.role())); }
    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    Trip create(@RequestBody CreateTripReq req) { return service.create(req); }
    @GetMapping("/trips/{id}")
    Trip byId(@PathVariable Long id) { return tripRepo.findById(id).orElseThrow(); }
    @GetMapping("/trips")
    List<Trip> byPassenger(@RequestParam("passenger_id") Long pid) { return tripRepo.findByPassengerIdOrderByCreatedAtDesc(pid); }
    @PatchMapping("/trips/{id}/status")
    Trip updateStatus(@PathVariable Long id, @RequestBody UpdateStatusReq req) { return service.updateStatus(id, req.status()); }
    @PostMapping("/trips/{id}/rating")
    Trip rate(@PathVariable Long id, @RequestBody RateReq req) {
        Trip t = tripRepo.findById(id).orElseThrow();
        t.rating = req.stars();
        t.updatedAt = Instant.now();
        return tripRepo.save(t);
    }
    @GetMapping("/stats")
    StatsResp stats() {
        Instant from = Instant.now().minusSeconds(86400);
        return new StatsResp(tripRepo.countSince(from), tripRepo.avgPriceSince(from));
    }
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
