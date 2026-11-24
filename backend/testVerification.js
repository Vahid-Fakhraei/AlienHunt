
const service = require(
    "./src/services/emailVerificationService"
);

const request = service.createVerificationRequest(
    "test@example.com"
);

console.log("Request created:", Boolean(request.requestId));

const firstResult = service.verifyEmailToken(request.token);

console.log("First verification:", firstResult !== null);

const secondResult = service.verifyEmailToken(request.token);

console.log("Second verification:", secondResult !== null);
