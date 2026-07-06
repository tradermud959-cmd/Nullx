const express = require('express');
const router = express.Router();
const chatController = require('../controllers/chatController');
const statusController = require('../controllers/statusController');

router.get('/health', statusController.health);
router.get('/status', statusController.status);
router.get('/logs', statusController.logs);
router.delete('/logs', statusController.clearLogs);
router.post('/chat', chatController.chat);

module.exports = router;
