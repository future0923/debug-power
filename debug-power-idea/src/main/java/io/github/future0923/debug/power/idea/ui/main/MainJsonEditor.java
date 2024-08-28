package io.github.future0923.debug.power.idea.ui.main;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParameterList;
import io.github.future0923.debug.power.common.utils.DebugPowerJsonUtils;
import io.github.future0923.debug.power.idea.setting.DebugPowerSettingState;
import io.github.future0923.debug.power.idea.setting.GenParamType;
import io.github.future0923.debug.power.idea.ui.editor.JsonEditor;
import io.github.future0923.debug.power.idea.utils.DebugPowerJsonElementUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.incremental.GlobalContextKey;

/**
 * @author future0923
 */
@Getter
public class MainJsonEditor extends JsonEditor {

    private final PsiParameterList psiParameterList;

    public static final String FILE_NAME = "DebugPowerContentEditFile.json";

    public static final GlobalContextKey<PsiParameterList> DEBUG_POWER_EDIT_CONTENT = GlobalContextKey.create("DebugPowerEditContent");

    public MainJsonEditor(String cacheText, PsiParameterList psiParameterList, Project project) {
        super(project, "");
        this.psiParameterList = psiParameterList;
        if (StringUtils.isBlank(cacheText)) {
            DebugPowerSettingState settingState = DebugPowerSettingState.getInstance(project);
            setText(getJsonText(psiParameterList, settingState.getDefaultGenParamType()));
        } else {
            setText(cacheText);
        }
    }

    public String getJsonText(@Nullable PsiParameterList psiParameterList, GenParamType genParamType) {
        return DebugPowerJsonElementUtil.getJsonText(psiParameterList, genParamType);
    }

    public void regenerateJsonText(GenParamType type) {
        if (GenParamType.SIMPLE.equals(type)) {
            setText(DebugPowerJsonElementUtil.getSimpleText(psiParameterList));
        } else {
            setText(getJsonText(psiParameterList, type));
        }
    }

    public void prettyJsonText() {
        setText(DebugPowerJsonUtils.pretty(getText()));
    }

    @Override
    protected String fileName() {
        return FILE_NAME;
    }

    @Override
    protected void onCreateEditor(EditorEx editor) {
        editor.putUserData(DEBUG_POWER_EDIT_CONTENT, psiParameterList);
    }

    @Override
    protected void onCreateDocument(PsiFile psiFile) {
        psiFile.putUserData(DEBUG_POWER_EDIT_CONTENT, psiParameterList);
    }
}
