const multer = require('multer');
const sharp = require('sharp');
const path = require('path');
const fs = require('fs');

// IMPORTANT: Use absolute path for persistence
// This ensures files survive server restarts
const uploadDir = path.join(__dirname, '../../uploads/attendance');

// Create directory if it doesn't exist
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
  console.log(`✅ Upload directory created at: ${uploadDir}`);
}

// Configure multer storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Create unique filename: timestamp-userid-random.jpg
    const userId = req.user?.user_id || 'unknown';
    const uniqueName = `${Date.now()}-${userId}-${Math.random().toString(36).substring(7)}.jpg`;
    cb(null, uniqueName);
  }
});

// File filter - only accept images
const fileFilter = (req, file, cb) => {
  const allowedMimes = ['image/jpeg', 'image/jpg', 'image/png'];
  
  if (allowedMimes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('Only JPEG and PNG images are allowed'), false);
  }
};

// Multer configuration
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB max (in case frontend didn't compress)
  },
  fileFilter: fileFilter,
});

module.exports = { upload, uploadDir };