package labs.zero_one.roundo

import android.content.SharedPreferences
import android.os.Bundle
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

/**
 * 关于页面的 Activity
 *
 * ## 子类列表
 * - [PreferenceAboutActivity]
 *
 * ## 重写方法列表
 * - [onCreate]
 * - [onOptionsItemSelected]
 *
 * @author lucka
 * @since 0.1
 */
class PreferenceAboutActivity : AppCompatActivity() {

    /**
     * 关于界面的 Fragment
     *
     * ## 重写方法列表
     * - [onCreatePreferencesFix]
     *
     * @author lucka-me
     * @since 0.1.1
     */
    class PreferenceAboutFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_about, rootKey)
            // Set the version information
            val versionName =
                context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionName
            val versionCode =
                context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionCode
            findPreference(getString(R.string.pref_about_summary_version_key)).summary =
                String.format(
                    getString(R.string.pref_about_summary_version_summary),
                    versionName,
                    versionCode
                )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        if (savedInstanceState == null) {
            val preferenceAboutFragment = PreferenceAboutFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.preferenceFrame, preferenceAboutFragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
