package io.github.future0923.debug.power.idea.ui.main;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import io.github.future0923.debug.power.common.dto.RunContentDTO;
import io.github.future0923.debug.power.common.dto.RunDTO;
import io.github.future0923.debug.power.common.exception.SocketCloseException;
import io.github.future0923.debug.power.common.protocal.http.AllClassLoaderRes;
import io.github.future0923.debug.power.common.protocal.packet.request.RunTargetMethodRequestPacket;
import io.github.future0923.debug.power.common.utils.DebugPowerJsonUtils;
import io.github.future0923.debug.power.idea.client.ApplicationProjectHolder;
import io.github.future0923.debug.power.idea.constant.IdeaPluginProjectConstants;
import io.github.future0923.debug.power.idea.context.MethodDataContext;
import io.github.future0923.debug.power.idea.model.ParamCache;
import io.github.future0923.debug.power.idea.setting.DebugPowerSettingState;
import io.github.future0923.debug.power.idea.utils.DebugPowerActionUtil;
import io.github.future0923.debug.power.idea.utils.DebugPowerNotifierUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
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
        Map<String, String> itemHeaderMap = mainPanel.getItemHeaderMap();
        AllClassLoaderRes classLoaderRes = (AllClassLoaderRes) mainPanel.getClassLoaderComboBox().getSelectedItem();
        MainJsonEditor editor = mainPanel.getEditor();
        String text = DebugPowerJsonUtils.compress(editor.getText());
        String xxlJobParam = mainPanel.getXxlJobParamField().getText();
        ParamCache paramCacheDto = new ParamCache(itemHeaderMap, text, xxlJobParam);
        settingState.putMethodParamCache(methodDataContext.getCacheKey(), paramCacheDto);
        Map<String, RunContentDTO> contentMap = DebugPowerJsonUtils.toRunContentDTOMap(text);
        Map<String, String> headers = Stream.of(itemHeaderMap, settingState.getGlobalHeader())
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1
                ));
        RunDTO runDTO = new RunDTO();
        runDTO.setHeaders(headers);
        runDTO.setClassLoader(classLoaderRes);
        runDTO.setTargetClassName(methodDataContext.getPsiClass().getQualifiedName());
        runDTO.setTargetMethodName(methodDataContext.getPsiMethod().getName());
        runDTO.setTargetMethodParameterTypes(DebugPowerActionUtil.toParamTypeNameList(methodDataContext.getPsiMethod().getParameterList()));
        runDTO.setTargetMethodContent(contentMap);
        runDTO.setXxlJobParam(xxlJobParam);
        RunTargetMethodRequestPacket packet = new RunTargetMethodRequestPacket(runDTO);
        ApplicationProjectHolder.Info info = ApplicationProjectHolder.getInfo(project);
        if (info == null) {
            Messages.showErrorDialog("Run attach first", "执行失败");
            return;
        }
        try {
            info.getClient().getHolder().send(packet);
        } catch (SocketCloseException e) {
            Messages.showErrorDialog("Socket close", "执行失败");
            return;
        } catch (Exception e) {
            Messages.showErrorDialog("Socket send error " + e.getMessage(), "执行失败");
            return;
        }
        try {
            String pathname = project.getBasePath() + IdeaPluginProjectConstants.PARAM_FILE;
            File file = new File(pathname);
            if (!file.exists()) {
                FileUtils.touch(file);
            }
            FileUtil.writeToFile(file, DebugPowerJsonUtils.toJsonStr(runDTO));
        } catch (IOException ex) {
            log.error("参数写入json文件失败", ex);
            DebugPowerNotifierUtil.notifyError(project, "参数写入json文件失败");
            return;
        }
        super.doOKAction();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }
}
