package labs.zero_one.roundo

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.support.v7.preference.PreferenceManager
import android.util.Log
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
 * - [data]
 * - [state]
 * - [checkPointList]
 * - [trackPointList]
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
 * - [clear]
 * - [pause]
 * - [resume]
 * - [setupTimer]
 * - [stopTimer]
 * - [processCORC]
 *
 * @param [context] 环境
 * @param [missionListener] 任务消息监听器
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [data] 任务数据
 * @property [state] 任务状态
 * @property [checkPointList] 任务点列表
 * @property [trackPointList] 轨迹点列表
 * @property [timer] 计时器
 */
class MissionManager(private var context: Context, private val missionListener: MissionListener) {

    var data: MissionData = MissionData()
    var state: MissionState = MissionState.Stopped
    var checkPointList: ArrayList<CheckPoint> = ArrayList(0)
    var trackPointList: ArrayList<TrackPoint> = ArrayList(0)
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
     * - [onCheckedAll]
     * - [onTimeUpdated]
     * - [onSecondUpdated]
     *
     * @author lucka-me
     * @since 0.1.4
     */
    interface MissionListener {
        /**
         * 任务开始
         *
         * @param [isResumed] 是否从恢复的任务中开始的
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun onStarted(isResumed: Boolean)
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
     * @property [started] 是否已开始，作为恢复任务时的依据
     * @property [center] 中心位置
     * @property [sequential] 是否为顺序任务
     * @property [radius] 任务圈半径
     * @property [seed] 随机种子
     * @property [startTime] 开始时间
     * @property [targetTime] 设定时间（秒）
     * @property [pastTime] 已耗时（秒）
     * @property [checked] 已签到的数量
     *
     * @author lucka-me
     * @since 0.1.13
     */
    data class MissionData(
        var started: Boolean = false,
        var center: GeoPoint = GeoPoint(Location("")),
        var sequential: Boolean = true,
        var radius: Double = 0.0,
        var seed: Long = 0L,
        var startTime: Date = Date(),
        var targetTime: Int = 0,
        var pastTime: Int = 0,
        var checked: Int = 0,
        var distance: Double = 0.0
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

        doAsync {

            data = MissionData()

            val sharedPreferences: SharedPreferences
            val waypointCount: Int
            val isCenterCustomized: Boolean
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
                        context.getString(R.string.setup_basic_checkpoint_count_key),
                        context.getString(R.string.setup_basic_checkpoint_count_default)
                    )
                    .toInt()

                val cal = Calendar.getInstance()
                cal.time = TimePickerPreference.FORMAT
                    .parse(sharedPreferences.getString(
                        context.getString(R.string.setup_basic_time_key),
                        context.getString(R.string.setup_basic_time_default)))
                data.targetTime =
                    cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60

                data.sequential = sharedPreferences
                    .getBoolean(context.getString(R.string.setup_basic_sequential_key), false)

                val prefSeed = sharedPreferences
                    .getString(context.getString(R.string.setup_advanced_seed_key), "0").toLong()
                data.seed = if (prefSeed == 0L) data.startTime.time else prefSeed

                isCenterCustomized = sharedPreferences.getBoolean(
                    context.getString(R.string.setup_advanced_center_customize_key),
                    false
                )

                data.center = if (isCenterCustomized) {
                    val centerLng = sharedPreferences.getFloat(
                        context.getString(R.string.setup_advanced_center_pick_longitude_key),
                        LocationKit.DEFAULT_LONGITUDE.toFloat()
                    ).toDouble()
                    val centerLat = sharedPreferences.getFloat(
                        context.getString(R.string.setup_advanced_center_pick_latitude_key),
                        LocationKit.DEFAULT_LATITUDE.toFloat()
                    ).toDouble()
                    GeoPoint(centerLng, centerLat)
                } else {
                    GeoPoint(centerLocation)
                }

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
            trackPointList.clear()
            checkPointList =
                generateCheckPointList(data.center.location, data.radius, waypointCount, data.seed)
            // Just for demo
            Thread.sleep(3000)
            state = MissionState.Started
            data.started = true
            uiThread {
                missionListener.onStarted(false)
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
            data.started = false
            state = MissionState.Stopped
            uiThread {
                missionListener.onStopped()
            }
        }
    }

    /**
     * 清空任务
     *
     * @author lucka-me
     * @since 0.3.9
     */
    fun clear() {
        state = MissionState.Stopped
        data = MissionData()
        checkPointList.clear()
        trackPointList.clear()
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
        // Serialize and save the checkPointList
        val tempFilename = context.getString(R.string.mission_temp_file)
        val tempFile = File(context.filesDir, tempFilename)
        val tempFileOutputStream: FileOutputStream
        val objectOutputStream: ObjectOutputStream

        try {
            tempFileOutputStream = FileOutputStream(tempFile)
            objectOutputStream = ObjectOutputStream(tempFileOutputStream)
            objectOutputStream.writeObject(data)
            objectOutputStream.writeObject(checkPointList)
            objectOutputStream.writeObject(trackPointList)
            objectOutputStream.close()
            tempFileOutputStream.close()
        } catch (error: Exception) {
            DialogKit.showSimpleAlert(context, error.message)
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
            state = MissionState.Stopped
            return
        }

        try {
            tempFileInputStream = FileInputStream(tempFile)
            objectInputStream = ObjectInputStream(tempFileInputStream)
            data = objectInputStream.readObject() as MissionData
            @Suppress("UNCHECKED_CAST")
            checkPointList = objectInputStream.readObject() as ArrayList<CheckPoint>
            @Suppress("UNCHECKED_CAST")
            trackPointList = objectInputStream.readObject() as ArrayList<TrackPoint>
            objectInputStream.close()
            tempFileInputStream.close()
            if (data.started) {
                Log.i("TEST RO", "任务已恢复")
                state = MissionState.Started
                missionListener.onStarted(true)
                setupTimer()
            } else {
                state = MissionState.Stopped
            }
        } catch (error: Exception) {
            data.started = false
            state = MissionState.Stopped
            DialogKit.showSimpleAlert(context, error.message)
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
        if (location.accuracy > ACCURACY) return
        data.distance += processCORC(location, trackPointList)
        doAsync {
            val newCheckedIndexList: ArrayList<Int> = ArrayList(0)
            var totalCheckedCount = 0
            if (data.sequential) {
                for (i: Int in data.checked until checkPointList.size) {
                    if (location.distanceTo(checkPointList[i].location) < 40) {
                        checkPointList[i].checked = true
                        newCheckedIndexList.add(i)
                    } else {
                        break
                    }
                }
                data.checked += newCheckedIndexList.size
                totalCheckedCount = data.checked
            } else {
                for (i: Int in data.checked until checkPointList.size) {
                    if (!checkPointList[i].checked &&
                        location.distanceTo(checkPointList[i].location) < 40
                    ) {
                        newCheckedIndexList.add(i)
                        checkPointList[i].checked = true
                    }
                    totalCheckedCount += if (checkPointList[i].checked) 1 else 0
                }
                data.checked += newCheckedIndexList.size
            }
            uiThread {
                if (newCheckedIndexList.isNotEmpty()) {
                    missionListener.onChecked(newCheckedIndexList.toList())
                    if (totalCheckedCount == checkPointList.size) missionListener.onCheckedAll()
                }
            }
        }
    }

    /**
     * 生成任务点列表
     *
     * ## Changelog
     * ### 0.3.6
     * - 支持设定随机种子 [seed]
     *
     * @param [center] 中心点位置
     * @param [radius] 任务圈半径（米）
     * @param [count] 任务点数量
     * @param [seed] 随机种子
     *
     * @return 生成的任务点列表
     *
     * @see <a href="http://www.geomidpoint.com/random/calculation.html">Calculation Method</a>
     *
     * @author lucka-me
     * @since 0.1.6
     */
    private fun generateCheckPointList(
        center: Location, radius: Double, count: Int, seed: Long
    ): ArrayList<CheckPoint> {

        val random = Random(seed)

        val resultList: ArrayList<CheckPoint> = ArrayList(0)
        // Convert center LatLng to radian
        val centerLatRad = Math.toRadians(center.latitude)
        val centerLngRad = Math.toRadians(center.longitude)
        // Convert radius to radians
        val radRadius = radius / LocationKit.EARTH_R
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
            resultList.add(CheckPoint(Math.toDegrees(radLng), Math.toDegrees(radLat)))
        }

        return resultList
    }

    private fun setupTimer() {
        val period = data.targetTime * 1000 / checkPointList.size
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

    companion object {
        const val ACCURACY = 20.0
        /**
         * 进行轨迹处理，包括简单预处理（距离判断）和累积偏移算法（CORC）
         *
         * ## Changelog
         * ### 0.3.1
         * - 添加距离预处理
         * - 直接传入新的位置并返回需要累加的距离，简化使用
         * ### 0.3.2
         * - 转为静态方法
         *
         * @param [newLocation] 新抵达的位置
         * @param [trackPointList] 轨迹点列表
         *
         * @return 应累加的距离
         *
         * @author lucka-me
         * @since 0.3
         *
         * @see <a href="http://kns.cnki.net/kns/detail/detail.aspx?QueryID=7&CurRec=1&recid=&FileName=DQXX201402005&DbName=CJFD2014&DbCode=CJFQ&yx=&pr=&URLID=">论文 | 中国知网</a>
         */
        fun processCORC(newLocation: Location, trackPointList: ArrayList<TrackPoint>): Double {
            // Add to list first
            trackPointList.add(TrackPoint(newLocation))
            // Keep the first
            if (trackPointList.size <= 1) return 0.0
            val size = trackPointList.size
            // Remove the newest if the distance is too short
            val distanceLast = trackPointList[size - 1].location
                .distanceTo(trackPointList[size - 2].location).toDouble()
            if (distanceLast < 10.0) {
                Log.i("TESTRO MM", "距离过短：" + (size - 2) + " -> " + (size - 1) + ": " + distanceLast)
                trackPointList.removeAt(size - 1)
                return 0.0
            }
            // Keep the first and second
            if (size == 2) return distanceLast
            // The third should be executed by CORC when the fourth comes
            if (size == 3) return 0.0

            // CORC Begin
            // 如果为非冗余点，返回倒数第三个点和倒数第二个点（被判定点）的距离
            // 第一步：判断累积变向点或变向拐点
            val angleA = abs(
                trackPointList[size - 4].location
                    .bearingTo(trackPointList[size - 3].location)
                    - trackPointList[size - 3].location
                    .bearingTo(trackPointList[size - 2].location)
            )
            val angleB = abs(
                trackPointList[size - 4].location
                    .bearingTo(trackPointList[size - 3].location)
                    - trackPointList[size - 2].location
                    .bearingTo(trackPointList[size - 1].location)
            )
            // 倒数第三个点和倒数第二个点的距离，同时也用在海伦公式中
            val distanceB = trackPointList[size - 3].location
                .distanceTo(trackPointList[size - 2].location).toDouble()
            if ((angleA > 90 && angleA < 270) || (angleB > 90 && angleB < 270)) return distanceB

            // 第二步：累积偏移距离判断
            // 海伦公式计算点到直线的距离
            val distanceA = trackPointList[size - 4].location
                .distanceTo(trackPointList[size - 3].location).toDouble()
            val distanceC = trackPointList[size - 2].location
                .distanceTo(trackPointList[size - 4].location).toDouble()
            val s = (distanceA + distanceB + distanceC) / 2.0
            val area = sqrt(s * (s - distanceA) * (s - distanceB) * (s - distanceC))
            val d = area * 2.0 / distanceA
            // CORC 累计偏移限差阈值
            val thresholdT = 20
            if (d >= thresholdT) {
                Log.i("TESTRO MM", "CORC 保留第 " + (size - 2) + " 个点，距离：" + distanceB)
                return distanceB
            }
            // 倒数第二个为冗余点
            trackPointList.removeAt(size - 2)
            return 0.0
        }
    }

}
