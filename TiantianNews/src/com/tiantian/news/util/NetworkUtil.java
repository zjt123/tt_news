package com.tiantian.news.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Log;

public class NetworkUtil {
	private static final String TAG = "NetworkUtil";
	
	private static final int DEFAULT_BUFFER_SIZE = 512;//byte
	private static final int DEFAULT_BUFFER_STREAM_SIZE = 8 * 1024;//byte
	private static final int DEFAULT_CONNECT_TIME_OUT = 10000;
	private static final int DEFAULT_READ_TIME_OUT = 10000;

	public static String getRss(String webUrl) {
		if (TextUtils.isEmpty(webUrl)) {
			return null;
		}
//		String ip = getPsdnIp();
//		Log.v("zhang", "getPsdnIp ========= "+ip);
		String rss = null;
		byte[] buffer = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		ByteArrayOutputStream baos = null;
		byte[] bytesResult = null;
		URL url = null;
		HttpURLConnection urlConnection = null;
		try {
			url = new URL(webUrl);
			String host = android.net.Proxy.getDefaultHost();
			if (!TextUtils.isEmpty(host)) {//TODO java.net.SocketTimeoutException: Transport endpoint is not connected
				int port = android.net.Proxy.getDefaultPort();
				InetSocketAddress vAddress = new InetSocketAddress(host, port);
				java.net.Proxy vProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, vAddress);
				urlConnection = (HttpURLConnection) url.openConnection(vProxy);
			} else {
				urlConnection = (HttpURLConnection) url.openConnection();
			}
//			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(DEFAULT_CONNECT_TIME_OUT);
			urlConnection.setReadTimeout(DEFAULT_READ_TIME_OUT);
			urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Content-Type","text/html; charset=UTF-8");
			if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				bis = new BufferedInputStream(urlConnection.getInputStream(), DEFAULT_BUFFER_STREAM_SIZE);
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
				rss = new String(bytesResult, "UTF-8");
			}
		} catch (java.net.SocketTimeoutException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bos != null) {
					bos.close();
					bos = null;
				}
				if (baos != null) {
					baos.close();
					baos = null;
				}
				if (bis != null) {
					bis.close();
					bis = null;
				}
				if (urlConnection != null) {
					urlConnection.disconnect();
					urlConnection = null;
				}
				buffer = null;
				bytesResult = null;
				url = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return rss;
	}

	public static String getRss2(String webUrl) {
		if (TextUtils.isEmpty(webUrl)) {
			return null;
		}
		String rss = null;
		HttpClient client = AndroidHttpClient.newInstance("Android");
		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, DEFAULT_CONNECT_TIME_OUT);
		HttpConnectionParams.setSoTimeout(params, DEFAULT_CONNECT_TIME_OUT);
		HttpConnectionParams.setSocketBufferSize(params, DEFAULT_BUFFER_SIZE);
		HttpResponse response = null;
		InputStream is = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		ByteArrayOutputStream baos = null;
		byte[] bytesResult = null;
		byte[] buffer = null;
		HttpGet httpGet = null;
		HttpEntity entity = null;
		try {
			httpGet = new HttpGet(webUrl);
			response = client.execute(httpGet);
			int stateCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "  loadImageFromInternet  [loadImage] stateCode = " + stateCode);
			if (stateCode == HttpStatus.SC_OK) {
				entity = response.getEntity();
				if (entity != null) {
					is = entity.getContent();
					bis = new BufferedInputStream(is, DEFAULT_BUFFER_STREAM_SIZE);
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
					rss = new String(bytesResult, "UTF-8");
				}
			}
		} catch (ClientProtocolException e) {
			httpGet.abort();
			e.printStackTrace();
		} catch (IOException e) {
			httpGet.abort();
			e.printStackTrace();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (entity != null) {
					entity.consumeContent();
				}
				if (bos != null) {
					bos.close();
					bos = null;
				}
				if (baos != null) {
					baos.close();
					baos = null;
				}
				if (bis != null) {
					bis.close();
					bis = null;
				}
				buffer = null;
				bytesResult = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			((AndroidHttpClient) client).close();
		}
		return rss;
	}

	/**
	 * 用来获取手机拨号上网（包括CTWAP和CTNET）时由PDSN分配给手机终端的源IP地址。
	 * 
	 * @return
	 * @author SHANHY
	 */
	public static String getPsdnIp() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
					//if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private static InetAddress getInetAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
					//if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
						return inetAddress;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
