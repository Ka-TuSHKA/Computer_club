package com.computer.lub.backend.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.BookingStatus;
import com.computer.lub.backend.entity.Pc;
import com.computer.lub.backend.entity.PcStatus;
import com.computer.lub.backend.entity.Role;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.SessionStatus;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.PcRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Управление ПК (создание, редактирование, удаление) - только админ
                .requestMatchers(HttpMethod.POST, "/api/pcs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/pcs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/pcs/**").hasRole("ADMIN")

                // Подтверждать бронь может только админ
                .requestMatchers(HttpMethod.PUT, "/api/bookings/*/confirm").hasRole("ADMIN")

                // Запуск и завершение сеансов - только админ
                .requestMatchers(HttpMethod.POST, "/api/sessions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/sessions/**").hasRole("ADMIN")

                // Список пользователей - только админ
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Остальные запросы требуют аутентификации
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
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Transactional
    public CommandLineRunner initData(UserRepository userRepository, PcRepository pcRepository,
                                      BookingRepository bookingRepository, SessionRepository sessionRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            }
            if (userRepository.findByUsername("client").isEmpty()) {
                User client = new User();
                client.setUsername("client");
                client.setPassword(passwordEncoder.encode("client"));
                client.setRole(Role.USER);
                userRepository.save(client);
            }

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
                    String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    pc.setPcNumber("PC-" + pcCounter + suffix);
                    pc.setProcessor(pos[0] <= 2 ? "Intel Core i5-12400" : "Intel Core i7-12700");
                    pc.setRam(pos[0] <= 2 ? "16 GB" : "32 GB");
                    pc.setGpu(pos[0] <= 2 ? "NVIDIA RTX 3060" : "NVIDIA RTX 4070 Ti");
                    pc.setStatus(pcCounter == 3 ? PcStatus.REPAIR : PcStatus.FREE);
                    pc.setComment(pcCounter == 3 ? "Не работает клавиатура" : "");
                    pcRepository.save(pc);
                    pcCounter++;
                }
            }

        };
    }
}
