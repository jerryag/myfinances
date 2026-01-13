package br.com.infotech.myfinances.controller.api;

import br.com.infotech.myfinances.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Login", description = "Autenticação de Usuários")
public interface ILoginController {

    @Operation(summary = "Realiza o login do usuário", description = "Autentica o usuário com login e senha via Form URL Encoded.")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "403", description = "Usuário bloqueado.")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas.")
    ResponseEntity<UserDto> login(
            @Parameter(description = "Login do usuário", required = true) @RequestParam("login") String login,
            @Parameter(description = "Senha do usuário", required = true) @RequestParam("password") String password);
}
