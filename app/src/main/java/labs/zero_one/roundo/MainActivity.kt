package labs.zero_one.roundo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.camera.CameraPosition
import com.minemap.minemapsdk.camera.CameraUpdateFactory
import com.minemap.minemapsdk.geometry.LatLng
import com.minemap.minemapsdk.maps.MineMap
import com.minemap.minemapsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/**
 * 主页面 Activity
 *
 * ## 属性列表
 * - [mineMap]
 * - [isMapInitialized]
 * - [locationKit]
 * - [locationKitListener]
 *
 * ## 子类列表
 * - [MainMenu]
 * - [AppRequest]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onPause]
 * - [onResume]
 * - [onCreateOptionsMenu]
 * - [onOptionsItemSelected]
 * - [onActivityResult]
 * - [onRequestPermissionsResult]
 *
 * ## 自定义方法列表
 * - [initMap]
 *
 * @author lucka-me
 * @since 0.1
 *
 * @property [mineMap] 地图控制器
 * @property [isMapInitialized] 地图是否初始化
 * @property [locationKit] 位置工具
 * @property [locationKitListener] 位置工具消息监听器
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mineMap: MineMap
    private var isMapInitialized = false
    private val locationKitListener = object : LocationKit.LocationKitListener {

        override fun onLocationUpdated(location: Location) {
            if (isMapInitialized) {
                mineMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition
                        .Builder()
                        .target(LatLng(location))
                        .build()
                ))
            }

        }

        override fun onProviderDisabled() {
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle(getString(R.string.title_alert))
            alert.setMessage(getString(R.string.location_provider_disabled))
            alert.setCancelable(false)
            alert.setNegativeButton(getString(R.string.permission_system_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            alert.setPositiveButton(getString(R.string.confirm), null)
            alert.show()
        }

        override fun onProviderEnabled() {

        }

        override fun onException(error: Exception) {

        }
    }
    private lateinit var locationKit: LocationKit

    /**
     * 主菜单项
     *
     * ## 列表
     * - [StartStop]
     * - [Preference]
     *
     * @param [index] 菜单项位置
     * @param [id] 菜单项资源 ID
     *
     * @author lucka
     * @since 0.1
     */
    private enum class MainMenu(val index: Int, val id: Int) {
        /**
         * 开始/停止
         */
        StartStop(0, R.id.menu_main_start_stop),
        /**
         * 设置
         */
        Preference(1, R.id.menu_main_preference)
    }

    /**
     * 请求代码
     *
     * ## 列表
     * - [ActivitySetup]
     * - [PermissionLocation]
     *
     * ## Changelog
     * ### 0.1.5
     * - 仅 Activity 请求代码 -> 所有请求代码
     *
     * @param [code] 请求代码
     *
     * @author lucka-me
     * @since 0.1.3
     */
    enum class AppRequest(val code: Int) {
        /**
         * Activity-准备
         */
        ActivitySetup(101),
        /**
         * 权限-定位
         */
        PermissionLocation(201)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        // Handle the permissions
        locationKit = LocationKit(this, locationKitListener)
        locationKit.requestPermission(this)

        // Setup Map
        MinemapAccountManager.getInstance(
            applicationContext,
            getString(R.string.minemap_token),
            "4807"
        )
        initMap(savedInstanceState)

    }

    override fun onPause() {
        locationKit.stopUpdate()
        super.onPause()
    }

    override fun onResume() {
        locationKit.startUpdate()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handel the selection on Main Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            MainMenu.StartStop.id -> {
                val intent: Intent = Intent(this, SetupActivity::class.java)
                    .apply {  }
                startActivityForResult(intent, AppRequest.ActivitySetup.code)
            }

            MainMenu.Preference.id -> {
                val intent: Intent = Intent(this, PreferenceMainActivity::class.java)
                    .apply {  }
                startActivity(intent)
            }
        }
        return when (item.itemId) {
            MainMenu.StartStop.id, MainMenu.Preference.id -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle the activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            AppRequest.ActivitySetup.code -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Start Mission
                    val alert = AlertDialog.Builder(this)
                    alert.setTitle("开始任务")
                    alert.setPositiveButton("确认", null)
                    alert.show()
                }
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            AppRequest.PermissionLocation.code -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationKit.startUpdate()
                } else {
                    locationKit.requestPermission(this)
                }
            }
        }

    }

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
    private fun initMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(OnMapReadyCallback { newMap: MineMap? ->
            if (newMap == null) throw Exception(getString(R.string.err_map_init_failed))
            mineMap = newMap
            isMapInitialized = true
            mineMap.uiSettings.isCompassEnabled = true
            mineMap.cameraPosition =
                CameraPosition.Builder()
                    .target(LatLng(locationKit.lastLocation))
                    .zoom(16.0)
                    .build()
        })
    }
}
