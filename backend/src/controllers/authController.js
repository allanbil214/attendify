const bcrypt = require('bcryptjs');
const db = require('../config/database');
const { generateAccessToken, generateRefreshToken, verifyRefreshToken } = require('../utils/jwt');

const register = async (req, res, next) => {
  try {
    const { email, password, full_name, phone, organization_id, role = 'employee' } = req.body;

    // Check if user exists
    const existingUser = await db.query('SELECT id FROM users WHERE email = $1', [email]);
    
    if (existingUser.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Email already registered',
      });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Create user
    const result = await db.query(
      `INSERT INTO users (email, password_hash, full_name, phone, organization_id, role)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, email, full_name, role, created_at`,
      [email, passwordHash, full_name, phone, organization_id, role]
    );

    const user = result.rows[0];

    // Generate tokens
    const accessToken = generateAccessToken({
      user_id: user.id,
      email: user.email,
      role: user.role,
      org_id: organization_id,
    });

    const refreshToken = generateRefreshToken({ user_id: user.id });

    // Save refresh token
    await db.query('UPDATE users SET refresh_token = $1 WHERE id = $2', [refreshToken, user.id]);

    res.status(201).json({
      success: true,
      message: 'Registration successful',
      data: {
        user,
        access_token: accessToken,
        refresh_token: refreshToken,
      },
    });
  } catch (error) {
    next(error);
  }
};

const login = async (req, res, next) => {
  try {
    const { email, password } = req.body;

    // Find user
    const result = await db.query(
      `SELECT u.*, o.name as organization_name 
       FROM users u 
       LEFT JOIN organizations o ON u.organization_id = o.id 
       WHERE u.email = $1 AND u.is_active = true`,
      [email]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Invalid credentials',
      });
    }

    const user = result.rows[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    
    if (!isValidPassword) {
      return res.status(401).json({
        success: false,
        message: 'Invalid credentials',
      });
    }

    // Generate tokens
    const accessToken = generateAccessToken({
      user_id: user.id,
      email: user.email,
      role: user.role,
      org_id: user.organization_id,
    });

    const refreshToken = generateRefreshToken({ user_id: user.id });

    // Update last login and refresh token
    await db.query(
      'UPDATE users SET last_login = NOW(), refresh_token = $1 WHERE id = $2',
      [refreshToken, user.id]
    );

    // Remove sensitive data
    delete user.password_hash;
    delete user.refresh_token;

    res.json({
      success: true,
      message: 'Login successful',
      data: {
        user,
        access_token: accessToken,
        refresh_token: refreshToken,
      },
    });
  } catch (error) {
    next(error);
  }
};

const refreshToken = async (req, res, next) => {
  try {
    const { refresh_token } = req.body;

    if (!refresh_token) {
      return res.status(400).json({
        success: false,
        message: 'Refresh token is required',
      });
    }

    // Verify refresh token
    const decoded = verifyRefreshToken(refresh_token);

    // Check if token exists in database
    const result = await db.query(
      'SELECT id, email, role, organization_id FROM users WHERE id = $1 AND refresh_token = $2',
      [decoded.user_id, refresh_token]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Invalid refresh token',
      });
    }

    const user = result.rows[0];

    // Generate new access token
    const newAccessToken = generateAccessToken({
      user_id: user.id,
      email: user.email,
      role: user.role,
      org_id: user.organization_id,
    });

    res.json({
      success: true,
      data: {
        access_token: newAccessToken,
      },
    });
  } catch (error) {
    next(error);
  }
};

const logout = async (req, res, next) => {
  try {
    await db.query('UPDATE users SET refresh_token = NULL WHERE id = $1', [req.user.user_id]);

    res.json({
      success: true,
      message: 'Logout successful',
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  register,
  login,
  refreshToken,
  logout,
};