package labs.zero_one.roundo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * ROM 工具，用于检测 ROM 类型并提供后台管理界面的链接
 *
 * @author lucka-me
 * @since 0.3.5
 *
 * @see <a href="https://blog.csdn.net/yu75567218/article/details/78109686">获取操作系统名称 | CSDN</a><br/>
 * @see <a href="https://www.jianshu.com/p/ba9347a5a05a">判断手机 ROM | 简书</a>
 * @see <a href="https://blog.csdn.net/zwww_/article/details/80340335">Android O 判断 ROM | CSDN</a>
 */
class RomKit {

    enum class RomType {
        OTHER, EMUI, MIUI, FLYME
    }

    companion object {

        private const val SYSTEM_PROP_CLASS = "android.os.SystemProperties"

        // EMUI
        private const val EMUI_KEY = "ro.build.version.emui"
        val EMUI_BACKGROUND_MANAGER: Intent = Intent().setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )

        // MIUI
        private const val MIUI_KEY = "ro.miui.ui.version.name"
        val MIUI_BACKGROUND_MANAGER: Intent = Intent().setClassName(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
        )

        // FLYME
        private const val FLYME_KEYWORD = "FLYME"

        /**
         * 获取 ROM 类型
         *
         * ## Changelog
         * ### 0.3.7
         * - 适配 Android O 及之后版本
         *
         * @return ROM 类型，[RomType]
         *
         * @author lucka-me
         * @since 0.3.5
         */
        fun getType(): RomType {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val buildProp = Properties()
                buildProp.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")))
                return when {
                    buildProp.containsKey(EMUI_KEY) -> RomType.EMUI
                    buildProp.containsKey(MIUI_KEY) -> RomType.MIUI
                    Build.DISPLAY.toUpperCase().contains(FLYME_KEYWORD) -> RomType.FLYME
                    else -> RomType.OTHER
                }
            } else {
                try {
                    @SuppressLint("PrivateApi")
                    val clazz = Class.forName(SYSTEM_PROP_CLASS)
                    val methodGet =
                        clazz.getMethod("get", String::class.java, String::class.java)
                    return when {
                        (methodGet.invoke(clazz, EMUI_KEY, "") as String) != "" -> RomType.EMUI
                        (methodGet.invoke(clazz, MIUI_KEY, "") as String) != "" -> RomType.MIUI
                        Build.DISPLAY.toUpperCase().contains(FLYME_KEYWORD) -> RomType.FLYME
                        else -> RomType.OTHER
                    }
                } catch (error: Exception) {
                    return RomType.OTHER
                }

            }

        }
    }
}

