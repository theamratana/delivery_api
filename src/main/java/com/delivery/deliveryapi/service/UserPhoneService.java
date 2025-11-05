package com.delivery.deliveryapi.service;

import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserPhone;
import com.delivery.deliveryapi.repo.UserPhoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserPhoneService {
    private final UserPhoneRepository repo;

    public UserPhoneService(UserPhoneRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UserPhone addPhone(User user, String phoneE164, boolean primary) {
        UserPhone up = new UserPhone();
        up.setUser(user);
        up.setPhoneE164(phoneE164);
        up.setPrimary(false);
        up = repo.save(up);
        if (primary) {
            setPrimary(user.getId(), up.getId());
        }
        return up;
    }

    @Transactional
    public void setPrimary(UUID userId, UUID phoneId) {
        List<UserPhone> phones = repo.findByUserId(userId);
        for (UserPhone p : phones) {
            boolean isTarget = p.getId().equals(phoneId);
            if (p.isPrimary() != isTarget) {
                p.setPrimary(isTarget);
                repo.save(p);
            }
        }
    }
}
