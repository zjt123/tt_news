package com.tiantian.news.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.os.Environment;

public class FileUtils {

	private static final int DEFAULT_BUFFER_SIZE = 512;// byte
	private static final int DEFAULT_BUFFER_STREAM_SIZE = 8 * 1024;// byte

	private static final String FILE_ROOT_OLD_PATH = "/MyNews";
	private static final String FILE_ROOT_PATH = "/Android/data";
	private static final String FILE_CHLID_DIR_NAME = "channel_";
	private static final String FILE_SUFFIX = ".jpg";

	public static String mAppPackageName;

	public static String readFileToString(String path, String charset) {
		byte[] buffer = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		ByteArrayOutputStream baos = null;
		byte[] bytesResult = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(path),
					DEFAULT_BUFFER_STREAM_SIZE);
			baos = new ByteArrayOutputStream();
			bos = new BufferedOutputStream(baos, DEFAULT_BUFFER_STREAM_SIZE);
			buffer = new byte[DEFAULT_BUFFER_SIZE];
			int len = 0;
			while ((len = bis.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
				bos.write(buffer, 0, len);
			}
			bos.flush();
			baos.flush();
			bytesResult = baos.toByteArray();
			return new String(bytesResult, charset);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bos != null) {
					bos.close();
					bos = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (baos != null) {
					baos.close();
					baos = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (bis != null) {
					bis.close();
					bis = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			buffer = null;
			bytesResult = null;
		}
		return null;
	}

	public static File getFileByChannelId(int channelId, String name) {
		return new File(getRootFile(), FILE_CHLID_DIR_NAME + channelId + File.separator + MD5Util.getMD5(name) + FILE_SUFFIX);
	}

	private static File getRootFile() {
		return Environment.getExternalStoragePublicDirectory(FILE_ROOT_PATH + File.separator + mAppPackageName);
	}

	public static void deleteOldRootDir() {
		deleteFileDir(Environment.getExternalStoragePublicDirectory(FILE_ROOT_OLD_PATH));
	}

	/**
	 * 递归删除文件目录
	 * @param dir 文件目录
	 */
	public static void deleteFileDir(File dir) {
		try {
			if (dir.exists() && dir.isDirectory()) {// 判断是文件还是目录
				if (dir.listFiles().length == 0) {// 若目录下没有文件则直接删除
					dir.delete();
				} else {// 若有则把文件放进数组，并判断是否有下级目录
					File delFile[] = dir.listFiles();
					int len = dir.listFiles().length;
					for (int j = 0; j < len; j++) {
						if (delFile[j].isDirectory()) {
							deleteFileDir(delFile[j]);// 递归调用deleteFileDir方法并取得子目录路径
						} else {
							boolean isDeltet = delFile[j].delete();// 删除文件
						}
					}
					delFile = null;
				}
				deleteFileDir(dir);// 递归调用
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除单个文件
	 * @param dir 文件目录
	 */
	public static void deleteFile(File file) {
		try {
			if (file != null && file.isFile() && file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
