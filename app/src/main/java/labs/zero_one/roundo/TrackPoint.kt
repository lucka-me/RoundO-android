package labs.zero_one.roundo

import android.location.Location
import java.util.*

/**
 * 轨迹类，用于存储用户轨迹，基类为 [GeoPoint]
 *
 * ## 属性列表
 * - [longitude]
 * - [latitude]
 * - [date]
 *
 * ## 访问器列表
 * - [location]
 *
 * @param [longitude] 经度
 * @param [latitude] 纬度
 * @param [date] 轨迹点对应的时间，默认为当前时间
 *
 * @author lucka-me
 * @since 0.3
 *
 * @see [GeoPoint]
 *
 * @property [date] 轨迹点对应的时间
 */
class TrackPoint(longitude: Double, latitude: Double, var date: Date = Date()) :
    GeoPoint(longitude, latitude) {

    /**
     * [TrackPoint] 的次构造函数
     *
     * @param [location] 位置
     * @param [date] 轨迹点对应的时间，默认为当前时间
     *
     * @author lucka
     * @since 0.3
     *
     * @see [TrackPoint]
     */
    constructor(location: Location, date: Date = Date())
        : this(location.longitude, location.latitude, date)

}