const { randomBytes, randomUUID, createHash } = require('node:crypto');
const db = require('../database/sqlite');
const TOKEN_TTL_MS = 24 * 60 * 60 * 1000;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const HEX_TOKEN = /^[0-9a-f]{64}$/i;
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const randomToken = () => randomBytes(32).toString('hex');
const sha256 = value => createHash('sha256').update(value).digest('hex');
function normalizeEmail(email) {
    if (typeof email !== 'string' || !EMAIL_RE.test(email.trim()) || email.length > 254) {
        throw new Error('Invalid email');
    }
    return email.trim().toLowerCase();
}
function createVerificationRequest(email) {
    const normalized = normalizeEmail(email);
    const requestId = randomUUID();
    const clientSecret = randomToken();
    const token = randomToken();
    const now = Date.now();
    db.prepare(`INSERT INTO email_verifications
        (request_id,email,token_hash,created_at,expires_at,client_secret_hash)
        VALUES (?,?,?,?,?,?)`).run(requestId,normalized,sha256(token),now,now+TOKEN_TTL_MS,sha256(clientSecret));
    return { requestId, clientSecret, token };
}
function verifyEmailToken(token) {
    if (typeof token !== 'string' || !HEX_TOKEN.test(token)) return null;
    const now = Date.now();
    return db.prepare(`UPDATE email_verifications SET verified_at=?
        WHERE token_hash=? AND verified_at IS NULL AND expires_at>?
        RETURNING request_id,email`).get(now,sha256(token),now) ?? null;
}
function getVerificationStatus(requestId, clientSecret) {
    if (typeof requestId !== 'string' || !UUID_RE.test(requestId)
        || typeof clientSecret !== 'string' || !HEX_TOKEN.test(clientSecret)) return 'not_found';
    const row = db.prepare(`SELECT verified_at,expires_at FROM email_verifications
        WHERE request_id=? AND client_secret_hash=?`).get(requestId,sha256(clientSecret));
    if (!row) return 'not_found';
    if (row.verified_at !== null) return 'verified';
    return row.expires_at <= Date.now() ? 'expired' : 'pending';
}
function hasVerifiedEmail(email) {
    const normalized=normalizeEmail(email);
    return !!db.prepare(`SELECT 1 FROM email_verifications
        WHERE email=? AND verified_at IS NOT NULL LIMIT 1`).get(normalized);
}
module.exports = {createVerificationRequest,verifyEmailToken,getVerificationStatus,
    hasVerifiedEmail,normalizeEmail};
