const db = require('../config/database');

const getCurrentUser = async (req, res, next) => {
  try {
    const userId = req.user.user_id;

    const result = await db.query(
      `SELECT u.id, u.email, u.phone, u.full_name, u.employee_id, u.role, 
              u.avatar_url, u.is_active, u.created_at, u.last_login,
              o.name as organization_name, o.plan_type
       FROM users u
       LEFT JOIN organizations o ON u.organization_id = o.id
       WHERE u.id = $1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'User not found',
      });
    }

    res.json({
      success: true,
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

const updateCurrentUser = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const { full_name, phone, avatar_url } = req.body;

    const result = await db.query(
      `UPDATE users 
       SET full_name = COALESCE($1, full_name),
           phone = COALESCE($2, phone),
           avatar_url = COALESCE($3, avatar_url),
           updated_at = NOW()
       WHERE id = $4
       RETURNING id, email, full_name, phone, avatar_url`,
      [full_name, phone, avatar_url, userId]
    );

    res.json({
      success: true,
      message: 'Profile updated successfully',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

const getAllUsers = async (req, res, next) => {
  try {
    const orgId = req.user.org_id;
    const { page = 1, limit = 20, search } = req.query;
    const offset = (page - 1) * limit;

    let query = `
      SELECT u.id, u.email, u.full_name, u.employee_id, u.role, 
             u.is_active, u.last_login, u.created_at
      FROM users u
      WHERE u.organization_id = $1
    `;
    const params = [orgId];

    if (search) {
      query += ` AND (u.full_name ILIKE $${params.length + 1} OR u.email ILIKE $${params.length + 1})`;
      params.push(`%${search}%`);
    }

    // Get total count
    const countResult = await db.query(
      query.replace(
        'SELECT u.id, u.email, u.full_name, u.employee_id, u.role, u.is_active, u.last_login, u.created_at',
        'SELECT COUNT(*)'
      ),
      params
    );
    const totalRecords = parseInt(countResult.rows[0].count);

    // Get paginated results
    query += ` ORDER BY u.created_at DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await db.query(query, params);

    res.json({
      success: true,
      data: {
        users: result.rows,
        pagination: {
          current_page: parseInt(page),
          total_pages: Math.ceil(totalRecords / limit),
          total_records: totalRecords,
          per_page: parseInt(limit),
        },
      },
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getCurrentUser,
  updateCurrentUser,
  getAllUsers,
};