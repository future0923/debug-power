package io.github.future0923.debug.power.idea.listener.data.impl;

import com.intellij.openapi.project.Project;
import io.github.future0923.debug.power.idea.listener.data.BaseDataListener;
import io.github.future0923.debug.power.idea.listener.data.event.ConvertDataEvent;
import io.github.future0923.debug.power.idea.ui.main.MainJsonEditor;
import io.github.future0923.debug.power.idea.ui.convert.ConvertDialog;

/**
 * @author future0923
 */
public class ConvertDataListener extends BaseDataListener<ConvertDataEvent> {

    private final Project project;

    private final MainJsonEditor jsonEditor;

    public ConvertDataListener(Project project, MainJsonEditor jsonEditor) {
        this.project = project;
        this.jsonEditor = jsonEditor;
    }

    @Override
    public void onEvent(ConvertDataEvent event) {
        ConvertDialog convertDialog = new ConvertDialog(project, jsonEditor, event.getConvertType());
        convertDialog.show();
    }

}
