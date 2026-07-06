module.exports = {
    port: process.env.PORT || 5000,
    ollamaUrl: process.env.OLLAMA_URL || 'http://127.0.0.1:11434',
    model: process.env.MODEL || 'llama3.2:1b'
};
