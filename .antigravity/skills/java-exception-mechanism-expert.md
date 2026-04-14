# Skill: Java Exception Mechanism Expert

## Descrição
Capacidade de gerenciar e aplicar o mecanismo unificado de exceções e tratativas de erros (Exceptions) na arquitetura do Backend.

## Critérios de Execução

1. **Abstração da Base:**
   A classe `BusinessException` é a raiz do domínio de erros, e deve ser obrigatoriamente **abstrata** (`public abstract class BusinessException extents RuntimeException`). Ela não deve ser instanciada diretamente (`new BusinessException(...)`).

2. **Tipagem Semântica (Polimorfismo):**
   Exceções de negócio devem sempre ser específicas e criadas sob medida para o domínio, cenário ou quebra de regra de negócio (ex: `TransactionTypeChangeTypeException`, `UserNotFoundException`, `LoginAlreadyExistsException`).
   Essas exceções devem herdar (extends) de `BusinessException`.

3. **Exceções de Validação Contínua:**
   `ValidationException` (que já herda de `BusinessException`) é um caso onde os cenários gerais de falhas de parâmetros/validações justificam o seu reuso sem precisar criar derivadas super restritas para cada campo.

4. **Lançamento Direto (Supressão de Catches):**
   Jamais silenciar exceções logando `ERROR` ou `WARN` em blocos `try/catch` perdidos pelo código. 
   Todo método que precisar levantar ou forçar uma falha de sistema deve lançá-la (throw) limpa para a pilha. A interrupção sempre fluirá até o Advice unificado para padronização.

5. **Responsabilidade do Advice (@ControllerAdvice / @RestControllerAdvice):**
   O tratamento unificado (responses HTTP) deve ser feito através de métodos injetados com `@ExceptionHandler` dentro de um `GlobalExceptionAdvice`.

6. **Regras de Log nos Advices (Herdado de Logging Expert):**
   - **BusinessException (exceções de negócio e filhas):** Devem possuir status code correspondente (como UNPROCESSABLE_ENTITY, UNAUTHORIZED, NOT_FOUND) e ser logadas silenciosamente. Rebaixamento do nível para **DEBUG**, sem imprimir a Stack Trace inteira do Java.
   - **InfrastructureException (erros de arquitetura, hardware, falhas pesadas):** Devem possuir status code 500 (INTERNAL_SERVER_ERROR), sendo logadas com nível **ERROR**, imprimindo toda a sua StackTrace real, obrigando a injeção textual (String interpolation) dos MDCs de `ThreadId` e `Login` para auditoria emergencial.
