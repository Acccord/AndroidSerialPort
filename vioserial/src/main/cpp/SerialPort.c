/*
 * Copyright 2009-2011 Cedric Priscal
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

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/select.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <jni.h>

#include "SerialPort.h"

#include "android/log.h"

static const char *TAG = "serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static void throwIOException(JNIEnv *env, const char *msg) {
	jclass exClass = (*env)->FindClass(env, "java/io/IOException");
	if (exClass != NULL) {
		(*env)->ThrowNew(env, exClass, msg);
	}
}

static speed_t getBaudrate(jint baudrate)
{
	switch(baudrate) {
		case 0: return B0;
		case 50: return B50;
		case 75: return B75;
		case 110: return B110;
		case 134: return B134;
		case 150: return B150;
		case 200: return B200;
		case 300: return B300;
		case 600: return B600;
		case 1200: return B1200;
		case 1800: return B1800;
		case 2400: return B2400;
		case 4800: return B4800;
		case 9600: return B9600;
		case 19200: return B19200;
		case 38400: return B38400;
		case 57600: return B57600;
		case 115200: return B115200;
		case 230400: return B230400;
		case 460800: return B460800;
		case 500000: return B500000;
		case 576000: return B576000;
		case 921600: return B921600;
		case 1000000: return B1000000;
		case 1152000: return B1152000;
		case 1500000: return B1500000;
		case 2000000: return B2000000;
		case 2500000: return B2500000;
		case 3000000: return B3000000;
		case 3500000: return B3500000;
		case 4000000: return B4000000;
		default: return -1;
	}
}

/*
 * Class:     com_temon_serial_internal_serialport_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;IIIIII)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_com_temon_serial_internal_serialport_SerialPort_open
		(JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint stopBits, jint dataBits,
		 jint parity, jint flowCon, jint flags, jint readTimeoutMs) {
	int fd;
	speed_t speed;
	jobject mFileDescriptor;

	/* Check arguments */
	{
		speed = getBaudrate(baudrate);
		if (speed == -1) {
			/* TODO: throw an exception */
			LOGE("Invalid baudrate");
			throwIOException(env, "Invalid baudrate");
			return NULL;
		}
	}

	/* Opening device */
	{
		jboolean iscopy;
		const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
		LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | flags);
		fd = open(path_utf, O_RDWR | flags);
		LOGD("open() fd = %d", fd);
		(*env)->ReleaseStringUTFChars(env, path, path_utf);
		if (fd == -1) {
			/* Throw an exception */
			LOGE("Cannot open port");
			{
				char buf[256];
				snprintf(buf, sizeof(buf), "Cannot open port: %s", strerror(errno));
				throwIOException(env, buf);
			}
			return NULL;
		}
	}

	/* Configure device */
	{
		struct termios cfg;
		LOGD("Configuring serial port");
		if (tcgetattr(fd, &cfg)) {
			LOGE("tcgetattr() failed");
			close(fd);
			{
				char buf[256];
				snprintf(buf, sizeof(buf), "tcgetattr failed: %s", strerror(errno));
				throwIOException(env, buf);
			}
			return NULL;
		}

		cfmakeraw(&cfg);
		cfsetispeed(&cfg, speed);
		cfsetospeed(&cfg, speed);

		cfg.c_cflag &= ~CSIZE;
		switch (dataBits) {
			case 5:
				cfg.c_cflag |= CS5;    //使用5位数据位
				break;
			case 6:
				cfg.c_cflag |= CS6;    //使用6位数据位
				break;
			case 7:
				cfg.c_cflag |= CS7;    //使用7位数据位
				break;
			case 8:
				cfg.c_cflag |= CS8;    //使用8位数据位
				break;
			default:
				cfg.c_cflag |= CS8;
				break;
		}

		switch (parity) {
			case 0:
				cfg.c_cflag &= ~PARENB;    //无奇偶校验
				break;
			case 1:
				cfg.c_cflag |= (PARODD | PARENB);   //奇校验
				break;
			case 2:
				cfg.c_iflag &= ~(IGNPAR | PARMRK); // 偶校验
				cfg.c_iflag |= INPCK;
				cfg.c_cflag |= PARENB;
				cfg.c_cflag &= ~PARODD;
				break;
			default:
				cfg.c_cflag &= ~PARENB;
				break;
		}

		switch (stopBits) {
			case 1:
				cfg.c_cflag &= ~CSTOPB;    //1位停止位
				break;
			case 2:
				cfg.c_cflag |= CSTOPB;    //2位停止位
				break;
			default:
				break;
		}

		// hardware flow control
		switch (flowCon) {
			case 0:
				cfg.c_cflag &= ~CRTSCTS;    //不使用流控
				break;
			case 1:
				cfg.c_cflag |= CRTSCTS;    //硬件流控
				break;
			case 2:
				cfg.c_cflag |= IXON | IXOFF | IXANY;    //软件流控
				break;
			default:
				cfg.c_cflag &= ~CRTSCTS;
				break;
		}

		// Configure read timeout: VMIN and VTIME
		// VMIN = 0: non-blocking mode, return immediately if no data
		// VTIME = readTimeoutMs / 100 (in deciseconds, 0.1s units)
		// If readTimeoutMs <= 0, use blocking mode (VMIN > 0, VTIME = 0)
		if (readTimeoutMs > 0) {
			// Non-blocking with timeout: VMIN=0, VTIME in deciseconds
			cfg.c_cc[VMIN] = 0;
			// Convert milliseconds to deciseconds (0.1s units), min 1 decisecond
			int vtime = (readTimeoutMs + 99) / 100;  // Round up
			if (vtime < 1) vtime = 1;
			if (vtime > 255) vtime = 255;  // VTIME is uint8_t
			cfg.c_cc[VTIME] = (unsigned char)vtime;
			LOGD("Configured read timeout: %d ms (VTIME=%d)", readTimeoutMs, vtime);
		} else {
			// Blocking mode: VMIN > 0, VTIME = 0 (wait for at least 1 byte)
			cfg.c_cc[VMIN] = 1;
			cfg.c_cc[VTIME] = 0;
			LOGD("Configured blocking read mode (no timeout)");
		}

		if (tcsetattr(fd, TCSANOW, &cfg)) {
			LOGE("tcsetattr() failed");
			close(fd);
			{
				char buf[256];
				snprintf(buf, sizeof(buf), "tcsetattr failed: %s", strerror(errno));
				throwIOException(env, buf);
			}
			return NULL;
		}
	}

	/* Create a corresponding file descriptor */
	{
		jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
		jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
		jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
		mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
		(*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint) fd);
	}

	return mFileDescriptor;
}

/*
 * Class:     com_temon_serial_internal_serialport_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_temon_serial_internal_serialport_SerialPort_close
		(JNIEnv *env, jobject thiz) {
	jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
	jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

	jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
	jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

	jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
	jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

	LOGD("close(fd = %d)", descriptor);
	close(descriptor);
}

/*
 * Class:     com_temon_serial_internal_serialport_SerialPort
 * Method:    isDeviceOnline
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_temon_serial_internal_serialport_SerialPort_isDeviceOnline
		(JNIEnv *env, jobject thiz) {
	jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
	jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

	jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
	jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

	jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
	if (mFd == NULL) {
		return JNI_FALSE;
	}
	jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);
	if (descriptor < 0) {
		return JNI_FALSE;
	}

	// Use select() with zero timeout to check if file descriptor is still valid
	fd_set readfds;
	struct timeval timeout;
	FD_ZERO(&readfds);
	FD_SET(descriptor, &readfds);
	timeout.tv_sec = 0;
	timeout.tv_usec = 0;

	// Check if descriptor is still valid by attempting to select on it
	// If select returns error, device is likely disconnected
	int result = select(descriptor + 1, &readfds, NULL, NULL, &timeout);
	if (result < 0) {
		// Error: device may be disconnected
		if (errno == EBADF) {
			LOGD("isDeviceOnline: EBADF - file descriptor invalid");
			return JNI_FALSE;
		}
		// Other errors might be temporary, but log them
		LOGD("isDeviceOnline: select() error %d", errno);
		return JNI_FALSE;
	}

	// Also check if we can still access the file descriptor
	// by attempting a non-blocking read (peek)
	char buf[1];
	ssize_t n = read(descriptor, buf, 0);  // Peek: read 0 bytes
	if (n < 0 && errno == EBADF) {
		LOGD("isDeviceOnline: EBADF on read peek - file descriptor invalid");
		return JNI_FALSE;
	}

	return JNI_TRUE;
}