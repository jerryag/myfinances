package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.TransactionMonth;
import br.com.infotech.myfinances.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionMonthRepository extends JpaRepository<TransactionMonth, Long> {
    Optional<TransactionMonth> findByUserAndMonthAndYear(User user, Integer month, Integer year);
}
