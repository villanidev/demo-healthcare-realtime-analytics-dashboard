package dev.healthcare.analytics.platform.appschema.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppOutboxEventRepository extends JpaRepository<AppOutboxEvent, Long> {

    List<AppOutboxEvent> findTop200ByIdGreaterThanOrderByIdAsc(Long id);
}
