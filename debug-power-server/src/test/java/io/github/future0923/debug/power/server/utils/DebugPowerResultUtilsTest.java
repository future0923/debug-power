package io.github.future0923.debug.power.server.utils;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import io.github.future0923.debug.power.common.dto.RunContentDTO;
import io.github.future0923.debug.power.common.dto.RunDTO;
import io.github.future0923.debug.power.common.dto.RunResultDTO;
import io.github.future0923.debug.power.common.enums.RunContentType;
import io.github.future0923.debug.power.common.utils.JdkUnsafeUtils;
import io.github.future0923.debug.power.server.http.DebugPowerHttpServer;
import io.github.future0923.debug.power.server.utils.dto.PageR;
import io.github.future0923.debug.power.server.utils.dto.ProfitBatchVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author future0923
 */
class DebugPowerResultUtilsTest {

    @Test
    public void object() {
        RunDTO runDTO = new RunDTO();
        runDTO.setTargetClassName("io.github.future0923.debug.power.test.application.service.TestService");
        runDTO.setTargetMethodName("test");
        runDTO.setTargetMethodParameterTypes(Arrays.asList("java.lang.String", "java.lang.Integer"));
        Map<String, RunContentDTO> contentMap = new HashMap<>();
        contentMap.put("name", RunContentDTO.builder()
                .type(RunContentType.SIMPLE.getType())
                .content("future0923")
                .build());
        contentMap.put("age", RunContentDTO.builder()
                .type(RunContentType.SIMPLE.getType())
                .content("19")
                .build());
        runDTO.setTargetMethodContent(contentMap);

        //Properties runDTO = System.getProperties();
        RunResultDTO runResultDTO = new RunResultDTO(null, runDTO);
        Object valueByOffset = DebugPowerResultUtils.getValueByOffset(runDTO, runResultDTO.getFiledOffset());
        List<RunResultDTO> runResultDTOS = DebugPowerResultUtils.convertRunResultDTO(runDTO, runResultDTO.getFiledOffset());
        String filedOffset1 = runResultDTOS.get(4).getFiledOffset();
        String filedOffset2 = DebugPowerResultUtils.convertRunResultDTO(DebugPowerResultUtils.getValueByOffset(runDTO, filedOffset1), filedOffset1).get(0).getFiledOffset();
        List<RunResultDTO> runResultDTOS2 = DebugPowerResultUtils.convertRunResultDTO(DebugPowerResultUtils.getValueByOffset(runDTO, filedOffset2), filedOffset2);
        String filedOffset3 = runResultDTOS2.get(0).getFiledOffset();
        List<RunResultDTO> runResultDTOS1 = DebugPowerResultUtils.convertRunResultDTO(DebugPowerResultUtils.getValueByOffset(runDTO, filedOffset3), filedOffset3);
        Object valueByOffset1 = DebugPowerResultUtils.getValueByOffset(RunContentType.SIMPLE, "1");
        List<RunResultDTO> runResultDTOS3 = DebugPowerResultUtils.convertRunResultDTO(valueByOffset1, "1");
        System.out.println(runResultDTOS1);
    }

    @Test
    public void properties() {
        Properties runDTO = System.getProperties();
        RunResultDTO runResultDTO = new RunResultDTO(null, runDTO);
        DebugPowerResultUtils.convertRunResultDTO(runDTO, runResultDTO.getFiledOffset());
        System.out.println(runResultDTO);
    }

    @Test
    public void test() throws InterruptedException {
        DebugPowerHttpServer httpServer = DebugPowerHttpServer.getInstance();
        httpServer.start();

        RunDTO runDTO = new RunDTO();
        runDTO.setTargetClassName("io.github.future0923.debug.power.test.application.service.TestService");
        runDTO.setTargetMethodName("test");
        runDTO.setTargetMethodParameterTypes(Arrays.asList("java.lang.String", "java.lang.Integer"));
        Map<String, RunContentDTO> contentMap = new HashMap<>();
        contentMap.put("name", RunContentDTO.builder()
                .type(RunContentType.SIMPLE.getType())
                .content("future0923")
                .build());
        contentMap.put("age", RunContentDTO.builder()
                .type(RunContentType.SIMPLE.getType())
                .content("19")
                .build());
        runDTO.setTargetMethodContent(contentMap);

        RunResultDTO runResultDTO = new RunResultDTO(null, runDTO);
        DebugPowerResultUtils.putCache(runResultDTO.getFiledOffset(), runDTO);
        System.out.println(runResultDTO.getFiledOffset());

        Thread.sleep(1000000L);

    }

    @Test
    public void Integer() {
        Integer runDTO = 1;
        RunResultDTO runResultDTO = new RunResultDTO(null, runDTO);
        DebugPowerResultUtils.putCache(runResultDTO.getFiledOffset(), runDTO);
        System.out.println(runResultDTO.getFiledOffset());
        List<RunResultDTO> runResultDTOS = DebugPowerResultUtils.convertRunResultDTO(runDTO, runResultDTO.getFiledOffset());
        System.out.println(runResultDTOS);

    }

    @Test
    public void vo() {
        String json = "{\"code\":200,\"message\":\"操作成功\",\"data\":[{\"id\":1823615117445890049,\"type\":1,\"settlementBatchNo\":\"2024002\",\"startTime\":1719763200000,\"endTime\":1723564800000,\"profitTime\":1723651199000,\"status\":1,\"statusName\":\"进行中\",\"createBy\":1762736500306149377,\"createTime\":1723618700000,\"updateBy\":1823237365605113857,\"updateTime\":1724062252000,\"version\":4,\"butPcPerm\":[80000]},{\"id\":1,\"type\":1,\"settlementBatchNo\":\"2024001\",\"startTime\":1717171200000,\"endTime\":1719676800000,\"performanceTime\":1719763199000,\"profitTime\":1719763199000,\"carryTime\":1719763199000,\"status\":3,\"statusName\":\"已结束\",\"createBy\":0,\"updateBy\":0,\"updateTime\":1723618700000,\"version\":52,\"butPcPerm\":[]}],\"total\":2,\"subTotal\":0}";
        PageR<ProfitBatchVO> runDTO = JSONUtil.toBean(json, new TypeReference<PageR<ProfitBatchVO>>() {
        }, true);
        RunResultDTO runResultDTO = new RunResultDTO(null, runDTO);
        Object valueByOffset = DebugPowerResultUtils.getValueByOffset(runDTO, runResultDTO.getFiledOffset());
        List<RunResultDTO> runResultDTOS = DebugPowerResultUtils.convertRunResultDTO(runDTO, runResultDTO.getFiledOffset());
    }

    @Test
    public void map() {
        HashMap<String, String> map = new HashMap<>();
        map.put("1", "2");
        RunResultDTO runResultDTO = new RunResultDTO("result", map);
        Object valueByOffset = DebugPowerResultUtils.getValueByOffset(map, runResultDTO.getFiledOffset());
        List<RunResultDTO> runResultDTOS = DebugPowerResultUtils.convertRunResultDTO(map, runResultDTO.getFiledOffset());
        System.out.println(runResultDTOS);
    }

    @Test
    public void iterator() {
        Iterator<String> iterator = Arrays.asList("1", "2").iterator();
        RunResultDTO runResultDTO = new RunResultDTO("result", iterator);
        DebugPowerResultUtils.putCache(runResultDTO.getFiledOffset(), iterator);
        List<RunResultDTO> runResultDTOS = DebugPowerResultUtils.convertRunResultDTO(iterator, runResultDTO.getFiledOffset());
        Object valueByOffset = DebugPowerResultUtils.getValueByOffset(runResultDTOS.get(3).getFiledOffset());
        List<RunResultDTO> runR = DebugPowerResultUtils.convertRunResultDTO(valueByOffset, runResultDTOS.get(3).getFiledOffset());
    }

}