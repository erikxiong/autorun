package com.ict.mcg.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ict.mcg.data.NewsClue;
import com.ict.mcg.util.ICTMongoClient;
import com.ict.mcg.util.ParamUtil;
import com.ict.mcg.util.TimeConvert;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class AutoRun {	
	public static int AUTO_RUN_INTERVAL = 18000000; // 1000 * 60 * 60 * 5
	public static int AUTO_RUN_NUM = 100;
	
	/**
	 * 获取配置信息
	 */
	static {
		try {
			Properties config = new Properties();
			config.load(NewsClue.class.getClassLoader().getResourceAsStream("webservice.properties"));
			AUTO_RUN_INTERVAL = Integer.parseInt(config.getProperty("auto_run_interval"));
			System.out.println(AUTO_RUN_INTERVAL);
			AUTO_RUN_NUM = Integer.parseInt(config.getProperty("auto_run_num"));
			System.out.println(AUTO_RUN_NUM);
		} catch (IOException e) {
			System.err.println("[AutoRun] Configure error");
			e.printStackTrace();
		}
	}
	
	static Map<String, String> loadUrls(String infile) {
		Map<String, String> urls = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "utf-8"));
			String content = "";
			String s = "";
			while ((s = reader.readLine()) != null) {
				content += s;
			}
			reader.close();

			JSONArray old = JSONArray.fromObject(content);
			for (int i = 0; i < old.size(); i++) {
				JSONObject jo = old.getJSONObject(i);
				if (!urls.containsKey(jo.getString("title")))
					urls.put(jo.getString("title"), jo.getString("url"));
			}
		} catch (IOException ex) {

		}
		return urls;
	}

	static void sendUrls(String file, int start) {
		HttpURLConnection url_con = null;

		Map<String, String> urls = loadUrls(".\\WebRoot\\pagefile\\" + file);
		System.out.println(urls.size());
		String prefix = "http://210.56.193.244:8080/"+ParamUtil.WEBAPP_NAME+"/AnalyzeEvent?";
		int cnt = 0;

		for (String title : urls.keySet()) {
			try {
				if (++cnt < start)
					continue;
				long begin = System.currentTimeMillis();
				String urlstring = prefix + urls.get(title) + "&test=yes";
				System.out.print(cnt + ": Send request: " + urlstring);
				URL url = new URL(urlstring);
				url_con = (HttpURLConnection) url.openConnection();
				url_con.setRequestMethod("POST");
				url_con.setDoOutput(true);
				url_con.setConnectTimeout(300 * 1000);
				url_con.setReadTimeout(300 * 1000);
				url_con.getOutputStream().flush();
				url_con.getOutputStream().close();
				url_con.connect();

				BufferedReader in = new BufferedReader(new InputStreamReader(url_con.getInputStream(), "UTF-8"));
				String inputLine, content = "";
				while ((inputLine = in.readLine()) != null)
					content += inputLine + "\n";

				// 当返回为空或者没有实质内容时，不予显示
				if (content.length() > 5 * 1000) {
//					FileWriter writer = new FileWriter("E:\\Software\\apache-tomcat-6.0.41\\webapps\\NewsCertify\\pages\\" + title + ".html", false);
//					writer.write(content);
//					writer.close();
				}

				in.close();

				// 当超时时，多休息一会儿
				long runTime = (System.currentTimeMillis() - begin) / 1000;
				System.out.print("\trecevied.\t" + runTime + "\n");

				Thread.sleep(60 * 1000);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if (url_con != null)
					url_con.disconnect();
			}
		}

	}

	static void sendUrlsFromDB(ArrayList<DBObject> newslist, int start) {
		if (newslist == null || newslist.size() == 0) {
			return;
		}

		System.out.println("[AutoRun] urls size:" + newslist.size());
		
		String prefix = "http://localhost:8080/NewsCertifyOffline/AnalyzeEvent?";
		//System.out.println(prefix);
		
		int cnt = 0;
		HttpURLConnection url_con = null;
		for (DBObject obj : newslist) {
			try {
				if (++cnt < start)
					continue;
				long begin = System.currentTimeMillis();
				String urlStr = (String)obj.get("url");
				String fromStr = (String)obj.get("from");
				String idStr = (String)obj.get("id");
				if (urlStr == null || urlStr.length() == 0 || fromStr == null || fromStr.length() == 0 || idStr == null || idStr.length() == 0) {
					continue;
				}
				String urlstring = prefix + urlStr + "&test=yes&from=" + fromStr + "&id=" + idStr;
				System.out.print("[" + TimeConvert.getStringTime(TimeConvert.getNow()) + "]" + cnt + ": Send request: " + urlstring);
				URL url = new URL(urlstring);
				url_con = (HttpURLConnection) url.openConnection();
				url_con.setRequestMethod("POST");
				url_con.setDoOutput(true);
				url_con.setConnectTimeout(300 * 1000);
				url_con.setReadTimeout(300 * 1000);
				url_con.getOutputStream().flush();
				url_con.getOutputStream().close();
				url_con.connect();

				BufferedReader in = new BufferedReader(new InputStreamReader(url_con.getInputStream(), "UTF-8"));
				String inputLine, content = "";
				while ((inputLine = in.readLine()) != null)
					content += inputLine + "\n";

				// 当返回为空或者没有实质内容时，不予显示
				if (content.length() > 5 * 1000) {
//					FileWriter writer = new FileWriter("E:\\Software\\apache-tomcat-6.0.41\\webapps\\NewsCertify\\pages\\" + title + ".html", false);
//					writer.write(content);
//					writer.close();
				}

				in.close();

				// 当超时时，多休息一会儿
				long runTime = (System.currentTimeMillis() - begin) / 1000;
				System.out.print("\trecevied.\t" + runTime + "\n");

				Thread.sleep(60 * 1000);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if (url_con != null)
					url_con.disconnect();
			}
		}

	}
	
	/**
	 * 将数据库的一条线索发送认证
	 * @param obj
	 * @return 错误类型 1:搜索无结果 2:参数错误
	 */
	static int sendOneUrl(DBObject obj) {
		String prefix = "http://localhost:8080/NewsCertifyOffline/AnalyzeEvent?";
		
		HttpURLConnection url_con = null;
		try {
			long begin = System.currentTimeMillis();
			String urlStr = (String)obj.get("url");
			String fromStr = (String)obj.get("from");
			String idStr = (String)obj.get("id");
			if (urlStr == null || urlStr.length() == 0 || fromStr == null || fromStr.length() == 0 || idStr == null || idStr.length() == 0) {
				return 2;
			}
			String urlstring = prefix + urlStr + "&test=yes&from=" + fromStr + "&id=" + idStr;
			System.out.println("[" + TimeConvert.getStringTime(TimeConvert.getNow()) + "] Send request: " + urlstring);
			URL url = new URL(urlstring);
			url_con = (HttpURLConnection) url.openConnection();
			url_con.setRequestMethod("POST");
			url_con.setDoOutput(true);
			url_con.setConnectTimeout(300 * 1000);
			url_con.setReadTimeout(300 * 1000);
			url_con.getOutputStream().flush();
			url_con.getOutputStream().close();
			url_con.connect();
	
			BufferedReader in = new BufferedReader(new InputStreamReader(url_con.getInputStream(), "UTF-8"));
			String inputLine, content = "";
			while ((inputLine = in.readLine()) != null)
				content += inputLine + "\n";
	
			in.close();
			
			
			// 当超时时，多休息一会儿
			long runTime = (System.currentTimeMillis() - begin) / 1000;
			System.out.print("\trecevied.\t" + runTime + "\n");
	
			// Thread.sleep(60 * 1000);
			// 错误类型
			if (content.contains("没有搜索到相关微博")) {
				return 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
		} finally {
			if (url_con != null)
				url_con.disconnect();
		}
		return 0;
	}
	
	static void addTitle() {
		String outfile = "D:\\mcg\\report\\tmp.txt";
		Map<String, String> urls = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\shaoj_000\\Workspaces\\MyEclipse 10\\NewsCertify\\WebRoot\\pagefile\\newslist_Sep.json"));
			String content = "";
			String s = "";
			while ((s = reader.readLine()) != null) {
				content += s;
			}
			reader.close();

			JSONArray old = JSONArray.fromObject(content);
			for (int i = 0; i < old.size(); i++) {
				JSONObject jo = old.getJSONObject(i);
				if (!urls.containsKey(jo.getString("key")))
					urls.put(jo.getString("key"), jo.getString("title"));
			}
			reader = new BufferedReader(new FileReader("D:\\mcg\\report\\query.txt"));
			content = "";
			String[] str;
			String key = "";

			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter("D:\\mcg\\report\\tmp.txt", true));
				// new FileWriter("D:\\mcg\\report\\tmp.txt", true);

			} catch (IOException e1) {
				e1.printStackTrace();
			}

			while ((s = reader.readLine()) != null) {
				if (s.startsWith("#"))
					continue;
				str = s.split("\t");
				key = str[1];
				str[8] = str[8].split(":")[0];
				// str[1] = new String(str[1].getBytes(), "utf-8");

				writer.write(str[0] + "\t" + urls.get(key.replace(" ", "%20")) + "\t");
				for (int i = 1; i < str.length; i++)
					writer.write(str[i] + "\t");
				writer.write('\n');
			}
			writer.close();
			reader.close();

		} catch (IOException ex) {

		}
	}

	public static String getToday() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		String date = "" + year + "-" + month + "-" + day;
		
		return date;
	}
	
	/**
	 * 重新构造url，减少关键词
	 * @param url
	 * @return
	 */
	public static String ReconstructURL(String url) {
		url = url.replaceFirst("%20", "#");
		url = url.replaceAll("%20.*?&", "&");
		url = url.replaceFirst("#", "%20");
		return url;
	}
	
	public static void autoTaskFromDB() {
		// 每隔5个小时从数据库取新的线索自动测试
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				DBCollection coll = ICTMongoClient.getCollection("NewsCertify", "newsclues");
				try {
					DBObject obj = coll.findOne(new BasicDBObject("isread", 0), new BasicDBObject(), new BasicDBObject("news_time", -1));
					int ret = sendOneUrl(obj);
					System.out.println("ret=" + ret);
					if (ret == 1) { // 搜索无结果，重新修改URL
						obj.put("url", ReconstructURL(obj.get("url").toString()));
						int ret2 = sendOneUrl(obj);
						System.out.println("ret2=" + ret2);
					}
					obj.put("isread", 1);
					coll.save(obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0, 2 * 60 * 1000);
	}
	
	public static JSONArray getWeiboNewslist(List<String> trends) {
		String today = getToday();
		JSONArray newslist = new JSONArray();
		for (String keyword : trends) {
			keyword = keyword.trim().replace(" ", "%20");
			JSONObject obj = new JSONObject();
			String url = "/"+ParamUtil.WEBAPP_NAME+"/AnalyzeEvent?k=" + keyword + "&s=" + today + "&e=" + today;
			obj.element("title", keyword);
			obj.element("url", url);
			newslist.add(obj);
		}
		return newslist;
	}
	
	public static void main(String[] args) {
		// addTitle();
		// sendUrls("newslist_Sep.json", 0);
		
		// scheduleTask();
		// autoTask();

		autoTaskFromDB();
	}
}
