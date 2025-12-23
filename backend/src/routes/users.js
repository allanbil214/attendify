const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { authenticate, authorize } = require('../middleware/auth');

router.get('/me', authenticate, userController.getCurrentUser);
router.put('/me', authenticate, userController.updateCurrentUser);
router.get('/', authenticate, authorize('admin', 'manager'), userController.getAllUsers);

module.exports = router;