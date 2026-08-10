package com.nhnacademy.recommendation.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(Long teamId, Long roomId) {
        super("정책을 찾을 수 없습니다. teamId:%s, roomId:%s".formatted(teamId, roomId));
    }
}
