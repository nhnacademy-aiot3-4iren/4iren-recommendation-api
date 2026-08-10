package com.nhnacademy.recommendation.repository;

import com.nhnacademy.recommendation.entity.WelcomeBriefingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WelcomeBriefingPolicyRepository extends JpaRepository<WelcomeBriefingPolicy, Long> {

    boolean existsByTeamIdAndRoomId(Long teamId, Long roomId);

    Optional<WelcomeBriefingPolicy> findByTeamIdAndRoomId(Long teamId, Long roomId);

    Optional<WelcomeBriefingPolicy> findByTeamIdAndRoomIdAndEnabledTrue(Long teamId, Long roomId);
}
