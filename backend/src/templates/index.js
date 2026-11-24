
const translations = {
    en: require("./en"),
    es: require("./es"),
    fa: require("./fa")
};

function resolveLanguage(language = "en") {

    if (typeof language !== "string") {
        return "en";
    }

    const code = language
        .trim()
        .toLowerCase()
        .replace("_", "-")
        .split("-")[0];

    return Object.hasOwn(translations, code)
        ? code
        : "en";
}

function getEmailTemplate(language = "en") {
    const code = resolveLanguage(language);
    return translations[code];
}

module.exports = {
    resolveLanguage,
    getEmailTemplate
};
