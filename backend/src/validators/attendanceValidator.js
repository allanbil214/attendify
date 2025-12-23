const { body } = require('express-validator');

const checkInValidator = [
  body('location_id').isUUID(),
  body('latitude').isFloat({ min: -90, max: 90 }),
  body('longitude').isFloat({ min: -180, max: 180 }),
  body('note').optional().trim(),
];

const checkOutValidator = [
  body('attendance_id').isUUID(),
  body('latitude').isFloat({ min: -90, max: 90 }),
  body('longitude').isFloat({ min: -180, max: 180 }),
  body('note').optional().trim(),
];

module.exports = {
  checkInValidator,
  checkOutValidator,
};