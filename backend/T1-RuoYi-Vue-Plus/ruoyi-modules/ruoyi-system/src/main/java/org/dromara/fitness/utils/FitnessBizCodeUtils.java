package org.dromara.fitness.utils;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;

/**
 * 健身业务编号工具
 */
public final class FitnessBizCodeUtils {

    private FitnessBizCodeUtils() {
    }

    public static String defaultIfBlank(String bizCode, String prefix) {
        return StringUtils.defaultIfBlank(bizCode, prefix + IdGeneratorUtil.nextId());
    }

    public static String fillOrKeep(String bizCode, String currentCode, String prefix) {
        return StringUtils.defaultIfBlank(bizCode, defaultIfBlank(currentCode, prefix));
    }
}

