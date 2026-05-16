package com.momently.orchestrator.adapter.out.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("postgres")
interface JpaUserAccountSpringDataRepository extends JpaRepository<UserAccountJpaEntity, String> {
}
