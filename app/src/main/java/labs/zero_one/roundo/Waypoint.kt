package labs.zero_one.roundo

import android.location.Location
import java.io.Serializable

/**
 * Waypoint 类
 *
 * 用于存储任务点信息，并提供序列化/反序列化功能
 *
 * 属性列表
 * [longitude]
 * [latitude]
 *
 * 访问器列表
 * [location]
 *
 * @param [location] 任务点位置
 * @param [isChecked] Waypoint 是否已被检查
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [longitude] Waypoint 的经度
 * @property [latitude] Waypoint 的纬度
 * @property [location] 位置（[Location]）访问器
 */
class Waypoint(location: Location, var isChecked: Boolean) : Serializable {

    private var longitude: Double = location.longitude
    private var latitude: Double = location.latitude
    /**
     * Waypoint 的位置属性访问器
     *
     * 由于实际使用的是 Location 实体而非直接使用经纬度属性，而 Location 类无法序列化/反序列化
     * 因此使用 Location 作为 Location 类属性访问器
     *
     * @author lucka
     * @since 0.1.4
     */
    var location: Location
        set(value) {
            this.longitude = value.longitude
            this.latitude = value.latitude
        }
        get() {
            val location = Location("")
            location.longitude = this.longitude
            location.latitude = this.latitude
            return location
        }

    init {
        this.location = location
    }


}