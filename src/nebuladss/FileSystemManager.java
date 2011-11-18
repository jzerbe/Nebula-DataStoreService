/*
 * singleton class to manage interactions with local filesystem
 */
package nebuladss;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * @author Jason Zerbe
 */
public class FileSystemManager implements ProgramConstants {

    private static FileSystemManager fsm_singleInstance = null;
    private boolean fsm_DebugOn = false;
    private Preferences fsm_Preferences = null;
    private String fsm_kMaxAvailableMegaBytesKeyStr = "fs_MaxAvailableMegaBytes";
    private long fsm_MaxAvailableMegaBytes = 0;
    private String fsm_kStorageRootPathStrKey = "fs_StorageRootPathStr";
    private String fsm_StorageRootPathStr = null;
    private long fsm_kBytesToMegaBytesConstant = 1024 * 1024;

    protected FileSystemManager(boolean theDebugOn) {
        fsm_DebugOn = theDebugOn;
        fsm_Preferences = Preferences.userNodeForPackage(getClass());
        fsm_MaxAvailableMegaBytes = fsm_Preferences.getLong(fsm_kMaxAvailableMegaBytesKeyStr, fsm_MaxAvailableMegaBytes);
        fsm_StorageRootPathStr = fsm_Preferences.get(fsm_kStorageRootPathStrKey, fsm_StorageRootPathStr);
    }

    public static FileSystemManager getInstance() {
        if (fsm_singleInstance == null) {
            fsm_singleInstance = new FileSystemManager(false);
        }
        return fsm_singleInstance;
    }

    public static FileSystemManager getInstance(boolean theDebugOn) {
        if (fsm_singleInstance == null) {
            fsm_singleInstance = new FileSystemManager(theDebugOn);
        }
        return fsm_singleInstance;
    }

    /**
     * deleting a directory in Java requires recursively deleting the contents
     * before removing the root directory
     * @param dir File
     * @return boolean - did delete work?
     */
    protected boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public boolean deleteContentDir() {
        return deleteDir(new File(getStorageRootPath()));
    }

    public boolean deleteTmpDir() {
        return deleteDir(new File(getStorageTmpPath()));
    }

    /**
     * how many MB are left for the application to use?
     * @return float - the remaining storage available in MegaBytes (1024^2 bytes)
     */
    public float getCurrentAvailableMegaBytes() {
        File aStorageRoot = null;
        if (fsm_StorageRootPathStr != null) {
            aStorageRoot = new File(fsm_StorageRootPathStr);
        }
        if ((aStorageRoot == null) || !aStorageRoot.isDirectory()) {
            return 0;
        }

        long aCurrentByteCount = getFileSizeBytes(aStorageRoot);
        return (fsm_MaxAvailableMegaBytes - getMegaBytes(aCurrentByteCount));
    }

    /**
     * recursively get the total size in bytes of the contents of the given path
     * @param theRootStorageFile String
     * @return long - the size in bytes of the contents of the given path
     */
    protected long getFileSizeBytes(File theRootStorageFile) {
        long aReturnFolderSize = 0;
        if (theRootStorageFile.isFile()) {
            aReturnFolderSize += theRootStorageFile.length();
        } else {
            File[] aStorageFileArray = theRootStorageFile.listFiles();
            for (File aStorageFile : aStorageFileArray) {
                if (aStorageFile.isDirectory()) {
                    aReturnFolderSize += getFileSizeBytes(aStorageFile);
                } else {
                    aReturnFolderSize += aStorageFile.length();
                }
            }
        }
        return aReturnFolderSize;
    }

    /**
     * helper function for properly formatting the file storage path
     * @param theStorageRootPathStr String
     * @param theNameSpace String
     * @param theFileName String
     * @return String
     */
    public String getFormattedFilePathStr(String theStorageRootPathStr,
            String theNameSpace, String theFileName) {
        String aReturnPathStr = theStorageRootPathStr + "/" + theNameSpace + "_-NS-_" + theFileName;

        if (fsm_DebugOn) {
            System.out.println("aReturnPathStr = '" + aReturnPathStr + "'");
        }

        return aReturnPathStr;
    }

    /**
     * convert bytes to megabytes
     * @param theByteCount long
     * @return long
     */
    protected long getMegaBytes(long theByteCount) {
        if (theByteCount <= 0) {
            return 0;
        } else {
            return (theByteCount / fsm_kBytesToMegaBytesConstant);
        }
    }

    /**
     * set the max available MB that the application is allowed to use for storage
     * @param theMaxAvailableMegaBytes long
     */
    public void setMaxAvailableMegaBytes(long theMaxAvailableMegaBytes) {
        fsm_MaxAvailableMegaBytes = theMaxAvailableMegaBytes;
        fsm_Preferences.putLong(fsm_kMaxAvailableMegaBytesKeyStr, fsm_MaxAvailableMegaBytes);
    }

    /**
     * set the root storage path for the datastore node, remove trailing slash
     * @param theStorageRootPathStr String
     */
    public void setStorageRootPath(String theStorageRootPathStr) {
        if (theStorageRootPathStr != null) {
            if (theStorageRootPathStr.endsWith("/")) { //unix
                theStorageRootPathStr = theStorageRootPathStr.substring(0, theStorageRootPathStr.lastIndexOf("/"));
            }
            if (theStorageRootPathStr.endsWith("\\")) { //windows
                theStorageRootPathStr = theStorageRootPathStr.substring(0, theStorageRootPathStr.lastIndexOf("\\"));
            }
            if (theStorageRootPathStr.equals("")) {
                theStorageRootPathStr = "nebula_dss";
            }
            fsm_StorageRootPathStr = theStorageRootPathStr;
            fsm_Preferences.put(fsm_kStorageRootPathStrKey, theStorageRootPathStr);

            if (fsm_DebugOn) {
                System.out.println("set fsm_StorageRootPathStr = '" + fsm_StorageRootPathStr + "'");
            }
        }
    }

    public String getStorageRootPath() {
        return fsm_StorageRootPathStr;
    }

    public String getStorageTmpPath() {
        return kNodeTmpStoragePathStr;
    }
}
