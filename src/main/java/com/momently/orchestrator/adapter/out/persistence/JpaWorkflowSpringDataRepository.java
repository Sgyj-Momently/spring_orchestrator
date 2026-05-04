package com.momently.orchestrator.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaWorkflowSpringDataRepository extends JpaRepository<WorkflowJpaEntity, UUID> {

    List<WorkflowJpaEntity> findAllByOrderByUpdatedAtDesc();
}
