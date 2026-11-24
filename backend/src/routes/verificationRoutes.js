const express=require('express');
const router=express.Router();
const {resolveLanguage,getEmailTemplate}=require('../templates');
const {createVerificationRequest,verifyEmailToken,getVerificationStatus,
  hasVerifiedEmail,normalizeEmail}=require('../services/emailVerificationService');
const {buildVerificationEmail}=require('../templates/renderVerificationEmail');
const {buildResetEmail}=require('../templates/renderResetEmail');
const {sendTransactionalEmail}=require('../services/emailService');
const {issueOtp,verifyOtpAndIssueGrant,consumeResetGrant}=require('../services/otpService');
const {allowEmail}=require('../services/rateLimitService');

function devOnly(req,res,next) {
    if (process.env.NODE_ENV!=='development') return res.sendStatus(403);
    next();
}
const validToken=value=>typeof value==='string' && /^[0-9a-f]{64}$/i.test(value);
function page(t,lang,message) {
    return `<!doctype html><html lang="${lang}" dir="${t.direction}">
    <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>${t.verificationPage.title}</title></head>
    <body style="font-family:Arial,sans-serif;text-align:center;padding:40px">
    <h2>AlienHunt</h2><p>${message}</p></body></html>`;
}

// This route displays a page only. A mail scanner cannot consume tokens via GET.
router.get('/verify-email',(req,res)=>{
    const lang=resolveLanguage(req.query.lang);
    const t=getEmailTemplate(lang);
    const token=req.query.token;
    res.set('Cache-Control','no-store').set('Referrer-Policy','no-referrer');
    if (!validToken(token)) return res.status(400).type('html').send(page(t,lang,t.verificationPage.invalid));
    return res.type('html').send(`<!doctype html><html lang="${lang}" dir="${t.direction}">
        <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>${t.verificationPage.title}</title></head>
        <body style="font-family:Arial,sans-serif;text-align:center;padding:40px">
        <h2>${t.verificationPage.title}</h2><p>${t.verificationPage.message}</p>
        <form action="/api/auth/verify-email" method="POST">
        <input type="hidden" name="token" value="${token}">
        <input type="hidden" name="lang" value="${lang}">
        <button type="submit" style="background:#228B22;color:white;border:0;padding:14px 28px;border-radius:6px">
        ${t.verificationPage.button}</button></form></body></html>`);
});
router.post('/verify-email',(req,res)=>{
    const lang=resolveLanguage(req.body?.lang);
    const t=getEmailTemplate(lang);
    const result=verifyEmailToken(req.body?.token);
    const success=result!==null;
    res.set('Cache-Control','no-store').set('Referrer-Policy','no-referrer');
    if (req.is('application/x-www-form-urlencoded')) {
        return res.status(success?200:400).type('html').send(page(t,lang,
            success?t.verificationPage.success:t.verificationPage.invalid));
    }
    return res.status(success?200:400).json({success,message:success?'Email verified successfully':'Invalid or expired token'});
});

// Development-only endpoint; returns a *private polling secret* separately from emailed token.
router.post('/send-verification',devOnly,async(req,res)=>{
    let email;
    try {email=normalizeEmail(req.body?.email);} catch {return res.status(400).json({success:false,message:'Invalid email'});}
    const language=resolveLanguage(req.body?.language);
    if (!allowEmail('register',email,req.ip)) return res.status(429).json({success:false,message:'Too many requests; try later'});
    try {
        const request=createVerificationRequest(email);
        const url=new URL('/api/auth/verify-email',process.env.VERIFICATION_BASE_URL);
        url.searchParams.set('token',request.token);
        url.searchParams.set('lang',language);
        const message=buildVerificationEmail(language,url.href);
        await sendTransactionalEmail(email,message.subject,message.html,true);
        return res.status(202).json({success:true,requestId:request.requestId,
            clientSecret:request.clientSecret,message:'Verification email requested'});
    } catch(err) {
        console.error('Register email failed:',err.message);
        return res.status(503).json({success:false,message:'Could not send email'});
    }
});
router.post('/verification-status',devOnly,(req,res)=>{
    const status=getVerificationStatus(req.body?.requestId,req.body?.clientSecret);
    res.set('Cache-Control','no-store');
    return res.json({status});
});

router.post('/reset/request',devOnly,async(req,res)=>{
    let email;
    try {email=normalizeEmail(req.body?.email);}catch{return res.status(400).json({success:false,message:'Invalid email'});}
    const requestIdPlaceholder=require('node:crypto').randomUUID();
    // Generic success response when the address is unknown: no account enumeration.
    if (!hasVerifiedEmail(email)) return res.status(202).json({success:true,requestId:requestIdPlaceholder,
        message:'If eligible, a code will be sent'});
    if (!allowEmail('reset',email,req.ip)) return res.status(429).json({success:false,message:'Too many requests; try later'});
    try {
        const issued=issueOtp(email);
        const message=buildResetEmail(req.body?.language,issued.code);
        await sendTransactionalEmail(email,message.subject,message.html,true);
        return res.status(202).json({success:true,requestId:issued.requestId,
            message:'If eligible, a code will be sent'});
    } catch(err) {
        console.error('Reset email failed:',err.message);
        return res.status(503).json({success:false,message:'Could not send email'});
    }
});
router.post('/reset/verify',devOnly,(req,res)=>{
    const result=verifyOtpAndIssueGrant(req.body?.requestId,req.body?.code);
    res.set('Cache-Control','no-store');
    if (!result) return res.status(400).json({success:false,message:'Invalid, expired or exhausted code'});
    // The grant is valid for 10 minutes and may be used exactly once.
    return res.json({success:true,resetGrant:result.resetGrant});
});
router.post('/reset/consume',devOnly,(req,res)=>{
    const email=consumeResetGrant(req.body?.resetGrant);
    res.set('Cache-Control','no-store');
    if (!email) return res.status(400).json({success:false,message:'Invalid or expired reset grant'});
    return res.json({success:true,email});
});
module.exports=router;
