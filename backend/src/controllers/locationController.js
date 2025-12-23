const db = require('../config/database');
const { calculateDistance } = require('../utils/helpers');

const getAllLocations = async (req, res, next) => {
  try {
    const orgId = req.user.org_id;

    const result = await db.query(
      `SELECT * FROM locations 
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

const getLocationById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const orgId = req.user.org_id;

    const result = await db.query(
      'SELECT * FROM locations WHERE id = $1 AND organization_id = $2',
      [id, orgId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Location not found',
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

const createLocation = async (req, res, next) => {
  try {
    const { name, address, latitude, longitude, radius = 100 } = req.body;
    const orgId = req.user.org_id;

    const result = await db.query(
      `INSERT INTO locations (organization_id, name, address, latitude, longitude, radius)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [orgId, name, address, latitude, longitude, radius]
    );

    res.status(201).json({
      success: true,
      message: 'Location created successfully',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

const updateLocation = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { name, address, latitude, longitude, radius, is_active } = req.body;
    const orgId = req.user.org_id;

    const result = await db.query(
      `UPDATE locations 
       SET name = COALESCE($1, name),
           address = COALESCE($2, address),
           latitude = COALESCE($3, latitude),
           longitude = COALESCE($4, longitude),
           radius = COALESCE($5, radius),
           is_active = COALESCE($6, is_active),
           updated_at = NOW()
       WHERE id = $7 AND organization_id = $8
       RETURNING *`,
      [name, address, latitude, longitude, radius, is_active, id, orgId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Location not found',
      });
    }

    res.json({
      success: true,
      message: 'Location updated successfully',
      data: result.rows[0],
    });
  } catch (error) {
    next(error);
  }
};

const deleteLocation = async (req, res, next) => {
  try {
    const { id } = req.params;
    const orgId = req.user.org_id;

    const result = await db.query(
      'DELETE FROM locations WHERE id = $1 AND organization_id = $2 RETURNING id',
      [id, orgId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Location not found',
      });
    }

    res.json({
      success: true,
      message: 'Location deleted successfully',
    });
  } catch (error) {
    next(error);
  }
};

const getNearbyLocations = async (req, res, next) => {
  try {
    const { latitude, longitude, max_distance = 5000 } = req.query;
    const orgId = req.user.org_id;

    if (!latitude || !longitude) {
      return res.status(400).json({
        success: false,
        message: 'Latitude and longitude are required',
      });
    }

    const result = await db.query(
      'SELECT * FROM locations WHERE organization_id = $1 AND is_active = true',
      [orgId]
    );

    // Filter by distance
    const nearbyLocations = result.rows
      .map((location) => {
        const distance = calculateDistance(
          parseFloat(latitude),
          parseFloat(longitude),
          parseFloat(location.latitude),
          parseFloat(location.longitude)
        );

        return {
          ...location,
          distance: Math.round(distance),
        };
      })
      .filter((location) => location.distance <= parseFloat(max_distance))
      .sort((a, b) => a.distance - b.distance);

    res.json({
      success: true,
      data: nearbyLocations,
    });
  } catch (error) {
    next(error);
  }
};

const validateLocation = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { latitude, longitude } = req.body;

    const result = await db.query('SELECT * FROM locations WHERE id = $1', [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Location not found',
      });
    }

    const location = result.rows[0];
    const distance = calculateDistance(
      parseFloat(latitude),
      parseFloat(longitude),
      parseFloat(location.latitude),
      parseFloat(location.longitude)
    );

    const isValid = distance <= location.radius;

    res.json({
      success: true,
      data: {
        is_valid: isValid,
        distance: Math.round(distance),
        allowed_radius: location.radius,
        location_name: location.name,
      },
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getAllLocations,
  getLocationById,
  createLocation,
  updateLocation,
  deleteLocation,
  getNearbyLocations,
  validateLocation,
};