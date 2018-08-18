package labs.zero_one.roundo

import android.content.Context

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
    fun start() {

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

}