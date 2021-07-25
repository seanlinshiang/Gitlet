package gitlet;

import java.io.Serializable;
import java.io.File;

public class Blob implements Serializable {
    /** the text contents of the Blob*/
    private String text;
    /** the SHA1 ID of the Blob*/
    private String ID;

    /** Blob constructor which creates a Blob object by the given file passed in during Main.add */
    public Blob(File file) {
        text = Utils.readContentsAsString(file);
        ID = Utils.sha1(text);
        saveBlob();
    }

    /** Saves the Blob to a file in the Blobs Directory in .gitlet folder */
    private void saveBlob() {
        File saveBlob = Utils.join(Repository.BLOBS_DIR, ID);
        Utils.writeObject(saveBlob, this);
    }
    /** looks for the Blob with the given SHA1 ID */
    public static Blob readBlob(String id) {
        File readBlob = Utils.join(Repository.BLOBS_DIR, id);
        return Utils.readObject(readBlob, Blob.class);
    }

    /** Getter methods for Blob*/
    public String getText() {
        return text;
    }
    public String getID() {
        return ID;
    }
}
