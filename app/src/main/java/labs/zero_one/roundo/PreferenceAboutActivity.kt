package labs.zero_one.roundo

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import org.jetbrains.anko.support.v4.defaultSharedPreferences

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

        private var dashCount = 5

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
            findPreference(getString(R.string.pref_about_summary_version_key))
                .onPreferenceClickListener =
                android.support.v7.preference.Preference.OnPreferenceClickListener {
                    dashCount--
                    if (dashCount == 0) {
                        val dashKey = getString(R.string.pref_dash_enable_key)
                        defaultSharedPreferences
                            .edit()
                            .putBoolean(
                                dashKey, !defaultSharedPreferences.getBoolean(dashKey, false)
                            )
                            .apply()
                        dashCount = 5
                        val thisView = view
                        if (thisView != null) {
                            if (defaultSharedPreferences.getBoolean(dashKey, false))
                                Snackbar.make(
                                    thisView,
                                    R.string.pref_dash_enable_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                    }
                    true
                }
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