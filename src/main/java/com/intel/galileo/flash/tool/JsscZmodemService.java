package com.intel.galileo.flash.tool;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class JsscZmodemService extends AbstractZmodemService {

    @Override
    protected Runnable createSerialOutputPipe(InputStream stdout, FileProgress progress) {
        return new SerialOutputPipe(stdout, progress);
    }

    @Override
    protected Runnable createSerialInputPipe(OutputStream stdin) {
        return new SerialInputPipe(stdin);
    }

    @Override
    protected boolean openSerialTransport(String portName) {
        port = new SerialPort(portName);
        try {
            return port.openPort();
        } catch (SerialPortException ex) {
            getLogger().severe(ex.getMessage());
        }
        return false;
    }

    @Override
    protected void closeSerialTransport() {
        if (port != null) {
            try {
                port.closePort();
            } catch (SerialPortException ex) {
                getLogger().severe(ex.getMessage());
            }
            port = null;
        }
    }

    @Override
    protected boolean isSerialTransportOpen() {
        return (port != null) && port.isOpened();
    }

    protected SerialPort port;
}
