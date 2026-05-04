package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PostgresWorkflowRepository implements WorkflowRepository {

    private final JpaWorkflowSpringDataRepository repository;

    public PostgresWorkflowRepository(JpaWorkflowSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Workflow save(Workflow workflow) {
        WorkflowJpaEntity entity = repository.findById(workflow.getWorkflowId())
            .orElseGet(() -> WorkflowJpaEntity.fromDomain(workflow));
        entity.updateFrom(workflow);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findById(UUID workflowId) {
        return repository.findById(workflowId).map(WorkflowJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
            .map(WorkflowJpaEntity::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}
