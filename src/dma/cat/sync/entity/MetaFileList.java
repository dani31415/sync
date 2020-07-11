package dma.cat.sync.entity;

import java.util.List;
import java.util.Vector;

public class MetaFileList {

    
    public MetaFileList() {
        children = new Vector<MetaFile>();
    }
    
    public void addChild(MetaFile c) {
        children.add(c);
    }

    public MetaFile find(String name) {    
        for (MetaFile c : children) {
            if (name.equalsIgnoreCase(c.name)) { // file exists?
                return c;
            }
        }
        return null;
    }
    
    public int descendants;
    public long modificationDate;
    public List<MetaFile> children;
}
