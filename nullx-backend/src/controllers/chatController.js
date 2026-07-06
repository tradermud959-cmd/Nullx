const logger = require('../utils/logger');
const config = require('../config/config');

exports.chat = async (req, res) => {
    const { message } = req.body;
    
    if (!message) {
        logger.error('Chat request missing message');
        return res.status(400).json({ error: 'Message is required' });
    }

    logger.info(`Chat request received: "${message.substring(0, 30)}${message.length > 30 ? '...' : ''}"`);

    try {
        const ollamaRes = await fetch(`${config.ollamaUrl}/api/generate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                model: config.model,
                prompt: message,
                stream: false
            })
        });

        if (!ollamaRes.ok) {
            throw new Error(`Ollama API error: ${ollamaRes.status}`);
        }

        const data = await ollamaRes.json();
        logger.success(`Ollama response received`);
        
        res.json({ reply: data.response });
    } catch (error) {
        logger.error(`Connection timeout or error: ${error.message}`);
        res.status(500).json({ error: 'Failed to communicate with Ollama' });
    }
};
