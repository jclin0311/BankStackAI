-- Runs automatically the first time the postgres container starts
-- with an empty data volume (docker-entrypoint-initdb.d).
-- Creates one database per service, matching each service's application.yml.

CREATE DATABASE accountsdb;        -- AccountService
CREATE DATABASE customerdb;        -- CusomerService
CREATE DATABASE billerdb;          -- BillerService
CREATE DATABASE billpayworkerdb;   -- BillPayWorkerService
CREATE DATABASE paymentdb;         -- PaymentOrchestrator
CREATE DATABASE settlementdb;      -- SettlementService
CREATE DATABASE ai_vector_db;      -- BankStackRag embeddings + agent action memory

-- ai_vector_db needs the pgvector extension for embedding similarity search
\connect ai_vector_db
CREATE EXTENSION IF NOT EXISTS vector;
