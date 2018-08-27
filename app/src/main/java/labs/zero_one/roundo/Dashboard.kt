package labs.zero_one.roundo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_dashboard.view.*
import java.util.*

/**
 * 仪表盘
 *
 * ## 属性列表
 * - [dashboardView]
 *
 * ## 方法列表
 * - [show]
 * - [update]
 * - [initDashboard]
 * - [isDashboardShown]
 * - [quitDashboardParent]
 * - [timeToString]
 *
 * @param [context] 环境
 * @param [missionManager] 任务管理器
 *
 * @author lucka-me
 * @since 0.3.8
 *
 * @property [dashboardView] 仪表盘视图
 */
class Dashboard(private val context: Context, private val missionManager: MissionManager) {

    private var dashboardView = View.inflate(context, R.layout.dialog_dashboard, null)

    /**
     * 打开仪表盘对话框，若任务开始则显示仪表盘，否则显示未开始任务。
     *
     * ## Changelog
     * ### 0.2.1
     * - 将 [dashboardView] 独立，实现实时刷新
     * ### 0.3.8
     * - 若仪表盘对话框已打开则直接返回
     *
     * @author lucka-me
     * @since 0.1.14
     */
    fun show(activity: MainActivity) {

        if (isDashboardShown()) return

        val isDash = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.pref_dash_enable_key), false)
        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle(if (isDash) R.string.dashboard_title_ee else R.string.dashboard_title)
            .setIcon(if (isDash) R.drawable.ic_dash else R.drawable.ic_dashboard)
            .setPositiveButton(R.string.confirm, null)
        val dashboard = if (missionManager.state == MissionManager.MissionState.Started) {

            initDashboard()
            var checkedCount = 0
            for (waypoint in missionManager.checkPointList) if (waypoint.checked) checkedCount++
            update(checkedCount, true)

            dialogBuilder
                .setView(dashboardView)
                .setNegativeButton(R.string.dashboard_stop) { _, _ ->
                    DialogKit.showDialog(
                        context,
                        R.string.alert_title, R.string.mission_stop_confirm_message,
                        R.string.confirm,
                        positiveButtonListener = { _, _ ->
                            missionManager.stop()
                        },
                        negativeButtonTextId = R.string.cancel,
                        cancelable = false
                    )
                }
                .setOnDismissListener {
                    quitDashboardParent()
                }
                .show()

        } else {

            dialogBuilder
                .setMessage(R.string.dashboard_mission_stopped)
                .setNegativeButton(R.string.dashboard_start) { dialog, _ ->
                    dialog.dismiss()
                    quitDashboardParent()
                    val intent = Intent(activity, SetupActivity::class.java)
                    activity.startActivityForResult(intent, MainActivity.AppRequest.ActivitySetup.code)
                    activity.overridePendingTransition(R.anim.slide_bottom_up, R.anim.slide_bottom_down)
                }
                .show()

        }

        // Set dialog style

        dashboard
            .getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        dashboard
            .getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }

    /**
     * 更新仪表盘
     *
     * @param [checkedCount] 新签到的任务点数量，默认为 0
     * @param [force] 强制更新，默认为否
     *
     * @author lucka-me
     * @since 0.2.1
     */
    fun update(checkedCount: Int = 0, force: Boolean = false) {

        if (!isDashboardShown() && !force) return

        val showSecond = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.pref_display_show_second_key), false)

        dashboardView.missionProgressBar
            .incrementProgressBy(checkedCount)
        dashboardView.missionProgressText.text = String.format(
            context.getString(R.string.dashboard_mission_progress_text),
            dashboardView.missionProgressBar.progress, missionManager.checkPointList.size
        )

        // Mission Time
        val realPastTime = ((Date().time - missionManager.data.startTime.time) / 1000).toInt()
        dashboardView.timeProgressText.text = String.format(
            context.getString(R.string.dashboard_time_progress_text),
            timeToString(missionManager.data.pastTime, showSecond),
            timeToString(missionManager.data.targetTime, showSecond)
        )
        dashboardView.timeStartText.text = if (showSecond) {
            String.format("%tT", missionManager.data.startTime)
        } else {
            String.format("%tR", missionManager.data.startTime)
        }
        dashboardView.timeRealPastText.text = timeToString(realPastTime, showSecond)
        dashboardView.timeProgressBar.incrementProgressBy(missionManager.data.pastTime
            - dashboardView.timeProgressBar.progress)
        dashboardView.timeProgressBar.incrementSecondaryProgressBy(realPastTime
            - dashboardView.timeProgressBar.secondaryProgress)

        // Distance
        dashboardView.distanceText.text = if (missionManager.data.distance > 1000) {
            String.format(
                context.getString(R.string.dashboard_distance_text_km),
                missionManager.data.distance / 1000
            )
        } else {
            String.format(
                context.getString(R.string.dashboard_distance_text_m),
                missionManager.data.distance
            )
        }

    }

    /**
     * 初始化仪表盘
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun initDashboard() {
        dashboardView = View.inflate(context, R.layout.dialog_dashboard, null)

        dashboardView.missionProgressBar.progress = 0
        dashboardView.missionProgressBar.max = missionManager.checkPointList.size
        dashboardView.missionSequentialTitle.text =
            context.getString(
                if (missionManager.data.sequential) R.string.dashboard_mission_sequential_title_true
                else R.string.dashboard_mission_sequential_title_false
            )
        dashboardView.missionSequentialText.text =
            context.getString(
                if (missionManager.data.sequential) R.string.dashboard_mission_sequential_text_true
                else R.string.dashboard_mission_sequential_text_false
            )
        dashboardView.timeProgressBar.progress = 0
        dashboardView.timeProgressBar.max = missionManager.data.targetTime
        dashboardView.timeProgressBar.secondaryProgress = 0
    }

    /**
     * 仪表盘是否正在显示
     *
     * @return 结果
     *
     * @author lucka-me
     * @since 0.2.1
     */
    private fun isDashboardShown(): Boolean {
        return dashboardView.parent != null
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
            (dashboardView.parent as ViewGroup).removeView(dashboardView)
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
            String.format(context.getString(R.string.format_time_sec), hrs, min, sec)
        } else {
            String.format(context.getString(R.string.format_time), hrs, min)
        }
    }

}