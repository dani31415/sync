package dma.cat.sync.entity;

public class MetaFile {
    public MetaFile() {
    }
    
    /**
     * Name of the file (excluding folder)
     */
    public String name;
    
    /**
     *  Date when the metadata was computed
     */
    public long trackDate;
    
    /**
     * Modification date
     */
    public long modificationDate;
    
    /**
     * Size of the file
     */
    public long size;
    
    /**
     * Whether exists or not (it does not exist if was removed and the entry already exist in the metadata)
     */
    public boolean exists;
    
    /**
     * The sha1 of the file. Not used for folders.
     */
    public String sha1;
    
    /**
     * Whether it is a folder
     */
    public boolean isFolder;
}
