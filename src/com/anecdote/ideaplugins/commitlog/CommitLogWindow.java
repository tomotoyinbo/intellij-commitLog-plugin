package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;

import javax.swing.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class CommitLogWindow {

    private Project project;

    private Set<Editor> commitLogs = new HashSet<>();

    private boolean isInitialized;

    private boolean isDisposed;

    private static final Logger LOG = Logger.getInstance("#com.intellij.cvsSupport2.ui.CvsTabbedWindow");

    private ContentManager contentManager;

    private static final String COMMIT_LOG_HELP_ID = "commitlog.commitlog";

    private static final String COMMIT_LOGS_TOOLWINDOW_ID = "Commit Logs";

    private static final String COMMIT_LOGS_SMALL_ICON_NAME = "/resources/commitlogsmall.png";

    private static final Icon COMMIT_LOGS_SMALL_ICON = IconLoader.getIcon("/resources/commitlogsmall.png");

    public CommitLogWindow(Project project) {

        this.project = project;

        Disposer.register(project, new Disposable() {

            @Override
            public void dispose() {

                try {

                    for (Editor commitLog : CommitLogWindow.this.commitLogs) {
                        EditorFactory.getInstance().releaseEditor(commitLog);
                    }

                    CommitLogWindow.this.commitLogs.clear();
                    CommitLogWindow.LOG.assertTrue(!CommitLogWindow.this.isDisposed);

                    if (!CommitLogWindow.this.isInitialized) {

                        CommitLogWindow.this.isDisposed = true;

                    } else {

                        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(CommitLogWindow.this.project);
                        toolWindowManager.unregisterToolWindow("Commit Logs");
                    }

                } finally {
                    CommitLogWindow.this.isDisposed = true;
                }
            }
        });
    }

    private void initialize() {

        if (!this.isInitialized) {

            this.isInitialized = true;
            this.isDisposed = false;
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.project);
            ToolWindow toolWindow = toolWindowManager.registerToolWindow("Commit Logs", true, ToolWindowAnchor.BOTTOM);

            toolWindow.setIcon(COMMIT_LOGS_SMALL_ICON);
            this.contentManager = toolWindow.getContentManager();
            toolWindow.installWatcher(this.contentManager);

            this.contentManager.addContentManagerListener(new ContentManagerAdapter() {

                public void contentRemoved(ContentManagerEvent event) {

                    JComponent component = event.getContent().getComponent();
                    JComponent removedComponent = (component instanceof CommitLogWindowComponent) ? ((CommitLogWindowComponent) component).getComponent() : component;

                    for (Iterator iterator = CommitLogWindow.this.commitLogs.iterator(); iterator.hasNext(); ) {

                        Editor editor = (Editor) iterator.next();

                        if (removedComponent == editor.getComponent()) {
                            EditorFactory.getInstance().releaseEditor(editor);
                            iterator.remove();
                        }
                    }
                }
            });
        }
    }

    private int getComponentAt(int i, boolean select) {

        if (select) {

            Content content = getContentManager().getContent(i);

            if(content != null){

                getContentManager().setSelectedContent(content);
            }
        }

        return i;
    }

    private int addTab(String s, JComponent component, boolean selectTab, boolean replaceContent,
                       boolean lockable, boolean addDefaultToolbar, ActionGroup toolbarActions, String helpId) {

        int existing = getComponentNumNamed(s);
        ContentManager contentManager = getContentManager();

        if (existing != -1) {

            Content existingContent = contentManager.getContent(existing);

            if (!replaceContent && existingContent != null) {

                contentManager.setSelectedContent(existingContent);

                return existing;
            }

        }

        CommitLogWindowComponent newComponent = new CommitLogWindowComponent(
                component, addDefaultToolbar, toolbarActions, contentManager, helpId);

        Content content = ContentFactory.SERVICE.getInstance().createContent(newComponent.getShownComponent(), s, lockable);

        newComponent.setContent(content);
        contentManager.addContent(content);

        return getComponentAt(contentManager.getContentCount() - 1, selectTab);
    }

    private int getComponentNumNamed(String s) {

        for (int i = 0; i < getContentManager().getContentCount(); i++) {

            Content content = getContentManager().getContent(i);

            if ((content != null) && (s.equals(content.getDisplayName()))) {
                return i;
            }
        }

        return -1;
    }

    public Editor addCommitLog(String title, Editor commitLog) {

        boolean notExist = !this.commitLogs.contains(commitLog);

        LOG.assertTrue(notExist);

        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(new CopyContentAction(commitLog));

        addTab(title, commitLog.getComponent(), true, false, false, true, actions, "commitlog.commitlog");

        this.commitLogs.add(commitLog);

        return commitLog;
    }

    public void ensureVisible(Project project) {

        if (project == null) {
            return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

        if (toolWindowManager != null) {

            ToolWindow toolWindow = toolWindowManager.getToolWindow("Commit Logs");

            if (toolWindow != null) {
                toolWindow.activate(null);
            }
        }
    }

    private ContentManager getContentManager() {

        initialize();

        return this.contentManager;
    }

    private static class CopyContentAction extends AnAction {

        private final Editor _commitLog;

        public void actionPerformed(AnActionEvent e) {

            boolean hasSelection = this._commitLog.getSelectionModel().hasSelection();

            if (!hasSelection) {
                this._commitLog.getSelectionModel().setSelection(0, this._commitLog.getDocument().getCharsSequence().length() - 1);
            }

            this._commitLog.getSelectionModel().copySelectionToClipboard();

            if (!hasSelection) {
                this._commitLog.getSelectionModel().removeSelection();
            }
        }

        CopyContentAction(Editor commitLog) {
            super("Copy", "Copy content to clipboard", IconLoader.getIcon("/actions/copy.png"));
            this._commitLog = commitLog;
        }
    }
}
