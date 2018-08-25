package labs.zero_one.roundo

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import org.jetbrains.anko.support.v4.defaultSharedPreferences

/**
 * 主设置页面的 Activity
 *
 * ## 子类列表
 * - [PreferenceMainFragment]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onOptionsItemSelected]
 *
 * @author lucka-me
 * @since 0.1.1
 */

class PreferenceMainActivity : AppCompatActivity() {

    /**
     * 主设置界面的 Fragment
     *
     * ## 重写方法列表
     * - [onCreatePreferencesFix]
     * - [onDestroy]
     * - [onSharedPreferenceChanged]
     *
     * @author lucka-me
     * @since 0.1.1
     */
    class PreferenceMainFragment :
        PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_main, rootKey)

            if (defaultSharedPreferences
                    .getBoolean(getString(R.string.pref_display_show_second_key), false)
            ) {
                findPreference(getString(R.string.pref_display_show_second_key)).icon =
                    requireContext().getDrawable(R.drawable.ic_pref_display_show_second_ee)
            }
            findPreference(getString(R.string.pref_other_background_key))
                .setOnPreferenceClickListener {
                    val romType = RomKit.getType()
                    var negativeButtonListener: ((DialogInterface, Int) -> (Unit))? = null
                    if (romType == RomKit.RomType.EMUI) {
                        negativeButtonListener = { _, _ ->
                            startActivity(RomKit.EMUI_BACKGROUND_MANAGER)
                        }
                    } else if (romType == RomKit.RomType.MIUI) {
                        negativeButtonListener = { _, _ ->
                            startActivity(RomKit.MIUI_BACKGROUND_MANAGER)
                        }
                    } else if (romType == RomKit.RomType.FLYME) {
                        negativeButtonListener = { _, _ ->
                            DialogKit.showDialog(
                                requireContext(),
                                R.string.pref_other_background_title,
                                R.string.pref_other_background_settings_flyme,
                                icon = requireContext()
                                    .getDrawable(R.drawable.ic_pref_other_background)
                            )
                        }
                    } else if (Build.VERSION.SDK_INT >= 23) {
                        negativeButtonListener = { _, _ ->
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    DialogKit.showDialog(
                        requireContext(),
                        R.string.pref_other_background_title,
                        R.string.pref_other_background_detail,
                        negativeButtonTextId =
                        if (negativeButtonListener != null)
                            R.string.pref_other_background_settings
                        else
                            null,
                        negativeButtonListener = negativeButtonListener,
                        icon = requireContext().getDrawable(R.drawable.ic_pref_other_background),
                        cancelable = false
                    )
                true
            }

            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        }

        override fun onDestroy() {
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == null || sharedPreferences == null) return
            if (key != getString(R.string.pref_display_show_second_key)) return
            if (defaultSharedPreferences
                    .getBoolean(getString(R.string.pref_display_show_second_key), false)
            ) {
                findPreference(getString(R.string.pref_display_show_second_key)).icon =
                    requireContext().getDrawable(R.drawable.ic_pref_display_show_second_ee)
            } else {
                findPreference(getString(R.string.pref_display_show_second_key)).icon =
                    requireContext().getDrawable(R.drawable.ic_pref_display_show_second)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        if (savedInstanceState == null) {
            val preferenceFragment = PreferenceMainFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.preferenceFrame, preferenceFragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> {
                    // Call onBackPress() when tap the back button on the toolbar instead of finish()
                    onBackPressed()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
