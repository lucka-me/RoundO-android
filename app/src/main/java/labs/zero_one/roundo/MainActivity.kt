package labs.zero_one.roundo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/**
 * 主页面 Activity
 *
 * ## 属性列表
 * - [mapKit]
 * - [locationKit]
 * - [locationKitListener]
 * - [missionManager]
 * - [missionListener]
 * - [dashboard]
 *
 * ## 子类列表
 * - [AppRequest]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onPause]
 * - [onResume]
 * - [onBackPressed]
 * - [onActivityResult]
 * - [onRequestPermissionsResult]
 *
 * ## 自定义方法列表
 *
 * @author lucka-me
 * @since 0.1
 *
 * @property [mapKit] 地图工具
 * @property [locationKit] 位置工具
 * @property [locationKitListener] 位置工具消息监听器
 * @property [missionManager] 任务管理器
 * @property [missionListener] 任务消息监听器
 * @property [dashboard] 仪表盘
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
                DialogKit.showDialog(
                    this@MainActivity,
                    R.string.location_provider_disabled_title, R.string.location_provider_disabled_text,
                    negativeButtonTextId = R.string.permission_system_settings,
                    negativeButtonListener = { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    cancelable = false
                )
            }

            override fun onProviderEnabled() {

            }

            override fun onProviderSwitchedTo(newProvider: String) {

            }

            override fun onException(error: Exception) {
                when (error.message) {

                    getString(R.string.err_location_permission_denied) -> {
                        LocationKit.showRequestPermissionDialog(this@MainActivity)
                    }

                    else -> {
                        DialogKit.showSimpleAlert(this@MainActivity, error.message)
                    }

                }
            }
    }
    private lateinit var locationKit: LocationKit

    // MissionManager
    private val missionListener: MissionManager.MissionListener =
        object : MissionManager.MissionListener {

            override fun onStarted(isResumed: Boolean) {
                mapKit.clearMarkers()
                for (i in 0 until missionManager.checkPointList.size) {
                    mapKit.addMarkerAt(
                        missionManager.checkPointList[i].location,
                        type =
                        if (missionManager.checkPointList[i].checked) {
                            MapKit.MarkerType.Checked
                        } else {
                            MapKit.MarkerType.Unchecked
                        },
                        title = if (missionManager.data.sequential) (i + 1).toString() else null
                    )
                }
                if (missionManager.data.sequential &&
                    missionManager.data.checked < missionManager.checkPointList.size
                ) {
                    mapKit.changeMarkerIconAt(missionManager.data.checked, MapKit.MarkerType.Next)
                    mapKit.changeMarkerIconAt(
                        missionManager.checkPointList.size - 1,
                        MapKit.MarkerType.Final
                    )
                }
                mapKit.resetZoomAndCenter(missionManager.checkPointList)
                // Update Progress Bar
                progressBar.isIndeterminate = false
                progressBar.max = missionManager.checkPointList.size
                progressBar.progress = 0
                progressBar.secondaryProgress = 0
                progressBar.visibility = View.VISIBLE
                progressBar.incrementProgressBy(missionManager.data.checked)
                progressBar.incrementSecondaryProgressBy((
                    missionManager.data.pastTime
                        * missionManager.checkPointList.size / missionManager.data.targetTime
                    )
                )
                if (isResumed) {
                    Toast.makeText(
                        this@MainActivity, R.string.mission_resumed, Toast.LENGTH_LONG
                    ).show()
                } else {
                    dashboard.showWhenMissionStarted()
                }

            }

            override fun onStopped() {
                mapKit.drawTrack(missionManager.trackPointList)
                progressBar.visibility = View.INVISIBLE

                dashboard.showWhenMissionStopped(mapKit)
            }

            override fun onStartFailed(error: Exception) {
                DialogKit.showSimpleAlert(this@MainActivity, error.message)
            }

            override fun onStopFailed(error: Exception) {
                DialogKit.showSimpleAlert(this@MainActivity, error.message)
            }

            override fun onChecked(indexList: List<Int>) {
                // Update markers
                for (index in indexList) {
                    mapKit.changeMarkerIconAt(index, MapKit.MarkerType.Checked)
                }
                if (missionManager.data.sequential &&
                    missionManager.data.checked < missionManager.checkPointList.size - 1
                ) {
                    mapKit.changeMarkerIconAt(missionManager.data.checked, MapKit.MarkerType.Next)
                }

                val icon = getDrawable(R.drawable.ic_check)
                DrawableCompat.setTint(
                    icon, ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
                )
                DialogKit.showDialog(
                    this@MainActivity,
                    R.string.mission_checked_title,
                    String.format(
                        getString(R.string.mission_checked_message),
                        indexList.size, missionManager.data.checked,
                        missionManager.checkPointList.size - missionManager.data.checked
                    ),
                    R.string.confirm,
                    negativeButtonTextId = R.string.dashboard_title,
                    negativeButtonListener = { _, _ ->
                        dashboard.showWhenMissionStarted()
                    },
                    icon = icon
                )

                dashboard.update(indexList.size)
                progressBar.incrementProgressBy(indexList.size)
            }

            override fun onCheckedAll() {
                DialogKit.showDialog(
                    this@MainActivity,
                    R.string.mission_all_checked_title,
                    R.string.mission_all_checked_message,
                    R.string.dashboard_title,
                    positiveButtonListener = { _, _ ->
                        dashboard.showWhenMissionStarted()
                    },
                    icon = getDrawable(R.drawable.ic_mission_all_checked)
                )
            }

            override fun onTimeUpdated(pastTime: Long) {
                progressBar.incrementSecondaryProgressBy(
                    (1.0 * pastTime
                        * missionManager.checkPointList.size / missionManager.data.targetTime
                        ).toInt() - progressBar.secondaryProgress
                )

            }

            override fun onSecondUpdated() {
                dashboard.update()
            }

    }
    private val missionManager = MissionManager(this, missionListener)

    private lateinit var dashboard: Dashboard

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

        // Stop the service
        val backgroundMissionService =
            Intent(this, BackgroundMissionService::class.java)
        stopService(backgroundMissionService)

        // Check emulator
        if (TrumeKit.checkEmulator()) {
            DialogKit.showDialog(
                this,
                R.string.alert_title,
                R.string.err_emulator_detected,
                positiveButtonTextId = R.string.confirm,
                positiveButtonListener = { _, _ ->
                    // Exit the app
                    Process.killProcess(Process.myPid())
                    System.exit(0)
                },
                cancelable = false
            )
        }

        // Initialize Dashboard
        dashboard = Dashboard(this, missionManager)

        // Handle the location and permissions
        locationKit = LocationKit(this, locationKitListener)
        if (LocationKit.requestPermission(this, AppRequest.PermissionLocation.code)) {
            LocationKit.showRequestPermissionDialog(this)
        }

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
                    mapKit.resetZoomAndCenter(missionManager.checkPointList)
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

                MissionManager.MissionState.Started -> {
                    dashboard.showWhenMissionStarted()
                }

                MissionManager.MissionState.Stopped -> {
                    if (mapKit.isMarkersDisplaying()) {
                        dashboard.showWhenMissionStopped(mapKit)
                    } else {
                        dashboard.showWhenMissionCleared(this)
                    }
                }

                else -> {

                }
            }
        }
        buttonPreference.setOnClickListener {
            when(missionManager.state) {
                MissionManager.MissionState.Started, MissionManager.MissionState.Stopped -> {
                    startActivity(Intent(this, PreferenceMainActivity::class.java))
                }
                else -> {

                }
            }
        }


        // Setup Progress Bar
        progressBar.visibility = View.INVISIBLE

        // Resume mission
        // Should resume mission here instead of in onResume()
        // In onResume(), it should resume mission only when the mission is paused
        missionManager.resume()

    }

    override fun onPause() {
        locationKit.stopUpdate()
        missionManager.pause()
        if (missionManager.state == MissionManager.MissionState.Paused) {
            // Start the background service

            val backgroundMissionService =
                Intent(this, BackgroundMissionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(backgroundMissionService)
            else
                startService(backgroundMissionService)
        }

        super.onPause()
    }

    override fun onResume() {
        if (missionManager.state == MissionManager.MissionState.Paused) {
            // Stop the background service
            val backgroundMissionService =
                Intent(this, BackgroundMissionService::class.java)
            stopService(backgroundMissionService)

            missionManager.resume()
        }
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
                    mapKit.clearMarkers()
                    mapKit.removeTrackPolyline()
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
                    grantResults[0] != PackageManager.PERMISSION_GRANTED
                ) {
                    LocationKit.requestPermission(this, requestCode)
                }
            }
        }

    }
}