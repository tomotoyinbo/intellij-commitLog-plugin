package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
public class CommitLogCheckinHandler extends CheckinHandler {

    private final CommitLogProjectComponent projectComponent;
    private final Project project;
    private final CheckinProjectPanel panel;
    private CommitLogBuilder commitLogBuilder;
    private AfterCheckinConfigPanel afterCheckinConfigPanel = new AfterCheckinConfigPanel();

    CommitLogCheckinHandler(CommitLogProjectComponent projectComponent, CheckinProjectPanel panel) {

        this.projectComponent = projectComponent;
        this.project = projectComponent.getProject();
        this.panel = panel;
    }

    public RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable parentDisposable) {
        return this.afterCheckinConfigPanel;
    }

    public CheckinHandler.ReturnResult beforeCheckin() {

        CommitLogProjectComponent.log("CommitLogCheckinHandler::beforeCheckin Entered");
        CheckinHandler.ReturnResult returnResult = super.beforeCheckin();

        if (this.projectComponent.isGenerateTextualCommitLog()) {

            try {
                List affectedVcses = new ArrayList();
                this.commitLogBuilder = CommitLogBuilder.createCommitLogBuilder(this.projectComponent.getTextualCommitLogTemplate(), this.panel.getCommitMessage(), this.panel.getProject(), this.panel.getFiles());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return returnResult;
    }

    public void checkinFailed(List<VcsException> exception) {

        CommitLogProjectComponent.log("CommitLogCheckinHandler::checkinFailed() Entered");

        try {

            super.checkinFailed(exception);

            if (this.projectComponent.isGenerateTextualCommitLog()) {

                outputCommitLog(true);
                this.commitLogBuilder = null;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void checkinSuccessful() {

        CommitLogProjectComponent.log("CommitLogCheckinHandler::checkinSuccessful() Entered");

        try {

            if (this.projectComponent.isGenerateTextualCommitLog()) {

                super.checkinSuccessful();
                outputCommitLog(false);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        this.commitLogBuilder = null;
    }

    private void outputCommitLog(final boolean failed) {

        CommitLogProjectComponent.log("CommitLogCheckinHandler::outputCommitLog() Entered");
        CommitLogProjectComponent.log("CommitLogCheckinHandler.outputCommitLog : failed = " + failed);

        updateEntryVersions();
        this.commitLogBuilder.removeUncommittedEntries();
        final Date date = new Date();
        String commitLog;

        try {
            commitLog = this.commitLogBuilder.buildCommitLog(date);
        } catch (CommitLogTemplateParser.TextTemplateParserException e) {
            commitLog = e.getMessage();
        }

        final String changeListName = this.commitLogBuilder.getChangeListName();

        final String finalCommitLog = commitLog;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                CommitLogProjectComponent.log("CommitLogCheckinHandler::outputCommitLog Runnable.run() Entered");
                EditorFactory editorFactory = EditorFactory.getInstance();
                Document document = editorFactory.createDocument(finalCommitLog);
                Editor viewer = editorFactory.createViewer(document, CommitLogCheckinHandler.this.project);
                EditorSettings editorsettings = viewer.getSettings();
                editorsettings.setFoldingOutlineShown(false);
                editorsettings.setLineMarkerAreaShown(false);
                editorsettings.setLineNumbersShown(false);
                editorsettings.setRightMarginShown(false);
                String tabTitle = DateFormat.getDateTimeInstance(3, 3).format(date) + " : " + changeListName;

                if (failed) {
                    tabTitle = tabTitle + " [FAILED]";
                }

                CommitLogWindow window = CommitLogCheckinHandler.this.projectComponent.getCommitLogWindow();
                window.addCommitLog(tabTitle, viewer);
                window.ensureVisible(CommitLogCheckinHandler.this.project);
            }
        });
    }

    private void updateEntryVersions() {

        Map<Change.Type, Collection<CommitLogEntry>> entries = this.commitLogBuilder.getCommitLogEntriesByTypeByRoot(null);

        for (Map.Entry mapEntry : entries.entrySet()) {

            Set<CommitLogEntry> commitLogEntries = (Set<CommitLogEntry>) mapEntry.getValue();

            for (CommitLogEntry commitLogEntry : commitLogEntries) {

                try {

                    String version = getCurrentFileVersion(commitLogEntry.getVcs(), commitLogEntry.getFilePath());

                    if (version == null) {
                        version = commitLogEntry.getOldVersion();
                    }

                    commitLogEntry.setNewVersion(version);

                } catch (VcsException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Nullable
    private static String getCurrentFileVersion(@NotNull AbstractVcs vcs, FilePath filePath) throws VcsException {

        String version = null;
        DiffProvider diffProvider = vcs.getDiffProvider();
        VirtualFile file = filePath.getVirtualFile();

        if ((diffProvider != null) && (file != null)) {

            VcsRevisionNumber revision = diffProvider.getCurrentRevision(file);

            if (revision != null) {
                version = revision.asString();
            }

        } else {

            VcsHistoryProvider historyProvider = vcs.getVcsHistoryProvider();

            if (historyProvider != null) {

                VcsHistorySession session = historyProvider.createSessionFor(filePath);

                if ((session != null) && (!session.getRevisionList().isEmpty())) {

                    VcsRevisionNumber currentRevisionNumber = session.getCurrentRevisionNumber();

                    if (currentRevisionNumber != null) {
                        version = currentRevisionNumber.asString();
                    }
                }
            }
        }

        return version;
    }

    private class AfterCheckinConfigPanel implements RefreshableOnComponent {

        private JCheckBox _generateCommitLog = new JCheckBox("Generate Commit Log");
        private JPanel _configPanel = new JPanel(new BorderLayout());

        private AfterCheckinConfigPanel() {
            this._configPanel.add(this._generateCommitLog, "West");
            this._configPanel.add(Box.createHorizontalGlue(), "Center");
        }

        public JComponent getComponent() {
            return this._configPanel;
        }

        public void refresh() {
        }

        public void saveState() {
            CommitLogCheckinHandler.this.projectComponent.setGenerateTextualCommitLog(this._generateCommitLog.isSelected());
        }

        public void restoreState() {
            this._generateCommitLog.setSelected(CommitLogCheckinHandler.this.projectComponent.isGenerateTextualCommitLog());
        }
    }
}
