package labs.zero_one.roundo

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
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
 *
 * ## 重写方法列表
 * - [onStartCommand]
 * - [onDestroy]
 * - [onBind]
 *
 * ## 自定义方法列表
 * - [saveMission]
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
                            checkPointList[i].isChecked = true
                            newCheckedIndexList.add(i)
                        } else {
                            break
                        }
                    }
                    missionData.checked += newCheckedIndexList.size
                    totalCheckedCount = missionData.checked
                } else {
                    for (waypoint in checkPointList) {
                        if (!waypoint.isChecked) {
                            if (location.distanceTo(waypoint.location) < 40) {
                                newCheckedIndexList.add(checkPointList.indexOf(waypoint))
                                waypoint.isChecked = true
                            }
                        }
                        totalCheckedCount += if (waypoint.isChecked) 1 else 0
                    }
                    missionData.checked += newCheckedIndexList.size
                }
                if (newCheckedIndexList.size > 0) {
                    saveMission()
                }
                // Notify checked
                if (totalCheckedCount == checkPointList.size) {
                    // Notify all checked
                }
            }

            override fun onProviderDisabled() {
                // Notify user
            }

            override fun onProviderEnabled() { }

            override fun onException(error: Exception) {
                // Notify user
            }
        }
    private var locationKit: LocationKit? = null

    private var timer: Timer = Timer(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Start foreground
        // ContentTitle, ContentText and SmallIcon is required
        // REFERENCE: https://www.jianshu.com/p/5792cf3090bc
        val notification = NotificationCompat.Builder(this.applicationContext, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_notification_text))
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0))
            .setSmallIcon(R.drawable.ic_dash)
            .build()
        startForeground(FOREGROUND_ID, notification)

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