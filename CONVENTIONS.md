# Java Engineering Conventions

## 1. Formatação e Estilo
- **Perfil Eclipse:** Todo código Java deve seguir o perfil definido em `java_code_formatter.xml` (raiz do repositório).
- **Regras Específicas:**
    - Indentação e chaves conforme o perfil XML.
    - Adicione `serialVersionUID` em toda classe `Serializable`.
    - Extraia literais de `String` repetidos para `private static final String` no topo da classe.
    - Remova obrigatoriamente imports e variáveis não utilizados.

## 2. Nomenclatura e Idioma no Código
- **Código:** Nomes de classes, tipos, enums, atributos, métodos e variáveis devem ser em **Inglês**.
- **Documentação Interna:** Comentários e Javadoc devem ser em **Português (Brasil)**.
- **Exceção SOAP:** Classes stub geradas pelo CFX no subpacote `soap` devem manter os nomes originais dos XSDs.

## 3. Padrões Arquiteturais e Frameworks
- **Princípios:** 
    1. Aderir ao princípio SOLID, clean code e uso de lambdas sempre que possível.
    2. Utilizar interfaces funcionais onde possível.
    3. Utilizar Streams API onde possível.
    4. Utilizar Optional onde possível.
    5. Utilizar try-with-resources onde possível.
    6. Utilizar try-finally com close() sempre que um resource for aberto e não for possível utilizar try-with-resources.
    7. Todas as respostas de sucesso da API devem retornar um `ResponseEntity` encapsulando um DTO específico ou uma coleção, nunca a entidade de domínio (domain) diretamente.
    8. Utilizar `StringUtils.isBlank()` / `StringUtils.isNotBlank()` da Apache Commons para verificar se Strings são (ou não) nulas ou vazias.
    9. Para campos de data e hora, utilizar sempre java.time.OffsetDateTime ou java.time.Instant para garantir a precisão de fuso horário. Evitar o uso de java.util.Date ou java.time.LocalDateTime em tabelas de transações.
- **Spring:** 
    1. Testes unitários devem utilizar `@ExtendWith(SpringExtension.class)`.
- **Lombok:** 
    1. Classes DTO e Domain devem usar anotações Lombok e `@Builder` (sem ocultar construtores NoArgs/AllArgs).
    2. Classes `@Configuration` ou `@Service` que tiverem algum log, devem ser anotadas com `@Slf4j`.
- **Logging:**
    1. Utilizar SLF4J com level DEBUG no início de blocos de linhas que realizam alguma operação específica e relevante dentro do código.
    2. Não logar ERROR ou WARN nos catches. Todo método que precisar lançar uma exceção, checada ou não, deve lançá-la e haverá um `@ControllerAdvice` ou `@RestControllerAdvice` para tratar as exceções.
- **Persistência (JPA):** 
    1. Prioridade: `methodQuery`.
    2. Segunda opção: JPQL anotada.
    3. Última opção: SQL Nativo anotado.
- **Camada de Serviço:** 
    1. Toda classe de serviço deve ser anotada com `@Transactional` SUPPORTS.
    2. Todo método que utiliza métodos de escrita dos repositórios ou métodos de escrita de outros serviços deve ser anotado com `@Transactional` REQUIRED.
    3. Todo método público deve validar os parâmetros recebidos. Para isso, deve-se utilizar uma classe utilitária para validação de parâmetros e lançar a exceção a ser criada `ValidationException` que extende a exceção a ser criada `BusinessException` (Exception), com mensagem em português.
      3.1. A classe utilitária deve utilizar a biblioteca `org.springframework.util.Assert` para validação de parâmetros e então encapsular a exceção lançada pela biblioteca em uma `ValidationException`.
      3.2. A mensagem deve informar qual o problema de validação e o atributo que teve a validação violada. Se a validação for de um objeto, deve-se informar qual o atributo do objeto que teve a validação violada e, se possível, o valor que causou a validação violada.
    4. Se um método chamador precisar tomar alguma decisão baseado em alguma condição de erro (de validação ou não) que ocorreu no método chamado, então o método chamado deve lançar uma exceção específica criada para esse cenário, que extende `BusinessException` e o método chamador deve capturar esta exceção e tomar a decisão baseada nela.
    5. Todo parâmetro de método de serviço deve ser considerado obrigatório a não ser que o código a seguir deixe implícito que o parâmetro é opcional.
    6. Ao capturar exceções checadas, mas que de alguma forma são consideradas exceções de infraestrutura (ex: IOException, JacksonException), encapsulá-las e relançar a exceção não checada a ser criada `InfrastructureException` (extende RuntimeException). É obrigatório preservar a Stack Trace original passando a exceção capturada como parâmetro no construtor da nova exceção.
    7. Dependências devem ser injetadas via construtor, porém se a classe de serviço tiver mais que 5 dependências, então elas devem ser injetadas com `@Autowired`.
    8. Properties devem ser injetadas com `@Value`.
- **Tratamento de exceções:**
    1. Toda exceção que chegar à camada de controller deve ser tratada em um `@ControllerAdvice` ou `@RestControllerAdvice`.
    2. Toda exceção deve ser lançada e não tratada no método que a lançou, exceto para casos onde aquela exceção PRECISAR ser tratada (e não relançada) de acordo com alguma regra de negócio ou necessidade específica.
    3. Erros de API devem seguir o padrão RFC 7807 (Problem Details for HTTP APIs).
    4. Exceções que forem lançadas pela camada de serviço mas que o método chamador não for um método de Controller devem subir até serem logadas pelo framework ou JDK.
## 4. Estrutura de pacotes e Naming Conventions
- **Packages:**
    1. A package raiz será `br.com.infotech.myfinances`.
    2. Toda classe `@Configuration` ficará na subpackage `config` e terá o sufixo `Config`.
    3. Toda classe `@ControllerAdvice` ou `@RestControllerAdvice` ficará na subpackage `advice` e terá o sufixo `Advice`.
    4. Toda classe `@Controller` ou `@RestController` ficará na subpackage `controller` e terá o sufixo `Controller`.
       4.1. Toda classe `@Controller` possuirá uma interface correspondente com o nome `I{NomeDaClasseController}.java` que residirá subpackage `controller.api`. Essas interfaces e seus métodos serão anotadas com anotações do Swagger (`io.swagger.v3.oas.annotations.*`) definindo a documentação OpenAPI para todos os elementos da API.
    5. Toda classe `@Service` ficará na subpackage `service` e terá o sufixo `Service`.
    6. Toda classe `@Repository` ficará na subpackage `repository` e terá o sufixo `Repository`.
    7. Toda classe utilitária ficará na subpackage `util` e terá o sufixo `Utils`.
    8. Toda classe DTO ficará na subpackage `dto` e terá o sufixo `Dto`.
    9. Toda classe ou enum de domínio ficará na subpackage `domain` e não terá nenhum sufixo.
    10. Toda classe Exception ficará na subpackage `exception` e terá o sufixo `Exception`.
    11. Toda classe de teste unitário ficará em `src/test/java/br/com/infotech/myfinances` utilizando o mesmo nome de subpackage da classe testada e terá o sufixo `Test`.

## 5. Database Migration (Flyway)
- **Diretórios:**
    1. Todo script flyway ficará no diretório `flyway`. 
       1.1. Abaixo de `flyway` existirão os subdiretórios `common`, `dev`, `hml` e `prd`.
       1.2. Em `common` ficarão os scripts que serão executados em todos os ambientes.
       1.3. Em `dev` ficarão os scripts que serão executados apenas no ambiente de desenvolvimento.
       1.4. Em `hml` ficarão os scripts que serão executados apenas no ambiente de homologação.
       1.5. Em `prd` ficarão os scripts que serão executados apenas no ambiente de produção.
- **Nomenclatura:**
    1. Todo script flyway deve seguir a seguinte nomenclatura: `V{version}.{sequencia}__{tipo}_{description}.sql`.
       1.1. `version` é um número composto por 3 grupos de 3 dígitos que deve ser extraído dinamicamente do nó `<version>` do `pom.xml`, convertendo o formato ex:`1.1.1` para "001.001.001".
       1.2. `sequencia` é um número sequencial composto por 3 dígitos, exemplo "001" representando a sequência de scripts dentro da versão.
       1.3. `tipo` é um dos seguintes valores "ddl" ou "dml", conforme sua natureza. No caso de scripts do tipo ddl, usar somente um comando DDL por script. 
       1.4. `description` é uma descrição do script. Usar lower case e `_` para separar palavras. Usar o idioma inglês para a descrição. A descrição deve representar o que o script faz de forma sucinta.

## 6. Guardrails
- Proibido nomes *full-qualified* no corpo do código, exceto em colisões de nomes de classes na mesma unidade de compilação.
- Proibido métodos com mais de 30 linhas de lógica. Se isso acontecer, dividir a lógica em sub métodos.
- A criação ou atribuição de objetos grandes, seja com construtor, setters ou builder, não é considerada lógica, nesse caso, não é proibido ultrapassar a quantidade limite de linhas. Se a criação do objeto for feita em até 10 linhas, pode manter dentro do método principal, mas se passar disso, usar um método privado específico para a criação do objeto (ex: var obj = createObject()).
- Proibido criar métodos ou construtores que recebam mais que 5 parâmetros. Se isso acontecer, criar uma classe para representar os parâmetros.
