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

import org.springframework.transaction.annotation.Propagation;
import br.com.infotech.myfinances.exception.BusinessException;
import br.com.infotech.myfinances.util.ValidationUtils;

@Service
@Transactional(propagation = Propagation.SUPPORTS)
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeService {

  private final TransactionTypeRepository transactionTypeRepository;
  private final UserService userService;

  /**
   * Busca os tipos de transação com suporte a filtros e paginação.
   *
   * @param description Filtro por descrição.
   * @param types Lista de tipos (INCOME, EXPENSE, etc) em string.
   * @param pageable Informações de paginação e ordenação da consulta.
   * @return Página de {@link TransactionTypeDto}.
   */
  public Page<TransactionTypeDto> findAll(String description, List<String> types, Pageable pageable) {
    log.debug("Finding all transaction types. Description: {}, Types: {}", description, types);

    List<TransactionTypeType> typeEnums;
    if (types != null && !types.isEmpty()) {
      typeEnums = types.stream().map(TransactionTypeType::valueOf).toList();
    } else {
      typeEnums = java.util.Arrays.asList(TransactionTypeType.values());
    }

    return transactionTypeRepository.search(userService.getCurrentUser(), TransactionTypeStatus.ACTIVE, description, typeEnums, pageable)
                                    .map(this::toDTO);
  }

  /**
   * Busca um tipo de transação ativo pelo ID.
   *
   * @param id ID do tipo de transação.
   * @return O tipo de transação em formato DTO.
   * @throws br.com.infotech.myfinances.exception.ValidationException se o ID não for informado.
   * @throws jakarta.persistence.EntityNotFoundException se não for encontrado.
   */
  public TransactionTypeDto findById(Long id) {
    ValidationUtils.notNull(id, "ID obrigatório");
    return toDTO(findByIdOrThrow(id));
  }

  /**
   * Cria um novo tipo de transação.
   *
   * @param dto DTO com os dados do novo tipo.
   * @return DTO correspondente ao tipo criado.
   * @throws br.com.infotech.myfinances.exception.ValidationException se campos obrigatórios não forem fornecidos.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionTypeDto create(TransactionTypeDto dto) {
    ValidationUtils.notNull(dto, "Objeto da transação é obrigatório");
    ValidationUtils.notNull(dto.getType(), "O tipo é obrigatório");
    ValidationUtils.hasText(dto.getDescription(), "A descrição é obrigatória");

    TransactionType entity = TransactionType.builder()
                                            .user(userService.getCurrentUser())
                                            .type(dto.getType())
                                            .description(dto.getDescription())
                                            .recurring(dto.getRecurring() != null ? dto.getRecurring() : false)
                                            .defaultDay(dto.getDefaultDay())
                                            .defaultAmount(dto.getDefaultAmount())
                                            .status(TransactionTypeStatus.ACTIVE)
                                            .iconName(dto.getIconName())
                                            .build();

    return toDTO(transactionTypeRepository.save(entity));
  }

  /**
   * Atualiza um tipo de transação ativo existente.
   *
   * @param id ID do tipo a atualizar.
   * @param dto Novos dados para o tipo de transação.
   * @return O tipo atualizado.
   * @throws br.com.infotech.myfinances.exception.ValidationException se id ou dto forem nulos, ou faltando informações obrigatórias.
   * @throws BusinessException se houver tentativa de alteração do Tipo fundamental (INCOME/EXPENSE).
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionTypeDto update(Long id, TransactionTypeDto dto) {
    ValidationUtils.notNull(id, "ID obrigatório");
    ValidationUtils.notNull(dto, "Objeto da transação é obrigatório");
    ValidationUtils.hasText(dto.getDescription(), "A descrição é obrigatória");

    TransactionType entity = findByIdOrThrow(id);

    // Algumas regras negociais: não altera o tipo
    if (dto.getType() != null && !dto.getType().equals(entity.getType())) {
      throw new BusinessException("O tipo de transação não pode ser alterado.");
    }

    entity.setDescription(dto.getDescription());
    entity.setRecurring(dto.getRecurring());
    entity.setDefaultDay(dto.getDefaultDay());
    entity.setDefaultAmount(dto.getDefaultAmount());
    entity.setIconName(dto.getIconName());

    return toDTO(transactionTypeRepository.save(entity));
  }

  /**
   * Remove (exclusão lógica) do tipo de transação.
   *
   * @param id ID do tipo a ser removido.
   * @throws br.com.infotech.myfinances.exception.ValidationException se o ID for nulo.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void delete(Long id) {
    ValidationUtils.notNull(id, "ID obrigatório");
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
                             .defaultDay(entity.getDefaultDay())
                             .defaultAmount(entity.getDefaultAmount())
                             .iconName(entity.getIconName())
                             .build();
  }
}
