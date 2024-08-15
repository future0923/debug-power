package io.github.future0923.debug.power.server.utils;

import cn.hutool.core.convert.Convert;
import io.github.future0923.debug.power.base.logging.Logger;
import io.github.future0923.debug.power.common.dto.RunContentDTO;
import io.github.future0923.debug.power.common.enums.RunContentType;
import io.github.future0923.debug.power.common.utils.DebugPowerClassUtils;
import io.github.future0923.debug.power.common.utils.DebugPowerJsonUtils;
import io.github.future0923.debug.power.common.utils.DebugPowerLambdaUtils;
import io.github.future0923.debug.power.common.utils.DebugPowerSpringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * @author future0923
 */
public class DebugPowerParamConvertUtils {

    private static final Logger log = Logger.getLogger(DebugPowerParamConvertUtils.class);

    public static Object[] getArgs(Method bridgedMethod, Map<String, RunContentDTO> targetMethodContent) {
        Object[] targetMethodArgs = new Object[bridgedMethod.getParameterCount()];
        for (int i = 0; i < bridgedMethod.getParameterCount(); i++) {
            Parameter parameter = bridgedMethod.getParameters()[i];
            targetMethodArgs[i] = getArg(targetMethodContent, parameter, i);
        }
        return targetMethodArgs;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object getArg(Map<String, RunContentDTO> contentMap, Parameter parameter, Integer parameterIndex) {
        if (contentMap == null || contentMap.isEmpty()) {
            return null;
        }
        RunContentDTO runContentDTO = getRunContentDTO(contentMap, parameterIndex);
        if (runContentDTO == null) {
            return null;
        }
        if (RunContentType.BEAN.getType().equals(runContentDTO.getType())) {
            return DebugPowerSpringUtils.getBean(parameter.getType());
        } else if (RunContentType.LAMBDA.getType().equals(runContentDTO.getType())) {
            if (runContentDTO.getContent() != null && parameter.getType().isInterface() && (runContentDTO.getContent().toString().contains("->") || runContentDTO.getContent().toString().contains("::"))) {
                return DebugPowerLambdaUtils.createLambda(runContentDTO.getContent().toString(), parameter.getParameterizedType());
            }
        } else if (RunContentType.JSON_ENTITY.getType().equals(runContentDTO.getType())) {
            return DebugPowerJsonUtils.toBean(DebugPowerJsonUtils.toJsonStr(runContentDTO.getContent()), parameter.getParameterizedType(), true);
        } else if (RunContentType.SIMPLE.getType().equals(runContentDTO.getType())) {
            if (DebugPowerClassUtils.isSimpleValueType(parameter.getType())) {
                try {
                    if (parameter.getType().isAssignableFrom(LocalDateTime.class)) {
                        return LocalDateTime.parse(runContentDTO.getContent().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                    if (parameter.getType().isAssignableFrom(LocalDate.class)) {
                        return LocalDate.parse(runContentDTO.getContent().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    if (parameter.getType().isAssignableFrom(Date.class)) {
                        return DateUtils.parseDate(runContentDTO.getContent().toString(), "yyyy-MM-dd HH:mm:ss");
                    }
                    // spring简单类型转换
                    return Convert.convert(parameter.getType(), runContentDTO.getContent());
                } catch (Exception e) {
                    log.error("转换失败", e);
                    return null;
                }
            }
        } else if (RunContentType.ENUM.getType().equals(runContentDTO.getType())) {
            return Enum.valueOf((Class<? extends Enum>) parameter.getType(), runContentDTO.getContent().toString());
        }
        return null;
    }

    /**
     * 获取参数传递的对应数据
     *
     * @param contentMap     入参信息
     * @param parameterIndex 参数下标（Parameter没有参数名字信息，只能通过顺序）
     * @return RunContentDTO
     */
    private static RunContentDTO getRunContentDTO(Map<String, RunContentDTO> contentMap, Integer parameterIndex) {
        int index = 0;
        for (RunContentDTO value : contentMap.values()) {
            if (parameterIndex == index++) {
                return value;
            }
        }
        return null;
    }

}