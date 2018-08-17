package labs.zero_one.roundo

import android.location.Location
import android.os.Build

/**
 * 反作弊工具
 *
 * 提供一些反作弊功能
 *
 * 属性列表
 *
 * 方法列表
 * [checkEmulator] 模拟器检测
 * [checkMock] 模拟位置检测
 *
 * @author lucka-me
 * @since 0.1.4
 */

class TrumeKit() {

    companion object {
        /**
         * 模拟器检测
         *
         * @return [Boolean] 是否为模拟器
         *
         * 注释参考
         * @see <a href="https://stackoverflow.com/a/21505193">Stack Overflow</a>
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun checkEmulator(): Boolean {
            return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (
                    Build.BRAND.startsWith("generic") &&
                        Build.DEVICE.startsWith("generic")
                    ) ||
                "google_sdk" == Build.PRODUCT
        }

        /**
         * 模拟位置检测
         *
         * @param [location] 待检测位置
         *
         * @return [Boolean] 是否为模拟位置
         *
         * @author lucka-me
         * @since 0.1.4
         */
        fun checkMock(location: Location?): Boolean {
            if (location == null) return false
            return location.isFromMockProvider
        }
    }
}