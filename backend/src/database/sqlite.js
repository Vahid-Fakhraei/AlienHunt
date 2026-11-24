const { DatabaseSync, Database } = require('node:sqlite');
const fs = require('node:fs');
const path = require('node:path');

const DatabaseClass = DatabaseSync ?? Database;
const databasePath = process.env.ALIENHUNT_DB_PATH
    ? path.resolve(process.env.ALIENHUNT_DB_PATH)
    : path.join(__dirname, '../../data/verification.db');
fs.mkdirSync(path.dirname(databasePath), { recursive: true });
const db = new DatabaseClass(databasePath);
db.exec('PRAGMA busy_timeout = 5000');
db.exec(`
    CREATE TABLE IF NOT EXISTS email_verifications (
        request_id TEXT PRIMARY KEY,
        email TEXT NOT NULL,
        token_hash TEXT NOT NULL UNIQUE,
        created_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        verified_at INTEGER
    );
    CREATE TABLE IF NOT EXISTS verification_requests (
        id TEXT PRIMARY KEY,
        email TEXT NOT NULL,
        purpose TEXT NOT NULL CHECK(purpose IN ('REGISTER', 'RESET_PASSWORD')),
        code_hash TEXT NOT NULL,
        expires_at INTEGER NOT NULL,
        attempts INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL,
        consumed_at INTEGER
    );
    CREATE TABLE IF NOT EXISTS reset_grants (
        grant_hash TEXT PRIMARY KEY,
        email TEXT NOT NULL,
        expires_at INTEGER NOT NULL,
        consumed_at INTEGER
    );
    CREATE TABLE IF NOT EXISTS email_rate_limits (
        kind TEXT NOT NULL,
        key_hash TEXT NOT NULL,
        sent_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_email_rate_limits
        ON email_rate_limits(kind,key_hash,sent_at);
`);
// Migration for the existing verification.db: preserve prior records.
const existing = db.prepare('PRAGMA table_info(email_verifications)').all();
if (!existing.some(c => c.name === 'client_secret_hash')) {
    db.exec('ALTER TABLE email_verifications ADD COLUMN client_secret_hash TEXT');
}
module.exports = db;
