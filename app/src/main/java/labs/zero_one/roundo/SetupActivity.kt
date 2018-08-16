package labs.zero_one.roundo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

/**
 * 准备界面的 Activity
 *
 * 子类列表
 * [SetupFragment]
 *
 * 重写方法列表
 * [onCreate]
 * [onCreateOptionsMenu]
 * [onOptionsItemSelected]
 *
 * @author lucka-me
 * @since 0.1.2
 */
class SetupActivity : AppCompatActivity() {

    /**
     * 准备界面的 Fragment
     *
     * 重写方法列表
     * [onCreatePreferencesFix]
     *
     * @author lucka-me
     * @since 0.1.2
     */
    class SetupFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_setup, rootKey)

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
                    onBackPressed()
                    return true
                }

                R.id.menu_setup_check -> {
                    // Back to the MainActivity and start mission
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}