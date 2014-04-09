package base;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Home {
	final String ELB_ADDR = "ec2-54-85-169-198.compute-1.amazonaws.com";
	Connection driver;
	DataSource ds;
	//HTable table;

	public Home() {
		try {
			HikariConfig config = new HikariConfig();
			config.setMaximumPoolSize(100);
			config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
			config.addDataSourceProperty("url", "jdbc:mysql://localhost:3306/tweet_db");
			config.addDataSourceProperty("user", "root");
			config.addDataSourceProperty("password", "db15319root");
			config.addDataSourceProperty("cachePrepStmts", true);
			config.addDataSourceProperty("useServerPrepStmts", true	);
			config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
			config.addDataSourceProperty("prepStmtCacheSize", 250);
			
			ds = new HikariDataSource(config);
			
			//HBase config
			//table = new HTable(HBaseConfiguration.create(), Bytes.toBytes("tweets"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param key - userid_timestamp
	 * @return all of the tweets posted by the user at a given time
	 */
	public String getSQLEntries(String key) {
		try {
			Connection con = ds.getConnection();
			PreparedStatement stmt = con.prepareStatement("SELECT tweet_list FROM tweets_q2 WHERE user_time=?");
			stmt.setString(1, key);
			ResultSet set = stmt.executeQuery();
			StringBuffer results = new StringBuffer();

			while (set.next()) {
				results.append(set.getString("tweet_list").replaceAll("_", "\n"));
			}

			set.close();
			stmt.close();
			con.close();
			return results.toString();	
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param userid
	 * @return a String containing all of the users that retweeted the post 
	 */
	public String getRetweets(String userid) {
		try {
			Connection connection = ds.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT retweet_user_list FROM tweets_q3 WHERE user_id=?");
			stmt.setString(1, userid);
			ResultSet rs = stmt.executeQuery();
			StringBuffer results = new StringBuffer();
			
			while(rs.next()) {
				results.append(rs.getString("retweet_user_list").replaceAll("_","\n"));
			}

			rs.close();
			stmt.close();
			connection.close();
			return results.toString();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/*
	public String getHBaseEntries(String key) {		
		Get g = new Get(Bytes.toBytes(key));
		Result r = null;
		try {
			r = table.get(g);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//		try {
		//			table.close();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		byte[] value = r.getValue(Bytes.toBytes("tweetID"), Bytes.toBytes(""));
		String res = Bytes.toString(value);

		res = res.replaceAll("_", "\n");
		return res; 
	}*/

	public static void main(String[] args) {
		final String info = "TeamSYC,8635-0832-4410\n";
		final SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		final Charset utf8 = Charset.forName("UTF-8");
		final Home home = new Home();
		
		Undertow.builder()
		.setWorkerThreads(4096)
		.setIoThreads(Runtime.getRuntime().availableProcessors() * 2)
		.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
		.setBufferSize(1024*16)
		.addHttpListener(80, args[0])
		.setHandler(new HttpHandler() {

			public void handleRequest(final HttpServerExchange exchange) throws Exception {
				char path = exchange.getRequestPath().charAt(2);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

				if (path == '1') {
					exchange.getResponseSender().send(ByteBuffer.wrap(info.concat(
							fmt.format(new Date())).getBytes(utf8)
							), IoCallback.END_EXCHANGE);
				}
				else if (path == '2') {
					Map<String, Deque<String>> queryMap = exchange.getQueryParameters();
					String userid = queryMap.get("userid").getFirst().trim();
					String tweet_time = queryMap.get("tweet_time").getFirst().trim();
					String result = home.getSQLEntries(userid + "_" + tweet_time.replaceAll(" ", "+"));
					//String result = home.getHBaseEntries(userid + "_" + tweet_time.replaceAll(" ", "+"));
					exchange.getResponseSender().send(ByteBuffer.wrap(
							info.concat(result).getBytes(utf8)
							), IoCallback.END_EXCHANGE);
				}
				else if (path == '3') {
					Map<String, Deque<String>> queryMap = exchange.getQueryParameters();
					String userid = queryMap.get("userid").getFirst().trim();
					String result = home.getRetweets(userid);
					exchange.getResponseSender().send(ByteBuffer.wrap(
							info.concat(result).getBytes(utf8)
							), IoCallback.END_EXCHANGE);
				}
			}
		}).build().start();
	}
}