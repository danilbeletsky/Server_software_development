package com.taxi.notification;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/token").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

enum NotificationStatus {PENDING, PROCESSING, SENT, FAILED}

@Entity
@Table(name = "notification_tasks")
class NotificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long tripId;
    String recipientType;
    Long recipientId;
    String message;
    @Enumerated(EnumType.STRING)
    NotificationStatus status = NotificationStatus.PENDING;
    int attempts = 0;
    Instant createdAt = Instant.now();
}

interface NotificationRepo extends JpaRepository<NotificationTask, Long> {
    List<NotificationTask> findByTripIdOrderByIdAsc(Long tripId);
    @Query(value = "select id from notification_tasks where status='PENDING' and attempts < 3 order by id for update skip locked limit 1", nativeQuery = true)
    Optional<Long> lockPendingId();
}

record TokenReq(String subject, String role) {}
record TokenResp(String token) {}
record NotificationMsg(Long tripId, String recipientType, Long recipientId, String message) {}
record CreateReq(Long trip_id, String recipient_type, Long recipient_id, String message) {}

@Service
class JwtService {
    private final SecretKey key = Keys.hmacShaKeyFor("very-secret-jwt-key-very-secret-jwt-key-123".getBytes());
    String create(String subject, String role) { return Jwts.builder().subject(subject).claim("role", role).signWith(key).compact(); }
    Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}

@Service
class NotificationWorkerService {
    private final NotificationRepo repo;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;

    NotificationWorkerService(NotificationRepo repo) { this.repo = repo; }

    @PostConstruct
    void start() {
        for (int i = 0; i < 4; i++) executor.submit(this::workLoop);
    }

    private void workLoop() {
        while (running) {
            try {
                Optional<NotificationTask> task = claimOne();
                if (task.isEmpty()) {
                    Thread.sleep(300);
                    continue;
                }
                process(task.get());
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    Optional<NotificationTask> claimOne() {
        Optional<Long> id = repo.lockPendingId();
        if (id.isEmpty()) return Optional.empty();
        NotificationTask t = repo.findById(id.get()).orElseThrow();
        t.status = NotificationStatus.PROCESSING;
        return Optional.of(repo.save(t));
    }

    void process(NotificationTask task) {
        try {
            Thread.sleep(600);
            task.status = NotificationStatus.SENT;
            repo.save(task);
        } catch (Exception e) {
            task.attempts = task.attempts + 1;
            task.status = task.attempts >= 3 ? NotificationStatus.FAILED : NotificationStatus.PENDING;
            repo.save(task);
        }
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        running = false;
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}

@RestController
class ApiController {
    private final NotificationRepo repo;
    private final JwtService jwt;
    ApiController(NotificationRepo repo, JwtService jwt) { this.repo = repo; this.jwt = jwt; }
    @PostMapping("/auth/token")
    TokenResp token(@RequestBody TokenReq req) { return new TokenResp(jwt.create(req.subject(), req.role())); }
    @PostMapping("/notifications")
    NotificationTask create(@RequestBody CreateReq req) {
        NotificationTask t = new NotificationTask();
        t.tripId = req.trip_id();
        t.recipientType = req.recipient_type();
        t.recipientId = req.recipient_id();
        t.message = req.message();
        return repo.save(t);
    }
    @GetMapping("/notifications")
    List<NotificationTask> byTrip(@RequestParam("trip_id") Long tripId) { return repo.findByTripIdOrderByIdAsc(tripId); }
}

@RestController
class QueueConsumer {
    private final NotificationRepo repo;
    QueueConsumer(NotificationRepo repo) { this.repo = repo; }
    @RabbitListener(queues = "notification.tasks")
    public void consume(NotificationMsg msg) {
        NotificationTask t = new NotificationTask();
        t.tripId = msg.tripId();
        t.recipientType = msg.recipientType();
        t.recipientId = msg.recipientId();
        t.message = msg.message();
        repo.save(t);
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
