
const {
    createVerificationRequest
} = require("./src/services/emailVerificationService");

const {
    buildVerificationEmail
} = require("./src/templates/renderVerificationEmail");

const {
    sendTransactionalEmail
} = require("./src/services/emailService");

async function main() {

    const email = process.env.BREVO_SENDER_EMAIL;
    const language = "fa";

    // 1. Generate and save verification request
    const request = createVerificationRequest(email);

    // 2. Build verification URL
    const url = new URL(
        "/api/auth/verify-email",
        process.env.VERIFICATION_BASE_URL
    );

    url.searchParams.set("token", request.token);

    // 3. Build localized email
    const message = buildVerificationEmail(
        language,
        url.href
    );

    // 4. Send email using Brevo
    const messageId = await sendTransactionalEmail(
        email,
        "[TEST] " + message.subject,
        message.html,
        true
    );

    console.log("Message ID:", messageId);
}

main().catch(error => {
    console.error("ERROR:", error.message);
});
