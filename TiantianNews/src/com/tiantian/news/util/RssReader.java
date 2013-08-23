package com.tiantian.news.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.db.NewsDatabase;

public class RssReader {

	public static ArrayList<NewsItem> pullParseXML(ArrayList<NewsItem> source, String rssXml, int channelId) {
		ArrayList<NewsItem> list = null;
		NewsItem item = null;
		// 构建XmlPullParserFactory
		try {
			XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
			// 获取XmlPullParser的实例
			XmlPullParser xmlPullParser = pullParserFactory.newPullParser();
			// 设置输入流 xml文件
			xmlPullParser.setInput(new ByteArrayInputStream(rssXml.getBytes("UTF-8")), "UTF-8");
			// 开始
			int eventType = xmlPullParser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String nodeName = xmlPullParser.getName();
				switch (eventType) {
				// 文档开始
				case XmlPullParser.START_DOCUMENT:
					list = new ArrayList<NewsItem>();
					break;
				// 开始节点
				case XmlPullParser.START_TAG:
					// 判断如果其实节点为item
					if ("item".equals(nodeName)) {
						// 实例化NewsItem对象
						item = new NewsItem();
						item.channelId = channelId;
					} else if (item != null) {
						if ("title".equals(nodeName)) {
							item.spiteTitle(xmlPullParser.nextText());
							if (source.indexOf(item) > -1) {//去重
								item = null;
							}
						} else if ("link".equals(nodeName)) {
							item.link = xmlPullParser.nextText();
							NewsItem newItem = NewsDatabase.getInstance().selectByArgs(channelId, item.link);
							if (newItem != null) {
								list.add(newItem);
								item = null;
							}
						} else if ("guid".equals(nodeName)) {
							item.guid = xmlPullParser.nextText();
						} else if ("category".equals(nodeName)) {
							item.category = xmlPullParser.nextText();
						} else if ("pubDate".equals(nodeName)) {
							item.pubDate = xmlPullParser.nextText();
							item.pubDateTime = Date.parse(item.pubDate);
						} else if ("description".equals(nodeName)) {
							item.description = xmlPullParser.nextText();
							item.fixOthers();
						}
					}
					break;
				// 结束节点
				case XmlPullParser.END_TAG:
					if ("item".equals(nodeName) && item != null) {
						item._id = NewsDatabase.getInstance().insert(item);
						list.add(item);
						item = null;
					}
					break;
				default:
					break;
				}
				eventType = xmlPullParser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

//	public static void main(String[] args) {
//		//Thread.currentThread().getContextClassLoader().getResourceAsStream("Student.xml")
//		String rss = FileUtils.readFileToString(RssReader.class.getResource("rss.xml").getPath(), "UTF-8");
//		List<NewsItem> items = pullParseXML(rss);
//		System.out.println(items);
//		
////		String s= "Thu, 08 Aug 2013 10:00:18 GMT";
////		Date now = new Date();
////		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒 E");
////		System.out.println(now.toGMTString());
////		System.out.println(now.toLocaleString());
////		System.out.println(now.toString());
////		System.out.println(Date.parse(s));
////		System.out.println(dateFormat.format(Date.parse(s)));
//        
//	}
	
}
