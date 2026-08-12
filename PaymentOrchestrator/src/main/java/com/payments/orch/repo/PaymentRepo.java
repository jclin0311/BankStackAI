package com.payments.orch.repo;

import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, UUID> {
  Optional<Payment> findByIdempotencyKey(String idemKey);
  List<Payment> findAllByBatchId(UUID batchId);

  @Modifying
  @Query("update Payment p set p.state=?2, p.updatedAt=current_timestamp where p.paymentId=?1")
  int updateState(String paymentId, PaymentState state);
}
