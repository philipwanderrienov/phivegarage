-- Run as PostgreSQL administrator; replace this password before executing.
-- psql -U postgres -f sql/000_database.sql
CREATE ROLE phivegarage LOGIN PASSWORD 'REPLACE_WITH_A_STRONG_PASSWORD';
CREATE DATABASE phivegarage OWNER phivegarage;
-- Start backend to apply schema automatically with Flyway.
-- For manual schema installation see README: do not run 001_schema before Flyway without baselining.
