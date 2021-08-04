package gitlet;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *
 *  @author Shiang Lin, Ian Huang
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static File CWD;
    /** The .gitlet directory. */
    public static File GITLET_DIR;

    public static File STAGE_DIR;

    public static File COMMITS_DIR;

    public static File BLOBS_DIR;

    public static File BRANCHES_DIR;
    /* The sha1 of the HEAD */
    private static String HEAD;
    /* The current branch name */
    private static String currentBranch;

    public static void setupRepository(String cwd) {
        CWD = new File(cwd);
        GITLET_DIR = Utils.join(CWD, ".gitlet");
        STAGE_DIR = Utils.join(GITLET_DIR, "staged");
        COMMITS_DIR = Utils.join(GITLET_DIR, "commits");
        BLOBS_DIR = Utils.join(GITLET_DIR, "blobs");
        BRANCHES_DIR = Utils.join(GITLET_DIR, "branches");
        if (!isInitialized()) {
            return;
        }
        HEAD = readHEAD();
        currentBranch = readCurrentBranch();
    }

    public static void saveRepository() {
        saveHEAD();
        saveCurrentBranch();
        saveBranch(currentBranch, HEAD);
    }
    public static void init() {
        /** if the CWD is initialized, exit with error */
        if (isInitialized()) {
            Main.exitWithError("A Gitlet version-control system already exists "
                    + "in the current directory.");
        }
        /** set up directory */
        GITLET_DIR.mkdir();
        STAGE_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        /** set initial commit */
        Commit initialCommit = new Commit("initial commit");
        initialCommit.saveCommit();
        HEAD = initialCommit.getID();
        currentBranch = "master";
        saveRepository();
    }
    /** Returns whether the CWD is initialized */
    public static boolean isInitialized() {
        return Utils.join(GITLET_DIR).isDirectory();
    }
    /** Save the sha1 of HEAD commit to the HEAD file in .gitlet */
    private static void saveHEAD() {
        File saveFile = Utils.join(GITLET_DIR, "HEAD");
        Utils.writeContents(saveFile, HEAD);
    }
    /** Reads the sha1 from the file HEAD*/
    private static String readHEAD() {
        File saveFile = Utils.join(GITLET_DIR, "HEAD");
        return Utils.readContentsAsString(saveFile);
    }

    private static void saveBranch(String branchName, String commitId) {
        File newBranchFile = Utils.join(BRANCHES_DIR, branchName);
        Utils.writeContents(newBranchFile, commitId);
    }

    private static String readBranch(String branchName) {
        File saveFile = Utils.join(BRANCHES_DIR, branchName);
        return Utils.readContentsAsString(saveFile);
    }

    private static void saveCurrentBranch() {
        File saveFile = Utils.join(GITLET_DIR, "currentBranch");
        Utils.writeContents(saveFile, currentBranch);
    }

    private static String readCurrentBranch() {
        File saveFile = Utils.join(GITLET_DIR, "currentBranch");
        return Utils.readContentsAsString(saveFile);
    }

    public static void add(String[] args) {
        validateNumArgs(args, 3);
        File fileToStage = Utils.join(CWD, args[2]);
        /** If the file does not exist, exit with error */
        if (!fileToStage.exists()) {
            Main.exitWithError("File does not exist.");
        }
        // Adds a copy of the file as it currently exists to the staging area
        StagingArea.readStage();
        StagingArea.addFileToStage(fileToStage);
        StagingArea.saveStage();
        // The added file will no longer be staged for removal
        StagingArea.readStageForRemoval();
        if (StagingArea.contains(fileToStage, true)) {
            StagingArea.removeFileFromStage(fileToStage, true);
        }
        StagingArea.saveStageForRemoval();
    }

    public static void commit(String[] args) {
        validateNumArgs(args, 3);
        if (args[2].isBlank()) {
            Main.exitWithError("Please enter a commit message.");
        }
        StagingArea.readStage();
        StagingArea.readStageForRemoval();
        Commit newCommit = new Commit(args[2], HEAD);
        Commit headCommit = Commit.readCommit(HEAD);
        newCommit.metadataPutAll(headCommit.getMetadata());
        if (StagingArea.stageIsEmpty() && StagingArea.stageForRemovalIsEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            newCommit.metadataPutAll(StagingArea.getStaged());
            newCommit.metadataRemoveAll(StagingArea.getStagedForRemoval());
        }
        HEAD = newCommit.getID();
        StagingArea.clearStage(false);
        StagingArea.clearStage(true);
        saveRepository();
        newCommit.saveCommit();
        StagingArea.saveStage();
        StagingArea.saveStageForRemoval();
    }

    public static void remove(String[] args) {
        validateNumArgs(args, 3);
        StagingArea.readStage();
        StagingArea.readStageForRemoval();
        File fileToRemove = Utils.join(CWD, args[2]);
        // failure case
        if (!StagingArea.contains(fileToRemove, false) && !Commit.readCommit(HEAD).metadataContains(args[1])) {
            Main.exitWithError("No reason to remove the file.");
        }
        // Unstage the file if it is currently staged for addition
        if (StagingArea.contains(fileToRemove, false)) {
            StagingArea.removeFileFromStage(fileToRemove, false);
        }
        // If the file is tracked in the current commit, stage it for removal
        // and remove the file from the working directory if the user has not already done so
        if (Commit.readCommit(HEAD).metadataContains(args[2])) {
            StagingArea.addFileToRemove(fileToRemove);
            if (fileToRemove.exists()) {
                fileToRemove.delete();
            }
        }
        StagingArea.saveStage();
        StagingArea.saveStageForRemoval();
    }

    public static void log() {
        Commit currentCommit = Commit.readCommit(HEAD);
        while (currentCommit != null) {
            System.out.println(currentCommit);
            currentCommit = currentCommit.getParentCommit();
        }
    }

    public static void globalLog() {
        List<String> commitList = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        Commit currentCommit;
        for (String commitFileName: commitList) {
            currentCommit = Commit.readCommit(commitFileName);
            System.out.println(currentCommit);
        }
    }

    public static void find(String[] args) {
        validateNumArgs(args, 3);
        List<String> commitList = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        Commit currentCommit;
        boolean commitExists = false;
        for (String commitFileName: commitList) {
            currentCommit = Commit.readCommit(commitFileName);
            if (currentCommit.getMessage().equals(args[2])) {
                System.out.println(currentCommit.getID());
                commitExists = true;
            }
        }
        if (!commitExists) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        // Branches
        List<String> branchesList = Utils.plainFilenamesIn(Repository.BRANCHES_DIR);
        System.out.println("=== Branches ===");
        for (String currentBranchName: branchesList) {
            if (currentBranch.equals(currentBranchName)) {
                System.out.print('*');
            }
            System.out.println(currentBranchName);
        }
        System.out.println();
        // Staged Files
        StagingArea.readStage();
        System.out.println("=== Staged Files ===");
        HashMap <String, String> stagedList = StagingArea.getStaged();
        for (String stagedItem: stagedList.keySet()) {
            System.out.println(stagedItem);
        }
        System.out.println();
        // Removed Files
        StagingArea.readStageForRemoval();
        System.out.println("=== Removed Files ===");
        HashMap <String, String> stagedForRemovalList = StagingArea.getStagedForRemoval();
        for (String stagedItem: stagedForRemovalList.keySet()) {
            System.out.println(stagedItem);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===\n");
        System.out.println("=== Untracked Files ===\n");
    }

    public static void checkout(String[] args) {
        if (args.length == 4 || args.length == 5) {
            checkoutCommit(args);
        } else if (args.length == 3) {
            checkoutBranch(args);
        } else {
            Main.exitWithError("Incorrect operands.");
        }
    }
    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory,
     * overwriting the versions of the files that are already there if they exist.
     *
     * java gitlet.Main checkout [branch name] */
    private static void checkoutBranch(String[] args) {
        if (currentBranch.equals(args[2])) {
            Main.exitWithError("No need to checkout the current branch.");
        }
        File branchFile = Utils.join(BRANCHES_DIR, args[2]);
        if (!branchFile.exists()) {
            Main.exitWithError("No such branch exists.");
        }
        StagingArea.readStage();
        StagingArea.readStageForRemoval();
        Commit currentCommit = Commit.readCommit(HEAD);
        String newBranchName = args[2];
        String newBranch = readBranch(newBranchName);
        Commit newBranchCommit = Commit.readCommit(newBranch);
        for (String newBranchFile: newBranchCommit.getMetadata().keySet()) {
            if (!currentCommit.metadataContains(newBranchFile)) {
                File file = Utils.join(CWD, newBranchFile);
                if (file.exists()) {
                    Main.exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }
        for (String newBranchFile: newBranchCommit.getMetadata().keySet()) {
            File file = Utils.join(CWD, newBranchFile);
            String blobSha1 = newBranchCommit.getBlobSha1(file);
            Blob blob = Blob.readBlob(blobSha1);
            writeContents(file, blob.getText());
        }
        for (String currentCommitFile: currentCommit.getMetadata().keySet()) {
            if (!newBranchCommit.metadataContains(currentCommitFile)) {
                File file = Utils.join(CWD, currentCommitFile);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        HEAD = newBranch;
        currentBranch = newBranchName;
        saveRepository();
        StagingArea.clearStage(true);
        StagingArea.clearStage(false);
        StagingArea.saveStage();
        StagingArea.saveStageForRemoval();
    }

    /** Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory,
     * overwriting the version of the file that's already there if there is one.
     * The new version of the file is not staged.
     *
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout -- [filename]*/
    private static void checkoutCommit(String[] args) {
        String commitID, filename;
        if (args.length == 4) {
            if (!args[2].equals("--")) {
                Main.exitWithError("Incorrect operands.");
            }
            commitID = HEAD;
            filename = args[3];
        } else {
            if (!args[3].equals("--")) {
                Main.exitWithError("Incorrect operands.");
            }
            commitID = args[2];
            filename = args[4];
        }
        Commit commit = Commit.readCommitAb(commitID);
        File file = Utils.join(CWD, filename);
        if (commit == null) {
            Main.exitWithError("No commit with that id exists.");
        }
        if (!commit.metadataContains(filename)) {
            Main.exitWithError("File does not exist in that commit.");
        }
        String blobSha1 = commit.getBlobSha1(file);
        Blob blob = Blob.readBlob(blobSha1);
        writeContents(file, blob.getText());
    }

    public static void branch(String[] args) {
        validateNumArgs(args, 3);
        String newBranchName = args[2];
        File branchFile = Utils.join(BRANCHES_DIR, newBranchName);
        if (branchFile.exists()) {
            Main.exitWithError("A branch with that name already exists.");
        }
        saveBranch(newBranchName, HEAD);
    }

    public static void rmBranch(String[] args) {
        validateNumArgs(args, 3);
        String branchToRemove = args[2];
        File branchFile = Utils.join(BRANCHES_DIR, branchToRemove);
        if (branchToRemove.equals(currentBranch)) {
            Main.exitWithError("Cannot remove the current branch.");
        }
        if (!branchFile.exists()) {
            Main.exitWithError("A branch with that name does not exist.");
        }
        branchFile.delete();
    }

    public static void reset(String[] args) {
        validateNumArgs(args, 3);
        StagingArea.readStageForRemoval();
        StagingArea.readStage();
        Commit resetCommit = Commit.readCommitAb(args[2]);
        Commit currentCommit = Commit.readCommit(HEAD);
        // Failure case 1: no commit with the given id exists
        if (resetCommit == null) {
            Main.exitWithError("No commit with that id exists.");
        }
        //Failure case 2: working file is untracked in the current branch and would be overwritten by the reset
        for (String resetCommitFile: resetCommit.getMetadata().keySet()) {
            if (!currentCommit.metadataContains(resetCommitFile)) {
                File file = Utils.join(CWD, resetCommitFile);
                if (file.exists()) {
                    Main.exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }
        // Checks out all the files tracked by the given commit
        for (String resetCommitFile: resetCommit.getMetadata().keySet()) {
            File file = Utils.join(CWD, resetCommitFile);
            String blobSha1 = resetCommit.getBlobSha1(file);
            Blob blob = Blob.readBlob(blobSha1);
            writeContents(file, blob.getText());
        }

        // Removes tracked files that are not present in that commit
        for (String currentCommitFile: currentCommit.getMetadata().keySet()) {
            if (!resetCommit.metadataContains(currentCommitFile)) {
                File file = Utils.join(CWD, currentCommitFile);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        // moves the current branch's head to that commit node
        HEAD = resetCommit.getID();
        saveRepository();
        StagingArea.clearStage(true);
        StagingArea.clearStage(false);
        StagingArea.saveStage();
        StagingArea.saveStageForRemoval();
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            Main.exitWithError("Incorrect operands.");
        }
    }

    public static boolean fileIsCurrentVersion(File file, String blobSha1) {
        Commit commit = Commit.readCommit(HEAD);
        String recentSha1 = commit.getBlobSha1(file);
        return recentSha1 != null && recentSha1.equals(blobSha1);
    }
}
