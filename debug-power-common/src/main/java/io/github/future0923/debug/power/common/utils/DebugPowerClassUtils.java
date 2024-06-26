package io.github.future0923.debug.power.common.utils;

import cn.hutool.core.util.ClassUtil;

import java.util.List;

/**
 * 类工具类 <br>
 */
public class DebugPowerClassUtils extends ClassUtil {

	/**
	 * 根据类全路径获取类信息
	 *
	 * @param className 类全路径集合
	 * @return 类信息集合
	 */
	public static Class<?>[] getTypes(List<String> className) {
		return className.stream().map(DebugPowerClassUtils::loadClass).toArray(Class[]::new);
	}
}
