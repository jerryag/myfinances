package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.*;
import br.com.infotech.myfinances.dto.TransactionDto;
import br.com.infotech.myfinances.dto.TransactionMonthDto;
import br.com.infotech.myfinances.repository.TransactionMonthRepository;
import br.com.infotech.myfinances.repository.TransactionRepository;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.infotech.myfinances.domain.TransactionTypeType;

@Service
@RequiredArgsConstructor
public class TransactionMonthService {

        private final TransactionMonthRepository transactionMonthRepository;
        private final TransactionRepository transactionRepository;
        private final TransactionTypeRepository transactionTypeRepository;
        private final UserService userService;

        @Transactional
        public TransactionMonthDto getOrCreateMonth(Integer month, Integer year) {
                User currentUser = userService.getCurrentUser();

                return transactionMonthRepository.findByUserAndMonthAndYear(currentUser, month, year)
                                .map(this::toDto)
                                .orElseGet(() -> createMonth(currentUser, month, year));
        }

        private TransactionMonthDto createMonth(User user, Integer month, Integer year) {
                TransactionMonth newMonth = TransactionMonth.builder()
                                .user(user)
                                .month(month)
                                .year(year)
                                .status("OPEN")
                                .initialBalance(BigDecimal.ZERO)
                                .build();

                TransactionMonth savedMonth = transactionMonthRepository.save(newMonth);

                // Auto-generate recurring transactions
                List<TransactionType> recurringTypes = transactionTypeRepository.findAll().stream()
                                .filter(t -> t.getUser().getId().equals(user.getId())
                                                && Boolean.TRUE.equals(t.getRecurring())
                                                && t.getStatus() == TransactionTypeStatus.ACTIVE)
                                .collect(Collectors.toList());

                List<Transaction> initialTransactions = new ArrayList<>();
                for (TransactionType type : recurringTypes) {

                        // Logic for Recurring Date
                        // Default to type's default day, or 1 if not set
                        int dayToUse = type.getDefaultDay() != null ? type.getDefaultDay() : 1;

                        // Ensure day is valid for this month
                        int maxDayInNewMonth = LocalDate.of(year, month, 1).lengthOfMonth();
                        if (dayToUse > maxDayInNewMonth) {
                                dayToUse = maxDayInNewMonth;
                        }

                        Transaction transaction = Transaction.builder()
                                        .user(user)
                                        .transactionType(type)
                                        .transactionMonth(savedMonth)
                                        .transactionDate(LocalDate.of(year, month, dayToUse))
                                        .description("")
                                        .amount(BigDecimal.ZERO)
                                        .status(TransactionStatus.PENDING)
                                        .createdAt(java.time.LocalDateTime.now())
                                        .build();
                        initialTransactions.add(transactionRepository.save(transaction));
                }

                return toDto(savedMonth, initialTransactions);
        }

        @Transactional
        public TransactionMonthDto updateInitialBalance(Long id, BigDecimal initialBalance) {
                TransactionMonth transactionMonth = transactionMonthRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Mês não encontrado"));

                // Optimize: Check user ownership

                transactionMonth.setInitialBalance(initialBalance);
                return toDto(transactionMonthRepository.save(transactionMonth));
        }

        @Transactional
        public TransactionMonthDto addTransaction(Long monthId, TransactionDto dto) {
                TransactionMonth month = transactionMonthRepository.findById(monthId)
                                .orElseThrow(() -> new RuntimeException("Mês não encontrado"));

                TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                .orElseThrow(() -> new RuntimeException("Tipo de Transação não encontrado"));

                Transaction transaction = Transaction.builder()
                                .user(month.getUser())
                                .transactionMonth(month)
                                .transactionType(type)
                                .transactionDate(LocalDate.of(month.getYear(), month.getMonth(), dto.getDay()))
                                .description(dto.getDescription())
                                .amount(dto.getAmount())
                                .status(dto.getStatus())
                                .remark(dto.getRemark())
                                .build();

                transactionRepository.save(transaction);
                return toDto(month);
        }

        @Transactional
        public TransactionMonthDto updateTransaction(Long transactionId, TransactionDto dto) {
                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

                TransactionType type = transactionTypeRepository.findById(dto.getTransactionTypeId())
                                .orElseThrow(() -> new RuntimeException("Tipo de Transação não encontrado"));

                // Update fields
                transaction.setTransactionType(type);
                transaction.setTransactionDate(LocalDate.of(transaction.getTransactionMonth().getYear(),
                                transaction.getTransactionMonth().getMonth(), dto.getDay()));
                transaction.setDescription(dto.getDescription());
                transaction.setAmount(dto.getAmount());
                transaction.setStatus(dto.getStatus());
                transaction.setRemark(dto.getRemark());

                transactionRepository.save(transaction);
                return toDto(transaction.getTransactionMonth());
        }

        @Transactional
        public TransactionMonthDto deleteTransaction(Long transactionId) {
                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

                TransactionMonth month = transaction.getTransactionMonth();
                transactionRepository.delete(transaction);

                return toDto(month);
        }

        public BigDecimal getLastTransactionValue(Long transactionTypeId, String description) {
                User currentUser = userService.getCurrentUser();
                TransactionType type = transactionTypeRepository.findById(transactionTypeId)
                                .orElseThrow(() -> new RuntimeException("Tipo de Transação não encontrado"));

                Optional<Transaction> transaction = Optional.empty();

                // 1. Try with Exact Description
                if (description != null && !description.trim().isEmpty()) {
                        transaction = transactionRepository
                                        .findFirstByUserAndTransactionTypeAndDescriptionAndAmountGreaterThanOrderByTransactionDateDesc(
                                                        currentUser, type, description.trim(), BigDecimal.ZERO);
                }

                // 2. Fallback to just Type
                if (transaction.isEmpty()) {
                        transaction = transactionRepository
                                        .findFirstByUserAndTransactionTypeAndAmountGreaterThanOrderByTransactionDateDesc(
                                                        currentUser, type, BigDecimal.ZERO);
                }

                return transaction.map(Transaction::getAmount).orElse(BigDecimal.ZERO);
        }
        // TODO: Add Transaction CRUD methods here (add, update, delete transaction
        // line)

        private TransactionMonthDto toDto(TransactionMonth entity) {
                List<Transaction> transactions = transactionRepository
                                .findByTransactionMonthOrderByTransactionDateAsc(entity);

                // Custom Sort: Day ASC (already from DB), then Type (Income before Expense)
                transactions.sort((t1, t2) -> {
                        int dateCompare = t1.getTransactionDate().compareTo(t2.getTransactionDate());
                        if (dateCompare != 0)
                                return dateCompare;

                        // If same date, INCOME comes before EXPENSE
                        boolean t1IsIncome = t1.getTransactionType().getType() == TransactionTypeType.INCOME;
                        boolean t2IsIncome = t2.getTransactionType().getType() == TransactionTypeType.INCOME;

                        if (t1IsIncome && !t2IsIncome)
                                return -1;
                        if (!t1IsIncome && t2IsIncome)
                                return 1;
                        return 0;
                });

                return toDto(entity, transactions);
        }

        private TransactionMonthDto toDto(TransactionMonth entity, List<Transaction> transactions) {
                List<TransactionDto> transactionDtos = transactions.stream()
                                .map(t -> TransactionDto.builder()
                                                .id(t.getId())
                                                .day(t.getTransactionDate().getDayOfMonth())
                                                .transactionTypeId(t.getTransactionType().getId())
                                                .description(t.getDescription())
                                                .amount(t.getAmount())
                                                .amount(t.getAmount())
                                                .status(t.getStatus())
                                                .remark(t.getRemark())
                                                .build())
                                .collect(Collectors.toList());

                return TransactionMonthDto.builder()
                                .id(entity.getId())
                                .month(entity.getMonth())
                                .year(entity.getYear())
                                .status(entity.getStatus())
                                .initialBalance(entity.getInitialBalance())
                                .transactions(transactionDtos)
                                .build();
        }
}
