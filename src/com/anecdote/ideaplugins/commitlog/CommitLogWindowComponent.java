package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CommitLogWindowComponent extends JPanel implements DataProvider {

    private final JComponent component;

    private final ContentManager contentManager;

    private Content content;

    private final boolean addToolbar;

    private final String helpId;

    public CommitLogWindowComponent(JComponent component,
                                    boolean addDefaultToolbar,
                                    ActionGroup toolbarActions,
                                    ContentManager contentManager,
                                    String helpId) {

        super(new BorderLayout());

        this.addToolbar = addDefaultToolbar;
        this.component = component;
        this.contentManager = contentManager;
        this.helpId = helpId;

        add(this.component, "Center");

        if (this.addToolbar) {

            DefaultActionGroup actionGroup = new DefaultActionGroup(null, false);
            actionGroup.add(new CloseAction());
            if (toolbarActions != null) {
                actionGroup.add(toolbarActions);
            }

            add(ActionManager.getInstance().createActionToolbar("CommitLogToolbar", actionGroup, false).getComponent(), "West");
        }
    }

    @Nullable
    @Override
    public Object getData(@NonNls String dataId) {

        if ("helpId".equals(dataId)) {
            return this.helpId;
        }
        return null;
    }

    public JComponent getComponent() {
        return component;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public JComponent getShownComponent() {
        return this.addToolbar ? this : this.component;
    }

    private class CloseAction extends AnAction {

        public void actionPerformed(AnActionEvent e) {
            CommitLogWindowComponent.this.contentManager.removeContent(CommitLogWindowComponent.this.content, true);
        }

        CloseAction() {
            super("", "", IconLoader.getIcon("/actions/cancel.png"));
        }
    }
}
