package labs.zero_one.roundo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import org.jetbrains.anko.runOnUiThread
import java.io.*
import java.util.*

/**
 * 后台服务，目前能在以下情况中运行：
 * - App 未被停止/清除
 *
 * 每隔5秒或成功签到时保存一次任务数据。
 *
 * ## Changelog
 * ### 0.3.4
 * - 作为前台服务运行
 *
 * ## 属性列表
 * - [missionData]
 * - [startTime]
 * - [checkPointList]
 * - [trackPointList]
 * - [locationKitListener]
 * - [locationKit]
 * - [timer]
 * - [notificationManager]
 * - [notificationId]
 * - [providerDisabledNotificationId]
 *
 * ## 重写方法列表
 * - [onStartCommand]
 * - [onDestroy]
 * - [onBind]
 *
 * ## 自定义方法列表
 * - [saveMission]
 *
 * @see <a href="https://stackoverflow.com/a/44705829">Foreground service notification in Android O | Stack Overflow</a>
 *
 * @author lucka-me
 * @since 0.3.3
 *
 * @property [missionData] 任务数据
 * @property [startTime] 任务在后台的开始时间（每次保存时会刷新）
 * @property [checkPointList] 签到点列表
 * @property [trackPointList] 轨迹点列表
 * @property [locationKitListener] 位置工具监听器
 * @property [locationKit] 位置工具
 * @property [timer] 计时器，用于定时保存数据
 * @property [notificationManager] 通知管理器
 * @property [notificationId] 通知 ID
 * @property [providerDisabledNotificationId] 定位不可用时发出的通知的 ID，用于在定位可用后自动取消通知
 */
class BackgroundMissionService : Service() {

    private var missionData: MissionManager.MissionData = MissionManager.MissionData()
    private var startTime = Date()
    private var checkPointList: ArrayList<CheckPoint> = ArrayList(0)
    private var trackPointList: ArrayList<TrackPoint> = ArrayList(0)

    private val locationKitListener: LocationKit.LocationKitListener =
        object : LocationKit.LocationKitListener {
            override fun onLocationUpdated(location: Location) {
                MissionManager.processCORC(location, trackPointList)

                val newCheckedIndexList: ArrayList<Int> = ArrayList(0)
                var totalCheckedCount = 0
                if (missionData.sequential) {
                    for (i: Int in missionData.checked until checkPointList.size) {
                        if (location.distanceTo(checkPointList[i].location) < 40) {
                            checkPointList[i].checked = true
                            newCheckedIndexList.add(i)
                        } else {
                            break
                        }
                    }
                    missionData.checked += newCheckedIndexList.size
                    totalCheckedCount = missionData.checked
                } else {
                    for (waypoint in checkPointList) {
                        if (!waypoint.checked) {
                            if (location.distanceTo(waypoint.location) < 40) {
                                newCheckedIndexList.add(checkPointList.indexOf(waypoint))
                                waypoint.checked = true
                            }
                        }
                        totalCheckedCount += if (waypoint.checked) 1 else 0
                    }
                    missionData.checked += newCheckedIndexList.size
                }
                if (newCheckedIndexList.size > 0) {
                    saveMission()
                    notificationManager?.notify(
                        notificationId,
                        NotificationCompat
                            .Builder(this@BackgroundMissionService, CHANNEL_ID)
                            .setContentTitle(getString(R.string.mission_checked_title))
                            .setContentText(String.format(
                                getString(R.string.mission_checked_message),
                                newCheckedIndexList.size, missionData.checked,
                                checkPointList.size - missionData.checked
                            ))
                            .setContentIntent(PendingIntent.getActivity(
                                this@BackgroundMissionService,
                                0,
                                Intent(this@BackgroundMissionService, MainActivity::class.java),
                                0
                            ))
                            .setSmallIcon(R.drawable.ic_check)
                            .build()
                    )
                    notificationId++
                }
                if (totalCheckedCount == checkPointList.size) {
                    // Notify all checked
                }
            }

            override fun onProviderEnabled() {
                val id = providerDisabledNotificationId
                if (id != null) {
                    notificationManager?.cancel(id)
                    providerDisabledNotificationId = null
                }
            }

            override fun onProviderSwitchedTo(newProvider: String) { }

            override fun onProviderDisabled() {
                notificationManager?.notify(
                    notificationId,
                    NotificationCompat
                        .Builder(this@BackgroundMissionService, CHANNEL_ID)
                        .setContentTitle(getString(R.string.location_provider_disabled_title))
                        .setContentText(getString(R.string.location_provider_disabled_text))
                        .setSmallIcon(R.drawable.ic_warning)
                        .setContentIntent(PendingIntent.getActivity(
                            this@BackgroundMissionService,
                            0,
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            0
                        ))
                        .build()
                )
                providerDisabledNotificationId = notificationId
                notificationId++
            }

            override fun onException(error: Exception) {
                // Notify user
            }
        }
    private var locationKit: LocationKit? = null

    private var timer: Timer = Timer(true)

    private var notificationManager: NotificationManager? = null
    private var notificationId: Int = 2
    private var providerDisabledNotificationId: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationChannel.description =
                getString(R.string.service_notification_channel_description)
            notificationManager?.createNotificationChannel(notificationChannel)
        }

        // Start foreground
        // ContentTitle, ContentText and SmallIcon is required
        // REFERENCE: https://www.jianshu.com/p/5792cf3090bc
        startForeground(
            FOREGROUND_ID,
            NotificationCompat.Builder(this.applicationContext, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_notification_text))
                .setContentIntent(PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    0
                ))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        )

        // Get the temp file
        try {
            val tempFilename = getString(R.string.mission_temp_file)
            val tempFile = File(filesDir, tempFilename)
            val tempFileInputStream: FileInputStream
            val objectInputStream: ObjectInputStream
            tempFileInputStream = FileInputStream(tempFile)
            objectInputStream = ObjectInputStream(tempFileInputStream)
            missionData = objectInputStream.readObject() as MissionManager.MissionData
            @Suppress("UNCHECKED_CAST")
            checkPointList = objectInputStream.readObject() as ArrayList<CheckPoint>
            @Suppress("UNCHECKED_CAST")
            trackPointList = objectInputStream.readObject() as ArrayList<TrackPoint>
            objectInputStream.close()
            tempFileInputStream.close()
        } catch (error: Exception) {
            Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    saveMission()
                    Log.i("TEST RO BKM", "时间更新")
                }
            }
        }, 0, 5000)
        locationKit = LocationKit(this, locationKitListener)
        locationKit?.startUpdate()

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }


    override fun onDestroy() {

        notificationManager?.cancelAll()
        timer.cancel()
        timer.purge()
        timer = Timer(true)
        locationKit?.stopUpdate()
        stopForeground(true)

        super.onDestroy()

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 保存任务并刷新开始时间（用于任务计时）
     *
     * @author lucka-me
     * @since 0.3.3
     */
    private fun saveMission() {

        val newStartTime = Date()
        missionData.pastTime += ((newStartTime.time - startTime.time) / 1000).toInt()
        startTime = newStartTime

        try {
            val tempFilename = getString(R.string.mission_temp_file)
            val tempFile = File(filesDir, tempFilename)
            val tempFileOutputStream: FileOutputStream
            val objectOutputStream: ObjectOutputStream
            tempFileOutputStream = FileOutputStream(tempFile)
            objectOutputStream = ObjectOutputStream(tempFileOutputStream)
            objectOutputStream.writeObject(missionData)
            objectOutputStream.writeObject(checkPointList)
            objectOutputStream.writeObject(trackPointList)
            objectOutputStream.close()
            tempFileOutputStream.close()
        } catch (error: Exception) {
            Log.i("TEST RO BKM", error.message)
            Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
        }

    }

    companion object {
        private const val CHANNEL_ID = "RoundO Notification"
        private const val FOREGROUND_ID = 1
    }
}