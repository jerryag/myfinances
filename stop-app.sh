#!/bin/bash

echo "Encerrando os processos associados ao Java (Backend)..."
pkill -f "mvn spring-boot:run" 2>/dev/null
pkill -f "java.*myfinances" 2>/dev/null

echo "Encerrando os processos associados ao Node/Vite (Frontend)..."
pkill -f "npm run dev" 2>/dev/null
pkill -f "vite" 2>/dev/null

echo "============================================="
echo "Todos os serviços foram encerrados com sucesso!"
echo "============================================="
