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
        SyncFile dest = new SyncFile();
        dest.meta.name="<merge>";
        dest.meta.isFolder = true;
        dest.meta.exists = true;
        addChildren(sf1,dest,1);
        addChildren(sf2,dest,2);
        return dest;
    }
    
    private void addChildren(SyncFile src, SyncFile dest, int listId) {
        for (SyncFile sf : src.children) {
            if (!sf.meta.isFolder) {
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
        SyncFile f=dest.setBest(e); // f is the SyncFile inside dest
        addChildren(sf,f,listId); // add children of "sf" into "f"
    }
    
    private void execute(SyncFile src, SyncFolder fold, SyncFile destParent, int listId, boolean copy) {
        if (src.meta.name.equals("ARCHIVE")) {
            if (!fold.hasArchive) {
                return;
            }
        }
        // if result is archive but dest has no archive return
        //System.out.println("Process "+result.name);
        SyncFile dest=src.meta.name.equals("<merge>")?destParent:destParent.findChildByName(src.meta.name);
        try {
            if (src.listId!=0 && src.listId!=listId) {
                if (copy) {
                    if (src.meta.isFolder && src.meta.exists) {
                        if (dest==null || !dest.meta.exists) {
                            if (dest==null) {
                                dest = new SyncFile();
                                destParent.addSyncFile(dest);
                            }
                            dest.fillFrom(src);
                            dest.fullName = new File(destParent.fullName,dest.meta.name).getAbsolutePath();
                            log("mkdir "+dest.fullName);
                            if (!DEBUG) new File(dest.fullName).mkdir();
                            dest.updateInvalidate();
                        }
                    }
                    if (!src.meta.isFolder && src.meta.exists) {
                        if (dest==null || !dest.meta.exists || !src.meta.sha1.equals(dest.meta.sha1)) { // skip existing equal file
                            if (dest==null) {
                                dest = new SyncFile();
                                destParent.addSyncFile(dest);
                            }
                            dest.fillFrom(src); // recover sha1, ...
                            dest.fullName = new File(destParent.fullName,dest.meta.name).getAbsolutePath();
                            log("copy "+src.fullName+" to "+dest.fullName);
                            if (!DEBUG) FileTools.copy(new File(src.fullName),new File(dest.fullName));
                            dest.updateInvalidate();
                        }
                    }
                    if (src.meta.isFolder && !src.meta.exists) {
                        return;
                    }
                    if (!src.meta.isFolder && !src.meta.exists) {
                        return;
                    }
                } else {
                    if (src.meta.isFolder && !src.meta.exists) {
                        if (dest!=null && dest.meta.exists) {
                            dest.meta.exists = false;
                            log("rmdir "+dest.fullName);
                            if (fold.hasArchive) {
                                if (!DEBUG) moveToArchive(dest,"ARCHIVE");
                            } else {
                                if (!DEBUG) moveToArchive(dest,".deleted");
                            }
                            dest.updateInvalidate();
                            return;
                        }
                    }
                    if (!src.meta.isFolder && !src.meta.exists) {
                        if (dest!=null && dest.meta.exists && dest.fullName!=null) {
                            dest.meta.exists = false;
                            log("rm "+dest.fullName);
                            if (!DEBUG) moveToArchive(dest,".deleted");
                            dest.updateInvalidate();
                            return;
                        }
                    }
                }
            }
            if (!src.meta.exists) {
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
                path += "/"+parent.meta.name;
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
