package dma.cat.sync.tools;

import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;

import java.security.MessageDigest;

public class FileTools {
    static final int BUUFER_LENGTH = 16*1024*1024;

    public FileTools() {
    }
    
    public String sha1(File f) {
        System.out.println("Computing sha1 for "+f.getAbsolutePath());
        FileInputStream fis;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            fis = new FileInputStream(f);
            while (fis.available()>0) {
                int n = Math.min(fis.available(),BUUFER_LENGTH);
                byte [] b = new byte[n];
                fis.read(b);
                md.update(b);
            }
            fis.close();
            byte [] b = md.digest();
            String result = "";
            for (int i=0; i < b.length; i++) {
                result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
            return result;
        } catch(Throwable ex) {
            ex.printStackTrace();
            return "unkown";
            //throw new Error(ex);
        }
    }

    static public void copy(File src, File dest) {
        //System.out.println("Copy from "+src.getAbsolutePath()+" to "+dest.getAbsolutePath());
        FileInputStream fis;
        try {
            fis = new FileInputStream(src);
            File dest2 = new File(dest.getAbsolutePath()+".tmp"); // Copy to tmp file
            dest2.delete();
            FileOutputStream fos = new FileOutputStream(dest2);
            while (fis.available()>0) {
                int n = Math.min(fis.available(),BUUFER_LENGTH);
                byte [] b = new byte[n];
                fis.read(b);
                fos.write(b);
            }
            fis.close();
            fos.flush();
            fos.close();
            dest.delete();
            dest2.renameTo(dest);
            dest.setLastModified(src.lastModified());
        } catch(Exception ex) {
            throw new Error(ex);
        }
        
    }
    
    static public void moveFolder(File from, File to) {
        to.mkdirs();
        File toName = new File(to,from.getName());
        if (!from.renameTo(toName)) {
            throw new Error("Failed renaming from "+from.getAbsolutePath()+" to "+toName.getAbsolutePath());
        }
    }
}
