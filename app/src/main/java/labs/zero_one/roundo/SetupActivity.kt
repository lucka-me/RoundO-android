package labs.zero_one.roundo

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.camera.CameraPosition
import com.minemap.minemapsdk.geometry.LatLng
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.dialog_center_picker.view.*
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
     * - [getNewSummary]
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
                getNewSummary(R.string.setup_basic_sequential_key)

            // Advanced - Seed
            findPreference(getString(R.string.setup_advanced_seed_key)).summary =
                getNewSummary(R.string.setup_advanced_seed_key)
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

            // Advanced - Center Customize
            findPreference(getString(R.string.setup_advanced_center_customize_key)).summary =
                getNewSummary(R.string.setup_advanced_center_customize_key)

            // Advanced - Center Pick
            findPreference(getString(R.string.setup_advanced_center_pick_key)).isEnabled =
                defaultSharedPreferences.getBoolean(
                    getString(R.string.setup_advanced_center_customize_key),
                    false
                )
            findPreference(getString(R.string.setup_advanced_center_pick_key))
                .onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                // Show the pick dialog
                val dialogLayout =
                    View.inflate(requireContext(), R.layout.dialog_center_picker, null)
                val centerPickerDialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.setup_advanced_center_pick_title)
                    .setView(dialogLayout)
                    .setCancelable(false)
                    .setPositiveButton(R.string.confirm, null)
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                centerPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                MinemapAccountManager.getInstance(
                    requireContext(),
                    getString(R.string.minemap_token),
                    "4810"
                )
                dialogLayout.mapView.onCreate(savedInstanceState)
                dialogLayout.mapView.getMapAsync { mineMap ->
                    if (mineMap == null) {
                        DialogKit.showSimpleAlert(
                            requireContext(),
                            getString(R.string.err_map_init_failed)
                        )
                        return@getMapAsync
                    }
                    mineMap.setStyleUrl(getString(R.string.map_style))
                    mineMap.setMaxZoomPreference(17.0)
                    mineMap.setMinZoomPreference(3.0)
                    val centerLng = defaultSharedPreferences.getFloat(
                        getString(R.string.setup_advanced_center_pick_longitude_key),
                        LocationKit.DEFAULT_LONGITUDE.toFloat()
                    )
                    val centerLat = defaultSharedPreferences.getFloat(
                        getString(R.string.setup_advanced_center_pick_latitude_key),
                        LocationKit.DEFAULT_LATITUDE.toFloat()
                    )
                    mineMap.cameraPosition =
                        CameraPosition.Builder()
                            .target(LatLng(centerLat.toDouble(), centerLng.toDouble()))
                            .zoom(16.0)
                            .build()
                    centerPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newCenter = mineMap.cameraPosition.target
                        defaultSharedPreferences
                            .edit()
                            .putFloat(
                                getString(R.string.setup_advanced_center_pick_longitude_key),
                                newCenter.longitude.toFloat()
                            )
                            .apply()
                        defaultSharedPreferences
                            .edit()
                            .putFloat(
                                getString(R.string.setup_advanced_center_pick_latitude_key),
                                newCenter.latitude.toFloat())
                            .apply()
                        centerPickerDialog.dismiss()
                    }
                    centerPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
                true
            }
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
                    if (defaultSharedPreferences
                            .getString(key, getString(R.string.setup_basic_radius_default))
                            .toDouble() < 0.1
                    ) {
                        resetValue(key, getString(R.string.setup_basic_radius_default))
                        warnIllegalValue(R.string.setup_basic_radius_warning)
                    }
                }

                getString(R.string.setup_basic_checkpoint_count_key) -> {
                    if (defaultSharedPreferences
                            .getString(
                                key, getString(R.string.setup_basic_checkpoint_count_default)
                            )
                            .toInt() < 1
                    ) {
                        resetValue(key, getString(R.string.setup_basic_checkpoint_count_default))
                        warnIllegalValue(R.string.setup_basic_checkpoint_count_warning)
                    }
                }

                getString(R.string.setup_basic_sequential_key) -> {
                    findPreference(key).summary = getNewSummary(R.string.setup_basic_sequential_key)
                }

                getString(R.string.setup_advanced_seed_key) -> {
                    findPreference(key).summary = getNewSummary(R.string.setup_advanced_seed_key)
                }

                getString(R.string.setup_advanced_center_customize_key) -> {
                    findPreference(key).summary =
                        getNewSummary(R.string.setup_advanced_center_customize_key)
                    findPreference(getString(R.string.setup_advanced_center_pick_key)).isEnabled =
                        defaultSharedPreferences.getBoolean(
                            getString(R.string.setup_advanced_center_customize_key),
                            false
                        )
                }
            }
        }

        /**
         * 生成新简介
         *
         * @author lucka-me
         * @since 0.3.11
         */
        private fun getNewSummary(keyId: Int): String {
            return when(keyId) {

                R.string.setup_basic_sequential_key -> {
                    if (defaultSharedPreferences.getBoolean(getString(keyId), false))
                        getString(R.string.setup_basic_sequential_summary_true)
                    else
                        getString(R.string.setup_basic_sequential_summary_false)
                }

                R.string.setup_advanced_seed_key -> {
                    if (defaultSharedPreferences
                            .getString(
                                getString(keyId),
                                getString(R.string.setup_advanced_seed_default)
                            )
                            .toLong() == 0L
                    ) {
                        getString(R.string.setup_advanced_seed_summary_default)
                    } else {
                        getString(R.string.setup_advanced_seed_summary_set)
                    }
                }

                R.string.setup_advanced_center_customize_key -> {
                    if (defaultSharedPreferences.getBoolean(getString(keyId), false)) {
                        getString(R.string.setup_advanced_center_customize_summary_true)
                    } else {
                        getString(R.string.setup_advanced_center_customize_summary_false)
                    }
                }

                else -> ""
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