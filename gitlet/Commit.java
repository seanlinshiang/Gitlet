package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet commit object.
 *
 *
 *  @author Shiang Lin, Ian Huang
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    /** The sha1 ID of this Commit. */
    private String ID;
    /** The message of this Commit. */
    private String message;
    /** The time when this Commit happened*/
    private Date timestamp;
    /** The parent of this Commit */
    private String parentCommit;
    /** Stores the metadata of the commit, HashMap< File name, sha1 of the blob> */
    private HashMap<String, String> metadata;

    public Commit(String message, String parentCommit) {
        this.message = message;
        this.timestamp = new Date();
        this.parentCommit = parentCommit;
        metadata = new HashMap<>();
        ID = Utils.createID(this);
    }

    public Commit(String message) {
        this.message = message;
        this.timestamp = new Date(0);
        this.parentCommit = null;
        metadata = new HashMap<>();
        ID = Utils.createID(this);
    }

    /** Save this commit object to a file with name ID in .gitlet/commits */
    public void saveCommit() {
        File saveFile = Utils.join(Repository.COMMITS_DIR, ID);
        Utils.writeObject(saveFile, this);
    }
    /** Reads in a Commit from a file with name ID */
    public static Commit readCommit(String id) {
        File saveFile = Utils.join(Repository.COMMITS_DIR, id);
        if (!saveFile.exists()) {
            return null;
        }
        return Utils.readObject(saveFile, Commit.class);
    }

    public static Commit readCommitAb(String id) {
        String fullId = id;
        List<String> fileList = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        for (String filename: fileList) {
            if (filename.startsWith(id)) {
                fullId = filename;
            }
        }
        return readCommit(fullId);
    }
    public String getID() {
        return ID;
    }

    public Commit getParentCommit() {
        if (parentCommit == null) {
            return null;
        }
        return readCommit(parentCommit);
    }

    public String getMessage() {
        return message;
    }

    /** Gets the blob of the given file in this commit */
    public String getBlobSha1(File file) {
        return metadata.get(file.getName());
    }

    /** Gets the most recent blob's sha1 of the given file. */
    public static String getRecentFileBlobSha1(String headSha1, File file) {
        Commit temp = readCommit(headSha1);
        while (!temp.metadata.containsKey(file)) {
            if (temp.parentCommit == null) {
                return null;
            }
            temp = readCommit(temp.parentCommit);
        }
        return temp.getBlobSha1(file);
    }

    @Override
    public String toString() {
        SimpleDateFormat sdFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdFormat.setTimeZone(TimeZone.getTimeZone("PST"));
        return "===\n" + "commit " + ID + "\nDate: "
                + sdFormat.format(timestamp) + "\n" + message + "\n";
    }

    public void metadataPutAll(HashMap<String, String> map) {
        metadata.putAll(map);
    }

    public void metadataRemoveAll(HashMap<String, String> map) {
        metadata.keySet().removeAll(map.keySet());
    }

    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    public boolean metadataContains(String filename) {
        return metadata.containsKey(filename);
    }
}
