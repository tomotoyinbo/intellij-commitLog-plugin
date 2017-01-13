package com.anecdote.ideaplugins.commitlog;

public interface CommitLogTemplate {

    String getTemplateText();

    void setTemplateText(String paramString);

    void reset();

    String getDefaultTemplateText();
}
