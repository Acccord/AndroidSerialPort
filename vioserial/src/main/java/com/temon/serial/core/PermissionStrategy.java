package com.temon.serial.core;

import java.io.File;

/**
 * Strategy for ensuring permission before opening a device node.
 */
public interface PermissionStrategy {
    void ensurePermission(File device) throws SecurityException;
}
