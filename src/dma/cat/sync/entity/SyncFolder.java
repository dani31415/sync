package dma.cat.sync.entity;

import dma.cat.sync.tools.LoadFolder;

import java.util.Hashtable;

/**
 * Represents a top folder to be syncrhonized
 */
public class SyncFolder {
    public SyncFile root;
    public boolean hasArchive;
    Hashtable<String,SyncFile> hash;

    public SyncFolder() {
    }
    
    public void saveMeta() {
        LoadFolder lf = new LoadFolder();
        saveMeta(lf,root);
    }
    
    private void saveMeta(LoadFolder lf, SyncFile sf) {
        for (SyncFile s : sf.children) {
            lf.saveBackupMeta(s);
        }
        lf.saveBackupMeta(sf);
    }

    public void createHash() {
        hash = new Hashtable<String,SyncFile>();
        createHashIter(root);
    }
    
    private void createHashIter(SyncFile sf) {
        hash.put(sf.fullName,sf);
        for (SyncFile s : sf.children) {
            createHashIter(s);    
        }
    }
    
    public SyncFile fromFullName(String str) {
        return hash.get(str);
    }
    
    public SyncFile findFromSha1(String sha1) {
        return findFromSha1Iter(root,sha1);
    }
    
    public SyncFile findFromSha1Iter(SyncFile sf, String sha1) {
        if (sf.meta.sha1==null) return null; // folder
        if (sf.meta.sha1.equals(sha1)) return sf;
        for (SyncFile s : sf.children) {
            SyncFile r = findFromSha1Iter(s,sha1);  
            if (r!=null) return r;
        }
        return null;
    }
}
