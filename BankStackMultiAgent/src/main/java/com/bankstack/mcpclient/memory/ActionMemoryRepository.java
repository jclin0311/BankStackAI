package com.bankstack.mcpclient.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionMemoryRepository extends JpaRepository<ActionMemoryEntity, UUID> {

    List<ActionMemoryEntity> findTop10ByActorIdAndToolNameAndOutcomeOrderByCreatedAtDesc(
            String actorId,
            String toolName,
            String outcome
    );
}