package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.BeforeCheckinDialogHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommitLogApplicationComponent extends CheckinHandlerFactory implements ApplicationComponent {

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "CommitLogApplicationComponent";
    }

    @Nullable
    @Override
    public BeforeCheckinDialogHandler createSystemReadyHandler(@NotNull Project project) {
        return null;
    }

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new CommitLogCheckinHandler(CommitLogProjectComponent.PROJECT_COMPONENTS.get(panel.getProject()), panel);
    }
}
