const express = require('express');
const router = express.Router();
const attendanceController = require('../controllers/attendanceController');
const { checkInValidator, checkOutValidator } = require('../validators/attendanceValidator');
const { validate } = require('../middleware/validation');
const { authenticate } = require('../middleware/auth');

router.post('/check-in', authenticate, checkInValidator, validate, attendanceController.checkIn);
router.post('/check-out', authenticate, checkOutValidator, validate, attendanceController.checkOut);
router.get('/today', authenticate, attendanceController.getTodayAttendance);
router.get('/history', authenticate, attendanceController.getAttendanceHistory);
router.get('/:id', authenticate, attendanceController.getAttendanceById);
router.post('/bulk-sync', authenticate, attendanceController.bulkSyncAttendance);

module.exports = router;