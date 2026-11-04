package com.computer.lub.backend.security;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.Transactional; // <--- ДОБАВЛЕН ИМПОРТ
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Transactional // <--- ДОБАВЛЕНА АННОТАЦИЯ
    public CommandLineRunner initData(UserRepository userRepository,
                                      PcRepository pcRepository,
                                      BookingRepository bookingRepository,
                                      SessionRepository sessionRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Создаем пользователей
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }
            if (userRepository.findByUsername("client").isEmpty()) {
                User client = new User();
                client.setUsername("client");
                client.setPassword(passwordEncoder.encode("client"));
                client.setRole("USER");
                userRepository.save(client);
            }

            // 2. Создаем карту ПК
            if (pcRepository.count() == 0) {
                int[][] layout = {
                    {1, 1}, {1, 2}, {1, 3}, {1, 5}, {1, 6}, {1, 7},
                    {2, 1}, {2, 2}, {2, 4}, {2, 5}, {2, 7}, {2, 8},
                    {3, 1}, {3, 2}, {3, 3}, {3, 4}, {3, 5}, {3, 6}, {3, 7}, {3, 8},
                    {4, 2}, {4, 3}, {4, 4}, {4, 5}, {4, 6}, {4, 7},
                    {5, 1}, {5, 2}, {5, 3}, {5, 5}, {5, 6}, {5, 7}, {5, 8},
                    {6, 1}, {6, 3}, {6, 4}, {6, 5}, {6, 6}, {6, 8},
                    {7, 2}, {7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7}
                };

                int pcCounter = 1;
                for (int[] pos : layout) {
                    Pc pc = new Pc();
                    pc.setRowNumber(pos[0]);
                    pc.setSeatNumber(pos[1]);

                    String randomPart = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    pc.setPcNumber("PC-" + pcCounter + randomPart);

                    if (pos[0] <= 2) {
                        pc.setProcessor("Intel Core i5-12400");
                        pc.setRam("16 GB");
                        pc.setGpu("NVIDIA RTX 3060");
                    } else {
                        pc.setProcessor("Intel Core i7-12700");
                        pc.setRam("32 GB");
                        pc.setGpu("NVIDIA RTX 4070 Ti");
                    }

                    if (pcCounter == 3 || pcCounter == 15 || pcCounter == 32) {
                        pc.setStatus("REPAIR");
                        pc.setComment("Не работает клавиатура, требуется замена.");
                    } else if (pcCounter % 7 == 0 || pcCounter == 10 || pcCounter == 22) {
                        pc.setStatus("BOOKED");
                        pc.setComment("");
                    } else {
                        pc.setStatus("FREE");
                        pc.setComment("");
                    }

                    pcRepository.save(pc);
                    pcCounter++;
                }
            }

            // 3. Добавляем тестовые данные
            if (bookingRepository.count() == 0 && sessionRepository.count() == 0) {
                User client = userRepository.findByUsername("client").orElse(null);
                List<Pc> allPcs = pcRepository.findAll();

                if (client != null && allPcs.size() >= 10) {

                    // --- БРОНИРОВАНИЯ ---
                    Booking pendingBooking = new Booking();
                    pendingBooking.setPc(allPcs.get(0));
                    pendingBooking.setUser(client);
                    pendingBooking.setStartTime(LocalDateTime.now().plusHours(1));
                    pendingBooking.setEndTime(LocalDateTime.now().plusHours(3));
                    pendingBooking.setStatus("PENDING");
                    bookingRepository.save(pendingBooking);

                    Booking confirmedBooking = new Booking();
                    confirmedBooking.setPc(allPcs.get(6));
                    confirmedBooking.setUser(client);
                    confirmedBooking.setStartTime(LocalDateTime.now().plusHours(2));
                    confirmedBooking.setEndTime(LocalDateTime.now().plusHours(4));
                    confirmedBooking.setStatus("CONFIRMED");
                    bookingRepository.save(confirmedBooking);

                    Booking cancelledBooking = new Booking();
                    cancelledBooking.setPc(allPcs.get(1));
                    cancelledBooking.setUser(client);
                    cancelledBooking.setStartTime(LocalDateTime.now().minusDays(1));
                    cancelledBooking.setEndTime(LocalDateTime.now().minusDays(1).plusHours(2));
                    cancelledBooking.setStatus("CANCELLED");
                    bookingRepository.save(cancelledBooking);

                    Session activeSession = new Session();
                    activeSession.setPc(allPcs.get(9));
                    activeSession.setUser(client);
                    activeSession.setStartTime(LocalDateTime.now().minusHours(1));
                    activeSession.setStatus("ACTIVE");
                    sessionRepository.save(activeSession);

                    Session finishedSession = new Session();
                    finishedSession.setPc(allPcs.get(3));
                    finishedSession.setUser(client);
                    finishedSession.setStartTime(LocalDateTime.now().minusDays(1).minusHours(3));
                    finishedSession.setEndTime(LocalDateTime.now().minusDays(1).minusHours(1));
                    finishedSession.setStatus("FINISHED");
                    sessionRepository.save(finishedSession);
                }
            }
        };
    }
}
