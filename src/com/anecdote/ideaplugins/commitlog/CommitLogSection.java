package com.anecdote.ideaplugins.commitlog;

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
