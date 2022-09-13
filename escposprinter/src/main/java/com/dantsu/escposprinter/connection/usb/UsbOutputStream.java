package com.dantsu.escposprinter.connection.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class UsbOutputStream extends OutputStream {
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpointIn;
    private UsbEndpoint usbEndpointOut;

    public UsbOutputStream(UsbManager usbManager, UsbDevice usbDevice) throws IOException {

        this.usbInterface = UsbDeviceHelper.findPrinterInterface(usbDevice);
        if(this.usbInterface == null) {
            throw new IOException("Unable to find USB interface.");
        }

        this.usbEndpointIn = UsbDeviceHelper.findEndpointIn(this.usbInterface);
        if(this.usbEndpointIn == null) {
            throw new IOException("Unable to find USB in endpoint.");
        }

        this.usbEndpointOut = UsbDeviceHelper.findEndpointOut(this.usbInterface);
        if(this.usbEndpointOut == null) {
            throw new IOException("Unable to find USB out endpoint.");
        }

        this.usbConnection = usbManager.openDevice(usbDevice);
        if(this.usbConnection == null) {
            throw new IOException("Unable to open USB connection.");
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.write(new byte[]{(byte) i});
    }

    @Override
    public void write(@NonNull byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(final @NonNull byte[] bytes, final int offset, final int length) throws IOException {
        if (this.usbInterface == null || this.usbEndpointIn == null || this.usbConnection == null) {
            throw new IOException("Unable to connect to USB device.");
        }

        if (!this.usbConnection.claimInterface(this.usbInterface, true)) {
            throw new IOException("Error during claim USB interface.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        UsbRequest usbRequest = new UsbRequest();
        try {
            usbRequest.initialize(this.usbConnection, this.usbEndpointIn);
            if (!usbRequest.queue(buffer, bytes.length)) {
                throw new IOException("Error queueing USB request.");
            }
            this.usbConnection.requestWait();
        } finally {
            usbRequest.close();
        }
    }

    public byte[] read() throws IOException {
        if (this.usbInterface == null || this.usbEndpointOut == null || this.usbConnection == null) {
            throw new IOException("Unable to connect to USB interface");
        }

        final int bufferLength = 1024;
        byte[] buffer = new byte[bufferLength];
        int receivedBytes = this.usbConnection.bulkTransfer(this.usbEndpointOut, buffer, bufferLength, 3000);
        if (receivedBytes > 0) {
            byte[] result = new byte[receivedBytes];
            System.arraycopy(buffer, 0, result, 0, receivedBytes);
            return result;
        } else {
            return new byte[0];
        }
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        if (this.usbConnection != null) {
            this.usbConnection.close();
            this.usbInterface = null;
            this.usbEndpointIn = null;
            this.usbEndpointOut = null;
            this.usbConnection = null;
        }
    }
}
