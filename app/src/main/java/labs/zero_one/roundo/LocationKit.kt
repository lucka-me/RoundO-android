package labs.zero_one.roundo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import kotlin.math.*

/**
 * 位置工具，封装 LocationManager，简化相关接口和方法，并提供坐标系转换等特色功能，可以在 GPS 和网络定位间自动切换。
 *
 * ## Changelog
 * ### 0.3.8
 * - Switch between GPS and Network provider automatically
 *
 * ## 属性列表
 * - [lastLocation]
 * - [isLocationAvailable]
 * - [locationManager]
 * - [currentProvider]
 * - [criteria]
 * - [locationListener]
 * - [assistLocationListener]
 * - [ELLIPSOID_A]
 * - [ELLIPSOID_EE]
 * - [EARTH_R]
 * - [FIXED_PROVIDER]
 * - [DEFAULT_LONGITUDE]
 * - [DEFAULT_LATITUDE]
 *
 * ## 子类列表
 * - [locationKitListener]
 *
 * ## 方法列表
 * - [requestPermission]
 * - [startUpdate]
 * - [stopUpdate]
 * - [fixCoordinate]
 * - [showRequestPermissionDialog]
 *
 * @param [context] 环境
 * @param [locationKitListener] 消息监听器
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [lastLocation] 最新位置（已修正）
 * @property [isLocationAvailable] 位置是否可用
 * @property [locationManager] 原生定位管理器
 * @property [currentProvider] 最新的定位源
 * @property [criteria] 定位要求，用于获取最佳定位源
 * @property [locationListener] 原生定位消息监听器
 * @property [assistLocationListener] 辅助监听器，用于切换定位源之后监听原有定位源，当原有定位源可用时向 [locationListener] 发送消息
 * @property [ELLIPSOID_A] 椭球参数：长半轴（米）
 * @property [ELLIPSOID_EE] 椭球参数：扁率
 * @property [EARTH_R] 地球平均半径（米）
 * @property [FIXED_PROVIDER] 修正坐标后位置的 Provider
 * @property [DEFAULT_LONGITUDE] 默认经度
 * @property [DEFAULT_LATITUDE] 默认维度
 */
class LocationKit(
    private var context: Context,
    private val locationKitListener: LocationKitListener
) {

    var lastLocation: Location = Location(FIXED_PROVIDER)
    var isLocationAvailable: Boolean = false
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var currentProvider = LocationManager.GPS_PROVIDER
    private val criteria = Criteria()
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
            Log.i("TESTRO", "精度：" + lastLocation.accuracy)
            isLocationAvailable = true
            locationKitListener.onLocationUpdated(lastLocation)
        }

        override fun onProviderDisabled(provider: String?) {

            Log.i("TESTRO", "定位不可用：" + provider)
            if (provider == currentProvider) {
                val newProvider = locationManager.getBestProvider(criteria ,true)
                if (newProvider != LocationManager.GPS_PROVIDER &&
                    newProvider != LocationManager.NETWORK_PROVIDER
                ) {
                    locationKitListener.onProviderDisabled()
                } else {
                    val oldProvider = currentProvider
                    currentProvider = newProvider
                    stopUpdate()
                    startUpdate(false)
                    startUpdateAssist(oldProvider)
                    Log.i("TESTRO", "切换至定位源：" + currentProvider)
                    locationKitListener.onProviderSwitchedTo(currentProvider)
                }
            }
        }

        override fun onProviderEnabled(provider: String?) {

            Log.i("TESTRO", "定位源可用：" + provider)
            val newProvider = locationManager.getBestProvider(criteria ,true)
            if (newProvider != LocationManager.GPS_PROVIDER &&
                newProvider != LocationManager.NETWORK_PROVIDER
            ) {
                locationKitListener.onProviderDisabled()
                currentProvider = LocationManager.GPS_PROVIDER
            } else if (newProvider != currentProvider) {
                currentProvider = newProvider
                stopUpdate()
                startUpdate(false)
                Log.i("TESTRO", "切换至定位源：" + currentProvider)
                locationKitListener.onProviderSwitchedTo(currentProvider)
            } else {
                locationKitListener.onProviderEnabled()
            }

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }

    private val assistLocationListener = object : LocationListener {
        override fun onProviderEnabled(provider: String?) {
            Log.i("TESTRO", "检测定位源可用：" + provider)
            locationListener.onProviderEnabled(provider)
            locationManager.removeUpdates(this)
        }
        override fun onLocationChanged(location: Location?) {}
        override fun onProviderDisabled(provider: String?) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    /**
     * 位置工具消息监听器
     *
     * ## 消息列表
     * - [onLocationUpdated]
     * - [onProviderDisabled]
     * - [onProviderSwitchedTo]
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
         * 定位被关闭
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onProviderDisabled()
        /**
         * 定位开启
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onProviderEnabled()
        /**
         * 定位源被切换
         *
         * @author lucka-me
         * @since 0.3.8
         */
        fun onProviderSwitchedTo(newProvider: String)
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
     * Note: 此处调用 getLastKnownLocation() 并修正坐标，会触发 onLocationChanged 并传入修正的坐标
     *       可通过修改 Provider 进行标记
     *
     * @author lucka-me
     * @since 0.1.5
     */
    init {
        criteria.accuracy = Criteria.ACCURACY_FINE
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Must get one whatever the provider is
            val location = locationManager
                .getLastKnownLocation(locationManager.getBestProvider(criteria, true))
            if (location == null) {
                lastLocation.longitude = DEFAULT_LONGITUDE
                lastLocation.latitude = DEFAULT_LATITUDE
                isLocationAvailable = false
            } else {
                lastLocation = fixCoordinate(location)
                isLocationAvailable = true
                locationListener.onLocationChanged(lastLocation)
            }
        } else {
            lastLocation.longitude = DEFAULT_LONGITUDE
            lastLocation.latitude = DEFAULT_LATITUDE
            isLocationAvailable = false
        }
    }

    /**
     * 请求权限
     *
     * ## Changelog
     * ### 0.3.3
     * - 返回是否需要显示请求权限对话框，由上层显示或做其他处理
     * ### 0.3.7
     *
     * @param [activity] 应用的 Activity
     * @param [requestCode] 请求代码
     *
     * @return 是否需要显示请求权限对话框
     *
     * @author lucka-me
     * @since 0.1.5
     */
    fun requestPermission(activity: MainActivity, requestCode: Int): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Explain if permission denied before
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                return true
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    requestCode
                )
            }
        }
        return false
    }

    /**
     * 开始定位
     *
     * @return 是否成功
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun startUpdate(resetProvider: Boolean = true): Boolean {
        if (resetProvider) {
            val newProvider = locationManager.getBestProvider(criteria ,true)
            if (newProvider == LocationManager.GPS_PROVIDER ||
                newProvider == LocationManager.NETWORK_PROVIDER
            ) {
                currentProvider = newProvider
            }
        }
        return if (ActivityCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                currentProvider,
                UPDATE_INTERVAL,
                UPDATE_DISTANCE,
                locationListener
            )
            true
        } else {
            val error = Exception(context.getString(R.string.err_location_permission_denied))
            locationKitListener.onException(error)
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

    /**
     * 开始用辅助监听器 [assistLocationListener] 监听定位源
     *
     * @param [provider] 要监听的定位源
     *
     * @author lucka-me
     * @since 0.3.8
     */
    private fun startUpdateAssist(provider: String) {
        if (ActivityCompat
            .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                provider,
                UPDATE_INTERVAL,
                UPDATE_DISTANCE,
                assistLocationListener
            )
        } else {
            val error = Exception(context.getString(R.string.err_location_permission_denied))
            locationKitListener.onException(error)
        }
    }

    companion object {
        const val ELLIPSOID_A = 6378137.0
        const val ELLIPSOID_EE = 0.00669342162296594323
        const val EARTH_R = 6372796.924
        const val UPDATE_INTERVAL: Long = 1000
        const val UPDATE_DISTANCE: Float = 1.0f
        const val FIXED_PROVIDER = "fixed"
        const val DEFAULT_LONGITUDE = 108.947031
        const val DEFAULT_LATITUDE = 34.259441

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
         * ### 0.3.2
         * - 不再修正参数 [location] 引用的对象
         * - 将修正的坐标 Provider 修改为 fixed 以供辨识，避免二次修正，同时在修正前进行辨识
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
            // 避免二次修正
            if (location.provider == FIXED_PROVIDER) return Location(location)
            val origLat = location.latitude
            val origLng = location.longitude
            // 不在国内不做转换
            if ((origLng < 72.004 || origLng > 137.8347) ||
                (origLat < 0.8293 || origLat > 55.8271)
            ) {
                val newLocation = Location(location)
                newLocation.provider = FIXED_PROVIDER
                return newLocation
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
            magic = 1 - ELLIPSOID_EE * magic * magic
            val sqrtMagic = sqrt(magic)
            dLat = (dLat * 180.0) / ((ELLIPSOID_A * (1 - ELLIPSOID_EE)) / (magic * sqrtMagic) * PI)
            dLng = (dLng * 180.0) / (ELLIPSOID_A / sqrtMagic * cos(radLat) * PI)
            val fixedLat = origLat + dLat
            val fixedLng = origLng + dLng
            val newLocation = Location(location)
            newLocation.provider = FIXED_PROVIDER
            newLocation.latitude = fixedLat
            newLocation.longitude = fixedLng
            return newLocation
        }

        /**
         * 显示请求权限对话框
         *
         * @param [activity] MainActivity
         *
         * @author lucka-me
         * @since 0.3.8
         */
        fun showRequestPermissionDialog(activity: MainActivity) {
            DialogKit.showDialog(
                activity,
                R.string.permission_request_title,
                R.string.permission_explain_location,
                positiveButtonTextId = R.string.confirm,
                negativeButtonTextId = R.string.permission_system_settings,
                negativeButtonListener = { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                },
                cancelable = false
            )
        }
    }
}