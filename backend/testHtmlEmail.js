
const { sendTransactionalEmail } = require(
    "./src/services/emailService"
);

const html = `
<html>
<body style="font-family:Arial,sans-serif; text-align:center;">

    <h2>Welcome to AlienHunt!</h2>

    <p>Please verify your email address.</p>

    <a href="https://example.com/"
       style="background-color:#228B22;
              color:white;
              padding:14px 28px;
              text-decoration:none;
              border-radius:6px;
              display:inline-block;">
        Verify Email
    </a>

    <p>This is a design test only.</p>

</body>
</html>
`;

sendTransactionalEmail(
    process.env.BREVO_SENDER_EMAIL,
    "AlienHunt - HTML Email Test",
    html,
    true
)
.then(id => console.log("Message ID:", id))
.catch(err => console.error("ERROR:", err.message));
