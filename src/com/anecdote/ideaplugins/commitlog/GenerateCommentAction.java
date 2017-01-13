package com.anecdote.ideaplugins.commitlog;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.LocalChangeList;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class GenerateCommentAction extends AnAction {

    private static final String GENERATE = "Generate";
    private static final DataKey<JTextArea> CHANGES_BAR_COMMENT_EDITOR_DATA_KEY = DataKey.create("CHANGES_BAR_COMMENT_EDITOR");
    private static final DataKey<LocalChangeList> SELECTED_CHANGE_LIST_DATA_KEY = DataKey.create("SELECTED_CHANGE_LIST");
    private static final String COMMIT_DIALOG_TEXT = "Generate a comment based on the files selected";
    private static final String COMMIT_DIALOG_DESC = "Generates a commit comment based on the files selected";
    private static final String CHANGES_BAR_TEXT = "Generate a comment using CommitLog plugin";
    private static final String CHANGES_BAR_DESC = "Generates a comment for the selected Changelist using CommitLog plugin";

    GenerateCommentAction() {

        super("Generate a comment based on the files selected",
                "Generates a commit comment based on the files selected",
                IconLoader.getIcon("/resources/generate.png")
        );
    }

    public void update(AnActionEvent e) {

        super.update(e);

        CheckinProjectPanel panel = (CheckinProjectPanel) e.getData(CheckinProjectPanel.PANEL_KEY);

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(true);
        e.getPresentation().setText("Generate a comment based on the files selected");
        e.getPresentation().setDescription("Generates a commit comment based on the files selected");

        if (panel == null) {

            if ("CHANGES_BAR_COMMIT_COMMENT_TOOLBAR".equals(e.getPlace())) {

                if (e.getData(SELECTED_CHANGE_LIST_DATA_KEY) == null) {
                    e.getPresentation().setEnabled(false);
                }

                e.getPresentation().setText("Generate a comment using CommitLog plugin");
                e.getPresentation().setDescription("Generates a comment for the selected Changelist using CommitLog plugin");

            } else {

                e.getPresentation().setVisible(false);
                e.getPresentation().setEnabled(false);
            }
        }
    }

    public void actionPerformed(AnActionEvent e) {

        CheckinProjectPanel panel = (CheckinProjectPanel) e.getData(CheckinProjectPanel.PANEL_KEY);
        JTextArea commentEditor = e.getData(CHANGES_BAR_COMMENT_EDITOR_DATA_KEY);

        if ((panel != null) || (commentEditor != null)) {

            Project project = panel != null ? panel.getProject() : DataKeys.PROJECT.getData(e.getDataContext());

            if (project != null) {

                int confirmation = Messages.showDialog(project, "Generate commit comment?         ",
                        "Confirm Generate Comment",
                        new String[]{"Generate", CommonBundle.getCancelButtonText()}, 0, Messages.getQuestionIcon());

                if (confirmation == 0) {

                    CommitLogProjectComponent projectComponent = project.getComponent(CommitLogProjectComponent.class);
                    Collection<File> files;
                    String commitMessage;

                    if (panel != null) {

                        commitMessage = panel.getCommitMessage();
                        files = panel.getFiles();

                    } else {

                        commitMessage = commentEditor.getText();
                        LocalChangeList changeList = e.getData(SELECTED_CHANGE_LIST_DATA_KEY);

                        if (changeList == null) {
                            return;
                        }

                        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
                        files = new HashSet();

                        for (Change change : changeList.getChanges()) {
                            FilePath filepath = ChangesUtil.getFilePath(change);
                            files.add(filepath.getIOFile());
                        }
                    }

                    CommitLogBuilder commitLogBuilder = CommitLogBuilder.createCommitLogBuilder(projectComponent.getTextualCommitCommentTemplate(), commitMessage, project, files);

                    try {

                        String commitLog = commitLogBuilder.buildCommitLog(new Date());

                        if (panel != null) {
                            panel.setCommitMessage(commitLog);
                        } else {
                            commentEditor.setText(commitLog);
                            commentEditor.requestFocusInWindow();
                        }

                    } catch (CommitLogTemplateParser.TextTemplateParserException e1) {

                        int result = Messages.showDialog(project,
                                "Error parsing Comment Template :\n" + e1.getMessage() + "\n\nEdit Comment Template now?",
                                "Error Generating Comment", new String[]{CommonBundle.getYesButtonText(),
                                        CommonBundle.getNoButtonText()}, 0, Messages.getErrorIcon()
                        );

                        if (result == 0) {
                            projectComponent.setFocusCommentTemplateEditor(true);
                            ShowSettingsUtil.getInstance().editConfigurable(project, projectComponent);
                        }
                    }
                }
            }
        }
    }
}
