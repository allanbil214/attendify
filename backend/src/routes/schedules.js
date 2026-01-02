const express = require('express');
const router = express.Router();
const scheduleController = require('../controllers/scheduleController');
const { authenticate, authorize } = require('../middleware/auth');

// Organization default schedule
router.get(
  '/organization',
  authenticate,
  scheduleController.getOrganizationSchedule
);

router.put(
  '/organization',
  authenticate,
  authorize('admin', 'manager'),
  scheduleController.updateOrganizationSchedule
);

// User schedules
router.get(
  '/users/:userId',
  authenticate,
  scheduleController.getUserSchedule
);

router.put(
  '/users/:userId',
  authenticate,
  authorize('admin', 'manager'),
  scheduleController.setUserSchedule
);

router.delete(
  '/users/:userId',
  authenticate,
  authorize('admin', 'manager'),
  scheduleController.deleteUserSchedule
);

router.put(
  '/users/:userId/employee-type',
  authenticate,
  authorize('admin', 'manager'),
  scheduleController.updateEmployeeType
);

// Schedule templates
router.get(
  '/templates',
  authenticate,
  scheduleController.getScheduleTemplates
);

router.post(
  '/templates/:templateId/apply/:userId',
  authenticate,
  authorize('admin', 'manager'),
  scheduleController.applyScheduleTemplate
);

// Today's schedule (for current user)
router.get(
  '/today',
  authenticate,
  scheduleController.getTodaySchedule
);

module.exports = router;