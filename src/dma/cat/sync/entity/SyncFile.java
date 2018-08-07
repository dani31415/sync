package dma.cat.sync.entity;

import dma.cat.sync.tools.FileTools;

import java.io.File;

import java.util.Collections;
import java.util.Date;
import java.util.Vector;

public class SyncFile implements Comparable {
    
    public String fullName;
    
    public MetaFile meta;

    public Vector<SyncFile> children;
    public boolean updateInvalid;
    
    public SyncFile parent;
    public int listId;
    public int descendants;

    public SyncFile() {
        children = new Vector<SyncFile>();
        meta = new MetaFile();
        meta.sha1 = "";
    }

    public void fillFrom(SyncFile sf) {    
        fullName = sf.fullName;
        fillFrom(sf.meta);
        listId = sf.listId;
    }
    
    public void fillFrom(MetaFile c) {    
        meta.name = c.name;
        meta.size = c.size;
        meta.trackDate = c.trackDate;
        meta.modificationDate = c.modificationDate;
        meta.sha1 = c.sha1;
        meta.exists = c.exists;
        meta.isFolder = c.isFolder;
    }

    public SyncFile cloneMe() {
        SyncFile output= new SyncFile();
        output.fillFrom(this);
        return output;
    }

    public SyncFile findChildByName(String name) {
        for (SyncFile c : children) {
            if (c.meta.name.equals(name)) {
                return c;
            }
        }
        return null;
    }

    public void addSyncFile(SyncFile f) {
        if (f.parent!=null) {
            throw new Error("Remove first from the previous parent.");
        }
        f.parent = this;
        children.add(f);
    }
    
    public SyncFile setBest(SyncFile f) {
        SyncFile e = findChildByName(f.meta.name);
        if (e!=null) {
            // Choose the best according to date
            // System.out.println(e.fullName+" ("+e.meta.modificationDate+") vs "+f.fullName+" ("+e.meta.modificationDate+")");
            if (e.meta.modificationDate<f.meta.modificationDate) {
                e.fillFrom(f);
            }
            return e;
        } else {
            addSyncFile(f);
            return f;
        }
    }
    
    public void sortChildren() {
        Collections.sort(children);
    }
    
    static public boolean ignoreName(String name) {
        return name.equals(".backup") || name.equals(".deleted") || name.endsWith(".lrdata") || name.equals(".DS_Store") || name.equals(".git") || name.equals(".gitignore");
    }
    
    /**
     * Detects whether the file needs the metadata to be updated
     * @param c
     * @return
     */
    public boolean needsMetaUpdate(MetaFile c) {
        if (c==null) {
            return true;
        }
        if (meta.exists!=c.exists) return true; // file removed
        if (meta.exists && meta.isFolder!=c.isFolder) return true; // changed folder status
        if (meta.exists && meta.size!=c.size) return true; // file changed
        if (meta.exists && meta.modificationDate!=c.modificationDate) return true; // file changed
        return false;
    }
    
    public String toXml(int ident) {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<ident;i++) sb.append("\t");
        sb.append("<");
        if (meta.isFolder) sb.append("folder");
        else sb.append("file");
        sb.append(" name=\"");
        String s=meta.name.replaceAll("&","&amp;");
        sb.append(s);
        sb.append("\" track=\"");
        sb.append(meta.trackDate);
        sb.append("\" descendants=\"");
        sb.append(descendants);
        sb.append("\"");
        if (!meta.isFolder) {
            sb.append(" modified=\"");
            sb.append(meta.modificationDate);
            sb.append("\" size=\"");
            sb.append(meta.size);
            sb.append("\"");
            sb.append(" sha1=\"");
            sb.append(meta.sha1);
            sb.append("\"");
        }
        if (!meta.exists) {
            sb.append(" removed=\"true\"");
        }
        sb.append(" />");
        return sb.toString();
    }
    
    public int compareTo(SyncFile sf1, SyncFile sf2) {
        return sf1.meta.name.compareTo(sf2.meta.name);
    }

    public int compareTo(Object o) {
        SyncFile sf = (SyncFile)o;
        if (meta.exists!=sf.meta.exists) {
            if (meta.exists) return -1;
            return 1;
        }
        if (meta.isFolder!=sf.meta.isFolder) {
            if (!meta.isFolder) return -1;
            return 1;
        }
        return meta.name.compareTo(sf.meta.name);
    }
    
    public void updateInvalidate() {
        if (parent!=null) {
            System.out.println("Invalidate: "+fullName);
            parent.updateInvalid = true;
        }
    }
    
    public void showTree() {
        showTree(0);
    }
    
    public void showTree(int depth) {
        System.out.print(listId+" ");
        for (int i=0;i<depth;i++) System.out.print(".");
        System.out.print(meta.name);
        if (meta.isFolder) System.out.print(" (dir)");
        if (!meta.exists) System.out.print(" (removed)");
        System.out.println();
        for (SyncFile c : children) {
            c.showTree(depth+1);
        }
    }
    
}
