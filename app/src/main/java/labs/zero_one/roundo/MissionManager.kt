package labs.zero_one.roundo

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.support.v7.preference.PreferenceManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

/**
 * 任务管理器
 *
 * ## 属性列表
 * - [waypointList]
 *
 * ## 子类列表
 * - [MissionListener]
 * - [MissionStatus]
 *
 * ## 方法列表
 * - [start]
 * - [stop]
 * - [pause]
 * - [resume]
 *
 * @param [context] 环境
 * @param [missionListener] 任务消息监听器
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [waypointList] 任务点列表
 * @property [status] 任务状态
 */
class MissionManager(private var context: Context, private val missionListener: MissionListener) {

    var waypointList: ArrayList<Waypoint> = ArrayList(0)
    var status: MissionStatus = MissionStatus.Stopped

    /**
     * 任务消息监听器
     *
     * ## 消息列表
     * - [onStarted]
     * - [onStartFailed]
     * - [onStopped]
     * - [onStopFailed]
     * - [onReached]
     *
     * @author lucka-me
     * @since 0.1.4
     */
    interface MissionListener {
        /**
         * 任务开始
         * @author lucka-me
         * @since 0.1.4
         */
        fun onStarted()
        /**
         * 任务开始失败
         *
         * @param [error] 发生的错误
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onStartFailed(error: Exception)
        /**
         * 任务结束
         * @author lucka-me
         * @since 0.1.4
         */
        fun onStopped()
        /**
         * 任务结束失败
         *
         * @param [error] 发生的错误
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onStopFailed(error: Exception)
        /**
         * 任务开始
         * @author lucka-me
         * @since 0.1.4
         */
        fun onReached()
    }

    /**
     * 任务状态
     *
     * ## 列表
     * - [Starting]
     * - [Started]
     * - [Stopping]
     * - [Stopped]
     *
     * @author lucka-me
     * @since 0.1.9
     *
     */
    enum class MissionStatus {
        /**
         * 正在开始
         */
        Starting,
        /**
         * 已开始（进行中）
         */
        Started,
        /**
         * 正在结束
         */
        Stopping,
        /**
         * 已结束（未开始）
         */
        Stopped
    }

    /**
     * 开始任务
     *
     * ## Changelog
     * ### 0.1.9
     * - 使用 Anko 在后台线程开始任务
     *
     * @param [centerLocation] 中心坐标
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun start(centerLocation: Location) {

        status = MissionStatus.Starting

        doAsync {
            val sharedPreferences: SharedPreferences
            val missionRadius: Double
            val waypointCount: Int
            try {
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                missionRadius = sharedPreferences
                    .getString(
                        context.getString(R.string.setup_basic_radius_key),
                        context.getString(R.string.setup_basic_radius_default)
                    )
                    .toDouble() * 1000 // km -> meter
                waypointCount = sharedPreferences
                    .getString(
                        context.getString(R.string.setup_basic_waypoint_count_key),
                        context.getString(R.string.setup_basic_waypoint_count_default)
                    )
                    .toInt()
            } catch (error: Exception) {
                uiThread {
                    status = MissionStatus.Stopped
                    missionListener.onStartFailed(Exception(
                        context.getString(R.string.err_preference_fetch_failed)
                            + "\n"
                            + error.message
                    ))
                }
                return@doAsync
            }
            // Carry the list to ui thread first, instead of assign the waypointList here dirctly
            // which may not working for unknown reason.
            val newWaypointList = generateWaypointList(centerLocation, missionRadius, waypointCount)
            uiThread {
                waypointList = newWaypointList
                status = MissionStatus.Started
                missionListener.onStarted()
            }
        }
    }

    /**
     * 停止任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun stop() {
        status = MissionStatus.Stopping
        doAsync {
            waypointList.clear()
            status = MissionStatus.Stopped
            uiThread {
                missionListener.onStopped()
            }
        }
    }

    /**
     * 暂停任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun pause() {
        // Serialize and save the waypointList
        val tempFilename = context.getString(R.string.mission_temp_file)
        val tempFile = File(context.filesDir, tempFilename)
        val tempFileOutputStream: FileOutputStream
        val objectOutputStream: ObjectOutputStream

        try {
            tempFileOutputStream = FileOutputStream(tempFile)
            objectOutputStream = ObjectOutputStream(tempFileOutputStream)
            objectOutputStream.writeObject(waypointList)
            objectOutputStream.close()
            tempFileOutputStream.close()
        } catch (error: Exception) {
            val alert = AlertDialog.Builder(context)
            alert.setTitle(context.getString(R.string.title_alert))
            alert.setMessage(error.message)
            alert.setCancelable(false)
            alert.setPositiveButton(context.getString(R.string.confirm), null)
            alert.show()
        }

    }

    /**
     * 恢复任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun resume() {

        val tempFilename = context.getString(R.string.mission_temp_file)
        val tempFile = File(context.filesDir, tempFilename)
        val tempFileInputStream: FileInputStream
        val objectInputStream: ObjectInputStream

        if (!tempFile.exists()) {
            return
        }

        try {
            tempFileInputStream = FileInputStream(tempFile)
            objectInputStream = ObjectInputStream(tempFileInputStream)
            @Suppress("UNCHECKED_CAST")
            waypointList = objectInputStream.readObject() as ArrayList<Waypoint>
            objectInputStream.close()
            tempFileInputStream.close()
            if (waypointList.isEmpty()) {
                status = MissionStatus.Stopped
            } else {
                status = MissionStatus.Started
                missionListener.onStarted()
            }
        } catch (error: Exception) {
            status = MissionStatus.Stopped
            val alert = AlertDialog.Builder(context)
            alert.setTitle(context.getString(R.string.title_alert))
            alert.setMessage(error.message)
            alert.setCancelable(false)
            alert.setPositiveButton(context.getString(R.string.confirm), null)
            alert.show()
        }
    }

    /**
     * 生成任务点列表
     *
     * 注：直接在副线程中更新 [waypointList] 可能无效，原因未知
     *
     * @param [center] 中心点位置
     * @param [radius] 任务圈半径（米）
     * @param [count] 任务点数量
     *
     * @return 生成的任务点列表
     *
     * @see <a ref="http://www.geomidpoint.com/random/calculation.html">Calculation Method</a>
     *
     * @author lucka-me
     * @since 0.1.6
     */
    private fun generateWaypointList(center: Location, radius: Double, count: Int): ArrayList<Waypoint> {

        val random = Random()

        val resultList: ArrayList<Waypoint> = ArrayList(0)
        // Convert center LatLng to radian
        val centerLatRad = Math.toRadians(center.latitude)
        val centerLngRad = Math.toRadians(center.longitude)
        // Convert radius to radians
        val radRadius = radius / LocationKit.earthR
        for (i: Int in 0 until count) {
            // Generate random distance and bearing both in radian
            val distanceRad = acos(random.nextDouble() * (cos(radRadius) - 1) + 1)
            val bearing = random.nextDouble() * PI * 2
            // Calculate the radian LatLng of random point
            val radLat = asin(sin(centerLatRad) * cos(distanceRad)
                + cos(centerLatRad) * sin(distanceRad) * cos(bearing))
            var radLng = centerLngRad + atan2(
                sin(bearing) * sin(distanceRad) * cos(centerLatRad),
                cos(distanceRad) - sin(centerLatRad) * sin(radLat)
            )
            radLng =
                if (radLng < - PI) radLng + 2 * PI else if (radLng > PI) radLng - 2 * PI else radLng
            resultList.add(Waypoint(Math.toDegrees(radLng), Math.toDegrees(radLat)))
        }

        return resultList
    }

}