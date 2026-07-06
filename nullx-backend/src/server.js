const express = require('express');
const cors = require('cors');
const apiRoutes = require('./routes/api');
const logger = require('./utils/logger');
const config = require('./config/config');

const app = express();

app.use(cors());
app.use(express.json());

// Logging middleware
app.use((req, res, next) => {
    // Avoid logging repetitive health checks if needed, but we'll log it
    if (req.originalUrl !== '/health' && req.originalUrl !== '/logs') {
        logger.info(`${req.method} ${req.originalUrl}`);
    }
    next();
});

app.use('/', apiRoutes);
app.use('/api', apiRoutes);

app.listen(config.port, '0.0.0.0', () => {
    logger.info(`Backend Started`);
    logger.info(`Listening on 0.0.0.0:${config.port}`);
});
