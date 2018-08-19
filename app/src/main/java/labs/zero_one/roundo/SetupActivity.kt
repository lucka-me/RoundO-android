package labs.zero_one.roundo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

/**
 * 准备界面的 Activity
 *
 * ## 子类列表
 * - [SetupFragment]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onCreateOptionsMenu]
 * - [onOptionsItemSelected]
 *
 * @author lucka-me
 * @since 0.1.2
 */
class SetupActivity : AppCompatActivity() {

    /**
     * 准备界面的 Fragment
     *
     * ## 子类列表
     * - [SetupPreference]
     *
     * ## 重写方法列表
     * - [onCreatePreferencesFix]
     * - [onDestroy]
     * - [onSharedPreferenceChanged]
     *
     * ## 自定义方法列表
     * - [isValueLegal]
     * - [setSummaryOf]
     * - [resetValue]
     * - [warnIllegalValue]
     *
     * @author lucka-me
     * @since 0.1.2
     */
    class SetupFragment :
        PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        /**
         * 准备页面 Preference 字符串资源
         *
         * ## 列表
         * - [BasicRadius]
         * - [BasicWaypointCount]
         *
         * @param [key] Key 资源
         * @param [summary] 介绍（格式）资源
         * @param [default] 默认值资源
         * @param [warning] 警告文本资源
         *
         * @author lucka
         * @since 0.1.4
         *
         * @property [BasicRadius] 基本-任务圈半径
         * @property [BasicWaypointCount] 基本-任务点数量
         */
        private enum class SetupPreference(
            val key: Int, val summary: Int, val default: Int, val warning: Int
        ) {
            BasicRadius(
                R.string.setup_basic_radius_key,
                R.string.setup_basic_radius_summary,
                R.string.setup_basic_radius_default,
                R.string.setup_basic_radius_warning
            ),
            BasicWaypointCount(
                R.string.setup_basic_waypoint_count_key,
                R.string.setup_basic_waypoint_count_summary,
                R.string.setup_basic_waypoint_count_default,
                R.string.setup_basic_waypoint_count_warning
            )
        }

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_setup, rootKey)

            // Set the summaries and listener
            setSummaryOf(SetupPreference.BasicRadius)
            SetupPreference.values().forEach { it : SetupPreference ->
                setSummaryOf(it)
            }
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        // Update Summary when preference changed
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == null || sharedPreferences == null) return
            val preference = when(key) {
                getString(SetupPreference.BasicRadius.key) ->
                    SetupPreference.BasicRadius
                getString(SetupPreference.BasicWaypointCount.key) ->
                    SetupPreference.BasicWaypointCount
                else ->
                    return
            }
            val value = sharedPreferences.getString(key, getString(preference.default))
            if (!isValueLegal(preference, value)) {
                resetValue(preference)
                warnIllegalValue(preference, value)
            }
            setSummaryOf(preference)
        }

        /**
         * 检查值是否合法
         *
         * @param [preference] 要检查的首选项所对应的 [SetupPreference]
         * @param [value] 要检查的值
         *
         * @author lucka
         * @since 0.1.8
         */
        private fun isValueLegal(preference: SetupPreference, value: String): Boolean  {
            return try {
                when (preference) {

                    SetupPreference.BasicRadius -> {
                        val valueDouble = value.toDouble()
                        valueDouble >= 0.1
                    }

                    SetupPreference.BasicWaypointCount -> {
                        val valueInt = value.toInt()
                        valueInt >= 1
                    }

                }
            } catch (error: Exception) {
                false
            }
        }

        /**
         * 更新简介
         *
         * @param [preference] 首选项所对应的 [SetupPreference]
         *
         * @author lucka-me
         * @since 0.1.4
         */
        private fun setSummaryOf(preference: SetupPreference) {
            val sharedPreference = PreferenceManager.getDefaultSharedPreferences(context)
            findPreference(getString(preference.key)).summary = String.format(
                getString(preference.summary),
                sharedPreference.getString(getString(preference.key), getString(preference.default))
            )
        }

        /**
         * 重置首选项值
         *
         * @param [preference] 要重置的首选项所对应的 [SetupPreference]
         *
         * @author lucka-me
         * @since 0.1.8
         */
        private fun resetValue(preference: SetupPreference) {
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(getString(preference.key), getString(preference.default))
                .apply()
        }

        /**
         * 显示首选项值不合法对话框
         *
         * @param [preference] 首选项所对应的 [SetupPreference]
         * @param [value] 不合法的值
         *
         * @author lucka-me
         * @since 0.1.8
         */
        private fun warnIllegalValue(preference: SetupPreference, value: String) {
            val alert = AlertDialog.Builder(context)
            alert.setTitle(String.format(getString(R.string.title_setup_illegal_value), value))
            alert.setMessage(getString(preference.warning))
            alert.setCancelable(false)
            alert.setPositiveButton(getString(R.string.confirm), null)
            alert.show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        if (savedInstanceState == null) {
            val preferenceFragment = SetupFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.preferenceFrame, preferenceFragment)
                .commit()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_setup, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {

                android.R.id.home -> {
                    // Call onBackPress() when tap the back button on the toolbar instead of finish()
                    setResult(Activity.RESULT_CANCELED)
                    onBackPressed()
                    return true
                }

                R.id.menu_setup_check -> {
                    // Back to the MainActivity and start mission
                    setResult(Activity.RESULT_OK)
                    onBackPressed()
                    //finish()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}