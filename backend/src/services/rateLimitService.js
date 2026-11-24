const {createHash} = require('node:crypto');
const db = require('../database/sqlite');
const WINDOW = 60*60*1000;
function allowEmail(kind,email,ip) {
    const now=Date.now();
    const hash=value=>createHash('sha256').update(value).digest('hex');
    const emailKey=hash(email);
    const ipKey=hash(ip || 'unknown');
    const select=db.prepare(`SELECT COUNT(*) AS count,MAX(sent_at) AS latest FROM email_rate_limits
        WHERE kind=? AND key_hash=? AND sent_at>?`);
    const a=select.get(kind+':email',emailKey,now-WINDOW);
    const b=select.get(kind+':ip',ipKey,now-WINDOW);
    // At most 3 mails/hour/email, at most 30 mails/hour/IP and 60s cooldown/email.
    if (a.count>=3 || b.count>=30 || (a.latest && now-a.latest<60_000)) return false;
    db.prepare('INSERT INTO email_rate_limits(kind,key_hash,sent_at) VALUES (?,?,?)')
      .run(kind+':email',emailKey,now);
    db.prepare('INSERT INTO email_rate_limits(kind,key_hash,sent_at) VALUES (?,?,?)')
      .run(kind+':ip',ipKey,now);
    db.prepare('DELETE FROM email_rate_limits WHERE sent_at<?').run(now-WINDOW);
    return true;
}
module.exports={allowEmail};
