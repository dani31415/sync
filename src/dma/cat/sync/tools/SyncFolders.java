package dma.cat.sync.tools;

import dma.cat.sync.entity.FileTask;
import dma.cat.sync.entity.SyncFile;

import dma.cat.sync.entity.SyncFolder;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class SyncFolders {

    public SyncFolders() {
    }
    
    public void sync(SyncFolder sf1, SyncFolder sf2) {
        SyncFile output = merge(sf1.root,sf2.root);
        System.out.println("Folders merged.");

        // Copy and create folders
        Vector<FileTask> tasks = new Vector<FileTask>();
        execute(output,sf1,sf1.root,1,tasks);
        execute(output,sf2,sf2.root,2,tasks);
        
        sf1.createHash();
        sf2.createHash();
        obtimize(sf1,sf2,tasks);
        
        // Create folders
        for (FileTask t : tasks) {
            if (t.type==FileTask.TaskName.MKDIR) {
                t.run();
            }
        }
        // Copy or rename files
        for (FileTask t : tasks) {
            if (t.type==FileTask.TaskName.COPY || t.type==FileTask.TaskName.RENAME) {
                t.run();
            }
        }
        // Remove files
        for (FileTask t : tasks) {
            if (t.type==FileTask.TaskName.DELETE) {
                t.run();
            }
        }
        
        if (!FileTask.DEBUG) {
            sf1.saveMeta();
            sf2.saveMeta();
        }
    }
    
    private SyncFile merge(SyncFile sf1,SyncFile sf2) {
        SyncFile dest = new SyncFile();
        dest.fullName="<merge>";
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
    
    /**
     * 
     * @param src has the desired estructure
     * @param fold
     * @param destParent folder parent where src has to be generated
     * @param listId
     * @param tasks
     */
    private void execute(SyncFile src, SyncFolder fold, SyncFile destParent, int listId, List<FileTask> tasks) {
        if (src.meta.name.equals("ARCHIVE")) {
            if (!fold.hasArchive) {
                return;
            }
        }
        // if result is archive but dest has no archive return
        //System.out.println("Process "+result.name);
        SyncFile dest=src.meta.name.equals("<merge>")?destParent:destParent.findChildByName(src.meta.name);
        if (src.listId!=0 && src.listId!=listId) {
            if (src.meta.isFolder && src.meta.exists) {
                if (dest==null || !dest.meta.exists) {
                    if (dest==null) {
                        dest = new SyncFile();
                        destParent.addSyncFile(dest);
                    }
                    dest.fillFrom(src);
                    dest.fullName = new File(destParent.fullName,dest.meta.name).getAbsolutePath();
                    tasks.add(FileTask.newMkdir(dest.fullName));
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
                    tasks.add(FileTask.newCopy(src.fullName,dest.fullName,dest.meta.sha1));
                    dest.updateInvalidate();
                }
            }
            if (src.meta.isFolder && !src.meta.exists) {
                if (dest!=null && dest.meta.exists) {
                    dest.meta.exists = false;
                    String [] pp=dest.splitInParentPath();
                    tasks.add(FileTask.newDelete(pp[0],pp[1]));
                    dest.updateInvalidate();
                }
            }
            if (!src.meta.isFolder && !src.meta.exists) {
                if (dest!=null && dest.meta.exists && dest.fullName!=null) {
                    dest.meta.exists = false;
                    String [] pp=dest.splitInParentPath();
                    tasks.add(FileTask.newDelete(pp[0],pp[1],dest.meta.sha1));
                    dest.updateInvalidate();
                }
            }
        }
        if (!src.meta.exists) {
            return; // if delete, do not iterate children
        }
        for (SyncFile c : src.children) {
            execute(c,fold,dest,listId,tasks);
        }
    }
    
    private void obtimize(SyncFolder f1, SyncFolder f2, List<FileTask> tasks) {
        boolean b = obtimizeIter(f1,f2,tasks);
        while (b) {
            b = obtimizeIter(f1,f2,tasks);
        }
    }
    
    private boolean obtimizeIter(SyncFolder f1, SyncFolder f2, List<FileTask> tasks) {
        for (FileTask t0 : tasks) {
            if (t0.type==FileTask.TaskName.COPY) {
                for (FileTask t1 : tasks) {
                    if (t1.type==FileTask.TaskName.DELETE) {
                        // Same origin to grant that move is possible
                        boolean delete = t0.fileSha1.equals(t1.fileSha1) && sameOrigin(f1,f2,t1.getRemoveFile(),t0.getCopyDestFile());
                        if (delete) {
                            // "a "= "c", copy "a" to "b" and remove "c" <=> move "c" to "b"
                            tasks.remove(t0); // remove the copy
                            tasks.remove(t1); // remove the delete
                            // Add the rename
                            tasks.add(FileTask.newRename(t1.getRemoveFile(),t0.getCopyDestFile(),t0.fileSha1));
                            return true;
                        } else {
                            // Another oportunity as descendant of a deleted folder
                            String desc = descendant(f1,f2,t1.getRemoveFile(),t0.getCopyDestFile(),t0.fileSha1);
                            if (desc!=null) {
                                tasks.remove(t0); // remove the copy
                                // We keep the delete of the folder
                                // Add the rename
                                tasks.add(FileTask.newRename(desc,t0.getCopyDestFile(),t0.fileSha1));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private boolean sameOrigin(SyncFolder f1, SyncFolder f2,String s1, String s2) {
        return f1.fromFullName(s1)!=null && f1.fromFullName(s2)!=null || f2.fromFullName(s1)!=null && f2.fromFullName(s2)!=null;
    }

    private String descendant(SyncFolder f1, SyncFolder f2,String deleteFolder, String file, String sha1) { 
        // Find "file" as descendant of "deleteFolder" only if they are from the same origin
        if (f1.fromFullName(deleteFolder)!=null && f1.fromFullName(file)!=null) {
            SyncFile df = f1.fromFullName(deleteFolder);
            SyncFile sf = f1.findFromSha1Iter(df,sha1);
            if (sf!=null && !sf.markToMove) {
                sf.markToMove = true; // do not pick again
                return sf.fullName;
            }
        }
        if (f2.fromFullName(deleteFolder)!=null && f2.fromFullName(file)!=null) {
            SyncFile df = f2.fromFullName(deleteFolder);
            SyncFile sf = f2.findFromSha1Iter(df,sha1);
            if (sf!=null && !sf.markToMove) {
                sf.markToMove = true; // do not pick again
                return sf.fullName;
            }
        }
        return null;
    }

}
