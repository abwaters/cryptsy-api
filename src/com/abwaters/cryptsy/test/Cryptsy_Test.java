package com.abwaters.cryptsy.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;
import com.abwaters.cryptsy.Cryptsy.Market;
import com.abwaters.cryptsy.Cryptsy.Markets;
import com.abwaters.cryptsy.Cryptsy.Trade;

public class Cryptsy_Test {

	private Cryptsy cryptsy ;
	
	private static Properties load(File pfile) throws Exception {
		FileInputStream pfs = new FileInputStream(pfile.getAbsoluteFile()) ;
		Properties properties = new Properties() ;
		properties.load(pfs) ;
		return properties ;
	}
	
	@Before
	public void setUp() throws Exception {
		// Note: Keys below do not have trade or withdraw permissions...only info
		String userdir = System.getProperty("user.dir") ;
		Properties p = load(new File(userdir,"config.properties")) ;
		String key = p.getProperty("cryptsy.key") ;
		String secret = p.getProperty("cryptsy.secret") ;
		int request_limit = Integer.parseInt(p.getProperty("cryptsy.request_limit")) ;
		int auth_request_limit = Integer.parseInt(p.getProperty("cryptsy.auth_request_limit")) ;
		cryptsy = new Cryptsy() ;
		cryptsy.setAuthKeys(key, secret) ;
		cryptsy.setAuthRequestLimit(auth_request_limit) ;
		cryptsy.setRequestLimit(request_limit) ;
	}

	@Test
	public void testMarketData() throws CryptsyException {
		Market[] markets = cryptsy.getMarketData() ;
		for(Market market:markets) {
			System.out.println(market) ;
		}
	}
	
	@Test
	public void testSingleMarketData() throws CryptsyException {
		Market market = cryptsy.getMarketData(Markets.LTC_BTC) ;
		System.out.println(market) ;
		for(Trade trade:market.recenttrades) {
			System.out.println(trade) ;
		}
	}
	
}
