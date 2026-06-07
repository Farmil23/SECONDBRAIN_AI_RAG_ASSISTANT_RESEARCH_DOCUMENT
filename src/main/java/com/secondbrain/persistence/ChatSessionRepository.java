package com.secondbrain.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    @Query("SELECT c FROM ChatSessionEntity c WHERE c.owner.id = :userId OR :userEntity MEMBER OF c.participants")
    List<ChatSessionEntity> findSessionsForUser(Long userId, UserEntity userEntity);
}
