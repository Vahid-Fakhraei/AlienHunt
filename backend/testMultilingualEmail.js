
const { sendTransactionalEmail } = require(
    "./src/services/emailService"
);

const { buildVerificationEmail } = require(
    "./src/templates/renderVerificationEmail"
);

const language = "fa";

const verificationUrl =
    "https://example.com/verify?test=1";

const email = buildVerificationEmail(
    language,
    verificationUrl
);

sendTransactionalEmail(
    process.env.BREVO_SENDER_EMAIL,
    "[TEST] " + email.subject,
    email.html,
    true
)
.then(id => console.log("Message ID:", id))
.catch(err => console.error("ERROR:", err.message));
