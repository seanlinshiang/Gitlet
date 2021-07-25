package gitlet;

import java.io.File;
import java.util.HashMap;

public class StagingArea {
    //
    private static HashMap<String, String> staged = new HashMap<>();
    private static HashMap<String, String> stagedForRemoval = new HashMap<>();

    /** adds file to Staging Area */
    public static void addFileToStage(File file) {
        Blob blob = new Blob(file);
        String blobSha1 = blob.getID();
        /** If the file is current version and it's staged, remove it from stage */
        if (Repository.fileIsCurrentVersion(file, blobSha1)) {
            if (staged.containsKey(file.getName())) {
                removeFileFromStage(file, true);
            }
        } else {
            staged.put(file.getName(), blobSha1);
        }
    }

    public static void addFileToRemove(File file) {
        stagedForRemoval.put(file.getName(), "");
    }
    /** removes file from Staging Area */
    public static void removeFileFromStage(File file, boolean removal) {
        HashMap<String, String> stageMap;
        if (removal) {
            stageMap = stagedForRemoval;
        } else {
            stageMap = staged;
        }
        stageMap.remove(file.getName());
    }

    /** clears the Staging Area */
    public static void clearStage(boolean removal) {
        HashMap<String, String> stageMap;
        if (removal) {
            stageMap = stagedForRemoval;
        } else {
            stageMap = staged;
        }
        stageMap.clear();
    }

    /** Saves the staged map to the file stagingArea */
    public static void saveStage() {
        File saveFile = Utils.join(Repository.STAGE_DIR, "staged");
        Utils.writeObject(saveFile, staged);
    }

    /** Reads the staged map from the file stagingArea */
    @SuppressWarnings("unchecked")
    public static void readStage() {
        File saveFile = Utils.join(Repository.STAGE_DIR, "staged");
        if (!saveFile.exists()) {
            return;
        }
        staged = Utils.readObject(saveFile, HashMap.class);
    }

    /** Saves the stagedForRemoval map to the file stagingArea */
    public static void saveStageForRemoval() {
        File saveFile = Utils.join(Repository.STAGE_DIR, "stagedForRemoval");
        Utils.writeObject(saveFile, stagedForRemoval);
    }

    /** Reads the stagedForRemoval from the file stagingArea */
    @SuppressWarnings("unchecked")
    public static void readStageForRemoval() {
        File saveFile = Utils.join(Repository.STAGE_DIR, "stagedForRemoval");
        if (!saveFile.exists()) {
            return;
        }
        stagedForRemoval = Utils.readObject(saveFile, HashMap.class);
    }

    public static boolean stageIsEmpty() {
        return staged.isEmpty();
    }

    public static boolean stageForRemovalIsEmpty() {
        return stagedForRemoval.isEmpty();
    }

    public static HashMap<String, String> getStaged() {
        return staged;
    }

    public static HashMap<String, String> getStagedForRemoval() {
        return stagedForRemoval;
    }

    public static boolean contains(String filename, boolean removal) {
        HashMap<String, String> stageMap;
        if (removal) {
            stageMap = stagedForRemoval;
        } else {
            stageMap = staged;
        }
        return stageMap.containsKey(filename);
    }

    public static boolean contains(File file, boolean removal) {
        return contains(file.getName(), removal);
    }
}
