package labs.zero_one.roundo

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.support.v7.preference.PreferenceManager
import com.takisoft.fix.support.v7.preference.TimePickerPreference
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
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
 * - [data]
 * - [state]
 * - [timer]
 *
 * ## 子类列表
 * - [MissionListener]
 * - [MissionState]
 * - [MissionData]
 *
 * ## 方法列表
 * - [start]
 * - [stop]
 * - [pause]
 * - [resume]
 * - [setupTimer]
 * - [stopTimer]
 * - [onActivityResume]
 *
 * @param [context] 环境
 * @param [missionListener] 任务消息监听器
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [waypointList] 任务点列表
 * @property [data] 任务数据
 * @property [state] 任务状态
 * @property [timer] 计时器
 */
class MissionManager(private var context: Context, private val missionListener: MissionListener) {

    var waypointList: ArrayList<Waypoint> = ArrayList(0)
    var data: MissionData =
        MissionData(Waypoint(Location("")), 0.0, Date(), 0, 0)
    var state: MissionState = MissionState.Stopped
    private var timer: Timer = Timer(true)

    /**
     * 任务消息监听器
     *
     * ## 消息列表
     * - [onStarted]
     * - [onStartFailed]
     * - [onStopped]
     * - [onStopFailed]
     * - [onChecked]
     * - [onTimeUpdated]
     * - [onSecondUpdated]
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
         * 完成签到
         *
         * @param [indexList] 新签到的任务点的序号
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onChecked(indexList: List<Int>)
        /**
         * 全部完成
         *
         * @author lucka-me
         * @since 0.1.10
         */
        fun onCheckedAll()

        /**
         * 耗时更新
         *
         * @author lucka-me
         * @since 0.2
         */
        fun onTimeUpdated(pastTime: Long)

        /**
         * 计时器秒更新
         *
         * @author lucka-me
         * @since 0.2.1
         */
        fun onSecondUpdated()
    }

    /**
     * 任务状态
     *
     * ## 列表
     * - [Starting]
     * - [Started]
     * - [Stopping]
     * - [Stopped]
     * - [Paused]
     *
     * @author lucka-me
     * @since 0.1.9
     *
     */
    enum class MissionState {
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
        Stopped,
        /**
         * 已暂停，即 Activity 不在顶层时（onPause() 后）
         */
        Paused
    }

    /**
     * 任务基本信息
     *
     * @property [center] 中心位置
     * @property [radius] 任务圈半径
     * @property [startTime] 开始时间
     * @property [totalTime] 设定时间（秒）
     * @property [pastTime] 已耗时（秒）
     *
     * @author lucka-me
     * @since 0.1.13
     */
    data class MissionData(
        var center: Waypoint,
        var radius: Double,
        var startTime: Date,
        var totalTime: Int,
        var pastTime: Int
    ): Serializable


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

        state = MissionState.Starting
        data.center = Waypoint(centerLocation)

        doAsync {

            val sharedPreferences: SharedPreferences
            val waypointCount: Int
            try {
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                data.radius = sharedPreferences
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
                data.startTime = Date()
                val cal = Calendar.getInstance()
                cal.time = TimePickerPreference.FORMAT
                    .parse(sharedPreferences.getString(
                        context.getString(R.string.setup_basic_time_key),
                        context.getString(R.string.setup_basic_time_default)))
                data.totalTime =
                    cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60
                data.pastTime = 0
            } catch (error: Exception) {
                state = MissionState.Stopped
                uiThread {
                    missionListener.onStartFailed(Exception(
                        context.getString(R.string.err_preference_fetch_failed)
                            + "\n"
                            + error.message
                    ))
                }
                return@doAsync
            }
            waypointList = generateWaypointList(centerLocation, data.radius, waypointCount)
            // Just for demo
            Thread.sleep(5000)
            state = MissionState.Started
            uiThread {
                missionListener.onStarted()
                // Setup timer
                setupTimer()
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
        stopTimer()
        state = MissionState.Stopping
        doAsync {
            waypointList.clear()
            state = MissionState.Stopped
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
        if (state == MissionState.Started) {
            stopTimer()
            state = MissionState.Paused
        }
        // Serialize and save the waypointList
        val tempFilename = context.getString(R.string.mission_temp_file)
        val tempFile = File(context.filesDir, tempFilename)
        val tempFileOutputStream: FileOutputStream
        val objectOutputStream: ObjectOutputStream

        try {
            tempFileOutputStream = FileOutputStream(tempFile)
            objectOutputStream = ObjectOutputStream(tempFileOutputStream)
            objectOutputStream.writeObject(data)
            objectOutputStream.writeObject(waypointList)
            objectOutputStream.close()
            tempFileOutputStream.close()
        } catch (error: Exception) {
            val alert = AlertDialog.Builder(context)
            alert.setTitle(context.getString(R.string.alert_title))
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
            data = objectInputStream.readObject() as MissionData
            @Suppress("UNCHECKED_CAST")
            waypointList = objectInputStream.readObject() as ArrayList<Waypoint>
            objectInputStream.close()
            tempFileInputStream.close()
            if (waypointList.isEmpty()) {
                state = MissionState.Stopped
            } else {
                state = MissionState.Started
                missionListener.onStarted()
                setupTimer()
            }
        } catch (error: Exception) {
            state = MissionState.Stopped
            val alert = AlertDialog.Builder(context)
            alert.setTitle(context.getString(R.string.alert_title))
            alert.setMessage(error.message)
            alert.setCancelable(false)
            alert.setPositiveButton(context.getString(R.string.confirm), null)
            alert.show()
        }
    }

    /**
     * 抵达地点并签到
     *
     * @param [location] 抵达的位置
     *
     * @author lucka-me
     * @since 0.1.10
     */
    fun reach(location: Location) {
        if (state != MissionState.Started) return
        doAsync {
            val checkedIndexList: ArrayList<Int> = ArrayList(0)
            var checkedTotal = 0
            for (waypoint in waypointList) {
                if (!waypoint.isChecked) {
                    if (location.distanceTo(waypoint.location) < 40) {
                        checkedIndexList.add(waypointList.indexOf(waypoint))
                        waypoint.isChecked = true
                    }
                }
                checkedTotal += if (waypoint.isChecked) 1 else 0
            }
            uiThread {
                if (checkedIndexList.isNotEmpty()) {
                    // Unsure if is necessary
                    for (index in checkedIndexList) {
                        waypointList[index].isChecked = true
                    }
                    missionListener.onChecked(checkedIndexList.toList())
                    if (checkedTotal == waypointList.size) missionListener.onCheckedAll()
                }
            }
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

    private fun setupTimer() {
        val period = data.totalTime * 1000 / waypointList.size
        timer.schedule(object : TimerTask() {
            override fun run() {
                context.runOnUiThread {
                    missionListener.onTimeUpdated(data.pastTime.toLong())
                }
            }
        }, 0, period.toLong())
        timer.schedule(object : TimerTask() {
            override fun run() {
                data.pastTime += 1
                context.runOnUiThread {
                    missionListener.onSecondUpdated()
                }
            }
        }, 0, 1000)
    }

    /**
     * 停止计时器
     *
     * @see <a href="https://blog.csdn.net/lanxingfeifei/article/details/51775371">IllegalStateException: Timer was canceled | CSDN</a>
     * @author lucka-me
     * @since 0.2
     */
    private fun stopTimer() {
        timer.cancel()
        timer.purge()
        timer = Timer(true)
    }

    /**
     * 当 Activity 执行 onResume() 且任务处在暂停状态时恢复计时器
     *
     * @author lucka-me
     * @since 0.2
     */
    fun onActivityResume() {
        if (state == MissionState.Paused) {
            state = MissionState.Started
            setupTimer()
        }
    }

}
