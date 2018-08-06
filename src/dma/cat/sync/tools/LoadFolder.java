package dma.cat.sync.tools;

import dma.cat.sync.entity.SyncFile;

import dma.cat.sync.entity.SyncFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LoadFolder {
    class CurrentLoad {
        int previousFound;
        int found;
        
        int lastShown;
        
        public void showStatus() {
            if (previousFound==0) return;
            int currentShow = 100*found/previousFound;
            if (lastShown!=currentShow) {
                System.out.print(currentShow+"% ");
                System.out.flush();
                if (currentShow%10==0) System.out.println();
                lastShown=currentShow;
            }
        }
    }

    public SyncFolder load(File file) {
        System.out.println("Loading "+file);
        SyncFile sf = loadSyncFolder(file,new CurrentLoad());
        SyncFolder sff = new SyncFolder();
        sff.root = sf;
        sff.hasArchive = new File(file,"ARCHIVE").exists();
        return sff;
    }

    public SyncFile loadSyncFolder(File file, CurrentLoad currentLoad) {
        existsAsFolder(file);
        SyncFile sf = new SyncFile();
        sf.fullName = file.getAbsolutePath();
        sf.name = file.getName();
        sf.isFolder = true;
        sf.exists = true;
        
        if (!loadBackupMeta(sf)) {
            // If not able to load metafile, create it
            sf.updateInvalid=true;
        }
        
        if (currentLoad.previousFound==0) {
            currentLoad.previousFound=sf.descendants;
        }
        
        File [] fs = file.listFiles();
        if (fs!=null) {
            for (File c : fs) {
                if (!SyncFile.ignoreName(c.getName())) {
                    currentLoad.found++;
                    currentLoad.showStatus();
                    if (c.isDirectory()) {
                        sf.addSyncFile(loadSyncFolder(c,currentLoad));
                    } else {
                        sf.addSyncFile(loadSyncFile(c));
                    }
                }
            }
        }
        int n = sf.children.size();
        for (SyncFile c : sf.children) {
            n += c.descendants;
            computeBackupMeta(c);
        }
        if (n!=sf.descendants) {
            // Changed the number of descendants
            sf.descendants = n;
            sf.updateInvalid = true;
        }
        saveBackupMeta(sf);
        return sf;
    }
    
    public SyncFile loadSyncFile(File file) {
        existsAsFile(file);
        SyncFile sf = new SyncFile();
        sf.fullName = file.getAbsolutePath();
        sf.name = file.getName();
        sf.isFolder = false;
        sf.exists = true;
        sf.modificationDate = file.lastModified();
        sf.size = file.length();
        return sf;
    }

    public void existsAsFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new Error("File "+file.getAbsolutePath()+" does not exist ot it is a folder.");
        }
    }

    public void existsAsFolder(File file) {
        if (!file.exists() || !file.isDirectory()) {
            throw new Error("Folder "+file.getAbsolutePath()+" does not exist or it is not a folder.");
        }
    }

    private boolean loadBackupMeta(SyncFile sf) {
        File f = new File(sf.fullName, ".backup");
        if (!f.exists()) {
            System.out.println("New folder: "+sf.fullName);           
            return false;
        }
        existsAsFile(f);
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            NodeList nl = doc.getElementsByTagName("file");
            for (int i=0;i<nl.getLength();i++) {
                Element elem = (Element)nl.item(i);
                SyncFile c = new SyncFile();
                c.name = elem.getAttribute("name");
                c.trackDate = Long.parseLong(elem.getAttribute("track"));
                c.modificationDate = Long.parseLong(elem.getAttribute("modified"));
                c.size = Long.parseLong(elem.getAttribute("size"));
                String r = elem.getAttribute("removed");
                if (r!=null && r.trim().equals("true")) {
                    c.exists = false;
                } else {
                    c.exists = true;
                }
                c.sha1 = elem.getAttribute("sha1").trim();
                c.isFolder = false;
                sf.addMeta(c);
            }
            nl = doc.getElementsByTagName("folder");
            Element current = (Element)nl.item(0); // Own folder
            String r0 = current.getAttribute("descendants");
            if (r0==null || r0.length()==0) {
                sf.descendants = 0;
            } else {
                sf.descendants = Integer.parseInt(r0);
            }
            for (int i=1;i<nl.getLength();i++) { // ignore first because it is the top one!
                Element elem = (Element)nl.item(i);
                SyncFile c = new SyncFile();
                c.name = elem.getAttribute("name");
                String r = elem.getAttribute("removed");
                c.trackDate = Long.parseLong(elem.getAttribute("track"));
                if (r!=null && r.trim().equals("true")) {
                    c.exists = false;
                } else {
                    c.exists = true;
                }
                c.isFolder = true;
                sf.addMeta(c);
            }
            return true;
        } catch (Exception ex) {
            // Dot not launch exception
            System.err.println("Error when reading: "+f);
            ex.printStackTrace();
        }
        return false;
    }

    public void computeBackupMeta(SyncFile sf) {
        // Compute sha1
        if (sf.needsMetaUpdate()) {
            sf.needsMetaUpdate();
            if (sf.exists) {
                if (!sf.isFolder) {
                    FileTools ft = new FileTools();
                    sf.sha1 = ft.sha1(new File(sf.fullName));
                }
                sf.trackDate = new Date().getTime();
            } else {
                sf.meta.trackDate = new Date().getTime();
            }
            sf.updateInvalidate();
        } else {
            if (sf.exists) {
                if (!sf.isFolder) {
                    sf.sha1 = sf.meta.sha1;
                }
                sf.trackDate = sf.meta.trackDate;
            }
        }

        if (sf.exists) {
            // Copy actual file to meta
            sf.meta.fillFrom(sf);
        } else {
            sf.meta.exists = false;
            // Copy meta to actual file
            sf.fillFrom(sf.meta);
        }
        sf.meta = null; /// not needed anymore
    }
    
    public void saveBackupMeta(SyncFile sf) {
        if (sf.updateInvalid) { // save only if modified
            try {
                File f = new File(sf.fullName);
                existsAsFolder(f);
                File output = new File(f,".backup");
                FileOutputStream fos = new FileOutputStream(output);
                System.out.println("Updating file: "+output.getAbsolutePath());
                OutputStreamWriter osw = new OutputStreamWriter(fos,"utf-8");
                osw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
                osw.write("<backup-meta>\r\n");
                osw.write(sf.toXml(1));
                osw.write("\r\n");
                osw.write("\t<files>\r\n");
                sf.sortChildren();
                for (SyncFile c : sf.children) {
                    osw.write(c.toXml(2));
                    osw.write("\r\n");
                }
                osw.write("\t</files>\r\n");
                osw.write("</backup-meta>\r\n");
                osw.flush();
                osw.close();
            } catch (Exception ex) {
                throw new Error(ex);
            }
            sf.updateInvalid = false;
        }
    }

}
