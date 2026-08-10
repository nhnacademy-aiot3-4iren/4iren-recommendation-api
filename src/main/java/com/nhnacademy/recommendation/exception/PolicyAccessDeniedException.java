package com.nhnacademy.recommendation.exception;

public class PolicyAccessDeniedException extends RuntimeException {

    public PolicyAccessDeniedException(Long userId, Long teamId) {
        super("정책을 관리할 권한이 없습니다. userId:%s, teamId:%s".formatted(userId, teamId));
    }
}
