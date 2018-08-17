package labs.zero_one.roundo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat

/**
 * 位置工具
 *
 * 封装 LocationManager，简化相关接口和方法，并提供坐标系转换等特色功能
 *
 * 属性列表
 * [lastLocation]
 * [isLocationAvailable]
 * [locationManager]
 * [locationListener]
 * [ellipsoidA]
 * [ellipsoidEE]
 *
 * 子类列表
 * [locationKitListener]
 *
 * 方法列表
 * [isPermissionGranted]
 * [startUpdate]
 * [stopUpdate]
 * [fixCoordinate]
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
 * @property [ellipsoidA] 椭球参数：长半轴
 * @property [ellipsoidEE] 椭球参数：扁率
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

        }

        override fun onProviderDisabled(provider: String?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }

    /**
     * 位置工具消息监听器
     *
     * 方法列表
     * [onLocationUpdated]
     * [onProviderDisabled]
     * [onProviderEnabled]
     *
     * @author lucka-me
     * @since 0.1.4
     */
    interface LocationKitListener {
        fun onLocationUpdated(location: Location)
        fun onProviderDisabled()
        fun onProviderEnabled()
    }

    /**
     * 检查定位权限
     *
     * @return 是否获得权限
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun isPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED)
    }

    /**
     * 开始定位
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun startUpdate() {

    }

    /**
     * 停止定位
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun stopUpdate() {

    }

    /**
     * 将 WGS-84 坐标系转换为 GCJ-02 坐标系
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
    private fun fixCoordinate(location: Location?): Location? {
        if (location == null) return location
        val lat = location.latitude
        val lng = location.longitude
        // 不在国内不做转换
        if ((lng < 72.004 || lng > 137.8347) || (lat < 0.8293 || lat > 55.8271)) {
            return location;
        }
        var dLat = (-100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat
            + 0.2 * Math.sqrt(Math.abs(lng))
            + (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI))
            * 2.0 / 3.0 + (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI))
            * 2.0 / 3.0 + (160.0 * Math.sin(lat / 12.0 * Math.PI)
            + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0)
        var dLng = (300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1
            * Math.sqrt(Math.abs(lng)) + (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0
            * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0 + (20.0 * Math.sin(lng * Math.PI) + 40.0
            * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0 + (150.0 * Math.sin(lng / 12.0 * Math.PI)
            + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0)
        val radLat = lat / 180.0 * Math.PI
        val magic = Math.sin(radLat)
        val sqrtmagic = Math.sqrt(magic)
        dLat = (dLat * 180.0) / ((ellipsoidA * (1 - ellipsoidEE)) / (magic * sqrtmagic) * Math.PI);
        dLng = (dLng * 180.0) / (ellipsoidA / sqrtmagic * Math.cos(radLat) * Math.PI);
        val fixedLat = lat + dLat
        val fixedLng = lng + dLng

        val fixedLocation = Location(location)
        fixedLocation.latitude = fixedLat
        fixedLocation.longitude = fixedLng
        return fixedLocation
    }

    /**
     * 纬度转换
     *
     * @param [lng] 原始经度
     * @param [lat] 原始纬度
     *
     * @return 转换后的纬度
     *
     * @see <a href="https://github.com/geosmart/coordtransform/blob/master/src/main/java/me/demo/util/geo/CoordinateTransformUtil.java">geosmart/coordtransform | Github</a>
     *
     * @author lucka-me
     * @since 0.1.4
     */
    private fun transformLat(lng: Double, lat: Double): Double {
        var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0
        return ret

    }

    /**
     * 经度转换
     *
     * @param [lng] 原始经度
     * @param [lat] 原始纬度
     *
     * @return 转换后的经度
     *
     * @see <a href="https://github.com/geosmart/coordtransform/blob/master/src/main/java/me/demo/util/geo/CoordinateTransformUtil.java">geosmart/coordtransform | Github</a>
     *
     * @author lucka-me
     * @since 0.1.4
     */
    private fun transformLng(lng: Double, lat: Double): Double {
        var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }

    companion object {
        private const val ellipsoidA = 6378245.0
        private const val ellipsoidEE = 0.00669342162296594323
    }
}