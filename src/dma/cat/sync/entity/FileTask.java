package dma.cat.sync.entity;

import dma.cat.sync.tools.FileTools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.Writer;

import java.util.Date;

public class FileTask {
    static public boolean DEBUG = false;

    public enum TaskName {
        COPY,
        MKDIR,
        DELETE,
        RENAME
    }
    
    private String file1;
    private String file2;
    public String fileSha1;
    public TaskName type;
    
    private FileTask() {
    }
    
    public static FileTask newCopy(String srcFile, String destFile, String fileSha1) {
        FileTask ft = new FileTask();
        ft.type = TaskName.COPY;
        ft.file1 = srcFile;
        ft.file2 = destFile;
        ft.fileSha1 = fileSha1;
        return ft;
    }
    
    public static FileTask newDelete(String parent, String path, String fileSha1) {
        FileTask ft = new FileTask();
        ft.type = TaskName.DELETE;
        ft.file1 = parent;
        ft.file2 = path;
        ft.fileSha1 = fileSha1;
        return ft;        
    }
    
    public static FileTask newDelete(String parent, String path) {
        FileTask ft = new FileTask();
        ft.type = TaskName.DELETE;
        ft.file1 = parent;
        ft.file2 = path;
        return ft;        
    }

    public static FileTask newMkdir(String folder) {
        FileTask ft = new FileTask();
        ft.type = TaskName.MKDIR;
        ft.file1 = folder;
        return ft;        
    }
    
    public static FileTask newRename(String srcFile, String destFile, String fileSha1) {
        FileTask ft = new FileTask();
        ft.type = TaskName.RENAME;
        ft.file1 = srcFile;
        ft.file2 = destFile;
        ft.fileSha1 = fileSha1;
        return ft;        
    }

    public String getRemoveFile() {
        if (type!=TaskName.DELETE) throw new Error("This is not a delete");
        return new File(file1,file2).getPath();
    }

    public String getCopyDestFile() {
        if (type!=TaskName.COPY) throw new Error("This is not a copy");
        return file2;
    }

    public void run() {
        switch (type) {
            case COPY:
                log("copy "+file1+" to "+file2);
                if (!DEBUG) FileTools.copy(new File(file1),new File(file2));
                break;
            case MKDIR:
                log("mkdir "+file1);
                if (!DEBUG) new File(file1).mkdir();
                break;
            case DELETE:
                File parent = new File(file1);
                File file = new File(file1,file2);
                log("rm "+file.getAbsolutePath());
                String deleteFolder;
                if (file.isDirectory() && new File(parent,"ARCHIVE").exists()) {
                    deleteFolder = "ARCHIVE";
                } else {
                    deleteFolder = ".deleted";
                }
                if (!DEBUG) moveToArchive(file1,file2,deleteFolder);
                break;
            case RENAME:
                log("rename "+file1+" to "+file2);
                if (!DEBUG) FileTools.rename(new File(file1),new File(file2));
                break;
        }
    }

    static public void moveToArchive(String parent, String path, String deleteFolder) {
        File from = new File(parent,path);
        if (from.isDirectory()) {
            File fs[] = from.listFiles();
            if (fs!=null) {
                int c = 0;
                File backup = null;
                for (int i=0;i<fs.length;i++) {
                    if (!fs[i].getName().equals(".backup")) {
                        c++;
                    } else {
                        backup = fs[i];
                    }
                }
                if (c==0) {
                    // Empty folder
                    if (backup!=null) backup.delete();
                    from.delete();
                    return;
                }
            }
        }
        File to = new File(parent,deleteFolder);
        String pathParent = new File(path).getParent();
        if (pathParent!=null) to = new File(to,pathParent);
        log("move "+from.getAbsolutePath()+" to "+to.getAbsolutePath());
        FileTools.moveFolder(from,to);
    }

    static Writer log;
    
    static {
        try {
            Date d = new Date();
            log = new FileWriter("changes-"+d.getTime()+".log",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void log(String s) {
        System.out.println(s);
        if (log==null) return;
        try {
            log.write(s+"\r\n");
            log.flush();
        } catch (IOException e) {
            // TODO
        }
    }

}
