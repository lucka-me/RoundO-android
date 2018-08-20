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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/**
 * 主页面 Activity
 *
 * ## 属性列表
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
 *
 * @author lucka-me
 * @since 0.1
 *
 * @property [locationKit] 位置工具
 * @property [locationKitListener] 位置工具消息监听器
 */
class MainActivity : AppCompatActivity() {

    // MapKit
    private val mapKit = MapKit(this)

    // LocationKit
    private val locationKitListener = object : LocationKit.LocationKitListener {

        override fun onLocationUpdated(location: Location) {

            if (!mapKit.isCameraFree) {
                mapKit.moveTo(location)
            }

            missionManager.reach(location)

        }

        override fun onProviderDisabled() {
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle(getString(R.string.title_alert))
            alert.setMessage(getString(R.string.location_provider_disabled))
            alert.setCancelable(false)
            alert.setPositiveButton(getString(R.string.permission_system_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            alert.setNegativeButton(getString(R.string.confirm), null)
            alert.show()
        }

        override fun onProviderEnabled() {

        }

        override fun onException(error: Exception) {

        }
    }
    private lateinit var locationKit: LocationKit

    // MissionManager
    private val missionListener: MissionManager.MissionListener =
        object : MissionManager.MissionListener {

            override fun onStarted() {
                for (waypoint in missionManager.waypointList) {
                    mapKit.addMarkerAt(
                        waypoint.location,
                        if (waypoint.isChecked) MapKit.MarkerType.Checked
                        else MapKit.MarkerType.Unchecked
                    )
                }
                invalidateOptionsMenu()
            }

            override fun onStopped() {
                mapKit.clearMarkers()
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("结束")
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onStartFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.title_alert))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onStopFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.title_alert))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onChecked(indexList: List<Int>) {
                // Update markers
                var msg = "序号：" + indexList[0]
                if (indexList.size > 1) {
                    for (index in indexList) {
                        msg += ", $index"
                    }
                }
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("签到")
                alert.setMessage(msg)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                for (index in indexList) {
                    mapKit.changeMarkerIconAt(index, MapKit.MarkerType.Checked)
                }
            }

            override fun onFinishedAll() {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("全部完成！")
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                missionManager.stop()
            }

    }
    private val missionManager = MissionManager(this, missionListener)

    /**
     * 主菜单项
     *
     * ## 列表
     * - [StartStop]
     * - [Preference]
     *
     * @param [id] 菜单项资源 ID
     *
     * @author lucka
     * @since 0.1
     */
    private enum class MainMenu(val id: Int) {
        /**
         * 开始/停止
         */
        StartStop(R.id.menu_main_start_stop),
        /**
         * 设置
         */
        Preference(R.id.menu_main_preference)
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
        mapKit.initMap(mapView, locationKit, savedInstanceState)

        // Setup FAB
        if (mapKit.isCameraFree) {
            buttonResetCamera.show()
        } else {
            buttonResetCamera.hide()
        }
        buttonResetCamera.setOnClickListener {
            mapKit.moveTo(locationKit.lastLocation)
            mapKit.isCameraFree = false
            buttonResetCamera.hide()
        }
        mapKit.addOnMoveBeginListener {
            if (!mapKit.isCameraFree) {
                mapKit.isCameraFree = true
                buttonResetCamera.show()
            }
        }

    }

    override fun onPause() {
        missionManager.pause()
        locationKit.stopUpdate()

        super.onPause()
    }

    override fun onResume() {
        missionManager.resume()
        locationKit.startUpdate()
        invalidateOptionsMenu()

        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu == null) return super.onPrepareOptionsMenu(menu)
        when (missionManager.status) {

            MissionManager.MissionStatus.Starting -> {
                menu.findItem(MainMenu.StartStop.id)
                    .setIcon(R.drawable.ic_menu_stop)
                    .setTitle(R.string.menu_main_stop)
                    .isEnabled = false
                menu.findItem(MainMenu.StartStop.id).isEnabled = false
            }

            MissionManager.MissionStatus.Started -> {
                menu.findItem(MainMenu.StartStop.id)
                    .setIcon(R.drawable.ic_menu_stop)
                    .setTitle(R.string.menu_main_stop)
                    .isEnabled = true
                menu.findItem(MainMenu.StartStop.id).isEnabled = true
            }

            MissionManager.MissionStatus.Stopping -> {
                menu.findItem(MainMenu.StartStop.id)
                    .setIcon(R.drawable.ic_menu_start)
                    .setTitle(R.string.menu_main_start)
                    .isEnabled = false
                menu.findItem(MainMenu.StartStop.id).isEnabled = false
            }

            MissionManager.MissionStatus.Stopped -> {
                menu.findItem(MainMenu.StartStop.id)
                    .setIcon(R.drawable.ic_menu_start)
                    .setTitle(R.string.menu_main_start)
                    .isEnabled = true
                menu.findItem(MainMenu.StartStop.id).isEnabled = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // Handel the selection on Main Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            MainMenu.StartStop.id -> {
                when (missionManager.status) {

                    MissionManager.MissionStatus.Started -> {
                        missionManager.stop()
                    }

                    MissionManager.MissionStatus.Stopped -> {
                        val intent: Intent = Intent(this, SetupActivity::class.java)
                            .apply {  }
                        startActivityForResult(intent, AppRequest.ActivitySetup.code)
                    }

                    else -> {

                    }
                }

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
                    missionManager.start(locationKit.lastLocation)
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
}