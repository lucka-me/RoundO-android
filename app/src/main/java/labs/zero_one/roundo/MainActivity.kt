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
import android.support.v7.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_dashboard.view.*
import java.util.*

/**
 * 主页面 Activity
 *
 * ## 属性列表
 * - [mapKit]
 * - [locationKit]
 * - [locationKitListener]
 * - [missionManager]
 * - [missionListener]
 * - [dashboardLayout]
 *
 * ## 子类列表
 * - [AppRequest]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onPause]
 * - [onResume]
 * - [onActivityResult]
 * - [onRequestPermissionsResult]
 *
 * ## 自定义方法列表
 * - [openDashboard]
 *
 * @author lucka-me
 * @since 0.1
 *
 * @property [mapKit] 地图工具
 * @property [locationKit] 位置工具
 * @property [locationKitListener] 位置工具消息监听器
 * @property [missionManager] 任务管理器
 * @property [missionListener] 任务消息监听器
 * @property [dashboardLayout] 仪表盘视图
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
                    mapKit.moveTo(location, false)
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
                initDashboard()
                updateDashboard(checkedCount)
                // Update Progress Bar
                progressBar.isIndeterminate = false
                progressBar.max = missionManager.waypointList.size
                progressBar.progress = 0
                progressBar.secondaryProgress = 0
                progressBar.visibility = View.VISIBLE
                progressBar.incrementProgressBy(checkedCount)
                progressBar.incrementSecondaryProgressBy((
                    missionManager.data.pastTime
                        * missionManager.waypointList.size / missionManager.data.totalTime
                    )
                )

            }

            override fun onStopped() {
                mapKit.clearMarkers()
                progressBar.visibility = View.INVISIBLE

                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("结束")
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
            }

            override fun onStartFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.alert_title))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
            }

            override fun onStopFailed(error: Exception) {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle(getString(R.string.alert_title))
                alert.setMessage(error.message)
                alert.setCancelable(false)
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
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

                updateDashboard(indexList.size)
                progressBar.incrementProgressBy(indexList.size)
            }

            override fun onCheckedAll() {
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("全部完成！")
                alert.setPositiveButton(getString(R.string.confirm), null)
                alert.show()
                missionManager.stop()
            }

            override fun onTimeUpdated(pastTime: Long) {
                updateDashboard(0)
                progressBar.incrementSecondaryProgressBy(
                    (1.0 * pastTime
                        * missionManager.waypointList.size / missionManager.data.totalTime
                        ).toInt() - progressBar.secondaryProgress
                )

            }

            override fun onSecondUpdated() {
                updateDashboard(0)
            }

    }
    private val missionManager = MissionManager(this, missionListener)

    private lateinit var dashboardLayout: View

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

        // Setup the dashboard layout
        initDashboard()

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
        buttonPreference.setOnClickListener {
            when(missionManager.state) {
                MissionManager.MissionState.Started, MissionManager.MissionState.Stopped -> {
                    val intent: Intent = Intent(this, PreferenceMainActivity::class.java)
                        .apply {  }
                    startActivity(intent)
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
        locationKit.stopUpdate()
        missionManager.pause()

        super.onPause()
    }

    override fun onResume() {
        missionManager.onActivityResume()
        locationKit.startUpdate()

        super.onResume()
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
     * ## Changelog
     * ### 0.2.1
     * - 将 [dashboardLayout] 独立，实现实时刷新
     *
     * @author lucka-me
     * @since 0.1.14
     */
    private fun openDashboard() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.dashboard_title)
            .setIcon(getDrawable(R.drawable.ic_dashboard))
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                quitDashboardParent()
            }
        val dashboard = if (missionManager.state == MissionManager.MissionState.Started) {

            initDashboard()
            var checkedCount = 0
            for (waypoint in missionManager.waypointList) if (waypoint.isChecked) checkedCount++
            updateDashboard(checkedCount, true)

            dialogBuilder
                .setView(dashboardLayout)
                .setNegativeButton(R.string.dashboard_stop) { dialog, _ ->
                    dialog.dismiss()
                    quitDashboardParent()
                    missionManager.stop()
                }
                .show()

        } else {

            dialogBuilder
                .setMessage(R.string.dashboard_mission_stopped)
                .setNegativeButton(R.string.dashboard_start) { dialog, _ ->
                    dialog.dismiss()
                    val intent: Intent = Intent(this, SetupActivity::class.java)
                        .apply {  }
                    startActivityForResult(intent, AppRequest.ActivitySetup.code)
                    overridePendingTransition(R.anim.slide_bottom_up, R.anim.slide_bottom_down)
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

    /**
     * 初始化仪表盘
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun initDashboard() {
        dashboardLayout = View.inflate(this, R.layout.dialog_dashboard, null)

        dashboardLayout.missionProgressBar.progress = 0
        dashboardLayout.missionProgressBar.max = missionManager.waypointList.size
        dashboardLayout.missionTimeProgressBar.progress = 0
        dashboardLayout.missionTimeProgressBar.max = missionManager.data.totalTime
        dashboardLayout.missionTimeProgressBar.secondaryProgress = 0
    }

    /**
     * 更新仪表盘
     *
     * @param [checkedCount] 新签到的任务点数量
     * @param [force] 强制更新
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun updateDashboard(checkedCount: Int, force: Boolean = false) {

        if (!isDashboardShown() && !force) return

        val showSecond = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean(getString(R.string.pref_display_show_second_key), false)

        dashboardLayout.missionProgressBar
            .incrementProgressBy(checkedCount)
        dashboardLayout.missionProgressText.text = String.format(
            getString(R.string.dashboard_mission_progress_text),
            dashboardLayout.missionProgressBar.progress, missionManager.waypointList.size
        )

        // Mission Time
        val realPastTime = ((Date().time - missionManager.data.startTime.time) / 1000).toInt()
        val cal = Calendar.getInstance()
        cal.time = missionManager.data.startTime
        dashboardLayout.missionTimeProgressText.text = String.format(
            getString(R.string.dashboard_mission_time_progress_text),
            timeToString(missionManager.data.pastTime, showSecond),
            timeToString(missionManager.data.totalTime, showSecond)
        )
        dashboardLayout.missionStartTimeText.text = if (showSecond) {
            String.format(
                getString(R.string.format_time_sec),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)
            )
        } else {
            String.format(
                getString(R.string.format_time),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
            )
        }
        dashboardLayout.missionRealPastTimeText.text = timeToString(realPastTime, showSecond)
        dashboardLayout.missionTimeProgressBar.incrementProgressBy(missionManager.data.pastTime
            - dashboardLayout.missionTimeProgressBar.progress)
        dashboardLayout.missionTimeProgressBar.incrementSecondaryProgressBy(realPastTime
            - dashboardLayout.missionTimeProgressBar.secondaryProgress)

    }

    /**
     * 将仪表盘从其亲视图移除，仅此才可以置入新的对话框视图中
     *
     * @see <a href="https://stackoverflow.com/a/28071422">Stack Overflow</a>
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun quitDashboardParent() {
        if (isDashboardShown())
            (dashboardLayout.parent as ViewGroup).removeView(dashboardLayout)
    }

    private fun isDashboardShown(): Boolean {
        return dashboardLayout.parent != null
    }

    /**
     * 将秒数转换成格式化字符串
     *
     * HH:mm:ss 或 HH:mm
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun timeToString(second: Int, showSecond: Boolean): String {
        val hrs: Int = second / 3600
        val min: Int = (second - hrs * 3600) / 60
        return if (showSecond) {
            val sec: Int = second - hrs * 3600 - min * 60
            String.format(getString(R.string.format_time_sec), hrs, min, sec)
        } else {
            String.format(getString(R.string.format_time), hrs, min)
        }
    }
}