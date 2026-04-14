# Skill: Logging Expert

## Descrição
Capacidade de gerar logging dos processos executados no backend.

## Critérios de Execução
1. Utilizar SLF4J com Logback para logar informações.
2. Logar com level DEBUG no início de métodos ou blocos de linhas que realizam alguma operação específica e relevante dentro do código. 
3. Utilizar o pattern `%.-1p [%30.30logger{0}][%X{mdc1}][%X{mdc2}]...[%X{mdcn}]: %m%n` para logs.
   3.1. Os MDCs devem ser utilizados para armazenar informações de identificação serão utilizadas nos logs. Teremos 2 MDCs:
        3.1.1. MDC1 deverá conter o threadId da thread
        3.1.2. MDC2 deverá conter o login do usuário logado.
   3.2. Se não houver usuário logado no momento da impressão do log, setar o id do usuário com o string "-".
   3.3. Criar uma classe utilitária MDC utils contendo um método para setar MDCs da thread. Esse método deverá receber um item de ENUM (definido em `domain`) que definirá qual a chave de MDC está sendo definida e um parâmetro contendo o valor a ser setado no MDC informado.
   3.4. Criar um filtro ou interceptor, que será responsável por setar os MDCs com o threadId da thread e o userId do usuário logado para threads iniciadas por requisições Rest.
   3.5. Para threads iniciadas por requisições assíncronas, como por exemplo, threads iniciadas por `@Async`, ou mensagens de filas recebidas por listeners, os MDCs devem ser setados programaticamente no início da thread com o threadId da thread e o userId do usuário logado no momento da criação da thread.
4. Não logar consultas simples de tela, ou operações triviais, mas logar consultas de informações que servirão para alguma tomada de decisão no fluxo de negócios.
5. Não logar ERROR ou WARN nos catches. Todo método que precisar lançar uma exceção, checada ou não, deve lançá-la e haverá um `@ControllerAdvice` ou `@RestControllerAdvice` para tratar as exceções.
   5.1 No caso de advices, deve-se incluir na mensagem de log.ERROR o threadId da thread e o login do usuário logado.
   5.2 No caso de excaptions que estendem BusinessException chegarem até o @ControllerAdvice, não logar a stacktrace, e usar o level DEBUG.
6. Mascarar senhas e informações sensíveis nos logs.