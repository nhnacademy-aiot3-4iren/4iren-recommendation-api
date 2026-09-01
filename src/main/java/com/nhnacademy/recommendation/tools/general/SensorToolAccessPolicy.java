package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.dto.UserRole;
import org.springframework.stereotype.Component;

@Component
public class SensorToolAccessPolicy {

    public boolean canRead(UserRole role) {
        return role == UserRole.OWNER || role == UserRole.ADMIN;
    }
}
