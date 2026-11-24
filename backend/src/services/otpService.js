const { randomInt, randomBytes, randomUUID, createHmac, createHash, timingSafeEqual } = require('node:crypto');
const db = require('../database/sqlite');
const {normalizeEmail} = require('./emailVerificationService');
const OTP_TTL_MS = 5*60*1000;
const GRANT_TTL_MS = 10*60*1000;
const MAX_ATTEMPTS = 5;
const keyHex = process.env.OTP_HMAC_KEY;
if (!/^[a-f0-9]{64}$/i.test(keyHex ?? '')) {
    throw new Error('Missing/invalid OTP_HMAC_KEY in backend/.env');
}
const key = Buffer.from(keyHex,'hex');
const randomToken = () => randomBytes(32).toString('hex');
const tokenHash = value => createHash('sha256').update(value).digest('hex');
const otpDigest = (id,email,code) => createHmac('sha256',key)
    .update(JSON.stringify([id,email,'RESET_PASSWORD',code])).digest('hex');
function issueOtp(email) {
    email=normalizeEmail(email);
    const requestId=randomUUID();
    const code=randomInt(0,1000000).toString().padStart(6,'0');
    const now=Date.now();
    db.prepare(`INSERT INTO verification_requests
        (id,email,purpose,code_hash,expires_at,attempts,created_at,consumed_at)
        VALUES (?,?,'RESET_PASSWORD',?,?,0,?,NULL)`).run(
            requestId,email,otpDigest(requestId,email,code),now+OTP_TTL_MS,now);
    return {requestId,code}; // Code must be sent by Brevo, never in API response/logs.
}
function verifyOtpAndIssueGrant(requestId,code) {
    if (typeof requestId!=='string' || !/^[a-f0-9-]{36}$/i.test(requestId)
        || typeof code!=='string' || !/^\d{6}$/.test(code)) return null;
    const now=Date.now();
    db.exec('BEGIN IMMEDIATE');
    try {
        const row=db.prepare(`SELECT email,code_hash,expires_at,attempts,consumed_at FROM verification_requests
            WHERE id=? AND purpose='RESET_PASSWORD'`).get(requestId);
        if (!row || row.consumed_at!==null || row.expires_at<=now || row.attempts>=MAX_ATTEMPTS) {
            db.exec('COMMIT'); return null;
        }
        const actual=Buffer.from(otpDigest(requestId,row.email,code),'hex');
        const expected=Buffer.from(row.code_hash,'hex');
        const correct=expected.length===actual.length && timingSafeEqual(actual,expected);
        if (!correct) {
            db.prepare('UPDATE verification_requests SET attempts=attempts+1 WHERE id=?').run(requestId);
            db.exec('COMMIT'); return null;
        }
        db.prepare('UPDATE verification_requests SET consumed_at=? WHERE id=?').run(now,requestId);
        const grant=randomToken();
        db.prepare(`INSERT INTO reset_grants(grant_hash,email,expires_at,consumed_at)
            VALUES (?,?,?,NULL)`).run(tokenHash(grant),row.email,now+GRANT_TTL_MS);
        db.exec('COMMIT');
        return {resetGrant:grant,email:row.email};
    } catch (err) {
        db.exec('ROLLBACK'); throw err;
    }
}
function consumeResetGrant(resetGrant) {
    if (typeof resetGrant !== 'string' || !/^[0-9a-f]{64}$/i.test(resetGrant)) return null;
    const now=Date.now();
    const row=db.prepare(`UPDATE reset_grants SET consumed_at=?
        WHERE grant_hash=? AND consumed_at IS NULL AND expires_at>?
        RETURNING email`).get(now,tokenHash(resetGrant),now);
    return row ? row.email : null;
}
module.exports={issueOtp,verifyOtpAndIssueGrant,consumeResetGrant};
