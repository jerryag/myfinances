package br.com.infotech.myfinances.domain;

/**
 * Define o status de um usuário.
 */
public enum UserStatus {
    /**
     * Usuário ativo.
     */
    ACTIVE,

    /**
     * Usuário bloqueado.
     */
    BLOCKED,

    /**
     * Usuário excluído (exclusão lógica).
     */
    DELETED
}
