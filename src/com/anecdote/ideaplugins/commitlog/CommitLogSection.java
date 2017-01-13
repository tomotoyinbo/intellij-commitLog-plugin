package com.anecdote.ideaplugins.commitlog;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
class CommitLogSection {

    private final String text;
    private final int usedNodes;

    CommitLogSection(String text, int usedNodes) {
        this.text = text;
        this.usedNodes = usedNodes;
    }

    int getUsedNodes() {
        return this.usedNodes;
    }

    String getText() {
        return this.text;
    }
}
