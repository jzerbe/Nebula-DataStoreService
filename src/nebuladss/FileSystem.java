/*
 * singleton class to manage interactions with local filesystem
 */
package nebuladss;

import java.io.File;

/**
 * @author Jason Zerbe
 */
public class FileSystem {

    protected FileSystem fs_Instance = null;
    protected long fs_MaxFileSystemUseBytes = 0;

    protected FileSystem() {
    }

    public FileSystem getInstance() {
        if (fs_Instance == null) {
            fs_Instance = new FileSystem();
        }
        return fs_Instance;
    }

    public File getFile(String theFileName, String theNameSpace) {
        return null;
    }

    public void putFile(File theFileToStore) {
        //
    }

    public long getRemainingFileSystemBytes() {
        return 0;
    }

    public void setMaxFileSystemUsageBytes(long theMaxFileSystemUsageBytes) {
        //
    }
}
