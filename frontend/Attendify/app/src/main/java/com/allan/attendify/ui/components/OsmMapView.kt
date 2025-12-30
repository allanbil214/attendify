package com.allan.attendify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.allan.attendify.domain.model.Location
import com.allan.attendify.ui.theme.GreenPrimary
import com.allan.attendify.ui.theme.BlueSecondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userLocation: GeoPoint?,
    officeLocations: List<Location>,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }
    
    // Convert Compose colors to Android Int colors
    val primaryColorInt = GreenPrimary.toArgb()
    val secondaryColorInt = BlueSecondary.toArgb()

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            onMapReady(mapView)
            mapView
        },
        update = { map ->
            map.overlays.clear()

            // Draw Office Locations
            officeLocations.forEach { loc ->
                val point = GeoPoint(loc.latitude, loc.longitude)
                
                // Radius Circle
                val circle = Polygon().apply {
                    points = Polygon.pointsAsCircle(point, loc.radius)
                    fillPaint.color = secondaryColorInt
                    fillPaint.alpha = 50
                    outlinePaint.color = primaryColorInt
                    outlinePaint.strokeWidth = 2f
                }
                map.overlays.add(circle)

                // Marker
                val marker = Marker(map)
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = loc.name
                marker.snippet = loc.address
                map.overlays.add(marker)
            }

            // Draw User Location
            userLocation?.let { userPos ->
                val userMarker = Marker(map)
                userMarker.position = userPos
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                userMarker.title = "You are here"
                map.overlays.add(userMarker)
                
                // Optional: Center map on user
                 map.controller.animateTo(userPos)
            }
            
            map.invalidate()
        }
    )
}
