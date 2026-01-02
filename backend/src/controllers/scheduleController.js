const db = require('../config/database');

// Get organization default work hours
const getOrganizationSchedule = async (req, res, next) => {
  try {
    const orgId = req.user.org_id;

    const result = await db.query(
      `SELECT default_work_hours, late_threshold_minutes 
       FROM organizations 
       WHERE id = $1`,
      [orgId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Organization not found',
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

// Update organization default work hours (Admin only)
const updateOrganizationSchedule = async (req, res, next) => {
  try {
    const orgId = req.user.org_id;
    const { default_work_hours, late_threshold_minutes } = req.body;

    const result = await db.query(
      `UPDATE organizations 
       SET default_work_hours = COALESCE($1, default_work_hours),
           late_threshold_minutes = COALESCE($2, late_threshold_minutes),
           updated_at = NOW()
       WHERE id = $3
       RETURNING default_work_hours, late_threshold_minutes`,
      [default_work_hours, late_threshold_minutes, orgId]
    );

    res.json({
      success: true,
      message: 'Organization schedule updated successfully',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

// Get user's work schedule
const getUserSchedule = async (req, res, next) => {
  try {
    const { userId } = req.params;
    const requestingUserId = req.user.user_id;
    const isAdmin = req.user.role === 'admin' || req.user.role === 'manager';

    // Users can only view their own schedule unless admin
    if (userId !== requestingUserId && !isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Insufficient permissions',
      });
    }

    // Get user info
    const userResult = await db.query(
      `SELECT employee_type, full_name FROM users WHERE id = $1`,
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'User not found',
      });
    }

    const user = userResult.rows[0];

    // Get custom schedule if exists
    const scheduleResult = await db.query(
      `SELECT * FROM work_schedules 
       WHERE user_id = $1 AND is_active = true
       ORDER BY day_of_week`,
      [userId]
    );

    // Get organization default schedule
    const orgResult = await db.query(
      `SELECT o.default_work_hours, o.late_threshold_minutes
       FROM users u
       JOIN organizations o ON u.organization_id = o.id
       WHERE u.id = $1`,
      [userId]
    );

    res.json({
      success: true,
      data: {
        user: {
          id: userId,
          full_name: user.full_name,
          employee_type: user.employee_type,
        },
        custom_schedule: scheduleResult.rows,
        organization_default: orgResult.rows[0],
        has_custom_schedule: scheduleResult.rows.length > 0,
      },
    });
  } catch (error) {
    next(error);
  }
};

// Set user's custom work schedule (Admin only)
const setUserSchedule = async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { schedules } = req.body; // Array of schedule objects

    if (!Array.isArray(schedules) || schedules.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Schedules array is required',
      });
    }

    const client = await db.pool.connect();

    try {
      await client.query('BEGIN');

      // Delete existing schedules
      await client.query(
        'DELETE FROM work_schedules WHERE user_id = $1',
        [userId]
      );

      // Insert new schedules
      for (const schedule of schedules) {
        await client.query(
          `INSERT INTO work_schedules 
           (user_id, day_of_week, start_time, end_time, is_working_day, break_duration_minutes)
           VALUES ($1, $2, $3, $4, $5, $6)`,
          [
            userId,
            schedule.day_of_week,
            schedule.start_time,
            schedule.end_time,
            schedule.is_working_day !== false, // Default to true
            schedule.break_duration_minutes || 0,
          ]
        );
      }

      await client.query('COMMIT');

      res.json({
        success: true,
        message: 'User schedule updated successfully',
      });
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  } catch (error) {
    next(error);
  }
};

// Delete user's custom schedule (revert to org default)
const deleteUserSchedule = async (req, res, next) => {
  try {
    const { userId } = req.params;

    await db.query(
      'DELETE FROM work_schedules WHERE user_id = $1',
      [userId]
    );

    res.json({
      success: true,
      message: 'Custom schedule deleted, reverted to organization default',
    });
  } catch (error) {
    next(error);
  }
};

// Update user employee type
const updateEmployeeType = async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { employee_type } = req.body;

    const validTypes = ['fixed', 'flexible', 'field_worker'];
    
    if (!validTypes.includes(employee_type)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid employee type. Must be: fixed, flexible, or field_worker',
      });
    }

    const result = await db.query(
      `UPDATE users 
       SET employee_type = $1, updated_at = NOW()
       WHERE id = $2
       RETURNING id, full_name, employee_type`,
      [employee_type, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'User not found',
      });
    }

    res.json({
      success: true,
      message: 'Employee type updated successfully',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

// Get all schedule templates
const getScheduleTemplates = async (req, res, next) => {
  try {
    const orgId = req.user.org_id;

    const result = await db.query(
      `SELECT * FROM schedule_templates 
       WHERE organization_id = $1 AND is_active = true
       ORDER BY name`,
      [orgId]
    );

    res.json({
      success: true,
      data: result.rows,
    });
  } catch (error) {
    next(error);
  }
};

// Apply schedule template to user
const applyScheduleTemplate = async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { templateId } = req.body;

    // Get template
    const templateResult = await db.query(
      'SELECT schedule_data FROM schedule_templates WHERE id = $1',
      [templateId]
    );

    if (templateResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Template not found',
      });
    }

    const scheduleData = templateResult.rows[0].schedule_data;

    const client = await db.pool.connect();

    try {
      await client.query('BEGIN');

      // Delete existing schedules
      await client.query(
        'DELETE FROM work_schedules WHERE user_id = $1',
        [userId]
      );

      // Insert schedules from template
      for (const schedule of scheduleData) {
        await client.query(
          `INSERT INTO work_schedules 
           (user_id, day_of_week, start_time, end_time, is_working_day, break_duration_minutes)
           VALUES ($1, $2, $3, $4, $5, $6)`,
          [
            userId,
            schedule.day,
            schedule.start,
            schedule.end,
            schedule.is_working_day,
            schedule.break_duration_minutes || 0,
          ]
        );
      }

      await client.query('COMMIT');

      res.json({
        success: true,
        message: 'Schedule template applied successfully',
      });
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  } catch (error) {
    next(error);
  }
};

// Get user's expected schedule for today
const getTodaySchedule = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const today = new Date();
    const dayOfWeek = today.getDay(); // 0=Sunday, 6=Saturday

    const result = await db.query(
      'SELECT * FROM get_user_schedule($1, $2)',
      [userId, dayOfWeek]
    );

    if (result.rows.length === 0) {
      return res.json({
        success: true,
        data: {
          is_working_day: false,
          message: 'Today is not a working day',
        },
      });
    }

    const schedule = result.rows[0];

    // Get user employee type
    const userResult = await db.query(
      'SELECT employee_type FROM users WHERE id = $1',
      [userId]
    );

    res.json({
      success: true,
      data: {
        employee_type: userResult.rows[0].employee_type,
        day_of_week: dayOfWeek,
        start_time: schedule.start_time,
        end_time: schedule.end_time,
        is_working_day: schedule.is_working_day,
        break_duration_minutes: schedule.break_duration_minutes,
        message: schedule.is_working_day 
          ? `Your work hours today: ${schedule.start_time} - ${schedule.end_time}`
          : 'Today is not a working day',
      },
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getOrganizationSchedule,
  updateOrganizationSchedule,
  getUserSchedule,
  setUserSchedule,
  deleteUserSchedule,
  updateEmployeeType,
  getScheduleTemplates,
  applyScheduleTemplate,
  getTodaySchedule,
};
