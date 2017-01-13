package com.anecdote.ideaplugins.commitlog;

import javax.swing.*;
import java.awt.*;

public class CommitLogConfigurationPanel extends JPanel {

    private CommitLogProjectComponent _projectComponent;

    private CommitLogConfigurationPage _commitLogTemplatePage;

    private CommitLogConfigurationPage _commitCommentTemplatePage;

    private JTabbedPane _tabbedPane;

    public CommitLogConfigurationPanel(CommitLogProjectComponent projectComponent)
    {
        super(new BorderLayout());
        setPreferredSize(new Dimension(400, 700));
        this._projectComponent = projectComponent;
        JTabbedPane tabbedPane = new JTabbedPane();
        this._commitLogTemplatePage = new CommitLogConfigurationPage(new CommitLogTemplate()
        {
            public String getTemplateText()
            {
                return CommitLogConfigurationPanel.this._projectComponent.getTextualCommitLogTemplate();
            }

            public void setTemplateText(String text)
            {
                CommitLogConfigurationPanel.this._projectComponent.setTextualCommitLogTemplate(text);
            }

            public void reset()
            {
                CommitLogConfigurationPanel.this._projectComponent.resetCommitLogTemplate();
            }

            public String getDefaultTemplateText()
            {
                return CommitLogProjectComponent.DEFAULT_COMMIT_LOG_TEMPLATE;
            }
        }
                , this._projectComponent);

        this._commitCommentTemplatePage = new CommitLogConfigurationPage(new CommitLogTemplate()
        {
            public String getTemplateText()
            {
                return CommitLogConfigurationPanel.this._projectComponent.getTextualCommitCommentTemplate();
            }

            public void setTemplateText(String text)
            {
                CommitLogConfigurationPanel.this._projectComponent.setTextualCommitCommentTemplate(text);
            }

            public void reset()
            {
                CommitLogConfigurationPanel.this._projectComponent.resetCommitCommentTemplate();
            }

            public String getDefaultTemplateText()
            {
                return CommitLogProjectComponent.DEFAULT_COMMIT_COMMENT_TEMPLATE;
            }
        }
                , this._projectComponent);

        this._tabbedPane = tabbedPane;
        this._tabbedPane.addTab("Commit Log Template", this._commitLogTemplatePage);
        tabbedPane.addTab("Commit Comment Template", this._commitCommentTemplatePage);
        add(tabbedPane, "Center");
        add(new JLabel("Version 1.3 : Copyright 2007 - 2009 Anecdote Software.  All Rights Reserved."), "South");
    }

    public boolean isModified()
    {
        return (this._commitCommentTemplatePage.isModified()) || (this._commitLogTemplatePage.isModified());
    }

    public void save()
    {
        this._commitCommentTemplatePage.save();
        this._commitLogTemplatePage.save();
    }

    public void load()
    {
        this._commitCommentTemplatePage.load();
        this._commitLogTemplatePage.load();
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Test Frame");
        CommitLogConfigurationPanel component = new CommitLogConfigurationPanel(null);
        frame.getContentPane().add(component, "Center");
        frame.pack();
        frame.setVisible(true);
    }

    public void setSelectedTab(int index)
    {
        this._tabbedPane.setSelectedIndex(index);
    }
}
