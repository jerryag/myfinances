package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.Transaction;
import br.com.infotech.myfinances.domain.TransactionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {
  List<TransactionDetail> findByTransactionOrderByDetailDateAscAmountAscDescriptionAsc(Transaction transaction);
  void deleteByTransaction(Transaction transaction);
}
