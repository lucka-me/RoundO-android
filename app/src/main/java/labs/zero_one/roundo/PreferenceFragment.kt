package labs.zero_one.roundo

import android.os.Bundle
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

/**
 * 主设置页面的 Fragment
 *
 * 重写方法列表
 * [onCreatePreferencesFix]
 *
 * 注释参考
 *
 * @author lucka
 * @since 0.1.1
 */
class PreferenceMainFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_main, rootKey)

    }

}

/**
 * 关于页面的 Fragment
 *
 * 重写方法列表
 * [onCreatePreferencesFix]
 *
 * @author lucka
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
        findPreference(getString(R.string.pref_about_summary_version_key)).summary = String.format(
            getString(R.string.pref_about_summary_version_summary),
            versionName,
            versionCode
        )
    }

}