package br.com.infotech.myfinances.controller.api;

import br.com.infotech.myfinances.dto.TransactionTypeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tipo de Transação", description = "Endpoints para gerenciamento de tipos de transação")
@RequestMapping("/transaction-types")
public interface ITransactionTypeController {

    @Operation(summary = "Listar tipos de transação", description = "Retorna uma lista paginada de tipos de transação com base nos filtros informados")
    @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
    @GetMapping
    ResponseEntity<Page<TransactionTypeDto>> findAll(
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "types", required = false) List<String> types,
            Pageable pageable);

    @Operation(summary = "Buscar tipo de transação por ID", description = "Retorna um único tipo de transação")
    @ApiResponse(responseCode = "200", description = "Tipo de transação recuperado com sucesso")
    @ApiResponse(responseCode = "404", description = "Tipo de transação não encontrado")
    @GetMapping("/{id}")
    ResponseEntity<TransactionTypeDto> findById(@PathVariable("id") Long id);

    @Operation(summary = "Criar novo tipo de transação", description = "Cria um novo tipo de transação")
    @ApiResponse(responseCode = "201", description = "Criado com sucesso")
    @PostMapping
    ResponseEntity<TransactionTypeDto> create(@RequestBody TransactionTypeDto dto);

    @Operation(summary = "Atualizar tipo de transação", description = "Atualiza um tipo de transação existente")
    @ApiResponse(responseCode = "200", description = "Atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Tipo de transação não encontrado")
    @PutMapping("/{id}")
    ResponseEntity<TransactionTypeDto> update(@PathVariable("id") Long id, @RequestBody TransactionTypeDto dto);

    @Operation(summary = "Excluir tipo de transação", description = "Realiza exclusão lógica de um tipo de transação")
    @ApiResponse(responseCode = "204", description = "Excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Tipo de transação não encontrado")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable("id") Long id);
}
