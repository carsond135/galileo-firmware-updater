package com.intel.galileo.flash.tool;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractZmodemService extends CommunicationService {

    @Override
    public String getServiceName() {
        return "Serial Connection Service";
    }

    @Override
    public String getConnectionLabel() {
        return "Port:";
    }

    @Override
    public final boolean isConnectionOpen() {
        return (zmodemDir != null) && (zmodem != null) && isSerialTransportOpen();
    }

    @Override
    public void setFileLocation(File dir) {
        zmodemDir = dir;
        try {
            zmodem = installResources();
        } catch (IOException ex) {
            Logger.getLogger(AbstractZmodemService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public final boolean openConnection(String portName) {
        try {
            if (zmodemDir == null) {
                File f = File.createTempFile("bogus", "");
                File tmpDir = f.getParentFile();
                f.delete();
                zmodemDir = new File(tmpDir, "zmodem");
                zmodemDir.mkdir();
                zmodemDir.deleteOnExit();
                zmodem = installResources();
            }
            return openSerialTransport(portName);
        } catch (IOException ex) {
            zmodemDir = null;
            Logger.getLogger(AbstractZmodemService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    protected abstract File installResources() throws IOException;

    @Override
    public final void closeConnection() {
        closeSerialTransport();
    }

    @Override
    public final String sendCommand(String remoteCommand) throws Exception {
        List<String> cmd = new LinkedList<>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--verbose");
        cmd.add("-c");
        cmd.add(remoteCommand);
        return zmodemOperation(cmd, null);
    }

    @Override
    public final String sendCommandWithTimeout(String remoteCommand, int timeout) throws Exception {
        List<String> cmd = new LinkedList<>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--verbose");
        cmd.add("-c");
        cmd.add(remoteCommand);
        return zmodemOperationWithTimeout(cmd, null, timeout);
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        List<String> cmd = new LinkedList<>();
        cmd.add(zmodem.getPath().replace("\\", "/"));
        cmd.add("--escape");
        cmd.add("--binary");
        cmd.add("--overwrite");
        cmd.add("--verbose");
        cmd.add(f.getName());
        getLogger().info(cmd.toString());
        zmodemOperation(cmd, p);
    }

    @Override
    public boolean isProgressSupported() {
        return true;
    }

    protected abstract Runnable createSerialOutputPipe(InputStream stdout, FileProgress progress);

    protected abstract Runnable createSerialInputPipe(OutputStream stdin);

    protected abstract boolean openSerialTransport(String portName);

    protected abstract void closeSerialTransport();

    protected abstract boolean isSerialTransportOpen();

    protected String zmodemOperation(List<String> cmd, FileProgress progress) throws Exception {
        ProcessBuilder pb = createProcessBuilder(cmd);
        quit = false;
        final Process p = pb.start();
        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(createSerialOutputPipe(p.getInputStream(), progress));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(createSerialInputPipe(p.getOutputStream()));
        serialIn.setName("serial-input");
        serialIn.start();
        Thread t = new Thread(outputReader);
        t.setName("output-sucker");
        t.start();
        int exit = p.waitFor();
        quit = true;
        if (exit != 0) {
            String msg = String.format("Remote command exited with %d\n", exit);
            getLogger().severe(msg);
            getLogger().log(Level.INFO, "Output was: {0}", outputReader.getOutput());
            throw new Exception(msg);
        }
        return outputReader.getOutput();
    }

    protected String zmodemOperationWithTimeout(List<String> cmd, FileProgress progress, int timeout) throws Exception {
        System.out.println("zmodemOperationWithTimeout");
        ProcessBuilder pb = createProcessBuilder(cmd);
        quit = false;
        Process p = pb.start();

        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(createSerialOutputPipe(p.getInputStream(), progress));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(createSerialInputPipe(p.getOutputStream()));
        serialIn.setName("serial-input");
        serialIn.start();
        Thread t = new Thread(outputReader);
        t.setName("output-sucker");
        t.start();

        try {
            ProcessManager pm = new ProcessManager(p);
            Thread thread = new Thread(pm);
            thread.start();
            pm.waitForOrKill(timeout);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Output was: " + outputReader.getOutput());

        return outputReader.getOutput();
    }

    protected ProcessBuilder createProcessBuilder(List<String> cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(zmodemDir);
        return pb;
    }

    protected Logger getLogger() {
        return Logger.getLogger(AbstractZmodemService.class.getName());
    }

    private File copyResourceToTmpFile(InputStream res, File tmp) throws IOException {
        FileOutputStream out = new FileOutputStream(tmp);
        byte[] buff = new byte[4096];
        for (int n = res.read(buff); n >= 0; n = res.read(buff)) {
            out.write(buff, 0, n);
        }

        out.flush();
        out.close();
        res.close();

        tmp.deleteOnExit();
        return tmp;
    }

    private InputStream getZmodemResource(String name) {
        String path = getOSResourcePath() + name;
        return getClass().getResourceAsStream(path);
    }

    File copyZmodemResource(String name) throws IOException {
        InputStream in = getZmodemResource(name);
        File tmp = new File(zmodemDir, name);
        return copyResourceToTmpFile(in, tmp);
    }

    protected abstract String getOSResourcePath();

    protected static final String OS_PROPERTY_KEY = "os.name";

    protected volatile boolean quit = false;

    protected File zmodemDir;

    private File zmodem;

    private class RemoteOutputPipe implements Runnable {

        private final InputStream es;
        private final StringBuffer output;

        RemoteOutputPipe(InputStream es) {
            this.es = es;
            this.output = new StringBuffer();
        }

        public String getOutput() {
            return output.toString().trim();
        }

        @Override
        public void run() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(es));

                String s;
                for (s = r.readLine(); s != null; s = r.readLine()) {
                    output.append(s);
                    output.append("\n");
                }
            } catch (IOException ioe) {
                getLogger().log(Level.SEVERE, null, ioe);
            } finally {
                try {
                    es.close();
                } catch (IOException ignored) { }
            }
        }
    }
}