package labs.zero_one.roundo

import android.content.Context
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import com.minemap.android.gestures.MoveGestureDetector
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.annotations.Icon
import com.minemap.minemapsdk.annotations.IconFactory
import com.minemap.minemapsdk.annotations.Marker
import com.minemap.minemapsdk.annotations.MarkerOptions
import com.minemap.minemapsdk.camera.CameraPosition
import com.minemap.minemapsdk.camera.CameraUpdateFactory
import com.minemap.minemapsdk.geometry.LatLng
import com.minemap.minemapsdk.geometry.LatLngBounds
import com.minemap.minemapsdk.maps.MapView
import com.minemap.minemapsdk.maps.MineMap

/**
 * 地图工具，封装 MineMap，提供简化接口
 *
 * ## 属性列表
 * - [mineMap]
 * - [isMapInitialized]
 * - [markerList]
 * - [onMapInitializedList]
 * - [isCameraFree]
 * - [markerIconList]
 *
 * ## 子类列表
 * - [MarkerType]
 *
 * ## 方法列表
 * - [initMap]
 * - [addOnMapInitialized]
 * - [addOnMoveBeginListener]
 * - [moveTo]
 * - [resetZoomAndCenter]
 * - [add]
 * - [addMarkerAt]
 * - [changeMarkerIconAt]
 * - [clearMarkers]
 *
 * @param [context] 环境
 *
 * @author lucka-me
 * @since 0.1.6
 *
 * @property [mineMap] 地图控制器
 * @property [isMapInitialized] [mineMap] 是否成功初始化
 * @property [onMapInitializedList] 地图初始化完成回调事件列表
 * @property [markerList] 标记列表
 * @property [isCameraFree] 中心是否自由
 * @property [markerIconList] 标记样式列表
 */
class MapKit(private val context: Context) {

    private lateinit var mineMap: MineMap
    private var isMapInitialized = false
    private var markerList: ArrayList<Marker> = ArrayList(0)
    //private var tempMarkerOptionsList: ArrayList<MarkerOptions> = ArrayList(0)
    private var onMapInitializedList: ArrayList<(MineMap) -> Unit> = ArrayList(0)
    var isCameraFree = false

    /**
     * 标记类型
     *
     * ## 列表
     * - [Unchecked] 未打卡点，橙色
     * - [Checked] 已打卡点，绿色
     *
     * @param [iconIndex] 图标在 [markerIconList] 中的序号
     *
     * @author lucka-me
     * @since 0.1.10
     */
    enum class MarkerType(val iconIndex: Int) {
        Unchecked(0),
        Checked(1)
    }
    private lateinit var markerIconList: Array<Icon>

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
            "4810"
        )
        markerIconList = arrayOf(
            IconFactory.getInstance(context).fromResource(R.mipmap.ic_marker_unchecked),
            IconFactory.getInstance(context).fromResource(R.mipmap.ic_marker_checked)
        )
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { newMap ->
            if (newMap == null) throw Exception(context.getString(R.string.err_map_init_failed))
            mineMap = newMap
            isMapInitialized = true
            /*
            for (markerOptions in tempMarkerOptionsList) {
                markerList.add(mineMap.addMarker(markerOptions))
            }
            tempMarkerOptionsList.clear()*/
            mineMap.setStyleUrl("http://minedata.cn/service/solu/style/id/4810")
            mineMap.uiSettings.isCompassEnabled = true
            mineMap.uiSettings.setCompassMargins(
                mineMap.uiSettings.compassMarginLeft,
                (16 * Resources.getSystem().displayMetrics.density).toInt(),
                (16 * Resources.getSystem().displayMetrics.density).toInt(),
                mineMap.uiSettings.compassMarginBottom)
            mineMap.setMaxZoomPreference(17.0)
            mineMap.setMinZoomPreference(3.0)
            mineMap.cameraPosition =
                CameraPosition.Builder()
                    .target(LatLng(locationKit.lastLocation))
                    .zoom(14.0)
                    .build()
            // Handle the callbacks
            for (callback in onMapInitializedList) {
                callback(mineMap)
            }
            onMapInitializedList.clear()
        }
    }

    /**
     * 添加地图初始化完成回调事件，当 [mineMap] 初始化完成后将会依次执行；若初始化已完成则直接执行
     *
     * @param [callback] 回调事件
     *
     * @return 当前的回调事件列表 [onMapInitializedList] 长度，若初始化已完成则返回 0
     *
     * @author lucka-me
     * @since 0.1.11
     */
    fun addOnMapInitialized(callback: (MineMap) -> Unit): Int {
        return if (!isMapInitialized) {
            onMapInitializedList.add(callback)
            onMapInitializedList.size - 1
        } else {
            callback.invoke(mineMap)
            0
        }
    }

    /**
     * 添加地图开始移动监听器，封装 [MineMap.OnMoveListener.onMoveBegin]，仅当用户移动地图时触发
     *
     * @param [listener] 监听器
     *
     * @author lucka-me
     * @since 0.1.11
     */
    fun addOnMoveBeginListener(listener: () -> Unit) {
        if (isMapInitialized) {
            mineMap.addOnMoveListener(object : MineMap.OnMoveListener {
                override fun onMoveBegin(p0: MoveGestureDetector?) {
                    listener()
                }
                override fun onMove(p0: MoveGestureDetector?) {}
                override fun onMoveEnd(p0: MoveGestureDetector?) {}
            })
        } else {
            addOnMapInitialized {
                addOnMoveBeginListener(listener)
            }
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
        } else {
            addOnMapInitialized {
                moveTo(location)
            }
        }

    }

    /**
     * 重置缩放和中心
     *
     * @param [waypointList] 任务点列表
     *
     * @author lucka-me
     * @since 0.1.13
     */
    fun resetZoomAndCenter(waypointList: ArrayList<Waypoint>) {
        var latNorth = waypointList[0].location.latitude
        var lonEast = waypointList[0].location.longitude
        var latSouth = waypointList[0].location.latitude
        var lonWest = waypointList[0].location.longitude
        for (waypoint in waypointList) {
            latNorth = if (waypoint.location.latitude > latNorth) waypoint.location.latitude else latNorth
            lonEast = if (waypoint.location.longitude > lonEast) waypoint.location.longitude else lonEast
            latSouth = if (waypoint.location.latitude < latSouth) waypoint.location.latitude else latSouth
            lonWest = if (waypoint.location.longitude < lonWest) waypoint.location.longitude else lonWest
        }
        val cameraUpdateFactory = CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.from(latNorth,  lonEast,  latSouth,  lonWest),
            (16 * Resources.getSystem().displayMetrics.density).toInt()
        )

        if (isMapInitialized) {
            mineMap.animateCamera(cameraUpdateFactory)
        } else {
            addOnMapInitialized {
                mineMap.animateCamera(cameraUpdateFactory)
            }
        }
    }

    /**
     * 添加标记
     *
     * ## Changelog
     * ### 0.1.9
     * - 若地图还未初始化则将标记存入 tempMarkerOptionsList，待初始化后再加入
     * ### 0.1.11
     * - 废除 tempMarkerOptionsList，改为使用 [addOnMapInitialized]
     *
     * @param [markerOptions] 标记
     *
     * @author lucka-me
     * @since 0.1.7
     */
    fun add(markerOptions: MarkerOptions) {
        if (isMapInitialized) {
            markerList.add(mineMap.addMarker(markerOptions))
        } else {
            //tempMarkerOptionsList.add(markerOptions)
            addOnMapInitialized {
                markerList.add(mineMap.addMarker(markerOptions))
            }
        }
    }

    /**
     * 在指定位置添加默认标记
     *
     * @param [location] 目标位置
     *
     * @author lucka-me
     * @since 0.1.7
     */
    fun addMarkerAt(location: Location) {
        add(MarkerOptions().position(LatLng(location)))
    }

    /**
     * 在指定位置添加指定标记
     *
     * @param [location] 目标位置
     * @param [type] 标记类型
     *
     * @author lucka-me
     * @since 0.1.10
     */
    fun addMarkerAt(location: Location, type: MarkerType) {
        add(MarkerOptions().position(LatLng(location)).icon(markerIconList[type.iconIndex]))
    }

    /**
     * 修改指定标记的样式
     *
     * @param [index] 标记在列表中的序号
     * @param [type] 目的类型
     *
     * @author lucka-me
     * @since 0.1.10
     */
    fun changeMarkerIconAt(index: Int, type: MarkerType) {
        if (isMapInitialized && index < markerList.size) {
            markerList[index].icon = markerIconList[type.iconIndex]
        } else {
            addOnMapInitialized {
                changeMarkerIconAt(index, type)
            }
        }
        /*
        if (!isMapInitialized && index < tempMarkerOptionsList.size) {
            tempMarkerOptionsList[index].icon = markerIconList[type.iconIndex]
        } else if (isMapInitialized && index < markerList.size) {
            markerList[index].icon = markerIconList[type.iconIndex]
        }*/
    }

    /**
     * 清空标记
     *
     * @author lucka-me
     * @since 0.1.9
     */
    fun clearMarkers() {
        for (marker in markerList) {
            mineMap.removeMarker(marker)
        }
        markerList.clear()
    }
}