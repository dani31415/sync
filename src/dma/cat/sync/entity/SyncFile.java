package dma.cat.sync.entity;

import dma.cat.sync.tools.FileTools;

import java.io.File;

import java.util.Collections;
import java.util.Date;
import java.util.Vector;

public class SyncFile implements Comparable {
    public String name;
    public String fullName;
    public long size;
    public long trackDate;
    public long modificationDate;
    public String sha1;
    public boolean exists;
    public boolean isFolder;

    public Vector<SyncFile> children;
    public boolean updateInvalid;
    public SyncFile meta;
    public SyncFile parent;
    public int listId;
    public int descendants;

    public SyncFile() {
        children = new Vector<SyncFile>();
        sha1 = "";
    }

    public void fillFrom(SyncFile sf) {    
        name = sf.name;
        fullName = sf.fullName;
        size = sf.size;
        trackDate = sf.trackDate;
        modificationDate = sf.modificationDate;
        sha1 = sf.sha1;
        exists = sf.exists;
        isFolder = sf.isFolder;
        listId = sf.listId;
    }
    
    public SyncFile cloneMe() {
        SyncFile output= new SyncFile();
        output.fillFrom(this);
        return output;
    }

    public SyncFile findChildByName(String name) {
        for (SyncFile c : children) {
            if (c.name.equals(name)) {
                return c;
            }
        }
        return null;
    }

    public void addSyncFile(SyncFile f) {
        for (SyncFile c : children) {
            if (c.name.equals(f.name)) { // file exists?
                // Replace, but keep meta
                f.meta = c.meta;
                c.parent = null;
                children.remove(c);
                f.parent = this;
                children.add(f);
                return;
            }
        }
        f.parent = this;
        children.add(f);
    }
    
    public SyncFile setBest(SyncFile f) {
        SyncFile e = f!=null?this.findChildByName(f.name):null;
        if (e!=null) {
            if (e.trackDate<f.trackDate) {
                e.fillFrom(f);
            }
            return e;
        } else {
            addSyncFile(f);
            return f;
        }
    }

    public void addMeta(SyncFile f) {
        for (SyncFile c : children) {
            if (c.name.equalsIgnoreCase(f.name)) { // file exists?
                c.meta = f;
                return;
            }
        }
        SyncFile n = new SyncFile();
        n.exists = false;
        n.name = f.name;
        n.meta = f;
        n.parent = this;
        children.add(n);
    }
    
    public void sortChildren() {
        Collections.sort(children);
    }
    
    static public boolean ignoreName(String name) {
        return name.equals(".backup") || name.equals(".deleted") || name.endsWith(".lrdata");
    }
    
    public boolean needsMetaUpdate() {
        if (meta==null) {
            meta = new SyncFile();
            return true;
        }
        if (exists!=meta.exists) return true; // file removed
        if (exists && isFolder!=meta.isFolder) return true; // changed folder status
        if (exists && size!=meta.size) return true; // file changed
        if (exists && modificationDate!=meta.modificationDate) return true; // file changed
        return false;
    }
    
    public String toXml(int ident) {
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<ident;i++) sb.append("\t");
        sb.append("<");
        if (isFolder) sb.append("folder");
        else sb.append("file");
        sb.append(" name=\"");
        String s=name.replaceAll("&","&amp;");
        sb.append(s);
        sb.append("\" track=\"");
        sb.append(trackDate);
        sb.append("\" descendants=\"");
        sb.append(descendants);
        sb.append("\"");
        if (!isFolder) {
            sb.append(" modified=\"");
            sb.append(modificationDate);
            sb.append("\" size=\"");
            sb.append(size);
            sb.append("\"");
            sb.append(" sha1=\"");
            sb.append(sha1);
            sb.append("\"");
        }
        if (!exists) {
            sb.append(" removed=\"true\"");
        }
        sb.append(" />");
        return sb.toString();
    }
    
    public int compareTo(SyncFile sf1, SyncFile sf2) {
        return sf1.name.compareTo(sf2.name);
    }

    public int compareTo(Object o) {
        SyncFile sf = (SyncFile)o;
        if (exists!=sf.exists) {
            if (exists) return -1;
            return 1;
        }
        if (isFolder!=sf.isFolder) {
            if (!isFolder) return -1;
            return 1;
        }
        return name.compareTo(sf.name);
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
        System.out.print(name);
        if (isFolder) System.out.print(" (dir)");
        if (!exists) System.out.print(" (removed)");
        System.out.println();
        for (SyncFile c : children) {
            c.showTree(depth+1);
        }
    }
}
