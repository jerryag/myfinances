package br.com.infotech.myfinances.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.infotech.myfinances.domain.UserStatus;
import br.com.infotech.myfinances.dto.UserDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Users", description = "Gerenciamento de Usuários")
public interface IUserController {

        @Operation(summary = "Altera a senha do usuário", description = "Altera a senha do usuário mediante validação da senha atual.")
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso")
        @ApiResponse(responseCode = "400", description = "Dados inválidos.")
        @ApiResponse(responseCode = "401", description = "Senha atual incorreta.")
        ResponseEntity<Void> changePassword(
                        @Parameter(description = "ID do usuário", required = true) @RequestParam("userId") Long userId,
                        @Parameter(description = "Senha atual", required = true) @RequestParam("oldPassword") String oldPassword,
                        @Parameter(description = "Nova senha", required = true) @RequestParam("newPassword") String newPassword);

        @Operation(summary = "Busca usuários com filtros e paginação", description = "Retorna uma lista paginada de usuários, podendo filtrar por status.")
        @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso")
        @ApiResponse(responseCode = "403", description = "Acesso negado.")
        ResponseEntity<Page<UserDto>> findAll(
                        @Parameter(description = "Lista de status para filtro") @RequestParam(value = "statuses", required = false) List<UserStatus> statuses,
                        @ParameterObject Pageable pageable);

        @Operation(summary = "Cria um novo usuário", description = "Cria um novo usuário com status ACTIVE.")
        @ApiResponse(responseCode = "200", description = "Usuário criado com sucesso")
        @ApiResponse(responseCode = "400", description = "Dados inválidos.")
        ResponseEntity<UserDto> create(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do usuário", required = true) @RequestBody UserDto userDto);

        @Operation(summary = "Atualiza um usuário", description = "Atualiza os dados de um usuário existente.")
        @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso")
        @ApiResponse(responseCode = "400", description = "Dados inválidos.")
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
        ResponseEntity<UserDto> update(
                        @Parameter(description = "ID do usuário", required = true) @PathVariable("id") Long id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do usuário", required = true) @RequestBody UserDto userDto);

        @Operation(summary = "Altera o status de um usuário", description = "Altera o status de um usuário (Bloquear, Desbloquear, Excluir).")
        @ApiResponse(responseCode = "204", description = "Status alterado com sucesso")
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
        ResponseEntity<Void> changeStatus(
                        @Parameter(description = "ID do usuário", required = true) @PathVariable("id") Long id,
                        @Parameter(description = "Novo status", required = true) @RequestParam("status") UserStatus status);
}
