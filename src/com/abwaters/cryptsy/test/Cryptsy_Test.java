package com.abwaters.cryptsy.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;
import com.abwaters.cryptsy.Cryptsy.InfoReturn;
import com.abwaters.cryptsy.Cryptsy.Market;
import com.abwaters.cryptsy.Cryptsy.Markets;
import com.abwaters.cryptsy.Cryptsy.PublicMarket;
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
		for (Trade trade : market.recenttrades) {
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

	@Test
	public void testGetMyTransactions() throws CryptsyException {
		System.out.println(cryptsy.getMyTransactions()) ;
	}

	@Test
	public void testGetMarketTrades() throws CryptsyException {
		System.out.println(cryptsy.getMarketTrades()) ;
	}

	@Test
	public void testGetMarketOrders() throws CryptsyException {
		System.out.println(cryptsy.getMarketOrders()) ;
	}

	@Test
	public void testGetMyTrades() throws CryptsyException {
		System.out.println(cryptsy.getMyTrades()) ;
	}

	@Test
	public void testGetAllMyTrades() throws CryptsyException {
		System.out.println(cryptsy.getAllMyTrades()) ;
	}

	@Test
	public void testGetMyOrders() throws CryptsyException {
		System.out.println(cryptsy.getMyOrders()) ;
	}

	@Test
	public void testGetDepth() throws CryptsyException {
		System.out.println(cryptsy.getDepth()) ;
	}

	@Test
	public void testGetAllMyOrders() throws CryptsyException {
		System.out.println(cryptsy.getAllMyOrders()) ;
	}

	@Test
	public void testCreateOrder() throws CryptsyException {
	}

	@Test
	public void testCancelOrder() throws CryptsyException {
	}

	@Test
	public void testCancelMarketOrders() throws CryptsyException {
	}

	@Test
	public void testCancelAllOrders() throws CryptsyException {
	}

	@Test
	public void testCalculateFees() throws CryptsyException {
	}

	@Test
	public void testGenerateNewAddress() throws CryptsyException {
	}
}
