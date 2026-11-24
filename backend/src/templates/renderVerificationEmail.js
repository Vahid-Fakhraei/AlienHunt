
const { getEmailTemplate } = require("./index");

function buildVerificationEmail(language, verificationUrl) {

    const t = getEmailTemplate(language);

    const url = new URL(verificationUrl);


    const isLocalDevelopment =
        process.env.NODE_ENV === "development" &&
        url.protocol === "http:" &&
        ["localhost", "127.0.0.1"].includes(url.hostname);

    if (url.protocol !== "https:" && !isLocalDevelopment) {
        throw new Error("Verification URL must use HTTPS");
    }


    const safeUrl = url.href.replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;");

    const html = `
        <!DOCTYPE html>
        <html>
        <body dir="${t.direction}"
              style="font-family:Arial,sans-serif;
                     text-align:center;
                     padding:30px;">

            <h2>${t.verification.title}</h2>

            <p>${t.verification.message}</p>

            <a href="${safeUrl}"
               style="background-color:#228B22;
                      color:white;
                      padding:14px 28px;
                      text-decoration:none;
                      border-radius:6px;
                      display:inline-block;">

                ${t.verification.button}

            </a>

            <p>${t.verification.expiry}</p>

        </body>
        </html>
    `;

    return {
        subject: t.verification.subject,
        html: html
    };
}

module.exports = { buildVerificationEmail };