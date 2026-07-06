const logger = require('../utils/logger');
const config = require('../config/config');

exports.health = async (req, res) => {
    let ollamaStatus = false;
    try {
        const response = await fetch(`${config.ollamaUrl}/`);
        if (response.ok || response.status === 200) {
            ollamaStatus = true;
        }
    } catch (e) {}

    res.json({
        status: "ok",
        ollama: ollamaStatus,
        model: config.model
    });
};

exports.status = (req, res) => {
    res.json({ status: "running", port: config.port });
};

exports.logs = (req, res) => {
    res.json(logger.getLogs());
};

exports.clearLogs = (req, res) => {
    logger.clearLogs();
    res.json({ status: "cleared" });
};
