package labs.zero_one.roundo

import android.content.Context
import android.location.Location
import android.os.Bundle
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.camera.CameraPosition
import com.minemap.minemapsdk.camera.CameraUpdateFactory
import com.minemap.minemapsdk.geometry.LatLng
import com.minemap.minemapsdk.maps.MapView
import com.minemap.minemapsdk.maps.MineMap

/**
 * 地图工具，封装 MineMap，提供简化接口
 *
 * ## 属性列表
 * - [mineMap]
 * - [isMapInitialized]
 * - [isFollowing]
 *
 * ## 方法列表
 * - [initMap]
 * - [moveTo]
 *
 * @param [context] 环境
 *
 * @author lucka-me
 * @since 0.1.6
 *
 * @property [mineMap] 地图控制器
 * @property [isMapInitialized] [mineMap] 是否成功初始化
 * @property [isFollowing] 中心是否跟随移动
 */
class MapKit(private val context: Context) {

    private lateinit var mineMap: MineMap
    private var isMapInitialized = false
    var isFollowing = true

    /**
     * 初始化地图控制器
     *
     * @param [savedInstanceState] 初始化所需参数
     *
     * @throws Exception 地图初始化失败
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun initMap(mapView: MapView, locationKit: LocationKit, savedInstanceState: Bundle?) {
        MinemapAccountManager.getInstance(
            context,
            context.getString(R.string.minemap_token),
            "4807"
        )
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { newMap ->
            if (newMap == null) throw Exception(context.getString(R.string.err_map_init_failed))
            mineMap = newMap
            isMapInitialized = true
            mineMap.uiSettings.isCompassEnabled = true
            mineMap.cameraPosition =
                CameraPosition.Builder()
                    .target(LatLng(locationKit.lastLocation))
                    .zoom(16.0)
                    .build()
        }
    }

    /**
     * 将地图中心移动至目标位置
     *
     * @param [location] 目标位置
     *
     * @author lucka-me
     * @since 0.1.6
     */
    fun moveTo(location: Location){
        if (isMapInitialized) {
            mineMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition
                    .Builder()
                    .target(LatLng(location))
                    .build()
            ))
        }
    }
}