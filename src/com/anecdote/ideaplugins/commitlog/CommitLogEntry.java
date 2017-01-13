package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

class CommitLogEntry implements Comparable {

    private final File file;
    private final FilePath filePath;
    private final Change.Type changeType;
    private final String vcsRootName;
    private String pathFromRoot;
    private final String packageName;
    private final AbstractVcs vcs;
    private String oldVersion;
    private String newVersion;

    CommitLogEntry(File file,
                   FilePath filePath,
                   String vcsRootName,
                   String pathFromRoot,
                   String packageName,
                   AbstractVcs vcs,
                   Change.Type changeType) {

        this.file = file;
        this.vcsRootName = vcsRootName;
        this.pathFromRoot = pathFromRoot;
        this.packageName = (packageName != null ? packageName : "<no package>");
        this.vcs = vcs;
        this.filePath = filePath;
        this.changeType = changeType;
    }

    String getVcsRootName() {
        return this.vcsRootName;
    }

    public File getFile() {
        return this.file;
    }

    FilePath getFilePath() {
        return this.filePath;
    }

    AbstractVcs getVcs() {
        return this.vcs;
    }

    String getNewVersion() {
        return this.newVersion;
    }

    void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    String getOldVersion() {
        return this.oldVersion;
    }

    void setOldVersion(String oldVersion) {
        this.oldVersion = oldVersion;
    }

    Change.Type getChangeType() {
        return this.changeType;
    }

    String getPackageName() {
        return this.packageName;
    }

    String getPathFromRoot() {
        return this.pathFromRoot;
    }

    @NotNull
    public String toString() {

        String tmp41_35 = (this.filePath + " : " + this.oldVersion + " -> " + this.newVersion);

        if (tmp41_35 == null) {
            throw new IllegalStateException("@NotNull method com/anecdote/ideaplugins/commitlog/" +
                    "CommitLogEntry.toString must not return null");
        }

        return tmp41_35;
    }

    public boolean equals(@Nullable Object obj) {

        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        CommitLogEntry that = (CommitLogEntry) obj;

        return this.file.equals(that.file);
    }

    public int hashCode() {
        return this.file.hashCode();
    }

    public int compareTo(@NotNull Object o) {

        int depth = getDepth(this.file);
        File otherFile = ((CommitLogEntry) o).file;
        int otherDepth = getDepth(otherFile);

        if (depth == otherDepth) {
            return this.file.compareTo(otherFile);
        }

        if (depth < otherDepth) {
            return 1;
        }

        return -1;
    }

    private static int getDepth(File file) {

        String path = file.getPath();
        int result = 1;
        int i = 0;

        while (i > -1) {

            i = path.indexOf(File.separatorChar, i);

            if (i > -1) {

                result++;
                i++;
            }
        }

        return result;
    }
}
