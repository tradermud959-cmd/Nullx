const logs = [];

function addLog(type, message) {
    const time = new Date().toLocaleTimeString('en-GB', { hour12: false });
    const logEntry = `[${time}] [${type}] ${message}`;
    console.log(logEntry);
    logs.push({ time: `[${time}]`, type, message });
    if (logs.length > 500) logs.shift();
}

module.exports = {
    info: (msg) => addLog('INFO', msg),
    success: (msg) => addLog('SUCCESS', msg),
    warning: (msg) => addLog('WARNING', msg),
    error: (msg) => addLog('ERROR', msg),
    getLogs: () => logs,
    clearLogs: () => { logs.length = 0; }
};
