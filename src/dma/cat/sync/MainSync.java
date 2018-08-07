package dma.cat.sync;

import dma.cat.sync.entity.SyncFile;
import dma.cat.sync.entity.SyncFolder;
import dma.cat.sync.tools.LoadFolder;

import dma.cat.sync.tools.SyncFolders;

import java.io.File;

public class MainSync {
    public MainSync() {
    }

    public static void main2(String [] args) {
        LoadFolder lf = new LoadFolder();
        File f = new File("Z:\\ARCHIVE");
        lf.load(f);
    }
    public static void main(String[] args) {
        //SyncFolders.DEBUG = true;
        MainSync mainSync = new MainSync();
        mainSync.run(args);
    }
    
    public void run(String[] args) {
        LoadFolder lf = new LoadFolder();
        File f1, f2;
        
        if (args.length==2) {
            f1 = new File(args[0]);
            //File f2 = new File("h:\\personal\\fotos");
            f2 = new File(args[1]);
        } else {
            f1 = new File("Z:");
            //File f2 = new File("h:\\personal\\fotos");
            f2 = new File("E:\\personal");
        }
        System.out.println("Synchronization folders:");
        System.out.println(f1.getAbsolutePath());
        System.out.println(f2.getAbsolutePath());
        SyncFolder sf1 = lf.load(f1);
        System.out.println(f1.getAbsoluteFile()+ " loaded.");
        SyncFolder sf2 = lf.load(f2);
        System.out.println(f2.getAbsoluteFile()+ " loaded.");
        SyncFolders sfs = new SyncFolders();
        sfs.sync(sf1,sf2);
    }
}
