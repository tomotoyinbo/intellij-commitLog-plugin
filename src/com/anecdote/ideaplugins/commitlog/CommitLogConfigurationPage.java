package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Date;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
public class CommitLogConfigurationPage extends JPanel {

    private CommitLogTemplate template;

    private boolean modified;

    @Nullable
    private Editor templateEditor;

    private Document templateDocument;

    private Editor sampleEditor;

    private Document sampleDocument;

    private static final String COMMIT_LOG_TEMPLATE_REFERENCE_RESOURCE = "/resources/CommitLogTemplateReference.txt";

    private static final FileType TEMPLATE_FILE_TYPE = ApplicationManager.getApplication() != null ? FileTypeManager.getInstance().getFileTypeByExtension("txt") : null;

    private static final String SAMPLE_COMMIT_MESSAGE = "This is a sample commit message.  I hope you usually write more than this for your commits ;^)";

    private DocumentListener templateDocumentListener = new DocumentListener() {

        public void beforeDocumentChange(DocumentEvent event) {
        }

        public void documentChanged(DocumentEvent event) {
            CommitLogConfigurationPage.this.templateDocumentChanged();
        }
    };

    private AbstractAction resetTemplateAction = new AbstractAction(null, IconLoader.getIcon("/actions/undo.png")) {

        public void actionPerformed(ActionEvent e) {

            DialogWrapper dialogWrapper = new DialogWrapper(CommitLogConfigurationPage.this, false) {

                @Nullable
                protected JComponent createCenterPanel() {

                    String s = "This will reset the Commit Log Template to the default template.  Continue?";

                    JPanel jpanel = new JPanel(new BorderLayout());
                    JLabel jlabel = new JLabel(s);
                    jlabel.setIconTextGap(10);
                    jlabel.setIcon(Messages.getQuestionIcon());
                    jpanel.add(jlabel, "Center");
                    jpanel.add(Box.createVerticalStrut(10), "South");

                    return jpanel;
                }
            };

            dialogWrapper.setModal(true);
            dialogWrapper.setTitle("Confirm Template Reset");
            dialogWrapper.pack();
            dialogWrapper.centerRelativeToParent();
            dialogWrapper.show();

            if (dialogWrapper.isOK()) {
                CommitLogConfigurationPage.this.template.reset();
                CommitLogConfigurationPage.this.load();
            }
        }
    };

    CommitLogConfigurationPage(CommitLogTemplate template, CommitLogProjectComponent projectComponent) {

        super(new BorderLayout());
        this.template = template;
        EditorFactory editorFactory = EditorFactory.getInstance();

        if (editorFactory != null) {

            this.templateDocument = editorFactory.createDocument(template.getTemplateText());
            this.templateDocument.addDocumentListener(this.templateDocumentListener);

            if (TEMPLATE_FILE_TYPE != null) {

                this.templateEditor = editorFactory.createEditor(this.templateDocument, projectComponent.getProject(), TEMPLATE_FILE_TYPE, false);

                initEditor(this.templateEditor);
            }
        }

        JToolBar templateEditorToolBar = new JToolBar();
        templateEditorToolBar.setFloatable(false);
        templateEditorToolBar.setBorderPainted(false);

        JButton resetButton = templateEditorToolBar.add(this.resetTemplateAction);
        resetButton.setText("Reset To Default");
        resetButton.setHorizontalTextPosition(SwingConstants.RIGHT);

        JPanel templateEditorPanel = new JPanel(new BorderLayout());

        JComponent comp = this.templateEditor != null ? this.templateEditor.getComponent() : new JLabel("TEMPLATE EDITOR");
        templateEditorPanel.add(comp, "Center");
        templateEditorPanel.add(templateEditorToolBar, "North");

        JPanel sampleEditorPanel = new JPanel(new BorderLayout());

        if (editorFactory != null) {

            this.sampleDocument = editorFactory.createDocument(projectComponent.getTextualCommitLogTemplate());
            this.sampleEditor = editorFactory.createViewer(this.sampleDocument, projectComponent.getProject());

            initEditor(this.sampleEditor);

            comp = this.sampleEditor.getComponent();

        } else {
            comp = new JLabel("SAMPLE EDITOR");
        }

        sampleEditorPanel.add(comp, "Center");

        JTextArea referenceEditor = new JTextArea(CommitLogProjectComponent.readResourceAsString("/resources/CommitLogTemplateReference.txt"));

        referenceEditor.setLineWrap(true);
        referenceEditor.setWrapStyleWord(true);
        JScrollPane referenceEditorPane = new JScrollPane(referenceEditor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        referenceEditorPane.setPreferredSize(new Dimension(512, 384));

        JTabbedPane templateTabbedPane = new JTabbedPane();
        templateTabbedPane.add("Sample Commit Log", sampleEditorPanel);
        templateTabbedPane.add("Commit Log Template Reference Documentation", referenceEditorPane);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                CommitLogConfigurationPage.this.updateSampleDocument();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, templateEditorPanel, templateTabbedPane);
        splitPane.setDividerLocation(0.5D);
        splitPane.setResizeWeight(0.5D);
        splitPane.setSize(splitPane.getPreferredSize());
        splitPane.validate();
        splitPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        add(splitPane, "Center");
    }

    private void templateDocumentChanged() {

        this.modified = true;

        updateSampleDocument();

        this.resetTemplateAction.setEnabled(!this.templateDocument.getText().equals(this.template.getDefaultTemplateText()));
    }

    private void updateSampleDocument() {

        final String template = this.templateDocument != null ? this.templateDocument.getText() : "NO TEMPLATE";

        Application application = ApplicationManager.getApplication();

        try {

            CommitLogBuilder sampleCommitLogBuilder = new CommitLogBuilder(template, "This is a sample commit message.  " +
                    "I hope you usually write more than this for your commits ;^)");

            addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass1", Change.Type.MODIFICATION, "MyVCSModule");
            addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass2", Change.Type.MODIFICATION, "MyVCSModule");
            addSampleCommitLogEntry(sampleCommitLogBuilder, "ObsoleteClass", Change.Type.DELETED, "MyVCSModule");
            addSampleCommitLogEntry(sampleCommitLogBuilder, "NewClass", Change.Type.NEW, "MyVCSModule");
            addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass1", Change.Type.MODIFICATION, "AnotherVCSModule");
            addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass2", Change.Type.MODIFICATION, "AnotherVCSModule");

            final String sample = sampleCommitLogBuilder.buildCommitLog(new Date());

            if (application != null){

                application.runWriteAction(new Runnable() {
                    public void run() {
                        CommitLogConfigurationPage.this.sampleDocument.setText(sample);
                    }
                });
            }

        } catch (CommitLogTemplateParser.TextTemplateParserException e) {

            application.runWriteAction(new Runnable() {
                public void run() {
                    CommitLogConfigurationPage.this.sampleDocument.setText(template.substring(0, e.getLocation() + 1) + "<<<ERROR\n" + e.getMessage());
                }
            });
        }
    }

    private static void addSampleCommitLogEntry(CommitLogBuilder sampleCommitLogBuilder, String className, Change.Type changeType, String vcsRootName) {

        File file = new File("c:/sandbox/" + vcsRootName + "/commitlog/samplecommit/" + className + ".java");

        CommitLogEntry logEntry = new CommitLogEntry(file, new FilePathImpl(file.getAbsolutePath(), false),
                vcsRootName, "commitlog/samplecommit", "commitlog.samplecommit", null, changeType);

        if (changeType == Change.Type.NEW) {
            logEntry.setNewVersion("1.0");
        } else {
            logEntry.setOldVersion("1.2.3.4");

            if (changeType != Change.Type.DELETED) {
                logEntry.setNewVersion("1.2.3.5");
            }
        }

        sampleCommitLogBuilder.addCommitLogEntry(logEntry);
    }

    private static void initEditor(Editor editor) {

        EditorSettings editorsettings = editor.getSettings();
        editorsettings.setFoldingOutlineShown(false);
        editorsettings.setLineMarkerAreaShown(false);
        editorsettings.setLineNumbersShown(false);
        editorsettings.setRightMarginShown(false);
        editor.getComponent().setPreferredSize(new Dimension(512, 384));
    }

    public void dispose() {

        this.templateDocument.removeDocumentListener(this.templateDocumentListener);

        if (this.templateEditor != null) {
            EditorFactory.getInstance().releaseEditor(this.templateEditor);
        }

        EditorFactory.getInstance().releaseEditor(this.sampleEditor);
    }

    boolean isModified() {
        return this.modified;
    }

    void save() {
        this.template.setTemplateText(this.templateDocument.getText());
        this.modified = false;
    }

    void load() {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                ApplicationManager.getApplication().runWriteAction(new Runnable() {

                    public void run() {
                        CommitLogConfigurationPage.this.templateDocument.setText(CommitLogConfigurationPage.this.template.getTemplateText());
                        CommitLogConfigurationPage.this.modified = false;
                    }
                });
            }
        });
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Test Frame");
        CommitLogConfigurationPage component = new CommitLogConfigurationPage(null, null);
        frame.getContentPane().add(component, "Center");
        frame.pack();
        frame.setVisible(true);
    }
}
