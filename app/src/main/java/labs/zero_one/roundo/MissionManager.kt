package labs.zero_one.roundo

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.support.v7.preference.PreferenceManager
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
 */
class MissionManager(
    private var context: Context,
    private val missionListener: MissionListener) {

    var waypointList: ArrayList<Waypoint> = ArrayList(0)

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
     * 开始任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun start(centerLocation: Location) {

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
            missionListener.onStartFailed(Exception(
                context.getString(R.string.err_preference_fetch_failed)
                    + "\n"
                    + error.message
            ))
            return
        }
        if (missionRadius < 100 || waypointCount < 1) {
            missionListener
                .onStartFailed(Exception(context.getString(R.string.err_preference_wrong)))
            return
        }
        waypointList = generateWaypointList(centerLocation, missionRadius, waypointCount)
        missionListener.onStarted()
    }

    /**
     * 停止任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun stop() {

    }

    /**
     * 暂停任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun pause() {

    }

    /**
     * 恢复任务
     *
     * @author lucka-me
     * @since 0.1.4
     */
    fun resume() {

    }

    /**
     * 生成任务点列表
     *
     * @param [center] 中心点位置
     * @param [radius] 任务圈半径（米）
     * @param [count] 任务点数量
     *
     * @return 任务点列表
     *
     * @see <a ref="http://www.geomidpoint.com/random/calculation.html">Calculation Method</a>
     *
     * @author lucka-me
     * @since 0.1.6
     */
    private fun generateWaypointList(
        center: Location,
        radius: Double, count: Int
    ): ArrayList<Waypoint> {

        val random = Random()

        val resultList: ArrayList<Waypoint> = ArrayList(0)
        // Convert center LatLng to radian
        val centerLatRad = Math.toRadians(center.latitude)
        val centerLngRad = Math.toRadians(center.longitude)
        // Convert radius to radians
        val radRadius = radius / LocationKit.earthR
        for (i in 0 until count) {
            // Generate random distance and bearing both in radian
            val distanceRad = acos(random.nextDouble() * (cos(radRadius) - 1) + 1)
            val bearing = random.nextDouble() * PI * 2
            // Calculate the radian LatLng of random point
            val radLat = asin(sin(centerLatRad) * cos(distanceRad) + cos(centerLatRad) * sin(distanceRad) * cos(bearing))
            var radLng = centerLngRad + atan2(sin(bearing) * sin(distanceRad) * cos(centerLatRad), cos(distanceRad) - sin(centerLatRad) * sin(radLat))
            radLng = if (radLng < - PI) radLng + 2 * PI else if (radLng > PI) radLng - 2 * PI else radLng
            resultList.add(Waypoint(Math.toDegrees(radLng), Math.toDegrees(radLat)))
        }
        Waypoint(Location(""))

        return resultList
    }

}