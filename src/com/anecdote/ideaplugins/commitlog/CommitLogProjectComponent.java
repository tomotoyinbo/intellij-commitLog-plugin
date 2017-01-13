package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory;
import com.intellij.openapi.vcs.impl.CheckinHandlersManager;
import com.intellij.openapi.vcs.impl.CheckinHandlersManagerImpl;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
@State(name = "CommitLogProjectComponent",
        storages = {@com.intellij.openapi.components.Storage(id = "COMMIT_LOG_PLUGIN", file = "$PROJECT_FILE$")})
public class CommitLogProjectComponent implements ProjectComponent, Configurable,
        PersistentStateComponent<CommitLogProjectComponent> {

    private final Project project;
    private CommitLogWindow commitLogWindow;
    private String textualCommitLogTemplate;
    private String textualCommitCommentTemplate;
    static final String DEFAULT_COMMIT_LOG_TEMPLATE_RESOURCE = "/resources/DefaultCommitLogTemplate.txt";
    static final String DEFAULT_COMMIT_COMMENT_TEMPLATE_RESOURCE = "/resources/DefaultCommitCommentTemplate.txt";
    public static final String DEFAULT_COMMIT_LOG_TEMPLATE = readResourceAsString("/resources/DefaultCommitLogTemplate.txt");
    public static final String DEFAULT_COMMIT_COMMENT_TEMPLATE = readResourceAsString("/resources/DefaultCommitCommentTemplate.txt");

    private static final String COMMIT_LOG_ICON_NAME = "/resources/commitlog.png";
    private static final Icon COMMIT_LOG_ICON = IconLoader.getIcon("/resources/commitlog.png");
    public static final String COMPONENT_NAME = "CommitLogProjectComponent";
    private CommitLogConfigurationPanel configurationPanel;
    private boolean generateTextualCommitLog = true;
    public static final String VERSION = "1.3";
    private static AnAction generateCommentAction = null;

    private boolean focusCommentTemplateEditor;

    public static final Map<Project, CommitLogProjectComponent> PROJECT_COMPONENTS = new HashMap();

    public CommitLogProjectComponent() {
        this(null);
    }

    public CommitLogProjectComponent(@Nullable Project project) {
        this.project = project;
    }

    public void initComponent() {

        PROJECT_COMPONENTS.put(this.project, this);

        AbstractVcs[] allActiveVcs = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();

        CheckinHandlersManager handlersManager = CheckinHandlersManager.getInstance();

        List<BaseCheckinHandlerFactory> CheckinHandlerFactories = handlersManager.getRegisteredCheckinHandlerFactories(allActiveVcs);

        if (generateCommentAction == null) {

            generateCommentAction = new GenerateCommentAction();
            ActionManager.getInstance().registerAction("CommitLogPlugin.GenerateComment", generateCommentAction);
            DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("Vcs.MessageActionGroup");

            actionGroup.add(generateCommentAction, Constraints.LAST);
            actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("CHANGES_BAR_COMMIT_COMMENT_ACTIONS");

            if (actionGroup != null) {
                actionGroup.add(generateCommentAction, Constraints.FIRST);
            }
        }
    }

    public void disposeComponent() {
        PROJECT_COMPONENTS.remove(this.project);
    }

    @NotNull
    public String getComponentName() {
        String tmp2_0 = "CommitLogProjectComponent";

        if (tmp2_0 == null)
            throw new IllegalStateException("@NotNull method com/anecdote/ideaplugins/commitlog/CommitLogProjectComponent.getComponentName must not return null");
        return tmp2_0;
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    public Project getProject() {
        return this.project;
    }

    public CommitLogWindow getCommitLogWindow() {
        if (this.commitLogWindow == null) {
            this.commitLogWindow = new CommitLogWindow(this.project);
        }
        return this.commitLogWindow;
    }

    public void setTextualCommitLogTemplate(String text) {
        this.textualCommitLogTemplate = text;
    }

    public String getTextualCommitLogTemplate() {
        if (this.textualCommitLogTemplate == null) {
            resetCommitLogTemplate();
        }
        return this.textualCommitLogTemplate;
    }

    public void resetCommitLogTemplate() {
        this.textualCommitLogTemplate = DEFAULT_COMMIT_LOG_TEMPLATE;
    }

    public void resetCommitCommentTemplate() {
        this.textualCommitCommentTemplate = DEFAULT_COMMIT_COMMENT_TEMPLATE;
    }

    public static String readResourceAsString(String resourceName) {

        StringBuilder stringBuilder = new StringBuilder(500);
        InputStream templateStream = CommitLogProjectComponent.class.getResourceAsStream(resourceName);

        if (templateStream == null) {
            return "Error : Could not find resource " + resourceName;
        }

        try {

            DataInputStream dataInputStream = new DataInputStream(templateStream);

            try {

                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append('\n');
                    }
                }
            } catch (IOException ex) {
                ex.getMessage();
            }

            return stringBuilder.toString();

        } finally {

            try {
                templateStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Nls
    public String getDisplayName() {
        return "Commit Log";
    }

    @Nullable
    public Icon getIcon() {
        return COMMIT_LOG_ICON;
    }

    @Nullable
    @NonNls
    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (this.configurationPanel == null) {
            this.configurationPanel = new CommitLogConfigurationPanel(this);
            if (this.focusCommentTemplateEditor) {
                this.focusCommentTemplateEditor = false;
                this.configurationPanel.setSelectedTab(1);
            }
        }
        return this.configurationPanel;
    }

    public boolean isModified() {
        return this.configurationPanel.isModified();
    }

    public void apply()
            throws ConfigurationException {
        this.configurationPanel.save();
    }

    public void reset() {
        this.configurationPanel.load();
    }

    public void disposeUIResources() {
        this.configurationPanel = null;
    }

    public CommitLogProjectComponent getState() {
        return this;
    }

    public void loadState(CommitLogProjectComponent state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void setGenerateTextualCommitLog(boolean generateTextualCommitLog) {
        this.generateTextualCommitLog = generateTextualCommitLog;
    }

    public boolean isGenerateTextualCommitLog() {
        return this.generateTextualCommitLog;
    }

    public static void log(String s) {
        System.out.println(s);
    }

    public String getTextualCommitCommentTemplate() {
        if (this.textualCommitCommentTemplate == null) {
            resetCommitCommentTemplate();
        }
        return this.textualCommitCommentTemplate;
    }

    public void setTextualCommitCommentTemplate(String textualCommitCommentTemplate) {
        this.textualCommitCommentTemplate = textualCommitCommentTemplate;
    }

    public void setFocusCommentTemplateEditor(boolean focusCommentTemplateEditor) {
        this.focusCommentTemplateEditor = focusCommentTemplateEditor;
    }
}
