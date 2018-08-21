package labs.zero_one.roundo

import android.location.Location
import java.io.Serializable

/**
 * 任务点类，用于存储任务点信息，并提供序列化/反序列化功能
 *
 * ## 属性列表
 * - [longitude]
 * - [latitude]
 * - [isChecked]
 *
 * ## 访问器列表
 * - [location]
 *
 * ## Changelog
 * ### 0.1.7
 * - 修改构造器参数，直接以经纬度构造
 *
 * @param [longitude] 经度
 * @param [latitude] 纬度
 * @param [isChecked] 任务点是否已完成，默认未完成
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @property [longitude] 经度
 * @property [latitude] 纬度
 * @property [location] 位置（[Location]）访问器
 */
class Waypoint(
    private var longitude: Double, private var latitude: Double,
    var isChecked: Boolean = false
) : Serializable {

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

    /**
     * @param [location] 位置
     * @param [isChecked] 任务点是否已完成，默认未完成
     *
     * @author lucka
     * @since 0.1.7
     */
    constructor(location: Location, isChecked: Boolean = false)
        : this(location.longitude, location.latitude, isChecked)

}