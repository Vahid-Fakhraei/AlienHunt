
async function sendTransactionalEmail(toEmail, subject, content, isHtml = false) {
    const apiKey = process.env.BREVO_API_KEY;
    const senderEmail = process.env.BREVO_SENDER_EMAIL;

    if (!apiKey || !senderEmail) {
        throw new Error("Brevo configuration is missing");
    }


    const emailData = {
        sender: {
            name: "AlienHunt",
            email: senderEmail
        },
        to: [
            { email: toEmail }
        ],
        subject: subject
    };

    if (isHtml) {
        emailData.htmlContent = content;
    } else {
        emailData.textContent = content;
    }



    const response = await fetch(
        "https://api.brevo.com/v3/smtp/email",
        {
            method: "POST",

            headers: {
                "api-key": apiKey,
                "Content-Type": "application/json",
                "Accept": "application/json"
            },

            body: JSON.stringify(emailData)
        }
    );


    const result = await response.json();

    if (!response.ok) {
        throw new Error(
            "Brevo error: " +
            (result.message || response.status)
        );
    }

    return result.messageId;
}

module.exports = { sendTransactionalEmail };
