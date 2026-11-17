package com.computer.lub.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.computer.lub.backend.dto.UserDto;
import com.computer.lub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole().name()))
                .toList();
    }
}
