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
