package com.delivery.deliveryapi.controller;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserPhone;
import com.delivery.deliveryapi.repo.UserPhoneRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.UserPhoneService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final UserRepository userRepository;
    private final UserPhoneRepository userPhoneRepository;
    private final UserPhoneService userPhoneService;

    public UsersController(UserRepository userRepository,
                           UserPhoneRepository userPhoneRepository,
                           UserPhoneService userPhoneService) {
        this.userRepository = userRepository;
        this.userPhoneRepository = userPhoneRepository;
        this.userPhoneService = userPhoneService;
    }

    @GetMapping("{userId}/phones")
    public ResponseEntity<?> listPhones(@PathVariable UUID userId) {
        List<UserPhone> phones = userPhoneRepository.findByUserId(userId);
        return ResponseEntity.ok(phones);
    }

    public record AddPhoneRequest(@NotBlank(message = "phoneE164_required") String phoneE164,
                                  Boolean primary) {}

    @PostMapping("{userId}/phones")
    @Transactional
    public ResponseEntity<?> addPhone(@PathVariable UUID userId, @RequestBody @Valid AddPhoneRequest req) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) return ResponseEntity.notFound().build();
        if (userPhoneRepository.existsByPhoneE164(req.phoneE164)) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone_already_in_use"));
        }
        boolean makePrimary = req.primary != null && req.primary;
        UserPhone up = userPhoneService.addPhone(user.get(), req.phoneE164.trim(), makePrimary);
        return ResponseEntity.ok(up);
    }

    @PatchMapping("{userId}/phones/{phoneId}/primary")
    @Transactional
    public ResponseEntity<?> setPrimary(@PathVariable UUID userId, @PathVariable UUID phoneId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) return ResponseEntity.notFound().build();
        if (userPhoneRepository.findById(phoneId).isEmpty()) return ResponseEntity.notFound().build();
        userPhoneService.setPrimary(userId, phoneId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
