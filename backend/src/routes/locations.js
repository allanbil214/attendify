const express = require('express');
const router = express.Router();
const locationController = require('../controllers/locationController');
const { authenticate, authorize } = require('../middleware/auth');

router.get('/', authenticate, locationController.getAllLocations);
router.get('/nearby', authenticate, locationController.getNearbyLocations);
router.get('/:id', authenticate, locationController.getLocationById);
router.post('/', authenticate, authorize('admin', 'manager'), locationController.createLocation);
router.put('/:id', authenticate, authorize('admin', 'manager'), locationController.updateLocation);
router.delete('/:id', authenticate, authorize('admin'), locationController.deleteLocation);
router.post('/:id/validate', authenticate, locationController.validateLocation);

module.exports = router;