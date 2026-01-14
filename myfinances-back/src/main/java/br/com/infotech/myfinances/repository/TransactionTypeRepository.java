package br.com.infotech.myfinances.repository;

import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeStatus;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {

        @Query("SELECT t FROM TransactionType t WHERE " +
                        "t.user = :user AND " +
                        "t.status = :status AND " +
                        "(:description IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:description AS text), '%'))) AND "
                        +
                        "t.type IN :types")
        Page<TransactionType> search(
                        @Param("user") User user,
                        @Param("status") TransactionTypeStatus status,
                        @Param("description") String description,
                        @Param("types") List<TransactionTypeType> types,
                        Pageable pageable);
}
