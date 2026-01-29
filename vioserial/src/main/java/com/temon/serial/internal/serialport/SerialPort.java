/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.temon.serial.internal.serialport;

import android.util.Log;

import com.temon.serial.core.PermissionStrategy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Low-level serial port wrapper with JNI.
 *
 * <p>Note: This is protocol-agnostic. Prefer {@code com.temon.serial.core.SerialConnection} for a
 * library-grade API.</p>
 */
public class SerialPort {
    /**
     * 串口波特率定义
     */
    public enum BAUDRATE {
        B0(0),
        B50(50),
        B75(75),
        B110(110),
        B134(134),
        B150(150),
        B200(200),
        B300(300),
        B600(600),
        B1200(1200),
        B1800(1800),
        B2400(2400),
        B4800(4800),
        B9600(9600),
        B19200(19200),
        B38400(38400),
        B57600(57600),
        B115200(115200),
        B230400(230400),
        B460800(460800),
        B500000(500000),
        B576000(576000),
        B921600(921600),
        B1000000(1000000),
        B1152000(1152000),
        B1500000(1500000),
        B2000000(2000000),
        B2500000(2500000),
        B3000000(3000000),
        B3500000(3500000),
        B4000000(4000000);

        int baudrate;

        BAUDRATE(int baudrate) {
            this.baudrate = baudrate;
        }

        int getBaudrate() {
            return this.baudrate;
        }
    }

    /**
     * 串口停止位定义
     */
    public enum STOPB {
        /**
         * 1位停止位
         */
        B1(1),
        /**
         * 2位停止位
         */
        B2(2);

        int stopBit;

        STOPB(int stopBit) {
            this.stopBit = stopBit;
        }

        public int getStopBit() {
            return this.stopBit;
        }
    }

    /**
     * 串口数据位定义
     */
    public enum DATAB {
        CS5(5),
        CS6(6),
        CS7(7),
        CS8(8);

        int dataBit;

        DATAB(int dataBit) {
            this.dataBit = dataBit;
        }

        public int getDataBit() {
            return this.dataBit;
        }
    }

    /**
     * 串口校验位定义
     */
    public enum PARITY {
        NONE(0),
        ODD(1),
        EVEN(2);

        int parity;

        PARITY(int parity) {
            this.parity = parity;
        }

        public int getParity() {
            return this.parity;
        }
    }

    /**
     * 串口流控定义
     */
    public enum FLOWCON {
        NONE(0),
        HARD(1),
        SOFT(2);

        int flowCon;

        FLOWCON(int flowCon) {
            this.flowCon = flowCon;
        }

        public int getFlowCon() {
            return this.flowCon;
        }
    }

    private static final String TAG = "SerialPort";

    private static final PermissionStrategy DEFAULT_PERMISSION_STRATEGY = new PermissionStrategy() {
        @Override
        public void ensurePermission(File device) throws SecurityException {
            if (!device.canRead() || !device.canWrite()) {
                throw new SecurityException("No read/write permission for " + device.getAbsolutePath()
                        + ". Grant device node permission in system image or use an external permission strategy.");
            }
        }
    };

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        this(device, baudrate, 1, 8, 0, 0, flags, 0, null);
    }

    public SerialPort(File device, int baudrate, int stopBits, int dataBits, int parity, int flowCon, int flags)
            throws SecurityException, IOException {
        this(device, baudrate, stopBits, dataBits, parity, flowCon, flags, 0, null);
    }

    public SerialPort(File device, int baudrate, int stopBits, int dataBits, int parity, int flowCon, int flags, int readTimeoutMs)
            throws SecurityException, IOException {
        this(device, baudrate, stopBits, dataBits, parity, flowCon, flags, readTimeoutMs, null);
    }

    public SerialPort(File device, int baudrate, int stopBits, int dataBits, int parity, int flowCon, int flags,
                      int readTimeoutMs, PermissionStrategy permissionStrategy)
            throws SecurityException, IOException {
        PermissionStrategy strategy = permissionStrategy != null ? permissionStrategy : DEFAULT_PERMISSION_STRATEGY;
        strategy.ensurePermission(device);

        mFd = open(device.getAbsolutePath(), baudrate, stopBits, dataBits, parity, flowCon, flags, readTimeoutMs);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    private native static FileDescriptor open(String path, int baudrate, int stopBits, int dataBits, int parity, int flowCon, int flags, int readTimeoutMs);

    public native void close();

    /**
     * Check if the device is still online/connected.
     * 
     * <p>Uses select() to check if the file descriptor is still valid.
     * This can detect device disconnection.</p>
     * 
     * @return true if device appears to be online, false if disconnected or invalid
     */
    public native boolean isDeviceOnline();

    static {
        System.loadLibrary("serial_port");
    }
}
