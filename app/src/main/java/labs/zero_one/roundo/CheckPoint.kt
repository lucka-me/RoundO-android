package labs.zero_one.roundo

import android.location.Location

/**
 * 任务点类，用于存储任务点信息，基类为 [GeoPoint]
 *
 * ## 属性列表
 * - [longitude]
 * - [latitude]
 * - [checked]
 *
 * ## 访问器列表
 * - [location]
 *
 * ## Changelog
 * ### 0.1.7
 * - 修改构造器参数，直接以经纬度构造
 * ### 0.3
 * - 将核心部分独立为基类 [GeoPoint] 并从它继承
 *
 * @param [longitude] 经度
 * @param [latitude] 纬度
 * @param [checked] 任务点是否已完成，默认未完成
 *
 * @author lucka-me
 * @since 0.1.4
 *
 * @see [GeoPoint]
 *
 * @property [checked] 是否已签到
 */
class CheckPoint(longitude: Double, latitude: Double, var checked: Boolean = false) :
    GeoPoint(longitude, latitude) {

    /**
     * [CheckPoint] 的次构造函数
     *
     * @param [location] 位置
     * @param [isChecked] 任务点是否已完成，默认未完成
     *
     * @author lucka-me
     * @since 0.1.7
     *
     * @see [CheckPoint]
     */
    constructor(location: Location, isChecked: Boolean = false)
        : this(location.longitude, location.latitude, isChecked)

}