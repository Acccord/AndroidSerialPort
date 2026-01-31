package com.temon.serial.core;

import java.io.File;
import java.io.IOException;

/**
 * Explicit permission strategy that attempts "su + chmod 666".
 *
 * <p>Use only in rooted environments and only when you accept the security risks.</p>
 */
public final class SuChmodPermissionStrategy implements PermissionStrategy {
    private static final String DEFAULT_SU_PATH = "/system/bin/su";

    private final String suPath;

    public SuChmodPermissionStrategy() {
        this(DEFAULT_SU_PATH);
    }

    public SuChmodPermissionStrategy(String suPath) {
        if (suPath == null || suPath.trim().isEmpty()) {
            throw new IllegalArgumentException("suPath == null or empty");
        }
        this.suPath = suPath;
    }

    @Override
    public void ensurePermission(File device) throws SecurityException {
        if (device.canRead() && device.canWrite()) {
            return;
        }
        String devicePath = device.getAbsolutePath();
        if (!isSafePath(devicePath)) {
            throw new SecurityException("Unsafe device path for su chmod: " + devicePath);
        }
        try {
            Process su = Runtime.getRuntime().exec(suPath);
            String cmd = "chmod 666 " + devicePath + "\n"
                    + "exit\n";
            su.getOutputStream().write(cmd.getBytes());
            su.getOutputStream().flush();
            int result = su.waitFor();
            if (result != 0 || !device.canRead() || !device.canWrite()) {
                throw new SecurityException("Failed to chmod device node via su: " + device.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SecurityException("Interrupted while running su chmod: " + device.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new SecurityException("Failed to run su chmod: " + device.getAbsolutePath(), e);
        }
    }

    private static boolean isSafePath(String path) {
        // Allow only common device-node path characters to avoid command injection.
        return path != null && path.matches("[A-Za-z0-9_./-]+");
    }
}
