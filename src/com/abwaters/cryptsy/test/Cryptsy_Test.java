package com.abwaters.cryptsy.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;
import com.abwaters.cryptsy.Cryptsy.Currencies;
import com.abwaters.cryptsy.Cryptsy.DepthReturn;
import com.abwaters.cryptsy.Cryptsy.FeeReturn;
import com.abwaters.cryptsy.Cryptsy.InfoReturn;
import com.abwaters.cryptsy.Cryptsy.Market;
import com.abwaters.cryptsy.Cryptsy.Markets;
import com.abwaters.cryptsy.Cryptsy.Order;
import com.abwaters.cryptsy.Cryptsy.OrderTypes;
import com.abwaters.cryptsy.Cryptsy.PriceQuantity;
import com.abwaters.cryptsy.Cryptsy.PublicMarket;
import com.abwaters.cryptsy.Cryptsy.PublicTrade;
import com.abwaters.cryptsy.Cryptsy.Trade;

public class Cryptsy_Test {

	private Cryptsy cryptsy;

	private static Properties load(File pfile) throws Exception {
		FileInputStream pfs = new FileInputStream(pfile.getAbsoluteFile());
		Properties properties = new Properties();
		properties.load(pfs);
		return properties;
	}

	@Before
	public void setUp() throws Exception {
		// Note: Keys below do not have trade or withdraw permissions...only
		// info
		String userdir = System.getProperty("user.dir");
		Properties p = load(new File(userdir, "config.properties"));
		String key = p.getProperty("cryptsy.key");
		String secret = p.getProperty("cryptsy.secret");
		int request_limit = Integer.parseInt(p
				.getProperty("cryptsy.request_limit"));
		int auth_request_limit = Integer.parseInt(p
				.getProperty("cryptsy.auth_request_limit"));
		cryptsy = new Cryptsy();
		cryptsy.setAuthKeys(key, secret);
		cryptsy.setAuthRequestLimit(auth_request_limit);
		cryptsy.setRequestLimit(request_limit);
	}

	@Test
	public void testPublicMarketData() throws CryptsyException {
		PublicMarket[] markets = cryptsy.getPublicMarketData();
		for (PublicMarket market : markets) {
			System.out.println(market);
		}
	}

	@Test
	public void testPublicSingleMarketData() throws CryptsyException {
		PublicMarket market = cryptsy.getPublicMarketData(Markets.LTC_BTC);
		System.out.println(market);
		for (PublicTrade trade : market.recenttrades) {
			System.out.println("    " + trade);
		}
	}

	@Test
	public void testGetInfo() throws CryptsyException {
		InfoReturn info = cryptsy.getInfo();
		System.out.println(info);
		for (String currency : info.balances_available.keySet()) {
			double val = info.balances_available.get(currency);
			if (val > 0)
				System.out.println("    Available " + currency + "=" + val);
		}
		for (String currency : info.balances_hold.keySet()) {
			double val = info.balances_hold.get(currency);
			if (val > 0)
				System.out.println("    Hold " + currency + "=" + val);
		}
	}

	@Test
	public void testGetMarkets() throws CryptsyException {
		Market[] markets = cryptsy.getMarkets() ;
		for (Market market : markets) {
			System.out.println(market);
		}
	}

	/* TODO */
	@Test
	public void testGetMyTransactions() throws CryptsyException {
		System.out.println(cryptsy.getMyTransactions()) ;
	}

	/* TODO */
	@Test
	public void testGetMarketTrades() throws CryptsyException {
		System.out.println(cryptsy.getMarketTrades(Markets.WDC_BTC)) ;
	}

	/* TODO */
	@Test
	public void testGetMarketOrders() throws CryptsyException {
		System.out.println(cryptsy.getMarketOrders(Markets.WDC_BTC)) ;
	}

	@Test
	public void testGetMyTrades() throws CryptsyException {
		Trade[] trades = cryptsy.getMyTrades(Markets.WDC_BTC,200) ;
		for (Trade trade : trades) {
			System.out.println("    " + trade);
		}
	}

	@Test
	public void testGetAllMyTrades() throws CryptsyException {
		Trade[] trades = cryptsy.getAllMyTrades() ;
		for (Trade trade : trades) {
			System.out.println("    " + trade);
		}
	}

	@Test
	public void testGetMyOrders() throws CryptsyException {
		Order[] orders = cryptsy.getMyOrders(Markets.DOGE_BTC) ;
		for(Order order:orders) {
			System.out.println(order) ;
		}
	}

	@Test
	public void testGetDepth() throws CryptsyException {
		DepthReturn depth = cryptsy.getDepth(Markets.WDC_BTC) ; 
		System.out.println("Sell Depth") ;
		for(PriceQuantity pq:depth.sell) {
			System.out.println(pq) ; 
		}
		System.out.println("Buy Depth") ;
		for(PriceQuantity pq:depth.buy) {
			System.out.println(pq) ; 
		}
	}

	@Test
	public void testGetAllMyOrders() throws CryptsyException {
		Order[] orders = cryptsy.getAllMyOrders() ;
		for(Order order:orders) {
			System.out.println(order) ;
		}
	}

	@Test
	public void testCreateOrder() throws CryptsyException {
		System.out.println("orderid="+cryptsy.createOrder(Markets.DOGE_BTC,OrderTypes.Sell, 500, 0.000001)) ;
		System.out.println("orderid="+cryptsy.createOrder(Markets.DOGE_BTC,OrderTypes.Sell, 500, 0.000001)) ;
		System.out.println("orderid="+cryptsy.createOrder(Markets.DOGE_BTC,OrderTypes.Sell, 500, 0.000001)) ;
	}

	@Test
	public void testCancelOrder() throws CryptsyException {
		long order_id = 23625768 ;
		System.out.println(cryptsy.cancelOrder(order_id)) ;
	}

	@Test
	public void testCancelMarketOrders() throws CryptsyException {
		long[] orderids = cryptsy.cancelMarketOrders(Markets.DOGE_BTC) ;
		if( orderids == null || orderids.length == 0 ) {
			System.out.println("No orders to cancel.") ;
		}else for(long orderid:orderids) {
			System.out.println("Canceled #"+orderid) ;
		}
	}

	@Test
	public void testCancelAllOrders() throws CryptsyException {
		long[] orderids = cryptsy.cancelAllOrders() ;
		if( orderids == null || orderids.length == 0 ) {
			System.out.println("No orders to cancel.") ;
		}else for(long orderid:orderids) {
			System.out.println("Canceled #"+orderid) ;
		}
	}

	@Test
	public void testCalculateFees() throws CryptsyException {
		FeeReturn buyfee = cryptsy.calculateFees(OrderTypes.Buy, 1, 1) ;
		FeeReturn sellfee = cryptsy.calculateFees(OrderTypes.Sell, 1, 1) ;
		System.out.println("buy="+buyfee) ;
		System.out.println("sell="+sellfee) ;
	}

	/* TODO */
	@Test
	public void testGenerateNewAddress() throws CryptsyException {
		String results = cryptsy.generateNewAddress(Currencies.AlphaCoin) ;
		System.out.println(results) ;
	}
}
