package com.computer.lub.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.computer.lub.backend.dto.BookingResponse;
import com.computer.lub.backend.dto.ProfileResponse;
import com.computer.lub.backend.dto.SessionResponse;
import com.computer.lub.backend.dto.UserDto;
import com.computer.lub.backend.entity.Booking;
import com.computer.lub.backend.entity.Session;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.BookingRepository;
import com.computer.lub.backend.repository.SessionRepository;
import com.computer.lub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ProfileResponse response = new ProfileResponse();
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());

        List<Booking> bookings = bookingRepository.findByUserUsernameOrderByStartTimeDesc(username);
        response.setBookings(bookings.stream().map(b -> {
            BookingResponse dto = new BookingResponse();
            dto.setId(b.getId());
            dto.setPcNumber(b.getPc().getPcNumber());
            dto.setUsername(b.getUser().getUsername());
            dto.setStartTime(b.getStartTime());
            dto.setEndTime(b.getEndTime());
            dto.setStatus(b.getStatus());
            return dto;
        }).collect(Collectors.toList()));

        List<Session> sessions = sessionRepository.findByUserUsernameOrderByStartTimeDesc(username);
        response.setSessions(sessions.stream().map(s -> {
            SessionResponse dto = new SessionResponse();
            dto.setId(s.getId());
            dto.setPcNumber(s.getPc().getPcNumber());
            dto.setUsername(s.getUser().getUsername());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setStatus(s.getStatus());
            return dto;
        }).collect(Collectors.toList()));

        return response;
    }
}
