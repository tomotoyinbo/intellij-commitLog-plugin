package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

class CommitLogBuilder {

    private static final String TIME_PLACEHOLDER = "TIME";
    private static final String DATE_PLACEHOLDER = "DATE";
    private static final String DATE_TIME_PLACEHOLDER = "DATE_TIME";
    private static final String FILE_COUNT_PLACEHOLDER = "FILE_COUNT";
    private static final String ROOT_COUNT_PLACEHOLDER = "ROOT_COUNT";
    private static final String ROOT_LIST_PLACEHOLDER = "ROOT_LIST";
    private static final String COMMIT_MESSAGE_PLACEHOLDER = "COMMIT_MESSAGE";
    private static final String ROOTS_SECTION_START_PLACEHOLDER = "ROOTS_SECTION";
    private static final String ROOTS_SECTION_END_PLACEHOLDER = "/ROOTS_SECTION";
    private static final String ROOT_ENTRY_START_PLACEHOLDER = "ROOT_ENTRY";
    private static final String ROOT_ENTRY_END_PLACEHOLDER = "/ROOT_ENTRY";
    private static final String MODIFIED_FILES_SECTION_START_PLACEHOLDER = "MODIFIED_FILES";
    private static final String ADDED_FILES_SECTION_START_PLACEHOLDER = "ADDED_FILES";
    private static final String DELETED_FILES_SECTION_START_PLACEHOLDER = "DELETED_FILES";
    private static final String ALL_FILES_BY_TYPE_SECTION_START_PLACEHOLDER = "ALL_FILES_BY_TYPE";
    private static final String ALL_FILES_SECTION_START_PLACEHOLDER = "ALL_FILES";
    private static final String MODIFIED_FILES_SECTION_END_PLACEHOLDER = "/MODIFIED_FILES";
    private static final String ADDED_FILES_SECTION_END_PLACEHOLDER = "/ADDED_FILES";
    private static final String DELETED_FILES_SECTION_END_PLACEHOLDER = "/DELETED_FILES";
    private static final String ALL_FILES_BY_TYPE_SECTION_END_PLACEHOLDER = "ALL_FILES_BY_TYPE";
    private static final String ALL_FILES_SECTION_END_PLACEHOLDER = "/ALL_FILES";
    private static final String FILE_ENTRY_START_PLACEHOLDER = "FILE_ENTRY";
    private static final String FILE_ENTRY_END_PLACEHOLDER = "/FILE_ENTRY";
    private static final String FILE_NAME_PLACEHOLDER = "FILE_NAME";
    private static final String FILE_PATH_PLACEHOLDER = "FILE_PATH";
    private static final String FILE_ACTION_PLACEHOLDER = "FILE_ACTION";
    private static final String ROOT_NAME_PLACEHOLDER = "ROOT_NAME";
    private static final String PACKAGE_NAME_PLACEHOLDER = "PACKAGE_NAME";
    private static final String PACKAGE_PATH_PLACEHOLDER = "PACKAGE_PATH";
    private static final String PATH_FROM_ROOT_PLACEHOLDER = "PATH_FROM_ROOT";
    private static final String OLD_REVISION_NUMBER_PLACEHOLDER = "OLD_REVISION_NUMBER";
    private static final String NEW_REVISION_NUMBER_PLACEHOLDER = "NEW_REVISION_NUMBER";
    private static final String CHANGE_SYMBOL_PLACEHOLDER = "CHANGE_SYMBOL";
    private int fileCount;
    private final Map<String, Map<Change.Type, Collection<CommitLogEntry>>> commitLogEntriesByRootAndType = new TreeMap();

    private Map<Change.Type, Collection<CommitLogEntry>> commitLogEntriesByType = new EnumMap(Change.Type.class);

    private Map<String, Collection<CommitLogEntry>> commitLogEntriesByRootAndPath = new HashMap();

    private Collection<CommitLogEntry> commitLogEntries = new TreeSet();
    private String commitMessage;
    private final String commitLogTemplate;
    private String changeListName;

    CommitLogBuilder(String commitLogTemplate, String commitMessage) {
        this.commitLogTemplate = commitLogTemplate;
        this.commitMessage = commitMessage;
    }

    public void addCommitLogEntry(CommitLogEntry commitLogEntry) {

        String rootName = commitLogEntry.getVcsRootName();
        getCommitLogEntries(rootName, commitLogEntry.getChangeType()).add(commitLogEntry);
        getCommitLogEntries(null, commitLogEntry.getChangeType()).add(commitLogEntry);
        this.commitLogEntries.add(commitLogEntry);
        getCommitLogEntriesByRoot(rootName).add(commitLogEntry);
        this.fileCount += 1;
    }

    public void removeUncommittedEntries() {
        removeUncommittedEntriesByRoot();
        removeUncommittedEntriesByType(this.commitLogEntriesByType);
    }

    private void removeUncommittedEntriesByRoot() {

        Iterator<Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>>> entriesByRootIterator = this.commitLogEntriesByRootAndType.entrySet().iterator();

        while (entriesByRootIterator.hasNext()) {

            Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>> rootEntry = (Map.Entry) entriesByRootIterator.next();
            Map<Change.Type, Collection<CommitLogEntry>> entriesForRootByType = (Map) rootEntry.getValue();

            this.fileCount -= removeUncommittedEntriesByType(entriesForRootByType);

            if (entriesForRootByType.isEmpty()) {
                entriesByRootIterator.remove();
            }
        }
    }

    private static int removeUncommittedEntriesByType(Map<Change.Type, Collection<CommitLogEntry>> entriesForRootByType) {

        int result = 0;
        Iterator<Map.Entry<Change.Type, Collection<CommitLogEntry>>> entriesByTypeIterator = entriesForRootByType.entrySet().iterator();

        while (entriesByTypeIterator.hasNext()) {

            Map.Entry<Change.Type, Collection<CommitLogEntry>> entry = (Map.Entry) entriesByTypeIterator.next();
            Collection<CommitLogEntry> commitLogEntries = (Collection) entry.getValue();
            Iterator<CommitLogEntry> commitLogEntryIterator = commitLogEntries.iterator();

            while (commitLogEntryIterator.hasNext()) {

                CommitLogEntry commitLogEntry = commitLogEntryIterator.next();

                if (commitLogEntry.getOldVersion() == null ? commitLogEntry.getNewVersion() == null : commitLogEntry.getOldVersion().equals(commitLogEntry.getNewVersion())) {

                    CommitLogProjectComponent.log("Removing Commit log entry for " + commitLogEntry.getFilePath() + " : file not committed");

                    commitLogEntryIterator.remove();
                    result++;
                }
            }

            if (commitLogEntries.isEmpty()) {
                entriesByTypeIterator.remove();
            }
        }

        return result;
    }

    private Collection<CommitLogEntry> getCommitLogEntries(String root, @Nullable Change.Type type) {

        if (type != null) {

            Map<Change.Type, Collection<CommitLogEntry>> byRoot = getCommitLogEntriesByTypeByRoot(root);
            Collection<CommitLogEntry> result = (Collection) byRoot.get(type);

            if (result == null) {
                result = new HashSet();
                byRoot.put(type, result);
            }

            return result;
        }

        return root != null ? (Collection) this.commitLogEntriesByRootAndPath.get(root) : this.commitLogEntries;
    }

    public Map<Change.Type, Collection<CommitLogEntry>> getCommitLogEntriesByTypeByRoot(String root) {

        Map<Change.Type, Collection<CommitLogEntry>> byRoot = root != null ? (Map) this.commitLogEntriesByRootAndType.get(root) : this.commitLogEntriesByType;

        if (byRoot == null) {
            byRoot = new EnumMap(Change.Type.class);
            this.commitLogEntriesByRootAndType.put(root, byRoot);
        }

        return byRoot;
    }

    private Collection<CommitLogEntry> getCommitLogEntriesByRoot(String root) {

        Collection<CommitLogEntry> byRoot = root != null ? (Collection) this.commitLogEntriesByRootAndPath.get(root) : this.commitLogEntries;

        if ((root != null) && (byRoot == null)) {
            byRoot = new TreeSet();
            this.commitLogEntriesByRootAndPath.put(root, byRoot);
        }

        return byRoot;
    }

    protected String buildCommitLog(Date date) throws CommitLogTemplateParser.TextTemplateParserException {

        CommitLogProjectComponent.log("CommitLogBuilder::buildCommitLog() Entered");

        CommitLogTemplateParser parser = new CommitLogTemplateParser();
        List<CommitLogTemplateParser.TextTemplateNode> textTemplateNodes = parser.parseTextTemplate(this.commitLogTemplate);

        if (textTemplateNodes.isEmpty()) {
            CommitLogProjectComponent.log("ERROR : Parsed template is empty!");
        }

        StringBuilder result = new StringBuilder(500);

        for (int i = 0; i < textTemplateNodes.size(); i++) {

            CommitLogTemplateParser.TextTemplateNode textTemplateNode = textTemplateNodes.get(i);
            String text = textTemplateNode.getText();

            if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {

                if (text.equals("ROOTS_SECTION")) {

                    i++;

                    List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(textTemplateNodes, i);
                    CommitLogSection logSection = buildCommitLogRootsSection(followingNodes, new Date());
                    i += logSection.getUsedNodes() - 1;
                    text = logSection.getText();

                } else if (isFileSectionStartPlaceholder(text)) {

                    i++;
                    List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(textTemplateNodes, i);
                    CommitLogSection logSection = buildCommitLogFilesSection(followingNodes, null, text);
                    i += logSection.getUsedNodes() - 1;
                    text = logSection.getText();

                }

            } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
                text = processCommonPlaceholders(text, date);
            }

            result.append(text);
        }

        return result.toString();
    }

    private static boolean isFilesSectionEndPlaceholder(String text) {

        return ("/DELETED_FILES".equals(text))
                || ("/MODIFIED_FILES".equals(text))
                || ("/ADDED_FILES".equals(text))
                || ("ALL_FILES_BY_TYPE".equals(text))
                || ("/ALL_FILES".equals(text)
        );
    }

    private static boolean isFileSectionStartPlaceholder(String text) {

        return ("DELETED_FILES".equals(text))
                || ("MODIFIED_FILES".equals(text))
                || ("ADDED_FILES".equals(text))
                || ("ALL_FILES_BY_TYPE".equals(text))
                || ("ALL_FILES".equals(text)
        );
    }

    private String processCommonPlaceholders(String nodeText, Date date) {

        switch (nodeText) {
            case "TIME":
                nodeText = DateFormat.getTimeInstance().format(date);
                break;
            case "DATE":
                nodeText = DateFormat.getDateInstance().format(date);
                break;
            case "DATE_TIME":
                nodeText = DateFormat.getDateTimeInstance().format(date);
                break;
            case "FILE_COUNT":
                nodeText = String.valueOf(this.fileCount);
                break;
            case "ROOT_COUNT":
                nodeText = String.valueOf(this.commitLogEntriesByRootAndType.size());
                break;
            case "ROOT_LIST":
                nodeText = toString(this.commitLogEntriesByRootAndType.keySet());
                break;
            case "COMMIT_MESSAGE":
                nodeText = this.commitMessage;
                break;
            default:
                nodeText = "Illegal Placeholder : $" + nodeText + "$";
                break;
        }

        return nodeText;
    }

    private CommitLogSection buildCommitLogFilesSection(List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                        @Nullable String rootName, String sectionPlaceholder) {

        CommitLogSection logSection;

        if (sectionPlaceholder.equals("ALL_FILES_BY_TYPE")) {

            logSection = buildCommitLogFilesSection(nodes, rootName, true);

        } else {

            if (sectionPlaceholder.equals("ALL_FILES")) {

                logSection = buildCommitLogFilesSection(nodes, rootName, false);

            } else {

                Change.Type type = null;

                switch (sectionPlaceholder) {
                    case "ADDED_FILES":
                        type = Change.Type.NEW;
                        break;
                    case "DELETED_FILES":
                        type = Change.Type.DELETED;
                        break;
                    case "MODIFIED_FILES":
                        type = Change.Type.MODIFICATION;
                        break;
                }

                logSection = type != null ? buildCommitLogFilesSection(nodes, rootName, type) : new CommitLogSection("Unknown placeholder in template : $" + sectionPlaceholder + '$', 1);
            }
        }

        return logSection;
    }

    private CommitLogSection buildCommitLogRootsSection(List<CommitLogTemplateParser.TextTemplateNode> nodes, Date date) {

        int usedNodes = 0;
        StringBuilder result = new StringBuilder(500);

        for (int i = 0; i < nodes.size(); i++) {

            CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
            String text = textTemplateNode.getText();

            if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {

                String nodeText = text;

                if (nodeText.equals("ROOT_ENTRY")) {

                    i++;
                    CommitLogSection logSection = buildCommitLogRootEntries(getFollowingNodes(nodes, i), date);
                    text = logSection.getText();
                    i += logSection.getUsedNodes() - 1;

                } else if (text.equals("/ROOTS_SECTION")) {

                    usedNodes = i + 1;
                    break;
                }

            } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
                text = processCommonPlaceholders(text, date);
            }

            if (!this.commitLogEntriesByRootAndType.isEmpty()) {
                result.append(text);
            }
        }

        return new CommitLogSection(result.toString(), usedNodes);
    }

    private CommitLogSection buildCommitLogRootEntries(List<CommitLogTemplateParser.TextTemplateNode> nodes, Date date) {

        StringBuilder result = new StringBuilder(500);
        int usedNodes = 0;

        if (this.commitLogEntriesByRootAndType.isEmpty()) {
            usedNodes = appendCommitLogRootEntries(result, nodes, date, null, null);
            return new CommitLogSection("", usedNodes);
        }

        for (Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>> entry : this.commitLogEntriesByRootAndType.entrySet()) {

            String rootName = entry.getKey();
            Map<Change.Type, Collection<CommitLogEntry>> logEntriesByType = (Map) entry.getValue();
            usedNodes = appendCommitLogRootEntries(result, nodes, date, rootName, logEntriesByType);
        }

        return new CommitLogSection(result.toString(), usedNodes);
    }

    private int appendCommitLogRootEntries(StringBuilder buffer,
                                           List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                           Date date,
                                           @Nullable String rootName,
                                           @Nullable Map<Change.Type, Collection<CommitLogEntry>> logEntriesByType) {

        int usedNodes = 0;

        for (int i = 0; i < nodes.size(); i++) {

            CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
            String text = textTemplateNode.getText();

            if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {

                String nodeText = text;

                if (isFileSectionStartPlaceholder(nodeText)) {

                    i++;
                    List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(nodes, i);
                    CommitLogSection logSection = buildCommitLogFilesSection(followingNodes, rootName, nodeText);
                    i += logSection.getUsedNodes() - 1;
                    text = logSection.getText();

                } else {

                    if (text.equals("/ROOT_ENTRY")) {
                        usedNodes = i + 1;
                        break;
                    }

                    text = "Illegal section placeholder " + text + " : expecting " + "[" + "/ROOT_ENTRY" + "]";
                }

            } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {

                String nodeText = text;

                switch (nodeText) {
                    case "ROOT_NAME":
                        text = rootName;
                        break;
                    case "FILE_COUNT":
                        int fileCount = 0;
                        if (logEntriesByType != null) {
                            for (Map.Entry<Change.Type, Collection<CommitLogEntry>> entries : logEntriesByType.entrySet()) {
                                fileCount += ((Collection) entries.getValue()).size();
                            }
                        }
                        text = String.valueOf(fileCount);
                        break;
                    default:
                        text = processCommonPlaceholders(nodeText, date);
                        break;
                }
            }

            buffer.append(text);
        }

        return usedNodes;
    }

    @NotNull
    private CommitLogSection buildCommitLogFilesSection(@NotNull List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                        @Nullable String rootName, boolean byType) {

        if (byType) {
            final CommitLogSection deleted = buildCommitLogFilesSection(nodes, rootName, Change.Type.DELETED);
            final CommitLogSection modified = buildCommitLogFilesSection(nodes, rootName, Change.Type.MODIFICATION);
            final CommitLogSection created = buildCommitLogFilesSection(nodes, rootName, Change.Type.NEW);
            final int usedNodes = deleted.getUsedNodes();
            // all used nodes should be same
            assert (usedNodes == modified.getUsedNodes() && usedNodes == created.getUsedNodes());
            return new CommitLogSection(deleted.getText() + modified.getText() + created.getText(), usedNodes);
        } else {
            return buildCommitLogFilesSection(nodes, rootName, (Change.Type)null);
        }

    }

    @NotNull
    private CommitLogSection buildCommitLogFilesSection(@NotNull List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                        @Nullable String rootName, @Nullable Change.Type type) {

        int usedNodes = 0;
        Collection<CommitLogEntry> entries = getCommitLogEntries(rootName, type);
        boolean hasEntries = (entries != null) && (!entries.isEmpty());
        StringBuilder result = hasEntries ? new StringBuilder(500) : new StringBuilder(0);

        for (int i = 0; i < nodes.size(); i++) {

            CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
            String text = textTemplateNode.getText();

            if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {

                if (text.equals("FILE_ENTRY")) {

                    i++;

                    List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(nodes, i);
                    CommitLogSection logSection = hasEntries ? buildCommitLogFileEntries(followingNodes, type, entries) : buildCommitLogFileEntry(followingNodes, type, null);

                    i += logSection.getUsedNodes() - 1;
                    text = logSection.getText();

                } else if (isFilesSectionEndPlaceholder(text)) {

                    usedNodes = i + 1;
                    break;
                }

            } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {

                if (text.equals("FILE_COUNT")) {

                    if (hasEntries) {
                        text = String.valueOf(entries.size());
                    }

                } else {
                    text = "Illegal Placeholder : $" + text + "$";
                }
            }

            if (hasEntries) {
                result.append(text);
            }
        }

        return new CommitLogSection(result.toString(), usedNodes);
    }

    @NotNull
    private static CommitLogSection buildCommitLogFileEntries(@NotNull List<CommitLogTemplateParser.TextTemplateNode> followingNodes,
                                                              Change.Type defaultType, Collection<CommitLogEntry> entries) {
        int usedNodes = 0;
        StringBuilder result = new StringBuilder(500);

        for (CommitLogEntry entry : entries) {

            CommitLogSection logSection = buildCommitLogFileEntry(followingNodes, defaultType, entry);

            result.append(logSection.getText());
            usedNodes = logSection.getUsedNodes();
        }

        return new CommitLogSection(result.toString(), usedNodes);
    }

    private static CommitLogSection buildCommitLogFileEntry(List<CommitLogTemplateParser.TextTemplateNode> followingNodes,
                                                            Change.Type defaultType, @Nullable CommitLogEntry entry) {

        Change.Type type = entry != null ? entry.getChangeType() : defaultType;
        int usedNodes = 0;
        StringBuilder result = new StringBuilder(500);
        FilePath filePath = entry != null ? entry.getFilePath() : null;

        for (int i = 0; i < followingNodes.size(); i++) {

            CommitLogTemplateParser.TextTemplateNode textTemplateNode = followingNodes.get(i);
            String text = textTemplateNode.getText();

            if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {

                if (text.equals("/FILE_ENTRY")) {
                    usedNodes = i + 1;
                    break;
                }

                text = "Illegal section placeholder " + text + " : expecting one of " +
                        "[" + "/DELETED_FILES" + "]" + ',' +
                        "[" + "/ADDED_FILES" + "]" + ',' +
                        "[" + "/MODIFIED_FILES" + "]" +
                        " or " +
                        "[" + "ALL_FILES_BY_TYPE" + "]";

            } else if ((textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE)
                    && (entry != null)) {

                switch (text) {
                    case "FILE_NAME":
                        text = filePath != null ? filePath.getName() : "<no file>";
                        break;
                    case "FILE_PATH":
                        text = filePath != null ? filePath.getPath() : "<no file>";
                        break;
                    case "FILE_ACTION":
                        if (type == Change.Type.DELETED) {
                            text = "Removed";
                        } else if (type == Change.Type.MODIFICATION) {
                            text = "Modified";
                        } else if (type == Change.Type.NEW) {
                            text = "Added";
                        }
                        break;
                    case "ROOT_NAME":
                        text = entry.getVcsRootName();
                        break;
                    case "PACKAGE_NAME":
                        text = entry.getPackageName();
                        break;
                    case "PACKAGE_PATH":
                    case "PATH_FROM_ROOT":
                        text = entry.getPathFromRoot();
                        break;
                    case "OLD_REVISION_NUMBER":
                        if ((entry.getOldVersion() == null) || (type == Change.Type.NEW)) {
                            text = "Added";
                        } else {
                            text = entry.getOldVersion();
                        }
                        break;
                    case "NEW_REVISION_NUMBER":
                        if ((entry.getNewVersion() == null) || (type == Change.Type.DELETED)) {
                            text = "Removed";
                        } else {
                            text = entry.getNewVersion();
                        }
                        break;
                    case "CHANGE_SYMBOL":
                        if ((entry.getOldVersion() == null) || (type == Change.Type.NEW)) {
                            text = "+";
                        } else if (type == Change.Type.DELETED) {
                            text = "-";
                        } else {
                            text = "*";
                        }
                        break;
                    default:
                        text = "Illegal Placeholder : $" + text + "$";
                        break;
                }
            }

            result.append(text);
        }

        return new CommitLogSection(result.toString(), usedNodes);
    }

    private static List<CommitLogTemplateParser.TextTemplateNode> getFollowingNodes(@NotNull List<CommitLogTemplateParser.TextTemplateNode> textTemplateNodes, int start) {

        return textTemplateNodes.subList(start, textTemplateNodes.size());
    }

    private static String toString(@NotNull Collection collection) {

        StringBuilder stringBuilder = new StringBuilder(100);

        for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {

            stringBuilder.append(iterator.next());

            if (iterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
    }

    public String getChangeListName() {
        return this.changeListName;
    }

    private void setChangeListName(String changeListName) {
        this.changeListName = changeListName;
    }

    public static CommitLogBuilder createCommitLogBuilder(String template,
                                                          String commitMessage,
                                                          Project project,
                                                          Collection<File> files) {

        CommitLogBuilder commitLogBuilder = new CommitLogBuilder(template, commitMessage);

        for (File file : files) {

            FilePath filePath = file.exists() ? VcsUtil.getFilePath(file) : VcsUtil.getFilePathForDeletedFile(file.getPath(), false);

            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            Change change = changeListManager.getChange(filePath);

            if (commitLogBuilder.getChangeListName() == null) {
                commitLogBuilder.setChangeListName(changeListManager.getChangeList(change).getName());
            }

            if (change != null) {

                Change.Type changeType = change.getType();
                ContentRevision beforeRevision = changeType == Change.Type.NEW ? null : change.getBeforeRevision();
                VirtualFile vcsRoot = VcsUtil.getVcsRootFor(project, filePath);
                String vcsRootName = vcsRoot != null ? vcsRoot.getPresentableName() : "";
                AbstractVcs vcs = VcsUtil.getVcsFor(project, filePath);

                if (vcs != null) {

                    String packageName = getPackageName(project, filePath);
                    String pathFromRoot = getPathFromRoot(vcsRoot, filePath);
                    CommitLogEntry commitLogEntry = new CommitLogEntry(file, filePath, vcsRootName, pathFromRoot, packageName, vcs, changeType);

                    commitLogBuilder.addCommitLogEntry(commitLogEntry);

                    if (beforeRevision != null) {
                        commitLogEntry.setOldVersion(beforeRevision.getRevisionNumber().asString());
                    }
                }
            }
        }

        return commitLogBuilder;
    }

    private static String getPathFromRoot(VirtualFile vcsRoot, FilePath filePath) {

        String pathFromRoot = null;
        FilePath path = filePath.getParentPath();

        while ((path != null) && (!vcsRoot.equals(path.getVirtualFile()))) {

            String name = path.getName();
            pathFromRoot = pathFromRoot != null ? name + '/' + pathFromRoot : name;
            path = path.getParentPath();
        }

        return pathFromRoot != null ? pathFromRoot : "";
    }

    private static String getPackageName(Project project, FilePath filePath) {

        VirtualFile parent = filePath.getVirtualFileParent();
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        String text;

        if (parent != null) {
            text = projectFileIndex.getPackageNameByDirectory(parent);
        } else {
            text = "";
        }

        return text;
    }
}
