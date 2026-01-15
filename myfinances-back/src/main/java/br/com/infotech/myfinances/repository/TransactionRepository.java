package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.Transaction;

import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTransactionMonthOrderByTransactionDateAsc(TransactionMonth transactionMonth);

    Optional<Transaction> findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(User user,
            TransactionType transactionType, java.math.BigDecimal amount);

    Optional<Transaction> findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(
            User user, TransactionType transactionType, String description, java.math.BigDecimal amount);
}
