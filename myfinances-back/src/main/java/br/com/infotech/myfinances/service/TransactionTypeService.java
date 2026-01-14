package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeStatus;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.dto.TransactionTypeDto;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<TransactionTypeDto> findAll(String description, List<String> types, Pageable pageable) {
        log.debug("Finding all transaction types. Description: {}, Types: {}", description, types);

        List<TransactionTypeType> typeEnums = null;
        if (types != null && !types.isEmpty()) {
            typeEnums = types.stream()
                    .map(TransactionTypeType::valueOf)
                    .toList();
        } else {
            typeEnums = java.util.Arrays.asList(TransactionTypeType.values());
        }

        return transactionTypeRepository.search(
                userService.getCurrentUser(),
                TransactionTypeStatus.ACTIVE,
                description,
                typeEnums,
                pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public TransactionTypeDto findById(Long id) {
        return toDTO(findByIdOrThrow(id));
    }

    @Transactional
    public TransactionTypeDto create(TransactionTypeDto dto) {
        TransactionType entity = TransactionType.builder()
                .user(userService.getCurrentUser())
                .type(dto.getType())
                .description(dto.getDescription())
                .recurring(dto.getRecurring())
                .status(TransactionTypeStatus.ACTIVE)
                .build();

        return toDTO(transactionTypeRepository.save(entity));
    }

    @Transactional
    public TransactionTypeDto update(Long id, TransactionTypeDto dto) {
        TransactionType entity = findByIdOrThrow(id);

        // Business Rule: Cannot change Type
        if (dto.getType() != null && !dto.getType().equals(entity.getType())) {
            throw new IllegalArgumentException("O tipo de transação não pode ser alterado.");
        }

        entity.setDescription(dto.getDescription());
        entity.setRecurring(dto.getRecurring());

        return toDTO(transactionTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        TransactionType entity = findByIdOrThrow(id);
        entity.setStatus(TransactionTypeStatus.DELETED);
        transactionTypeRepository.save(entity);
    }

    private TransactionType findByIdOrThrow(Long id) {
        return transactionTypeRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(userService.getCurrentUser().getId()))
                .filter(t -> t.getStatus() == TransactionTypeStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de transação não encontrado."));
    }

    private TransactionTypeDto toDTO(TransactionType entity) {
        return TransactionTypeDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .description(entity.getDescription())
                .recurring(entity.getRecurring())
                .build();
    }
}
