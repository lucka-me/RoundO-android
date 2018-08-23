package labs.zero_one.roundo

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import org.jetbrains.anko.support.v4.defaultSharedPreferences

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
 * - [onBackPressed]
 *
 * @author lucka-me
 * @since 0.1.2
 */
class SetupActivity : AppCompatActivity() {

    /**
     * 准备界面的 Fragment
     *
     * ## Changelog
     * ### 0.2.3
     * - Replace EditTextPreference with AutoSummaryEditTextPreference and simplify the code
     *
     * ## 重写方法列表
     * - [onCreatePreferencesFix]
     * - [onDestroy]
     *
     * ## 自定义方法列表
     * - [resetValue]
     * - [warnIllegalValue]
     *
     * @author lucka-me
     * @since 0.1.2
     */
    class SetupFragment :
        PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_setup, rootKey)

            // Set the summaries
            // Basic - Sequential
            findPreference(getString(R.string.setup_basic_sequential_key)).summary =
                if (defaultSharedPreferences.getBoolean(
                        getString(R.string.setup_basic_sequential_key), false
                    ))
                    getString(R.string.setup_basic_sequential_summary_true)
                else
                    getString(R.string.setup_basic_sequential_summary_false)

            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            if (sharedPreferences == null || key == null) return
            when (key) {

                getString(R.string.setup_basic_radius_key) -> {
                    if (sharedPreferences.getString(
                            key, getString(R.string.setup_basic_radius_default)
                        ).toDouble() < 0.1
                    ) {
                        resetValue(key, getString(R.string.setup_basic_radius_default))
                        warnIllegalValue(R.string.setup_basic_radius_warning)
                    }
                }

                getString(R.string.setup_basic_checkpoint_count_key) -> {
                    if (sharedPreferences.getString(
                            key, getString(R.string.setup_basic_checkpoint_count_default)
                        ).toInt() < 1
                    ) {
                        resetValue(key, getString(R.string.setup_basic_checkpoint_count_default))
                        warnIllegalValue(R.string.setup_basic_checkpoint_count_warning)
                    }
                }

                getString(R.string.setup_basic_sequential_key) -> {
                    findPreference(key).summary =
                        if (sharedPreferences.getBoolean(key, false))
                            getString(R.string.setup_basic_sequential_summary_true)
                        else
                            getString(R.string.setup_basic_sequential_summary_false)
                }
            }
        }

        /**
         * 重置首选项值（String）
         *
         * @param [key] 要重置的首选项的 Key
         * @param [default] 首选项默认 String
         *
         * @see <a href="https://stackoverflow.com/a/31671831">Refresh view after reset value | Stack Overflow</a>
         *
         * @author lucka-me
         * @since 0.1.8
         */
        private fun resetValue(key: String, default: String) {
            defaultSharedPreferences.edit().putString(key, default).apply()
            preferenceScreen.removeAll()
            addPreferencesFromResource(R.xml.preference_setup)
        }

        /**
         * 重置首选项值（String）
         *
         * @param [key] 要重置的首选项的 Key ID
         * @param [default] 首选项默认 String
         *
         * @author lucka-me
         * @since 0.1.8
         */
        private fun resetValue(key: Int, default: String) {
            resetValue(getString(key), default)
        }

        /**
         * 显示首选项值不合法对话框
         *
         * ## Changelog
         * ### 0.2.3
         * - 使用 [DialogKit]
         * - 简化使用，仅传入警告文本即可
         *
         * @param [warning] 警告文本 ID
         *
         * @author lucka-me
         * @since 0.1.8
         */
        private fun warnIllegalValue(warning: Int) {
            DialogKit.showDialog(
                requireContext(), R.string.title_setup_illegal_value, warning, cancelable = false
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

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

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_bottom_up, R.anim.slide_bottom_down)
    }

}