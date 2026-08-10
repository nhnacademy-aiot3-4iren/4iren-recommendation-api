package com.nhnacademy.recommendation.exception;

public class PolicyDuplicateException extends RuntimeException {
    public PolicyDuplicateException(Long teamId, Long roomId) {
        super("이미 존재하는 정책입니다. teamId:" + teamId + ", roomId:" + roomId);
    }
}
