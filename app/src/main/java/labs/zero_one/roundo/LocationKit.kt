package labs.zero_one.roundo

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import com.minemap.minemapsdk.geometry.LatLng
import kotlin.math.*

/**
 * 位置工具，封装 LocationManager，简化相关接口和方法，并提供坐标系转换等特色功能
 *
 * ## 属性列表
 * - [lastLocation]
 * - [isLocationAvailable]
 * - [locationManager]
 * - [locationListener]
 * - [ellipsoidA]
 * - [ellipsoidEE]
 * - [locationProvider]
 *
 * ## 子类列表
 * - [locationKitListener]
 *
 * ## 方法列表
 * - [requestPermission]
 * - [showPermissionRequestDialog]
 * - [startUpdate]
 * - [stopUpdate]
 * - [fixCoordinate]
 *
 * @param [context] 环境
 * @param [locationKitListener] 消息监听器
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [lastLocation] 最新位置
 * @property [isLocationAvailable] 位置是否可用
 * @property [locationManager] 原生定位管理器
 * @property [locationListener] 原生定位消息监听器
 * @property [ellipsoidA] 椭球参数：长半轴（米）
 * @property [ellipsoidEE] 椭球参数：扁率
 * @property [earthR] 地球平均半径（米）
 * @property [locationProvider] 定位 Provider
 */
class LocationKit(
    private var context: Context,
    private val locationKitListener: LocationKitListener
) {

    var lastLocation: Location = Location("")
    var isLocationAvailable: Boolean = false
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location?) {


            if (TrumeKit.checkMock(location)) {
                val error = Exception(context.getString(R.string.err_location_mock))
                isLocationAvailable = false
                locationKitListener.onException(error)
                return
            }
            if (location == null) {
                isLocationAvailable = false
                return
            }
            lastLocation = fixCoordinate(location)
            isLocationAvailable = true
            locationKitListener.onLocationUpdated(lastLocation)
            LatLng().wrap()
        }

        override fun onProviderDisabled(provider: String?) {
            if (provider == locationProvider) {
                isLocationAvailable = false
                locationKitListener.onProviderDisabled()
            }
        }

        override fun onProviderEnabled(provider: String?) {
            if (provider == locationProvider) {
                isLocationAvailable = true
                locationKitListener.onProviderEnabled()
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }

    /**
     * 位置工具消息监听器
     *
     * ## 消息列表
     * - [onLocationUpdated]
     * - [onProviderDisabled]
     * - [onProviderEnabled]
     * - [onException]
     *
     * ## Changelog
     * ### 0.1.5
     * - Add [onException]
     *
     * @author lucka-me
     * @since 0.1.4
     */
    interface LocationKitListener {
        /**
         * 位置更新
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onLocationUpdated(location: Location)
        /**
         * 网络辅助定位被启用
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onProviderDisabled()
        /**
         * 网络辅助定位被禁用
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onProviderEnabled()
        /**
         * 返回错误
         *
         * @author lucka-me
         * @since 0.1.5
         */
        fun onException(error: Exception)
    }


    /**
     * 初始化
     *
     * 设置初始坐标（西安）
     *
     * @author lucka-me
     * @since 0.1.5
     */
    init {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(locationProvider)
            if (location == null) {
                lastLocation.longitude = 108.947031
                lastLocation.latitude = 34.259441
                isLocationAvailable = false
            } else {
                lastLocation = fixCoordinate(location)
                isLocationAvailable = true
                locationListener.onLocationChanged(lastLocation)
            }
            if (!locationManager.isProviderEnabled(locationProvider)) {
                locationKitListener.onProviderDisabled()
            }
        } else {
            lastLocation.longitude = 108.947031
            lastLocation.latitude = 34.259441
            isLocationAvailable = false
        }
    }

    /**
     * 请求权限
     *
     * @param [activity] 应用的 Activity
     *
     * @author lucka-me
     * @since 0.1.5
     */
    fun requestPermission(activity: MainActivity) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Explain if permission denied before
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showPermissionRequestDialog()
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MainActivity.AppRequest.PermissionLocation.code
                )
            }
        }
    }

    /**
     * 显示请求权限对话框
     *
     * @author lucka-me
     * @since 0.1.5
     */
    fun showPermissionRequestDialog() {
        val alert = AlertDialog.Builder(context)
        alert.setTitle(context.getString(R.string.permission_request_title))
        alert.setMessage(context.getString(R.string.permission_explain_location))
        alert.setCancelable(false)
        alert.setNegativeButton(context.getString(R.string.permission_system_settings)) { _, _ ->
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
        }
        alert.setPositiveButton(context.getString(R.string.confirm), null)
        alert.show()
    }

    /**
     * 开始定位
     *
     * @return 是否成功
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun startUpdate(): Boolean {
        return if (ActivityCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                locationProvider,
                1000.toLong(),
                1.0.toFloat(),
                locationListener
            )
            true
        } else {
            false
        }
    }

    /**
     * 停止定位
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun stopUpdate() {
        locationManager.removeUpdates(locationListener)
    }

    companion object {
        const val ellipsoidA = 6378137.0
        const val ellipsoidEE = 0.00669342162296594323
        const val earthR = 6372796.924
        const val locationProvider = LocationManager.NETWORK_PROVIDER

        /**
         * 将 WGS-84 坐标系转换为 GCJ-02 坐标系
         *
         * ## Changelog
         * ### 0.1.5
         * - [Location] 改为非空类型
         * ### 0.1.6
         * - 参数 [location] 引用的对象也会被转换
         * ### 0.1.7
         * - [Math.PI] -> [PI]
         * ### 0.1.10
         * - 修正算法
         * ### 0.3.1
         * - 作为静态函数提供
         *
         * @param [location] 待转换的位置
         *
         * @return 转换后的位置
         *
         * @see <a href="https://github.com/geosmart/coordtransform/blob/master/src/main/java/me/demo/util/geo/CoordinateTransformUtil.java">geosmart/coordtransform | Github</a>
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun fixCoordinate(location: Location): Location {
            val origLat = location.latitude
            val origLng = location.longitude
            // 不在国内不做转换
            if ((origLng < 72.004 || origLng > 137.8347) || (origLat < 0.8293 || origLat > 55.8271)) {
                return location
            }
            val lat = origLat - 35.0
            val lng = origLng - 105.0
            var dLat = (-100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat
                + 0.2 * sqrt(abs(lng))
                + (20.0 * sin(6.0 * lng * PI) + 20.0 * sin(2.0 * lng * PI))
                * 2.0 / 3.0 + (20.0 * sin(lat * PI) + 40.0 * sin(lat / 3.0 * PI))
                * 2.0 / 3.0 + (160.0 * sin(lat / 12.0 * PI)
                + 320 * sin(lat * PI / 30.0)) * 2.0 / 3.0)
            var dLng = (300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1
                * sqrt(abs(lng)) + (20.0 * sin(6.0 * lng * PI) + 20.0
                * sin(2.0 * lng * PI)) * 2.0 / 3.0 + (20.0 * sin(lng * PI) + 40.0
                * sin(lng / 3.0 * PI)) * 2.0 / 3.0 + (150.0 * sin(lng / 12.0 * PI)
                + 300.0 * sin(lng / 30.0 * PI)) * 2.0 / 3.0)
            val radLat = origLat / 180.0 * PI
            var magic = sin(radLat)
            magic = 1 - ellipsoidEE * magic * magic
            val sqrtMagic = sqrt(magic)
            dLat = (dLat * 180.0) / ((ellipsoidA * (1 - ellipsoidEE)) / (magic * sqrtMagic) * PI)
            dLng = (dLng * 180.0) / (ellipsoidA / sqrtMagic * cos(radLat) * PI)
            val fixedLat = origLat + dLat
            val fixedLng = origLng + dLng

            location.latitude = fixedLat
            location.longitude = fixedLng
            return location
        }
    }
}