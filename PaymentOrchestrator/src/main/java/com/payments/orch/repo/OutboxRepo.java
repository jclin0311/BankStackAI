package com.payments.orch.repo;

import com.payments.orch.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxRepo extends JpaRepository<Outbox, Long> {
  List<Outbox> findTop200ByStateOrderByIdAsc(String state);
}
