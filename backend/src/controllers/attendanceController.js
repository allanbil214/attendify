const db = require('../config/database');
const { isWithinRadius, calculateWorkDuration } = require('../utils/helpers');

const checkIn = async (req, res, next) => {
  try {
    const { location_id, latitude, longitude, note, device_info } = req.body;
    const userId = req.user.user_id;

    // Check if already checked in today
    const existingAttendance = await db.query(
      `SELECT id FROM attendance_records 
       WHERE user_id = $1 
       AND DATE(check_in_time) = CURRENT_DATE 
       AND status = 'active'`,
      [userId]
    );

    if (existingAttendance.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Already checked in today',
      });
    }

    // Get location details
    const locationResult = await db.query(
      'SELECT * FROM locations WHERE id = $1 AND is_active = true',
      [location_id]
    );

    if (locationResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Location not found',
      });
    }

    const location = locationResult.rows[0];

    // Validate if user is within radius
    const withinRadius = isWithinRadius(
      latitude,
      longitude,
      parseFloat(location.latitude),
      parseFloat(location.longitude),
      location.radius
    );

    if (!withinRadius) {
      return res.status(400).json({
        success: false,
        message: 'You are not within the allowed location radius',
      });
    }

    // Check if late (example: work starts at 08:00)
    const currentTime = new Date();
    const workStartTime = new Date();
    workStartTime.setHours(8, 0, 0, 0);
    const isLate = currentTime > workStartTime;

    // Create attendance record
    const result = await db.query(
      `INSERT INTO attendance_records 
       (user_id, location_id, check_in_time, check_in_latitude, check_in_longitude, 
        check_in_note, is_late, device_info, status)
       VALUES ($1, $2, NOW(), $3, $4, $5, $6, $7, 'active')
       RETURNING *`,
      [userId, location_id, latitude, longitude, note, isLate, JSON.stringify(device_info)]
    );

    const attendance = result.rows[0];

    // Create activity log
    await db.query(
      `INSERT INTO activities 
       (user_id, attendance_id, activity_type, latitude, longitude, description)
       VALUES ($1, $2, 'check_in', $3, $4, $5)`,
      [userId, attendance.id, latitude, longitude, note || 'Checked in']
    );

    res.status(201).json({
      success: true,
      message: 'Check-in successful',
      data: {
        ...attendance,
        location: {
          name: location.name,
          address: location.address,
        },
      },
    });
  } catch (error) {
    next(error);
  }
};

const checkOut = async (req, res, next) => {
  try {
    const { attendance_id, latitude, longitude, note } = req.body;
    const userId = req.user.user_id;

    // Get attendance record
    const attendanceResult = await db.query(
      `SELECT * FROM attendance_records 
       WHERE id = $1 AND user_id = $2 AND status = 'active'`,
      [attendance_id, userId]
    );

    if (attendanceResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Active attendance record not found',
      });
    }

    const attendance = attendanceResult.rows[0];

    // Calculate work duration
    const duration = calculateWorkDuration(attendance.check_in_time, new Date());

    // Update attendance record
    const result = await db.query(
      `UPDATE attendance_records 
       SET check_out_time = NOW(),
           check_out_latitude = $1,
           check_out_longitude = $2,
           check_out_note = $3,
           work_duration = $4,
           status = 'completed',
           updated_at = NOW()
       WHERE id = $5
       RETURNING *`,
      [latitude, longitude, note, duration, attendance_id]
    );

    // Create activity log
    await db.query(
      `INSERT INTO activities 
       (user_id, attendance_id, activity_type, latitude, longitude, description)
       VALUES ($1, $2, 'check_out', $3, $4, $5)`,
      [userId, attendance_id, latitude, longitude, note || 'Checked out']
    );

    res.json({
      success: true,
      message: 'Check-out successful',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

const getTodayAttendance = async (req, res, next) => {
  try {
    const userId = req.user.user_id;

    const result = await db.query(
      `SELECT a.*, l.name as location_name, l.address as location_address
       FROM attendance_records a
       LEFT JOIN locations l ON a.location_id = l.id
       WHERE a.user_id = $1 
       AND DATE(a.check_in_time) = CURRENT_DATE
       ORDER BY a.check_in_time DESC`,
      [userId]
    );

    res.json({
      success: true,
      data: result.rows[0] || null,
    });
  } catch (error) {
    next(error);
  }
};

const getAttendanceHistory = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const { start_date, end_date, page = 1, limit = 20 } = req.query;

    const offset = (page - 1) * limit;

    let query = `
      SELECT a.*, l.name as location_name, l.address as location_address
      FROM attendance_records a
      LEFT JOIN locations l ON a.location_id = l.id
      WHERE a.user_id = $1
    `;
    const params = [userId];

    if (start_date && end_date) {
      query += ` AND DATE(a.check_in_time) BETWEEN $${params.length + 1} AND $${params.length + 2}`;
      params.push(start_date, end_date);
    }

    // Get total count
    const countResult = await db.query(
      query.replace('SELECT a.*, l.name as location_name, l.address as location_address', 'SELECT COUNT(*)'),
      params
    );
    const totalRecords = parseInt(countResult.rows[0].count);

    // Get paginated results
    query += ` ORDER BY a.check_in_time DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await db.query(query, params);

    // Calculate summary
    const summaryQuery = `
      SELECT 
        COUNT(*) as total_days,
        COUNT(CASE WHEN status = 'completed' THEN 1 END) as present_days,
        COUNT(CASE WHEN is_late = true THEN 1 END) as late_days,
        COALESCE(AVG(work_duration), 0) as average_duration
      FROM attendance_records
      WHERE user_id = $1
      ${start_date && end_date ? `AND DATE(check_in_time) BETWEEN $2 AND $3` : ''}
    `;
    const summaryResult = await db.query(
      summaryQuery,
      start_date && end_date ? [userId, start_date, end_date] : [userId]
    );

    res.json({
      success: true,
      data: {
        records: result.rows,
        pagination: {
          current_page: parseInt(page),
          total_pages: Math.ceil(totalRecords / limit),
          total_records: totalRecords,
          per_page: parseInt(limit),
        },
        summary: summaryResult.rows[0],
      },
    });
  } catch (error) {
    next(error);
  }
};

const getAttendanceById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.user_id;
    const isAdmin = req.user.role === 'admin' || req.user.role === 'manager';

    let query = `
      SELECT a.*, l.name as location_name, l.address as location_address,
             u.full_name as user_name, u.employee_id
      FROM attendance_records a
      LEFT JOIN locations l ON a.location_id = l.id
      LEFT JOIN users u ON a.user_id = u.id
      WHERE a.id = $1
    `;
    const params = [id];

    if (!isAdmin) {
      query += ` AND a.user_id = $2`;
      params.push(userId);
    }

    const result = await db.query(query, params);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Attendance record not found',
      });
    }

    // Get activities for this attendance
    const activitiesResult = await db.query(
      `SELECT * FROM activities 
       WHERE attendance_id = $1 
       ORDER BY created_at ASC`,
      [id]
    );

    res.json({
      success: true,
      data: {
        ...result.rows[0],
        activities: activitiesResult.rows,
      },
    });
  } catch (error) {
    next(error);
  }
};

const bulkSyncAttendance = async (req, res, next) => {
  try {
    const { records } = req.body;
    const userId = req.user.user_id;

    if (!Array.isArray(records) || records.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Records array is required',
      });
    }

    const client = await db.pool.connect();
    const syncedRecords = [];
    const errors = [];

    try {
      await client.query('BEGIN');

      for (const record of records) {
        try {
          // Check if already exists (by local ID or timestamp)
          const existing = await client.query(
            `SELECT id FROM attendance_records 
             WHERE user_id = $1 AND check_in_time = $2`,
            [userId, record.check_in_time]
          );

          if (existing.rows.length > 0) {
            errors.push({
              local_id: record.local_id,
              error: 'Duplicate record',
            });
            continue;
          }

          // Insert record
          const result = await client.query(
            `INSERT INTO attendance_records 
             (user_id, location_id, check_in_time, check_in_latitude, check_in_longitude,
              check_in_note, check_out_time, check_out_latitude, check_out_longitude,
              check_out_note, status, work_duration)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
             RETURNING id`,
            [
              userId,
              record.location_id,
              record.check_in_time,
              record.check_in_latitude,
              record.check_in_longitude,
              record.check_in_note,
              record.check_out_time,
              record.check_out_latitude,
              record.check_out_longitude,
              record.check_out_note,
              record.status,
              record.work_duration,
            ]
          );

          syncedRecords.push({
            local_id: record.local_id,
            server_id: result.rows[0].id,
          });
        } catch (err) {
          errors.push({
            local_id: record.local_id,
            error: err.message,
          });
        }
      }

      await client.query('COMMIT');

      res.json({
        success: true,
        message: `Synced ${syncedRecords.length} records`,
        data: {
          synced: syncedRecords,
          errors,
        },
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

module.exports = {
  checkIn,
  checkOut,
  getTodayAttendance,
  getAttendanceHistory,
  getAttendanceById,
  bulkSyncAttendance,
};