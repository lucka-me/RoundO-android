package labs.zero_one.roundo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.minemap.minemapsdk.MinemapAccountManager
import com.minemap.minemapsdk.camera.CameraPosition
import com.minemap.minemapsdk.maps.MineMap
import com.minemap.minemapsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/**
 * 主页面 Activity
 *
 * 属性列表
 *
 * 子类列表
 * [MainMenu]
 * [ActivityRequest]
 *
 * 重写方法列表
 * [onCreate]
 * [onCreateOptionsMenu]
 * [onOptionsItemSelected]
 * [onActivityResult]
 *
 * 自定义方法列表
 *
 * @author lucka-me
 * @since 0.1
 */
class MainActivity : AppCompatActivity() {

    //private lateinit var mineMap: MineMap

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

    /**
     * Activity 请求代码
     *
     * @param [code] 请求代码
     *
     * @property [Setup] 准备任务
     *
     * @author lucka-me
     * @since 0.1.3
     */
    private enum class ActivityRequest(val code: Int) {
        Setup(1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        // Setup Map
        /*MinemapAccountManager.getInstance(applicationContext, "9282e20b60d248c5b0a2048ff39b625d", "4787")
        initMap(savedInstanceState)*/


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
                val intent: Intent = Intent(this, SetupActivity::class.java)
                    .apply {  }
                startActivityForResult(intent, ActivityRequest.Setup.code)
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

    // Handle the activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            ActivityRequest.Setup.code -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Start Mission
                    val alert = AlertDialog.Builder(this)
                    alert.setTitle("开始任务")
                    alert.setPositiveButton("确认", null)
                    alert.show()
                }
            }

        }
    }
/*
    private fun initMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl("http://minedata.cn/service/solu/style/id/4787")
        mapView.getMapAsync(OnMapReadyCallback { newMap: MineMap? ->
            if (newMap == null) return@OnMapReadyCallback
            mineMap = newMap
            mineMap.uiSettings.isCompassEnabled = true
            mineMap.cameraPosition = CameraPosition.DEFAULT
        })
    }*/
}
