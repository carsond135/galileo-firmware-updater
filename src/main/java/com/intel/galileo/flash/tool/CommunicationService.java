package com.intel.galileo.flash.tool;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public abstract class CommunicationService {
	
	public static List<CommunicationService> getCommunicationServices() {
		List<CommunicationService> services = new LinkedList<>();
		Class<CommunicationService> c = CommunicationService.class;
        ServiceLoader<CommunicationService> available = ServiceLoader.load(c);
        for (CommunicationService link : available) {
            if (link.isSupportedOnThisOS()) {
                services.add(link);
            }
        }
        return services;
	}

    public abstract String getServiceName();

    public abstract List<String> getAvailableConnections();

    public abstract String getConnectionLabel();

    public abstract boolean openConnection(String connection);

    public abstract void closeConnection();

    public abstract boolean isConnectionOpen();

    public abstract String sendCommand(String cmd) throws Exception;

    public abstract String sendCommandWithTimeout(String cmd, int timeout) throws Exception;

    public abstract void sendFile(File f, FileProgress p) throws Exception;

    @Override
    public String toString() {
        return getServiceName();
    }

    public abstract boolean isSupportedOnThisOS();

    public abstract void setFileLocation(File dir);

    public abstract boolean isProgressSupported();

    public interface FileProgress {
        void bytesSent(int nsent);
    }
}