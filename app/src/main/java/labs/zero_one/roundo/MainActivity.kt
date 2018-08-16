package labs.zero_one.roundo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*

/**
 * 主页面 Activity
 *
 * 属性列表
 *
 * 子类列表
 *
 * 重写方法列表
 * [onCreate]
 * [onCreateOptionsMenu]
 * [onOptionsItemSelected]
 *
 * 自定义方法列表
 *
 * @author lucka-me
 * @since 0.1
 */
class MainActivity : AppCompatActivity() {

    /**
     * 主菜单项
     *
     * @param [index] 菜单项位置
     * @param [id] 菜单项资源 ID
     *
     * @property [StartStop] 开始/停止
     * @property [Preference] 设置
     *
     * @author lucka
     * @since 0.1
     */
    private enum class MainMenu(val index: Int, val id: Int) {
        StartStop(0, R.id.menu_main_start_stop),
        Preference(1, R.id.menu_main_preference)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handel the selection on Main Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            MainMenu.StartStop.id -> {

            }

            MainMenu.Preference.id -> {
                val intent: Intent = Intent(this, PreferenceMainActivity::class.java)
                    .apply {  }
                startActivity(intent)
            }
        }
        return when (item.itemId) {
            MainMenu.StartStop.id, MainMenu.Preference.id -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
