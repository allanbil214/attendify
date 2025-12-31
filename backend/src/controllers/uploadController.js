const path = require('path');
const sharp = require('sharp');
const fs = require('fs');

const uploadPhoto = async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file uploaded',
      });
    }

    const originalPath = req.file.path;
    const filename = req.file.filename;
    
    // Optional: Further compress on server if file is still large
    const fileSizeKB = req.file.size / 1024;
    
    if (fileSizeKB > 500) {
      // If file is larger than 500KB, compress it more
      await sharp(originalPath)
        .resize(800, 800, {
          fit: 'inside',
          withoutEnlargement: true,
        })
        .jpeg({ quality: 80 })
        .toFile(originalPath + '.tmp');
      
      // Replace original with compressed
      fs.unlinkSync(originalPath);
      fs.renameSync(originalPath + '.tmp', originalPath);
      
      console.log(`🗜️ Compressed image from ${fileSizeKB.toFixed(2)}KB`);
    }

    // Generate URL
    const photoUrl = `${req.protocol}://${req.get('host')}/uploads/attendance/${filename}`;

    res.json({
      success: true,
      message: 'Photo uploaded successfully',
      data: {
        filename: filename,
        url: photoUrl,
        size: req.file.size,
        originalSize: req.file.size,
      },
    });
  } catch (error) {
    // Clean up file if error occurs
    if (req.file) {
      fs.unlink(req.file.path, (err) => {
        if (err) console.error('Error deleting file:', err);
      });
    }
    
    next(error);
  }
};

// Optional: Delete photo endpoint (for cleanup)
const deletePhoto = async (req, res, next) => {
  try {
    const { filename } = req.params;
    const filePath = path.join(__dirname, '../../uploads/attendance', filename);

    // Check if file exists
    if (!fs.existsSync(filePath)) {
      return res.status(404).json({
        success: false,
        message: 'File not found',
      });
    }

    // Delete file
    fs.unlinkSync(filePath);

    res.json({
      success: true,
      message: 'Photo deleted successfully',
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  uploadPhoto,
  deletePhoto,
};