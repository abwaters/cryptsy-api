package com.abwaters.cryptsy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

public class Cryptsy {

	private static final String USER_AGENT = "Mozilla/5.0 (compatible; CRYPTSY-API/1.0; MSIE 6.0 compatible; +https://github.com/abwaters/cryptsy-api)";
	private static final String TICKER_TRADES_URL = "https://btc-e.com/api/2/";
	private static final String PUB_API_URL = "http://pubapi.cryptsy.com/api.php";
	private static final String AUTH_API_URL = "https://www.cryptsy.com/api";

	private static long auth_last_request = 0;
	private static long auth_request_limit = 1000; // request limit in
													// milliseconds
	private static long last_request = 0;
	private static long request_limit = 15000; // request limit in milliseconds
												// for non-auth calls...defaults
												// to 15 seconds
	private static long nonce = 0, last_nonce = 0;

	private boolean initialized = false;
	private String secret, key;
	private Mac mac;
	private Gson gson;

	/**
	 * Constructor
	 */
	public Cryptsy() {
		GsonBuilder gson_builder = new GsonBuilder();
		gson_builder.registerTypeAdapter(PublicMarketDataReturn.class,
				new PublicMarketDataReturnDeserializer());
		gson_builder.registerTypeAdapter(Balances.class,
				new BalancesDeserializer());
		gson_builder.registerTypeAdapter(Date.class, new DateDeserializer());
		gson_builder.registerTypeAdapter(DepthReturn.class, new DepthDeserializer());
		gson = gson_builder.create();
		if (nonce == 0)
			nonce = System.currentTimeMillis() / 1000;
	}

	/*
	 * General Market Data (All Markets): (NEW METHOD)
	 * http://pubapi.cryptsy.com/api.php?method=marketdatav2
	 */
	public PublicMarket[] getPublicMarketData() throws CryptsyException {
		String results = request(PUB_API_URL + "?method=marketdatav2");
		PublicMarketData md = gson.fromJson(results, PublicMarketData.class);
		return md.info.markets;
	}

	/*
	 * General Market Data (Single Market):
	 * http://pubapi.cryptsy.com/api.php?method
	 * =singlemarketdata&marketid={MARKET ID}
	 */
	public PublicMarket getPublicMarketData(int market_id) throws CryptsyException {
		String results = request(PUB_API_URL
				+ "?method=singlemarketdata&marketid=" + market_id);
		PublicMarketData md = gson.fromJson(results, PublicMarketData.class);
		return md.info.markets[0];
	}

	/*
	 * General Orderbook Data (All Markets):
	 * http://pubapi.cryptsy.com/api.php?method=orderdata
	 */
	public String getPublicOrderBook() throws CryptsyException {
		return request(PUB_API_URL + "?method=orderdata");
	}

	/*
	 * General Orderbook Data (Single Market):
	 * http://pubapi.cryptsy.com/api.php?method=singleorderdata&marketid={MARKET
	 * ID}
	 */
	public String getPublicOrderBook(int market_id) throws CryptsyException {
		return request(PUB_API_URL + "?method=singleorderdata&marketid="
				+ market_id);
	}

	public InfoReturn getInfo() throws CryptsyException {
		String results = authrequest("getinfo", null);
		Info info = gson.fromJson(results, Info.class);
		return info.info;
	}

	public Market[] getMarkets() throws CryptsyException {
		String results = authrequest("getmarkets",null) ;
		AccountMarkets acct_market = gson.fromJson(results,AccountMarkets.class) ;
		return acct_market.markets ;
	}

	public Transaction[] getMyTransactions() throws CryptsyException {
		String results = authrequest("mytransactions", null);
		Transactions t = gson.fromJson(results,Transactions.class);
		return t.transactions ;
	}

	public Trade[] getMarketTrades(int market_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		String results = authrequest("markettrades", args);
		Trades trades = gson.fromJson(results,Trades.class) ;
		
		// provide a complete structure by setting the market id
		for(int i=0;i<trades.trades.length;i++) trades.trades[i].marketid = market_id ;
		return trades.trades ;
	}

	public MarketOrderReturn getMarketOrders(int market_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		String results = authrequest("marketorders", args);
		MarketOrder mo = gson.fromJson(results,MarketOrder.class) ;
		return mo.info ; 
	}

	/*
	 * Returns trades from a given market for limit entries.
	 */
	public Trade[] getMyTrades(int market_id,int limit) throws CryptsyException {
		
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		args.put("limit",Integer.toString(limit)) ;
		String results = authrequest("mytrades", args);
		Trades trades = gson.fromJson(results,Trades.class) ;
		
		// provide a complete structure by setting the market id
		for(int i=0;i<trades.trades.length;i++) trades.trades[i].marketid = market_id ;
		return trades.trades ;
	}
	
	/*
	 * Returns all trades for all markets. 
	 */
	public Trade[] getAllMyTrades() throws CryptsyException {
		String results = authrequest("allmytrades", null);
		Trades trades = gson.fromJson(results,Trades.class) ;
		return trades.trades ;
	}

	public Order[] getMyOrders(int market_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		String results = authrequest("myorders", args);
		MyOrders mo = gson.fromJson(results,MyOrders.class) ;
		for(int i=0;i<mo.orders.length;i++) {
			mo.orders[i].marketid = market_id ;
		}
		return mo.orders ;
	}

	public DepthReturn getDepth(int market_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		String results = authrequest("depth", args);
		Depth depth = gson.fromJson(results, Depth.class) ;
		return depth.info ;
	}

	public Order[] getAllMyOrders() throws CryptsyException {
		String results = authrequest("allmyorders", null);
		MyOrders mo = gson.fromJson(results,MyOrders.class) ;
		return mo.orders ;
	}

	/*
	 * order_type - Buy/Sell
	 */
	public long createOrder(int market_id,String order_type,double quantity,double price) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		args.put("ordertype",order_type) ;
		args.put("quantity",Double.toString(quantity)) ;
		args.put("price",Double.toString(price)) ;
		String results = authrequest("createorder", args);
		CreateOrder co = gson.fromJson(results,CreateOrder.class) ;
		return (co.success == 1)?co.orderid:0 ;
	}

	public boolean cancelOrder(long order_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("orderid",Long.toString(order_id)) ;
		String results = authrequest("cancelorder", args);
		StringResults r = gson.fromJson(results,StringResults.class) ;
		return (r.success == 1) ;
	}

	public long[] cancelMarketOrders(int market_id) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("marketid",Integer.toString(market_id)) ;
		String results = authrequest("cancelmarketorders", args);
		StringArrayResults r = gson.fromJson(results,StringArrayResults.class) ;
		long[] orderids = null ;
		if( r.success == 1 && r.info != null ) {
			orderids = new long[r.info.length] ;
			Pattern p = Pattern.compile("(\\d+)");
			for(int i=0;i<orderids.length;i++) {
				Matcher m = p.matcher(r.info[i]) ;
				if( m.find() ) orderids[i] = Long.parseLong(m.group(0)) ;
			}
		}
		return orderids ; 
	}

	public long[] cancelAllOrders() throws CryptsyException {
		String results = authrequest("cancelallorders", null);
		StringArrayResults r = gson.fromJson(results,StringArrayResults.class) ;
		long[] orderids = null ;
		if( r.success == 1 && r.info != null ) {
			orderids = new long[r.info.length] ;
			Pattern p = Pattern.compile("(\\d+)");
			for(int i=0;i<orderids.length;i++) {
				Matcher m = p.matcher(r.info[i]) ;
				if( m.find() ) orderids[i] = Long.parseLong(m.group(0)) ;
			}
		}
		return orderids ; 
	}

	public FeeReturn calculateFees(String order_type,double quantity,double price) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("ordertype",order_type) ;
		args.put("quantity",Double.toString(quantity)) ;
		args.put("price",Double.toString(price)) ;
		String results = authrequest("calculatefees", args);
		CalculateFees fees = gson.fromJson(results, CalculateFees.class) ;
		return fees.info ;
	}

	public String generateNewAddress(String currencycode) throws CryptsyException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("currencycode",currencycode) ;
		String results = authrequest("generatenewaddress", args);
		GenerateAddress adr = gson.fromJson(results,GenerateAddress.class) ;
		return adr.info.address ;
	}

	/**
	 * Limits how frequently calls to the open API for trade history and tickers
	 * can be made. If calls are attempted more frequently, the thread making
	 * the call is put to sleep for the duration of the time left before the
	 * limit is reached.
	 * <p>
	 * This is set to 15 second based on a forum post indicating that support
	 * specified this limit. <a href=
	 * "https://bitcointalk.org/index.php?topic=127553.msg1764391#msg1764391"
	 * >Forum post can be read here</a>.
	 * 
	 * @param request_limit
	 *            call limit in milliseconds
	 */
	public void setRequestLimit(long request_limit) {
		Cryptsy.request_limit = request_limit;
	}

	/**
	 * Limits how frequently calls to the authenticated BTCE can be made. If
	 * calls are attempted more frequently, the thread making the call is put to
	 * sleep for the duration of the time left before the limit is reached.
	 * <p>
	 * This is set to 1 second on the assumption that authenticated (calls using
	 * keys) are made infrequently and should receive priority.
	 * 
	 * @param auth_request_limit
	 *            call limit in milliseconds
	 */
	public void setAuthRequestLimit(long auth_request_limit) {
		Cryptsy.auth_request_limit = auth_request_limit;
	}

	/**
	 * Sets the account API keys to use for calling methods that require access
	 * to a BTC-E account.
	 * 
	 * @param key
	 *            the key obtained from Profile->API Keys in your BTC-E account.
	 * @param secret
	 *            the secret obtained from Profile->API Keys in your BTC-E
	 *            account.
	 */
	public void setAuthKeys(String key, String secret) throws CryptsyException {
		this.key = key;
		this.secret = secret;
		SecretKeySpec keyspec = null;
		try {
			keyspec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
		} catch (UnsupportedEncodingException uee) {
			throw new CryptsyException(
					"HMAC-SHA512 doesn't seem to be installed", uee);
		}

		try {
			mac = Mac.getInstance("HmacSHA512");
		} catch (NoSuchAlgorithmException nsae) {
			throw new CryptsyException(
					"HMAC-SHA512 doesn't seem to be installed", nsae);
		}

		try {
			mac.init(keyspec);
		} catch (InvalidKeyException ike) {
			throw new CryptsyException("Invalid key for signing request", ike);
		}
		initialized = true;
	}

	private final void preCall() {
		while (nonce == last_nonce)
			nonce++;
		long elapsed = System.currentTimeMillis() - last_request;
		if (elapsed < request_limit) {
			try {
				Thread.currentThread().sleep(request_limit - elapsed);
			} catch (InterruptedException e) {

			}
		}
		last_request = System.currentTimeMillis();
	}

	private final String request(String urlstr) throws CryptsyException {

		// handle precall logic
		preCall();

		// create connection
		URLConnection conn = null;
		StringBuffer response = new StringBuffer();
		try {
			URL url = new URL(urlstr);
			conn = url.openConnection();
			conn.setUseCaches(false);
			conn.setRequestProperty("User-Agent", USER_AGENT);

			// read response
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null)
				response.append(line);
			in.close();
		} catch (MalformedURLException e) {
			throw new CryptsyException("Internal error.", e);
		} catch (IOException e) {
			throw new CryptsyException("Error connecting to Cryptsy.", e);
		}
		return response.toString();
	}

	private final void preAuth() {
		while (nonce == last_nonce)
			nonce++;
		long elapsed = System.currentTimeMillis() - auth_last_request;
		if (elapsed < auth_request_limit) {
			try {
				Thread.currentThread().sleep(auth_request_limit - elapsed);
			} catch (InterruptedException e) {

			}
		}
		auth_last_request = System.currentTimeMillis();
	}

	private final String authrequest(String method, Map<String, String> args)
			throws CryptsyException {
		if (!initialized)
			throw new CryptsyException("Cryptsy not initialized.");

		// prep the call
		preAuth();

		// add method and nonce to args
		if (args == null)
			args = new HashMap<String, String>();
		args.put("method", method);
		args.put("nonce", Long.toString(nonce));
		last_nonce = nonce;

		// create url form encoded post data
		String postData = "";
		for (Iterator<String> iter = args.keySet().iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (postData.length() > 0)
				postData += "&";
			postData += arg + "=" + URLEncoder.encode(args.get(arg));
		}

		// create connection
		URLConnection conn = null;
		StringBuffer response = new StringBuffer();
		try {
			URL url = new URL(AUTH_API_URL);
			conn = url.openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Key", key);
			conn.setRequestProperty("Sign",
					toHex(mac.doFinal(postData.getBytes("UTF-8"))));
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			conn.setRequestProperty("User-Agent", USER_AGENT);

			// write post data
			OutputStreamWriter out = new OutputStreamWriter(
					conn.getOutputStream());
			out.write(postData);
			out.close();

			// read response
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null)
				response.append(line);
			in.close();
		} catch (MalformedURLException e) {
			throw new CryptsyException("Internal error.", e);
		} catch (IOException e) {
			throw new CryptsyException("Error connecting to Cryptsy.", e);
		}
		return response.toString();
	}

	private String toHex(byte[] b) throws UnsupportedEncodingException {
		return String.format("%040x", new BigInteger(1, b));
	}

	/*
	 * CryptsyBase
	 */
	public static class Results {
		public int success;
		public String error;

		public Results() {
			error = "";
		}
	}
	
	public static class StringResults extends Results {
		@SerializedName("return")
		public String info ;
	}

	public static class StringArrayResults extends Results {
		@SerializedName("return")
		public String[] info ;
	}
	
	public static class CalculateFees extends Results {
		@SerializedName("return")
		public FeeReturn info ;
	}
	
	public static class FeeReturn {
		public double fee ;
		public double net ;
		
		@Override
		public String toString() {
			return "FeeReturn [fee=" + fee + ", net=" + net + "]";
		}
	}

	public static class GenerateAddress extends Results {
		@SerializedName("return")
		public AddressReturn info ;
	}
	
	public static class AddressReturn {
		public String address ;
	}
	
	public static class Transactions extends Results {
		@SerializedName("return")
		public Transaction[] transactions ;
	}
	
	public static class Transaction {
		public String currency ;
		public long timestamp ;
		public Date datetime ;
		public String timezone ;
		public String type ;
		public String address ;
		public double amount ;
		public double fee ;
		public String trxid ;
		
		@Override
		public String toString() {
			return "Transaction [currency=" + currency + ", timestamp="
					+ timestamp + ", datetime=" + datetime + ", timezone="
					+ timezone + ", type=" + type + ", address=" + address
					+ ", amount=" + amount + ", fee=" + fee + ", trxid="
					+ trxid + "]";
		}
		
	}
	
	public static class PublicMarketData extends Results {
		@SerializedName("return")
		public PublicMarketDataReturn info;
	}

	public static class PublicMarketDataReturn {
		public PublicMarket[] markets;
	}

	private static class AccountMarkets extends Results {
		@SerializedName("return")
		public Market[] markets ;
	}

	/*
	 * 
	 */
	public static class Trades extends Results {
		@SerializedName("return")
		public Trade[] trades ;
	}
	
	public static class Balances extends HashMap<String, Double> {
	}

	public static class PriceQuantity {
		public double price ;
		public double quantity ;
		@Override
		public String toString() {
			return "PriceQuantity [price=" + price + ", quantity=" + quantity
					+ "]";
		}
	}
	
	public static class Depth extends Results {
		@SerializedName("return")
		public DepthReturn info ;
	}
	
	public static class DepthReturn {
		public PriceQuantity[] sell ;
		public PriceQuantity[] buy ;
	}
	
	public static class MarketOrder extends Results {
		@SerializedName("return")
		public MarketOrderReturn info ;

		@Override
		public String toString() {
			return "MarketOrder [info=" + info + "]";
		}
	}
	
	public static class MarketOrderReturn {
		public MarketSellOrder[] sellorders ;
		public MarketBuyOrder[] buyorders ;
		@Override
		public String toString() {
			return "MarketOrderReturn [sellorders="
					+ Arrays.toString(sellorders) + ", buyorders="
					+ Arrays.toString(buyorders) + "]";
		}
	}
	
	public static class MarketBuyOrder {
		public double buyprice ;
		public double quantity ;
		public double total ;
		
		@Override
		public String toString() {
			return "MarketBuyOrder [buyprice=" + buyprice + ", quantity="
					+ quantity + ", total=" + total + "]";
		}
	}
	
	public static class MarketSellOrder {
		public double sellprice ;
		public double quantity ;
		public double total ;
		
		@Override
		public String toString() {
			return "MarketSellOrder [sellprice=" + sellprice + ", quantity="
					+ quantity + ", total=" + total + "]";
		}
	}

	public static class MyOrders extends Results {
		@SerializedName("return")
		public Order[] orders ;

		@Override
		public String toString() {
			return "MyOrders [orders=" + Arrays.toString(orders) + "]";
		}
	}
	
	public static class CreateOrder extends Results {
		public long orderid ;
		public String moreinfo ;
	}
	
	public static class Order {
		public long orderid ;
		public Date created ;
		public int marketid ;
		public String ordertype ;
		public double price ;
		public double quantity ;
		public double orig_quantity ;
		public double total ;
		
		@Override
		public String toString() {
			return "Order [orderid=" + orderid + ", created=" + created
					+ ", marketid=" + marketid + ", ordertype=" + ordertype
					+ ", price=" + price + ", quantity=" + quantity
					+ ", orig_quantity=" + orig_quantity + ", total=" + total
					+ "]";
		}
	}
	
	public static class Info extends Results {
		@SerializedName("return")
		public InfoReturn info;
	}

	public static class InfoReturn {
		public Balances balances_available;
		public Balances balances_hold;
		public long servertimestamp;
		public String servertimezone;
		public Date serverdatetime;
		public int openordercount;

		@Override
		public String toString() {
			return "InfoReturn [servertimestamp=" + servertimestamp
					+ ", servertimezone=" + servertimezone
					+ ", serverdatetime=" + serverdatetime
					+ ", openordercount=" + openordercount + "]";
		}
	}

	/**
	 * 
	 */
	private class PublicMarketDataReturnDeserializer implements
			JsonDeserializer<PublicMarketDataReturn> {
		public PublicMarketDataReturn deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			PublicMarketDataReturn mdr = new PublicMarketDataReturn();
			JsonObject mobj = json.getAsJsonObject().getAsJsonObject("markets");
			List<PublicMarket> markets = new ArrayList<PublicMarket>();
			Iterator<Entry<String, JsonElement>> iter = mobj.entrySet()
					.iterator();
			while (iter.hasNext()) {
				Entry<String, JsonElement> jsonOrder = iter.next();
				PublicMarket market = context.deserialize(jsonOrder.getValue(),
						PublicMarket.class);
				markets.add(market);
			}
			mdr.markets = markets.toArray(new PublicMarket[0]);
			return mdr;
		}
	}

	/**
	 * 
	 */
	private class BalancesDeserializer implements JsonDeserializer<Balances> {
		public Balances deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			Balances balances = new Balances();
			if (json.isJsonObject()) {
				JsonObject o = json.getAsJsonObject();
				List<Market> markets = new ArrayList<Market>();
				Iterator<Entry<String, JsonElement>> iter = o.entrySet()
						.iterator();
				while (iter.hasNext()) {
					Entry<String, JsonElement> jsonOrder = iter.next();
					String currency = jsonOrder.getKey();
					double balance = context.deserialize(jsonOrder.getValue(),
							Double.class);
					balances.put(currency, balance);
				}
			}
			return balances;
		}
	}

	/**
	 * 
	 */
	private class DateDeserializer implements JsonDeserializer<Date> {
		private SimpleDateFormat df = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		public Date deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			Date dt = null;
			if (json.isJsonPrimitive())
				try {
					dt = df.parse(json.getAsString());
				} catch (ParseException e) {
					dt = null;
				}
			return dt;
		}
	}

	/**
	 * 
	 */
	private class DepthDeserializer implements JsonDeserializer<DepthReturn> {
		public DepthReturn deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			DepthReturn depth = new DepthReturn() ;
			if (json.isJsonObject() ) {
				JsonObject o = json.getAsJsonObject() ;
				JsonArray asell = o.getAsJsonArray("sell") ;
				JsonArray abuy = o.getAsJsonArray("buy") ;
				depth.sell = convertToPriceQuantityArray(asell) ;
				depth.buy = convertToPriceQuantityArray(abuy) ;
			}
			return depth ;
		}
	}
	
	private PriceQuantity[] convertToPriceQuantityArray(JsonArray pqa) {
		List<PriceQuantity> pqlist = new ArrayList<PriceQuantity>() ;
		int cnt = pqa.size();
		for(int i=0;i<cnt;i++) {
			PriceQuantity pq = new PriceQuantity() ;
			JsonArray va = pqa.get(i).getAsJsonArray() ;
			pq.price = va.get(0).getAsDouble() ;
			pq.quantity = va.get(1).getAsDouble() ;
			pqlist.add(pq) ;
		}
		return pqlist.toArray(new PriceQuantity[0]) ;
	}

	/*
	 * Trade data
	 */
	public static class PublicTrade {
		public long id;
		public Date time;
		public double price;
		public double quantity;
		public double total;

		@Override
		public String toString() {
			return "Trade [id=" + id + ", time=" + time + ", price=" + price
					+ ", quantity=" + quantity + ", total=" + total + "]";
		}
	}

	/*
	 * Market data
	 */
	public static class Market {
		public int marketid ;
		public String label ;
		public String primary_currency_code ;
		public String primary_currency_name ;
		public String secondary_currency_code ;
		public String secondary_currency_name ;
		public double current_volume ;
		public double last_trade ;
		public double high_trade ;
		public double low_trade ;
		public Date created ;
		
		@Override
		public String toString() {
			return "Market [marketid=" + marketid + ", label=" + label
					+ ", primary_currency_code=" + primary_currency_code
					+ ", primary_currency_name=" + primary_currency_name
					+ ", secondary_currency_code=" + secondary_currency_code
					+ ", secondary_currency_name=" + secondary_currency_name
					+ ", current_volume=" + current_volume + ", last_trade="
					+ last_trade + ", high_trade=" + high_trade
					+ ", low_trade=" + low_trade + ", created=" + created + "]";
		}
	}
	
	/*
	 * PublicMarket data
	 */
	public static class PublicMarket {
		public String marketid;
		public String label;
		public double lasttradeprice;
		public double volume;
		public Date lasttradetime;
		public String primaryname;
		public String primarycode;
		public String secondaryname;
		public String secondarycode;
		public PublicTrade[] recenttrades;

		@Override
		public String toString() {
			return "PublicMarket [lasttradetime=" + lasttradetime + ", marketid="
					+ marketid + ", label=" + label + ", primaryname="
					+ primaryname + ", primarycode=" + primarycode
					+ ", secondaryname=" + secondaryname + ", secondarycode="
					+ secondarycode + "]";
		}
	}

	/*
	 * 
	 */
	public static class Trade {
		public long tradeid ;
		public String tradetype ;
		public Date datetime ;
		public int marketid ;
		public double tradeprice ;
		public double quantity ;
		public double fee ;
		public double total ;
		public String initiate_ordertype ;
		public long order_id ;
		@Override
		public String toString() {
			return "Trade [tradeid=" + tradeid + ", tradetype=" + tradetype
					+ ", datetime=" + datetime + ", marketid=" + marketid
					+ ", tradeprice=" + tradeprice + ", quantity=" + quantity
					+ ", fee=" + fee + ", total=" + total
					+ ", initiate_ordertype=" + initiate_ordertype
					+ ", order_id=" + order_id + "]";
		}
		
	}

	/*
	 * 
	 */
	public static class OrderTypes {
		public static final String Sell = "Sell" ;
		public static final String Buy = "Buy" ;
	}
	
	/*
	 * Currencies
	 */
	public static class Currencies {
		public static final String _42Coin = "42" ;
		public static final String AlphaCoin = "ALF" ;
		public static final String AmericanCoin = "AMC" ;
		public static final String AnonCoin = "ANC" ;
		public static final String Argentum = "ARG" ;
		public static final String AsicCoin = "ASC" ;
		public static final String BBQCoin = "BQC" ;
		public static final String BattleCoin = "BCX" ;
		public static final String Betacoin = "BET" ;
		public static final String BitBar = "BTB" ;
		public static final String BitCoin = "BTC" ;
		public static final String BitGem = "BTG" ;
		public static final String BottleCaps = "CAP" ;
		public static final String ByteCoin = "BTE" ;
		public static final String CHNCoin = "CNC" ;
		public static final String CasinoCoin = "CSC" ;
		public static final String CatCoin = "CAT" ;
		public static final String ColossusCoin = "COL" ;
		public static final String CopperBars = "CPR" ;
		public static final String CopperLark = "CLR" ;
		public static final String Cosmoscoin = "CMC" ;
		public static final String CraftCoin = "CRC" ;
		public static final String CryptoBuck = "BUK" ;
		public static final String CryptogenicBullion = "CGB" ;
		public static final String CryptsyPoints = "Points" ;
		public static final String DevCoin = "DVC" ;
		public static final String Diamond = "DMD" ;
		public static final String DigitalCoin = "DGC" ;
		public static final String Dogecoin = "DOGE" ;
		public static final String Doubloons = "DBL" ;
		public static final String EZCoin = "EZC" ;
		public static final String EarthCoin = "EAC" ;
		public static final String ElaCoin = "ELC" ;
		public static final String ElephantCoin = "ELP" ;
		public static final String Emerald = "EMD" ;
		public static final String FastCoin = "FST" ;
		public static final String FeatherCoin = "FTC" ;
		public static final String FedoraCoin = "TIPS" ;
		public static final String FireflyCoin = "FFC" ;
		public static final String FlorinCoin = "FLO" ;
		public static final String Franko = "FRK" ;
		public static final String FreiCoin = "FRC" ;
		public static final String Galaxycoin = "GLX" ;
		public static final String GameCoin = "GME" ;
		public static final String Globalcoin = "GLC" ;
		public static final String GoldCoin = "GLD" ;
		public static final String GrandCoin = "GDC" ;
		public static final String HoboNickels = "HBN" ;
		public static final String IXCoin = "IXC" ;
		public static final String InfiniteCoin = "IFC" ;
		public static final String JouleCoin = "XJO" ;
		public static final String JunkCoin = "JKC" ;
		public static final String KrugerCoin = "KGC" ;
		public static final String LiteCoin = "LTC" ;
		public static final String LottoCoin = "LOT" ;
		public static final String Lucky7Coin = "LK7" ;
		public static final String LuckyCoin = "LKY" ;
		public static final String MasterCoin = "MST" ;
		public static final String MegaCoin = "MEC" ;
		public static final String MemeCoin = "MEM" ;
		public static final String MinCoin = "MNC" ;
		public static final String MoonCoin = "MOON" ;
		public static final String NameCoin = "NMC" ;
		public static final String NanoToken = "NAN" ;
		public static final String NeoCoin = "NEC" ;
		public static final String Netcoin = "NET" ;
		public static final String Nibble = "NBL" ;
		public static final String NoirBits = "NRB" ;
		public static final String NovaCoin = "NVC" ;
		public static final String OpenSourceCoin = "OSC" ;
		public static final String Orbitcoin = "ORB" ;
		public static final String PayCoin = "PYC" ;
		public static final String Peercoin = "PPC" ;
		public static final String Pennies = "CENT" ;
		public static final String PhilosopherStone = "PHS" ;
		public static final String PhoenixCoin = "PXC" ;
		public static final String PrimeCoin = "XPM" ;
		public static final String ProtoShares = "PTS" ;
		public static final String Quark = "QRK" ;
		public static final String RedCoin = "RED" ;
		public static final String RonPaulCoin = "RPC" ;
		public static final String RoyalCoin = "RYC" ;
		public static final String SecureCoin = "SRC" ;
		public static final String SexCoin = "SXC" ;
		public static final String Spots = "SPT" ;
		public static final String StableCoin = "SBC" ;
		public static final String StarCoin = "STR" ;
		public static final String TagCoin = "TAG" ;
		public static final String TekCoin = "TEK" ;
		public static final String TerraCoin = "TRC" ;
		public static final String Tickets = "TIX" ;
		public static final String TigerCoin = "TGC" ;
		public static final String Unobtanium = "UNO" ;
		public static final String WorldCoin = "WDC" ;
		public static final String XenCoin = "XNC" ;
		public static final String YBCoin = "YBC" ;
		public static final String ZetaCoin = "ZET" ;
		public static final String eMark = "DEM" ;
	}

	/*
	 * Market ids
	 */
	public static class Markets {
		public static final int _42_BTC = 141 ;
		public static final int ALF_BTC = 57 ;
		public static final int AMC_BTC = 43 ;
		public static final int ANC_BTC = 66 ;
		public static final int ANC_LTC = 121 ;
		public static final int ARG_BTC = 48 ;
		public static final int ASC_LTC = 111 ;
		public static final int ASC_XPM = 112 ;
		public static final int BCX_BTC = 142 ;
		public static final int BET_BTC = 129 ;
		public static final int BQC_BTC = 10 ;
		public static final int BTB_BTC = 23 ;
		public static final int BTE_BTC = 49 ;
		public static final int BTG_BTC = 50 ;
		public static final int BUK_BTC = 102 ;
		public static final int CAP_BTC = 53 ;
		public static final int CAT_BTC = 136 ;
		public static final int CENT_XPM = 118 ;
		public static final int CGB_BTC = 70 ;
		public static final int CGB_LTC = 123 ;
		public static final int CLR_BTC = 95 ;
		public static final int CMC_BTC = 74 ;
		public static final int CNC_BTC = 8 ;
		public static final int CNC_LTC = 17 ;
		public static final int COL_LTC = 109 ;
		public static final int COL_XPM = 110 ;
		public static final int CPR_LTC = 91 ;
		public static final int CRC_BTC = 58 ;
		public static final int CSC_BTC = 68 ;
		public static final int DBL_LTC = 46 ;
		public static final int DEM_BTC = 131 ;
		public static final int DGC_BTC = 26 ;
		public static final int DGC_LTC = 96 ;
		public static final int DMD_BTC = 72 ;
		public static final int DOGE_BTC = 132 ;
		public static final int DOGE_LTC = 135 ;
		public static final int DVC_BTC = 40 ;
		public static final int DVC_LTC = 52 ;
		public static final int DVC_XPM = 122 ;
		public static final int EAC_BTC = 139 ;
		public static final int ELC_BTC = 12 ;
		public static final int ELP_LTC = 93 ;
		public static final int EMD_BTC = 69 ;
		public static final int EZC_LTC = 55 ;
		public static final int FFC_BTC = 138 ;
		public static final int FLO_LTC = 61 ;
		public static final int FRC_BTC = 39 ;
		public static final int FRK_BTC = 33 ;
		public static final int FST_BTC = 44 ;
		public static final int FST_LTC = 124 ;
		public static final int FTC_BTC = 5 ;
		public static final int GDC_BTC = 82 ;
		public static final int GLC_BTC = 76 ;
		public static final int GLD_BTC = 30 ;
		public static final int GLD_LTC = 36 ;
		public static final int GLX_BTC = 78 ;
		public static final int GME_LTC = 84 ;
		public static final int HBN_BTC = 80 ;
		public static final int IFC_LTC = 60 ;
		public static final int IFC_XPM = 105 ;
		public static final int IXC_BTC = 38 ;
		public static final int JKC_LTC = 35 ;
		public static final int KGC_BTC = 65 ;
		public static final int LK7_BTC = 116 ;
		public static final int LKY_BTC = 34 ;
		public static final int LOT_BTC = 137 ;
		public static final int LTC_BTC = 3 ;
		public static final int MEC_BTC = 45 ;
		public static final int MEC_LTC = 100 ;
		public static final int MEM_LTC = 56 ;
		public static final int MNC_BTC = 7 ;
		public static final int MOON_BTC = 146 ;
		public static final int MOON_LTC = 145 ;
		public static final int MST_LTC = 62 ;
		public static final int NAN_BTC = 64 ;
		public static final int NBL_BTC = 32 ;
		public static final int NEC_BTC = 90 ;
		public static final int NET_BTC = 134 ;
		public static final int NET_LTC = 108 ;
		public static final int NET_XPM = 104 ;
		public static final int NMC_BTC = 29 ;
		public static final int NRB_BTC = 54 ;
		public static final int NVC_BTC = 13 ;
		public static final int ORB_BTC = 75 ;
		public static final int OSC_BTC = 144 ;
		public static final int PHS_BTC = 86 ;
		public static final int POINTS_BTC = 120 ;
		public static final int PPC_BTC = 28 ;
		public static final int PPC_LTC = 125 ;
		public static final int PTS_BTC = 119 ;
		public static final int PXC_BTC = 31 ;
		public static final int PXC_LTC = 101 ;
		public static final int PYC_BTC = 92 ;
		public static final int QRK_BTC = 71 ;
		public static final int QRK_LTC = 126 ;
		public static final int RED_LTC = 87 ;
		public static final int RPC_BTC = 143 ;
		public static final int RYC_LTC = 37 ;
		public static final int SBC_BTC = 51 ;
		public static final int SBC_LTC = 128 ;
		public static final int SPT_BTC = 81 ;
		public static final int SRC_BTC = 88 ;
		public static final int STR_BTC = 83 ;
		public static final int SXC_LTC = 98 ;
		public static final int TAG_BTC = 117 ;
		public static final int TEK_BTC = 114 ;
		public static final int TGC_BTC = 130 ;
		public static final int TIPS_LTC = 147 ;
		public static final int TIX_LTC = 107 ;
		public static final int TIX_XPM = 103 ;
		public static final int TRC_BTC = 27 ;
		public static final int UNO_BTC = 133 ;
		public static final int WDC_BTC = 14 ;
		public static final int WDC_LTC = 21 ;
		public static final int XJO_BTC = 115 ;
		public static final int XNC_LTC = 67 ;
		public static final int XPM_BTC = 63 ;
		public static final int XPM_LTC = 106 ;
		public static final int YBC_BTC = 73 ;
		public static final int ZET_BTC = 85 ;
		public static final int ZET_LTC = 127 ;
	}

	public static Map<String,String> CurrencyNames = new HashMap<String,String>() ;
	static {
		CurrencyNames.put("42","42Coin") ;
		CurrencyNames.put("ALF","AlphaCoin") ;
		CurrencyNames.put("AMC","AmericanCoin") ;
		CurrencyNames.put("ANC","AnonCoin") ;
		CurrencyNames.put("ARG","Argentum") ;
		CurrencyNames.put("ASC","AsicCoin") ;
		CurrencyNames.put("BCX","BattleCoin") ;
		CurrencyNames.put("BET","Betacoin") ;
		CurrencyNames.put("BQC","BBQCoin") ;
		CurrencyNames.put("BTB","BitBar") ;
		CurrencyNames.put("BTC","BitCoin") ;
		CurrencyNames.put("BTE","ByteCoin") ;
		CurrencyNames.put("BTG","BitGem") ;
		CurrencyNames.put("BUK","CryptoBuck") ;
		CurrencyNames.put("CAP","BottleCaps") ;
		CurrencyNames.put("CAT","CatCoin") ;
		CurrencyNames.put("CENT","Pennies") ;
		CurrencyNames.put("CGB","CryptogenicBullion") ;
		CurrencyNames.put("CLR","CopperLark") ;
		CurrencyNames.put("CMC","Cosmoscoin") ;
		CurrencyNames.put("CNC","CHNCoin") ;
		CurrencyNames.put("COL","ColossusCoin") ;
		CurrencyNames.put("CPR","CopperBars") ;
		CurrencyNames.put("CRC","CraftCoin") ;
		CurrencyNames.put("CSC","CasinoCoin") ;
		CurrencyNames.put("DBL","Doubloons") ;
		CurrencyNames.put("DEM","eMark") ;
		CurrencyNames.put("DGC","DigitalCoin") ;
		CurrencyNames.put("DMD","Diamond") ;
		CurrencyNames.put("DOGE","Dogecoin") ;
		CurrencyNames.put("DVC","DevCoin") ;
		CurrencyNames.put("EAC","EarthCoin") ;
		CurrencyNames.put("ELC","ElaCoin") ;
		CurrencyNames.put("ELP","ElephantCoin") ;
		CurrencyNames.put("EMD","Emerald") ;
		CurrencyNames.put("EZC","EZCoin") ;
		CurrencyNames.put("FFC","FireflyCoin") ;
		CurrencyNames.put("FLO","FlorinCoin") ;
		CurrencyNames.put("FRC","FreiCoin") ;
		CurrencyNames.put("FRK","Franko") ;
		CurrencyNames.put("FST","FastCoin") ;
		CurrencyNames.put("FTC","FeatherCoin") ;
		CurrencyNames.put("GDC","GrandCoin") ;
		CurrencyNames.put("GLC","Globalcoin") ;
		CurrencyNames.put("GLD","GoldCoin") ;
		CurrencyNames.put("GLX","Galaxycoin") ;
		CurrencyNames.put("GME","GameCoin") ;
		CurrencyNames.put("HBN","HoboNickels") ;
		CurrencyNames.put("IFC","InfiniteCoin") ;
		CurrencyNames.put("IXC","IXCoin") ;
		CurrencyNames.put("JKC","JunkCoin") ;
		CurrencyNames.put("KGC","KrugerCoin") ;
		CurrencyNames.put("LK7","Lucky7Coin") ;
		CurrencyNames.put("LKY","LuckyCoin") ;
		CurrencyNames.put("LOT","LottoCoin") ;
		CurrencyNames.put("LTC","LiteCoin") ;
		CurrencyNames.put("MEC","MegaCoin") ;
		CurrencyNames.put("MEM","MemeCoin") ;
		CurrencyNames.put("MNC","MinCoin") ;
		CurrencyNames.put("MOON","MoonCoin") ;
		CurrencyNames.put("MST","MasterCoin (Hydro)") ;
		CurrencyNames.put("NAN","NanoToken") ;
		CurrencyNames.put("NBL","Nibble") ;
		CurrencyNames.put("NEC","NeoCoin") ;
		CurrencyNames.put("NET","Netcoin") ;
		CurrencyNames.put("NMC","NameCoin") ;
		CurrencyNames.put("NRB","NoirBits") ;
		CurrencyNames.put("NVC","NovaCoin") ;
		CurrencyNames.put("ORB","Orbitcoin") ;
		CurrencyNames.put("OSC","OpenSourceCoin") ;
		CurrencyNames.put("PHS","PhilosopherStone") ;
		CurrencyNames.put("PPC","Peercoin") ;
		CurrencyNames.put("PTS","ProtoShares") ;
		CurrencyNames.put("PXC","PhoenixCoin") ;
		CurrencyNames.put("PYC","PayCoin") ;
		CurrencyNames.put("Points","CryptsyPoints") ;
		CurrencyNames.put("QRK","Quark") ;
		CurrencyNames.put("RED","RedCoin") ;
		CurrencyNames.put("RPC","RonPaulCoin") ;
		CurrencyNames.put("RYC","RoyalCoin") ;
		CurrencyNames.put("SBC","StableCoin") ;
		CurrencyNames.put("SPT","Spots") ;
		CurrencyNames.put("SRC","SecureCoin") ;
		CurrencyNames.put("STR","StarCoin") ;
		CurrencyNames.put("SXC","SexCoin") ;
		CurrencyNames.put("TAG","TagCoin") ;
		CurrencyNames.put("TEK","TekCoin") ;
		CurrencyNames.put("TGC","TigerCoin") ;
		CurrencyNames.put("TIPS","FedoraCoin") ;
		CurrencyNames.put("TIX","Tickets") ;
		CurrencyNames.put("TRC","TerraCoin") ;
		CurrencyNames.put("UNO","Unobtanium") ;
		CurrencyNames.put("WDC","WorldCoin") ;
		CurrencyNames.put("XJO","JouleCoin") ;
		CurrencyNames.put("XNC","XenCoin") ;
		CurrencyNames.put("XPM","PrimeCoin") ;
		CurrencyNames.put("YBC","YBCoin") ;
		CurrencyNames.put("ZET","ZetaCoin") ;
	}
	
	/**
	 * An exception class specifically for the Cryptsy API. The goal here is to
	 * provide a specific exception class for this API while not losing any of
	 * the details of the inner exceptions.
	 * <p>
	 * This class is just a wrapper for the Exception class.
	 */
	public class CryptsyException extends Exception {

		private static final long serialVersionUID = 1L;

		public CryptsyException(String msg) {
			super(msg);
		}

		public CryptsyException(String msg, Throwable e) {
			super(msg, e);
		}
	}

}
