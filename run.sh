#!/bin/bash

# Função para encerrar os processos em background
cleanup() {
    echo ""
    echo "Encerrando os serviços..."
    if [ -n "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null
    fi
    if [ -n "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null
    fi
    exit 0
}

# Captura sinais de interrupção ou término para executar a função cleanup
trap cleanup SIGINT SIGTERM EXIT

DB_STATUS=$(docker inspect -f '{{.State.Health.Status}}' myfinances-db 2>/dev/null)

if [ "$DB_STATUS" == "healthy" ]; then
  echo "Banco de Dados PostgreSQL já está em execução e pronto!"
else
    echo "Iniciando Banco de Dados PostgreSQL..."
    cd dev
    docker compose up -d database
    cd ..

    echo "Aguardando inicialização do banco de dados..."
  for i in {1..15}; do
    DB_STATUS=$(docker inspect -f '{{.State.Health.Status}}' myfinances-db 2>/dev/null)
    if [ "$DB_STATUS" == "healthy" ]; then
      echo "Banco de dados pronto!"
      break
    fi
    sleep 2
  done
fi

KC_STATUS=$(docker inspect -f '{{.State.Status}}' myfinances-keycloak 2>/dev/null)

if [ "$KC_STATUS" == "running" ]; then
  echo "Keycloak já está em execução e pronto!"
else
    echo "Iniciando Keycloak..."
    cd dev
    docker compose up -d keycloak
    cd ..

    echo "Aguardando inicialização do Keycloak..."
  for i in {1..15}; do
    KC_STATUS=$(docker inspect -f '{{.State.Status}}' myfinances-keycloak 2>/dev/null)
    if [ "$KC_STATUS" == "running" ]; then
      echo "Keycloak iniciou com sucesso!"
      break
    fi
    sleep 2
  done
fi

echo "Iniciando Backend Spring Boot..."
cd myfinances-back
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

echo "Iniciando Frontend..."
cd myfinances-front
npm run dev &
FRONTEND_PID=$!
cd ..

echo "============================================="
echo "Serviços em execução!"
echo "Pressione [Ctrl+C] para encerrar todos."
echo "============================================="

# Aguarda indefinidamente os processos rodando em background
wait
