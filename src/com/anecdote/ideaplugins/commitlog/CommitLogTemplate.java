package com.anecdote.ideaplugins.commitlog;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
public interface CommitLogTemplate {

    String getTemplateText();

    void setTemplateText(String paramString);

    void reset();

    String getDefaultTemplateText();
}
