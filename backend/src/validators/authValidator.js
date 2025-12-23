const { body } = require('express-validator');

const registerValidator = [
  body('email').isEmail().normalizeEmail(),
  body('password').isLength({ min: 8 }).withMessage('Password must be at least 8 characters'),
  body('full_name').notEmpty().trim(),
  body('phone').optional().isMobilePhone(),
  body('organization_id').optional().isUUID(),
];

const loginValidator = [
  body('email').isEmail().normalizeEmail(),
  body('password').notEmpty(),
];

module.exports = {
  registerValidator,
  loginValidator,
};