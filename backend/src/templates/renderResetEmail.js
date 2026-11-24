const {getEmailTemplate,resolveLanguage}=require('./index');
function buildResetEmail(language,code) {
    if (typeof code!=='string' || !/^\d{6}$/.test(code)) throw new Error('Invalid OTP');
    const lang=resolveLanguage(language);
    const t=getEmailTemplate(lang);
    return {
        subject:t.passwordReset.subject,
        html:`<!doctype html><html lang="${lang}" dir="${t.direction}">
            <head><meta charset="utf-8"></head>
            <body style="font-family:Arial,sans-serif;text-align:center;padding:28px">
                <h2>${t.passwordReset.title}</h2>
                <p>${t.passwordReset.message}</p>
                <p style="font-size:32px;letter-spacing:7px;font-weight:bold" dir="ltr">${code}</p>
                <p>${t.passwordReset.expiry}</p>
            </body></html>`
    };
}
module.exports={buildResetEmail};
