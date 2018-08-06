package dma.cat.sync.tools;

import dma.cat.sync.entity.SyncFile;

import dma.cat.sync.entity.SyncFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Date;

public class SyncFolders {
    Writer log;
    static public boolean DEBUG = false;

    public SyncFolders() {
    }
    
    public void sync(SyncFolder sf1, SyncFolder sf2) {
        try {
            Date d = new Date();
            log = new FileWriter("changes-"+d.getTime()+".log",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            SyncFile output = merge(sf1.root,sf2.root);
            System.out.println("Folders merged.");
            //output.showTree();
            // Copy and create folders
            execute(output,sf1,sf1.root,1,true);
            execute(output,sf2,sf2.root,2,true);
            System.out.println("Folders copied.");
            // Remove files and folders
            execute(output,sf1,sf1.root,1,false);
            execute(output,sf2,sf2.root,2,false);
            System.out.println("Folders removed.");
        } finally {
            try {
                if (log!=null) {
                    log.flush();
                    log.close();
                }
            } catch (IOException e) {
            }
        }        
    }
    
    private SyncFile merge(SyncFile sf1,SyncFile sf2) {
        SyncFile sf = new SyncFile();
        sf.name="<merge>";
        sf.isFolder = true;
        sf.exists = true;
        add(sf1,sf,1);
        add(sf2,sf,2);
        return sf;
    }
    
    private void add(SyncFile src, SyncFile dest, int listId) {
        for (SyncFile sf : src.children) {
            if (!sf.isFolder) {
                addFile(sf,dest,listId);
            } else {
                addFolder(sf,dest,listId);
            }
        }
    }

    private void addFile(SyncFile sf, SyncFile dest, int listId) {
        SyncFile e = sf.cloneMe();
        e.listId = listId;
        dest.setBest(e);
    }
    
    private void addFolder(SyncFile sf, SyncFile dest, int listId) {
        SyncFile e = sf.cloneMe();
        e.listId = listId;
        e=dest.setBest(e);
        add(sf,e,listId);
    }
    
    private void execute(SyncFile src, SyncFolder fold, SyncFile destParent, int listId, boolean copy) {
        if (src.name.equals("ARCHIVE")) {
            if (!fold.hasArchive) {
                return;
            }
        }
        // if result is archive but dest has no archive return
        //System.out.println("Process "+result.name);
        SyncFile dest=src.name.equals("<merge>")?destParent:destParent.findChildByName(src.name);
        try {
            if (src.listId!=0 && src.listId!=listId) {
                if (copy) {
                    if (src.isFolder && src.exists) {
                        if (dest==null || !dest.exists) {
                            if (dest==null) {
                                dest = new SyncFile();
                                destParent.addSyncFile(dest);
                            }
                            dest.fillFrom(src);
                            dest.fullName = new File(destParent.fullName,dest.name).getAbsolutePath();
                            log("mkdir "+dest.fullName);
                            if (!DEBUG) new File(dest.fullName).mkdir();
                            dest.updateInvalidate();
                        }
                    }
                    if (!src.isFolder && src.exists) {
                        if (dest==null || !dest.exists || !src.sha1.equals(dest.sha1)) { // skip existing equal file
                            if (dest==null) {
                                dest = new SyncFile();
                                destParent.addSyncFile(dest);
                            }
                            dest.fillFrom(src); // recover sha1, ...
                            dest.fullName = new File(destParent.fullName,dest.name).getAbsolutePath();
                            log("copy "+src.fullName+" to "+dest.fullName);
                            if (!DEBUG) FileTools.copy(new File(src.fullName),new File(dest.fullName));
                            dest.updateInvalidate();
                        }
                    }
                    if (src.isFolder && !src.exists) {
                        return;
                    }
                    if (!src.isFolder && !src.exists) {
                        return;
                    }
                } else {
                    if (src.isFolder && !src.exists) {
                        if (dest!=null && dest.exists) {
                            dest.exists = false;
                            log("rmdir "+dest.fullName);
                            if (fold.hasArchive)
                                if (!DEBUG) moveToArchive(dest,"ARCHIVE");
                            else
                                if (!DEBUG) moveToArchive(dest,".deleted");
                            dest.updateInvalidate();
                            return;
                        }
                    }
                    if (!src.isFolder && !src.exists) {
                        if (dest!=null && dest.exists && dest.fullName!=null) {
                            dest.exists = false;
                            log("rm "+dest.fullName);
                            if (!DEBUG) moveToArchive(dest,".deleted");
                            dest.updateInvalidate();
                            return;
                        }
                    }
                }
            }
            if (!src.exists) {
                return;
            }
            for (SyncFile c : src.children) {
                execute(c,fold,dest,listId,copy);
            }
        } finally {
            if (dest!=null) {
                LoadFolder lf = new LoadFolder();
                if (!DEBUG) lf.saveBackupMeta(dest);
            }
        }
    }
    
    public void moveToArchive(SyncFile folder, String folderName) {
        SyncFile parent = folder.parent;
        String path = "";
        String parentPath = "";
        while (parent!=null) {
            parentPath = parent.fullName;
            if (parent.parent!=null)
                path += "/"+parent.name;
            parent = parent.parent;
        };
        File from = new File(folder.fullName);
        File to = new File(parentPath+"/"+folderName+path);
        log("move "+from.getAbsolutePath()+" to "+to.getAbsolutePath());
        FileTools.moveFolder(from,to);
    }
    
    public void log(String s) {
        System.out.println(s);
        if (log==null) return;
        try {
            log.write(s+"\r\n");
        } catch (IOException e) {
            // TODO
        }
    }
}
