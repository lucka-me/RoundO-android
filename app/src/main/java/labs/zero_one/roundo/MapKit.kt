package labs.zero_one.roundo

import android.content.Context
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.annotations.Icon
import com.minemap.minemapsdk.annotations.IconFactory
import com.minemap.minemapsdk.annotations.Marker
import com.minemap.minemapsdk.annotations.MarkerOptions
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
 * - [markerList]
 * - [tempMarkerOptionsList]
 * - [isFollowing]
 * - [markerIconList]
 *
 * ## 子类列表
 * - [MarkerType]
 *
 * ## 方法列表
 * - [initMap]
 * - [moveTo]
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
 * @property [tempMarkerOptionsList] 标记暂存列表
 * @property [markerList] 标记列表
 * @property [isFollowing] 中心是否跟随移动
 * @property [markerIconList] 标记样式列表
 */
class MapKit(private val context: Context) {

    private lateinit var mineMap: MineMap
    private var isMapInitialized = false
    private var markerList: ArrayList<Marker> = ArrayList(0)
    private var tempMarkerOptionsList: ArrayList<MarkerOptions> = ArrayList(0)
    var isFollowing = true

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
            isMapInitialized = true
            mineMap = newMap
            for (markerOptions in tempMarkerOptionsList) {
                markerList.add(mineMap.addMarker(markerOptions))
            }
            tempMarkerOptionsList.clear()
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
        if (!isMapInitialized) return
        mineMap.animateCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition
                .Builder()
                .target(LatLng(location))
                .build()
        ))
    }

    /**
     * 添加标记
     *
     * ## Changelog
     * ### 0.1.9
     * - 若地图还未初始化则将标记存入 [tempMarkerOptionsList]，待初始化后再加入
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
            tempMarkerOptionsList.add(markerOptions)
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
        if (!isMapInitialized && index < tempMarkerOptionsList.size) {
            tempMarkerOptionsList[index].icon = markerIconList[type.iconIndex]
        } else if (isMapInitialized && index < markerList.size) {
            markerList[index].icon = markerIconList[type.iconIndex]
        }
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