package labs.zero_one.roundo

import android.location.Location
import java.io.Serializable

/**
 * 地理点类，用于存储经纬度信息，提供序列化/反序列化功能
 *
 * ## 属性列表
 * - [longitude]
 * - [latitude]
 *
 * ## 访问器列表
 * - [location]
 *
 * @param [longitude] 经度
 * @param [latitude] 纬度
 *
 * @author lucka-me
 * @since 0.3
 *
 * @property [longitude] 经度
 * @property [latitude] 纬度
 * @property [location] 位置（[Location]）访问器
 */
open class GeoPoint (var longitude: Double, var latitude: Double) : Serializable {

    /**
     * 位置的 [Location] 类属性访问器
     *
     * 由于通常实际使用的是 [Location] 实体而非直接使用经纬度属性，而 Location 类无法序列化/反序列化，因此使用
     * 访问器获得/设置它。
     *
     * @author lucka
     * @since 0.3
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
     * [GeoPoint] 的次构造函数
     *
     * @param [location] 位置
     *
     * @author lucka
     * @since 0.3
     *
     * @see [GeoPoint]
     */
    constructor(location: Location) : this(location.longitude, location.latitude)
}