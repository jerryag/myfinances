# Workflow: Pre-Delivery Quality Check

## Passos de Execução
1. **Validation:** Verificar se o código Java reflete o `java_code_formatter.xml`.
2. **Naming Check:** Validar se o código está em Inglês e o Javadoc em Português.
3. **Architecture Review:** Confirmar se os princípios SOLID e Clean Code foram aplicados.
4. **Security & Lint:** Simular uma varredura Sonar/Snyk para identificar vulnerabilidades ou "code smells".
5. **Refactor:** Se qualquer desvio for encontrado (ex: String literal duplicada ou import não utilizado), o agente deve corrigir automaticamente antes de entregar.