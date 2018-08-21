package labs.zero_one.roundo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_dashboard.view.*

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
    private val locationKitListener: LocationKit.LocationKitListener =
        object : LocationKit.LocationKitListener {

            override fun onLocationUpdated(location: Location) {

                if (!mapKit.isCameraFree &&
                    missionManager.state != MissionManager.MissionState.Started) {
                    mapKit.moveTo(location)
                }

                missionManager.reach(location)

            }

            override fun onProviderDisabled() {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.alert_title))
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
                var checkedCount = 0
                for (waypoint in missionManager.waypointList) {
                    mapKit.addMarkerAt(
                        waypoint.location,
                        if (waypoint.isChecked) {
                            checkedCount++
                            MapKit.MarkerType.Checked
                        } else {
                            MapKit.MarkerType.Unchecked
                        }
                    )
                }
                mapKit.resetZoomAndCenter(missionManager.waypointList)
                invalidateOptionsMenu()
                // Update Progress Bar
                progressBar.isIndeterminate = false
                progressBar.max = missionManager.waypointList.size
                progressBar.progress = 0
                progressBar.visibility = View.VISIBLE
                progressBar.incrementProgressBy(checkedCount)

            }

            override fun onStopped() {
                mapKit.clearMarkers()

                //progressBar.layoutParams.height = 0
                progressBar.visibility = View.INVISIBLE

                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("结束")
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onStartFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.alert_title))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onStopFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.alert_title))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                invalidateOptionsMenu()
            }

            override fun onChecked(indexList: List<Int>) {
                // Update markers
                var msg = "序号："
                for (index in indexList) {
                    msg += "$index "
                    mapKit.changeMarkerIconAt(index, MapKit.MarkerType.Checked)
                }

                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("签到")
                alert.setMessage(msg)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()

                progressBar.incrementProgressBy(indexList.size)
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
     * ## Changelog
     * ### 0.1.14
     * - 废除 StartStop
     *
     * ## 列表
     * - [Preference]
     *
     * @param [id] 菜单项资源 ID
     *
     * @author lucka
     * @since 0.1
     */
    private enum class MainMenu(val id: Int) {
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

        // Setup FABs
        if (mapKit.isCameraFree) {
            buttonResetCamera.show()
        } else {
            buttonResetCamera.hide()
        }
        buttonResetCamera.setOnClickListener {
            when(missionManager.state) {
                MissionManager.MissionState.Started -> {
                    mapKit.resetZoomAndCenter(missionManager.waypointList)
                }
                else -> {
                    mapKit.moveTo(locationKit.lastLocation)
                }
            }
            mapKit.isCameraFree = false
            buttonResetCamera.hide()
        }
        mapKit.addOnMoveBeginListener {
            if (!mapKit.isCameraFree) {
                mapKit.isCameraFree = true
                buttonResetCamera.show()
            }
        }
        buttonDashboard.setOnClickListener {
            when(missionManager.state) {
                MissionManager.MissionState.Started, MissionManager.MissionState.Stopped -> {
                    openDashboard()
                }
                else -> {

                }
            }
        }

        // Setup Progress Bar
        progressBar.visibility = View.INVISIBLE

        // Resume mission
        // Should resume mission here instead of in onPause()
        missionManager.resume()

    }

    override fun onPause() {
        missionManager.pause()
        locationKit.stopUpdate()

        super.onPause()
    }

    override fun onResume() {
        /*
        if (missionManager.status == MissionManager.MissionStatus.Stopped) {
            missionManager.resume()
        }*/
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
        when (missionManager.state) {

            MissionManager.MissionState.Starting -> {
                menu.findItem(MainMenu.Preference.id).isEnabled = false
            }

            MissionManager.MissionState.Started -> {
                menu.findItem(MainMenu.Preference.id).isEnabled = true
            }

            MissionManager.MissionState.Stopping -> {
                menu.findItem(MainMenu.Preference.id).isEnabled = false
            }

            MissionManager.MissionState.Stopped -> {
                menu.findItem(MainMenu.Preference.id).isEnabled = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // Handel the selection on Main Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            MainMenu.Preference.id -> {

                val intent: Intent = Intent(this, PreferenceMainActivity::class.java)
                    .apply {  }
                startActivity(intent)
            }
        }
        return when (item.itemId) {
            MainMenu.Preference.id -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle the activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            AppRequest.ActivitySetup.code -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Show progress Bar
                    progressBar.isIndeterminate = true
                    progressBar.visibility = View.VISIBLE
                    // Start Mission
                    missionManager.start(locationKit.lastLocation)
                    invalidateOptionsMenu()
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
     * 打开仪表盘对话框，若任务开始则显示仪表盘，否则显示未开始任务。
     *
     * @author lucka-me
     * @since 0.1.14
     */
    private fun openDashboard() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.dashboard_title)
            .setIcon(getDrawable(R.drawable.ic_dashboard))
            .setPositiveButton(R.string.confirm, null)
        val dashboard = if (missionManager.state == MissionManager.MissionState.Started) {

            var checkedCount = 0
            for (waypoint in missionManager.waypointList) if (waypoint.isChecked) checkedCount++
            // Setup layout
            val dashboardLayout = View.inflate(this, R.layout.dialog_dashboard, null)
            dashboardLayout.missionProgressBar.max = missionManager.waypointList.size
            dashboardLayout.missionProgressBar.progress = 0
            dashboardLayout.missionProgressText.text = String.format(
                getString(R.string.dashboard_mission_progress_text),
                checkedCount, missionManager.waypointList.size
            )
            dashboardLayout.missionProgressBar.incrementProgressBy(checkedCount)
            // Build
            dialogBuilder
                .setView(dashboardLayout)
                .setNegativeButton(R.string.dashboard_stop) { dialog, _ ->
                    dialog.dismiss()
                    missionManager.stop()
                }
                .show()

        } else {

            // Build
            dialogBuilder
                .setMessage(R.string.dashboard_mission_stopped)
                .setNegativeButton(R.string.dashboard_start) { dialog, _ ->
                    dialog.dismiss()
                    val intent: Intent = Intent(this, SetupActivity::class.java)
                        .apply {  }
                    startActivityForResult(intent, AppRequest.ActivitySetup.code)
                }
                .show()

        }

        // Set dialog style
        dashboard
            .getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        dashboard
            .getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
    }
}