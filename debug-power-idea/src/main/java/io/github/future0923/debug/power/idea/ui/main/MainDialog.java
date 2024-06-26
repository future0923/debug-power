package io.github.future0923.debug.power.idea.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import io.github.future0923.debug.power.common.dto.RunContentDTO;
import io.github.future0923.debug.power.common.utils.DebugPowerJsonUtils;
import io.github.future0923.debug.power.idea.constant.IdeaPluginProjectConstants;
import io.github.future0923.debug.power.idea.context.MethodDataContext;
import io.github.future0923.debug.power.idea.model.ParamCache;
import io.github.future0923.debug.power.idea.model.ServerDisplayValue;
import io.github.future0923.debug.power.idea.setting.DebugPowerSettingState;
import io.github.future0923.debug.power.idea.ui.JsonEditor;
import io.github.future0923.debug.power.idea.utils.DebugPowerActionUtil;
import io.github.future0923.debug.power.idea.utils.DebugPowerAttachUtils;
import io.github.future0923.debug.power.idea.utils.DebugPowerNotifierUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author future0923
 */
public class MainDialog extends DialogWrapper {

    private static final Logger log = Logger.getInstance(MainDialog.class);

    private final Project project;

    private final MethodDataContext methodDataContext;

    private final DebugPowerSettingState settingState;

    private MainPanel mainPanel;

    public MainDialog(MethodDataContext methodDataContext, Project project) {
        super(project, true, IdeModalityType.MODELESS);
        this.project = project;
        this.methodDataContext = methodDataContext;
        this.settingState = DebugPowerSettingState.getInstance(project);
        setTitle("Quick Debug");
        setOKButtonText("Run");
        setOKButtonIcon(AllIcons.Actions.RunAll);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        mainPanel = new MainPanel(project, methodDataContext);
        return mainPanel;
    }

    @Override
    protected void doOKAction() {
        JsonEditor editor = mainPanel.getEditor();
        String text = DebugPowerJsonUtils.compress(editor.getText());
        Map<String, String> itemHeaderMap = mainPanel.getItemHeaderMap();
        ParamCache paramCacheDto = new ParamCache(itemHeaderMap, text);
        settingState.putMethodParamCache(methodDataContext.getCacheKey(), paramCacheDto);
        Map<String, RunContentDTO> contentMap = DebugPowerJsonUtils.toRunContentDTOMap(text);
        String jsonDtoStr = DebugPowerJsonUtils.toDebugPowerJson(
                methodDataContext.getPsiClass().getQualifiedName(),
                methodDataContext.getPsiMethod().getName(),
                DebugPowerActionUtil.toParamTypeNameList(methodDataContext.getPsiMethod().getParameterList()),
                contentMap,
                Stream.of(itemHeaderMap, settingState.getGlobalHeader())
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1
                        )),
                settingState.convertRunConfigDTO()
        );
        ServerDisplayValue attach = settingState.getAttach();
        if (attach == null || StringUtil.isEmpty(attach.getKey())) {
            Messages.showErrorDialog("Run attach first", "执行失败");
            return;
        }
        String agentParam;
        try {
            String pathname = project.getBasePath() + IdeaPluginProjectConstants.PARAM_FILE;
            File file = new File(pathname);
            if (!file.exists()) {
                FileUtils.touch(file);
            }
            FileUtil.writeToFile(file, jsonDtoStr);
            agentParam = "file://" + URLEncoder.encode(pathname, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("参数写入json文件失败", ex);
            DebugPowerNotifierUtil.notifyError(project, "参数写入json文件失败");
            return;
        }
        DebugPowerAttachUtils.attach(project, attach.getKey(), settingState.loadAgentPath(), agentParam);
        super.doOKAction();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }
}
