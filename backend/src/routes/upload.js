// src/routes/upload.js
const express = require('express');
const router = express.Router();
const { upload } = require('../config/upload');
const { uploadPhoto, deletePhoto } = require('../controllers/uploadController');
const { authenticate } = require('../middleware/auth');

// Upload photo
router.post('/photo', authenticate, upload.single('photo'), uploadPhoto);

// Delete photo (optional)
router.delete('/photo/:filename', authenticate, deletePhoto);

module.exports = router;