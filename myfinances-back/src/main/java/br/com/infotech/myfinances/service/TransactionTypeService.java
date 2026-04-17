package br.com.infotech.myfinances.service;

import br.com.infotech.myfinances.domain.TransactionType;
import br.com.infotech.myfinances.domain.TransactionTypeStatus;
import br.com.infotech.myfinances.domain.TransactionTypeType;
import br.com.infotech.myfinances.domain.User;
import br.com.infotech.myfinances.dto.TransactionTypeDto;
import br.com.infotech.myfinances.repository.TransactionTypeRepository;
import br.com.infotech.myfinances.exception.TransactionTypeNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import br.com.infotech.myfinances.exception.TransactionTypeChangeTypeException;
import br.com.infotech.myfinances.exception.ValidationException;
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
   * @param types       Lista de tipos (INCOME, EXPENSE, etc) em string.
   * @param pageable    Informações de paginação e ordenação da consulta.
   * @return Página de {@link TransactionTypeDto}.
   */
  public Page<TransactionTypeDto> findAll(String description, List<String> types, Pageable pageable) {
    List<TransactionTypeType> typeEnums;
    if (types != null && !types.isEmpty()) {
      typeEnums = types.stream().map(TransactionTypeType::valueOf).toList();
    } else {
      typeEnums = java.util.Arrays.asList(TransactionTypeType.values());
    }

    return transactionTypeRepository
        .search(userService.getCurrentUser(), TransactionTypeStatus.ACTIVE, description, typeEnums, pageable)
        .map(this::toDTO);
  }

  /**
   * Busca um tipo de transação ativo pelo ID.
   *
   * @param id ID do tipo de transação.
   * @return O tipo de transação em formato DTO.
   * @throws ValidationException     se o ID não for informado.
   * @throws EntityNotFoundException se não for encontrado.
   */
  @Cacheable(value = "transaction-types", key = "#id")
  public TransactionTypeDto findById(Long id) {
    ValidationUtils.notNull(id, "ID obrigatório");
    return toDTO(findByIdOrThrow(id));
  }

  /**
   * Cria um novo tipo de transação.
   *
   * @param dto DTO com os dados do novo tipo.
   * @return DTO correspondente ao tipo criado.
   * @throws ValidationException se campos obrigatórios não forem fornecidos.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public TransactionTypeDto create(TransactionTypeDto dto) {
    log.debug("Iniciando criação de um novo tipo de transação da categoria: {}", dto != null ? dto.getType() : null);
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
   * @param id  ID do tipo a atualizar.
   * @param dto Novos dados para o tipo de transação.
   * @return O tipo atualizado.
   * @throws ValidationException                se id ou dto forem nulos, ou
   *                                            faltando informações obrigatórias.
   * @throws TransactionTypeChangeTypeException se houver tentativa de alteração
   *                                            do tipo fundamental
   *                                            (INCOME/EXPENSE).
   */
  @Transactional(propagation = Propagation.REQUIRED)
  @CacheEvict(value = "transaction-types", key = "#id")
  public TransactionTypeDto update(Long id, TransactionTypeDto dto) {
    log.debug("Iniciando atualização de tipo de transação com ID: {}", id);
    ValidationUtils.notNull(id, "ID obrigatório");
    ValidationUtils.notNull(dto, "Objeto da transação é obrigatório");
    ValidationUtils.hasText(dto.getDescription(), "A descrição é obrigatória");

    TransactionType entity = findByIdOrThrow(id);

    // Regra de negócio: não é permitido alterar o tipo
    if (dto.getType() != null && !dto.getType().equals(entity.getType())) {
      throw new TransactionTypeChangeTypeException("O tipo de transação não pode ser alterado.");
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
   * @throws ValidationException se o ID for nulo.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  @CacheEvict(value = "transaction-types", key = "#id")
  public void delete(Long id) {
    log.debug("Iniciando exclusão lógica do tipo de transação com ID: {}", id);
    ValidationUtils.notNull(id, "ID obrigatório");
    TransactionType entity = findByIdOrThrow(id);
    entity.setStatus(TransactionTypeStatus.DELETED);
    transactionTypeRepository.save(entity);
  }

  public TransactionType findByIdOrThrow(Long id) {
    return transactionTypeRepository.findById(id)
        .filter(t -> t.getUser().getId().equals(userService.getCurrentUser().getId()))
        .filter(t -> t.getStatus() == TransactionTypeStatus.ACTIVE)
        .orElseThrow(() -> new TransactionTypeNotFoundException("Tipo de transação não encontrado."));
  }

  /**
   * Retorna os tipos de transação recorrentes e ativos de um determinado usuário.
   *
   * @param user O usuário dono dos tipos de transação.
   * @return Lista de {@link TransactionType} recorrentes e com status
   *         {@link TransactionTypeStatus#ACTIVE}.
   */
  public List<TransactionType> findRecurringActiveByUser(User user) {
    return transactionTypeRepository.findByUserAndStatusAndRecurringTrue(user, TransactionTypeStatus.ACTIVE);
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
