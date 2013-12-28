package com.abwaters.cryptsy.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import sun.util.resources.CurrencyNames;

import com.abwaters.cryptsy.Cryptsy;
import com.abwaters.cryptsy.Cryptsy.CryptsyException;
import com.abwaters.cryptsy.Cryptsy.Currencies;
import com.abwaters.cryptsy.Cryptsy.DepthReturn;
import com.abwaters.cryptsy.Cryptsy.FeeReturn;
import com.abwaters.cryptsy.Cryptsy.InfoReturn;
import com.abwaters.cryptsy.Cryptsy.Market;
import com.abwaters.cryptsy.Cryptsy.MarketBuyOrder;
import com.abwaters.cryptsy.Cryptsy.MarketOrderReturn;
import com.abwaters.cryptsy.Cryptsy.MarketSellOrder;
import com.abwaters.cryptsy.Cryptsy.Markets;
import com.abwaters.cryptsy.Cryptsy.Order;
import com.abwaters.cryptsy.Cryptsy.OrderTypes;
import com.abwaters.cryptsy.Cryptsy.PriceQuantity;
import com.abwaters.cryptsy.Cryptsy.PublicMarket;
import com.abwaters.cryptsy.Cryptsy.PublicTrade;
import com.abwaters.cryptsy.Cryptsy.Trade;
import com.abwaters.cryptsy.Cryptsy.Transaction;

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
	public void testAllCurrencies() throws CryptsyException {
		Map<String,String> currencies = new HashMap<String,String>() ;
		PublicMarket[] markets = cryptsy.getPublicMarketData();
		for(PublicMarket market:markets) {
			if( !currencies.containsKey(market.primarycode) )
				currencies.put(market.primarycode,market.primaryname) ;
			else if( !currencies.containsKey(market.secondarycode) )
				currencies.put(market.secondarycode,market.secondaryname) ;
		}
		for(String currency:currencies.keySet()) {
			System.out.println("CurrencyNames.put(\""+currency+"\",\""+currencies.get(currency)+"\") ;") ;
		}
	}
	
	@Test
	public void testAccountAddresses() throws CryptsyException {
		PublicMarket[] markets = cryptsy.getPublicMarketData();
		Transaction[] txns = cryptsy.getMyTransactions() ;
		Map<String,String> addresses = new HashMap<String,String>() ;
		for(Transaction tx:txns) {
			if( tx.type.equalsIgnoreCase("Deposit") ) {
				if( !addresses.containsKey(tx.currency) ) addresses.put(tx.currency, tx.address) ;
			}
		}
		InfoReturn info = cryptsy.getInfo();
		for (String currency : info.balances_available.keySet()) {
			if( !currency.equalsIgnoreCase("points") ) {
				double val = info.balances_available.get(currency);
				if( !addresses.containsKey(currency) ) {
					try {
						String address = cryptsy.generateNewAddress(currency) ;
						addresses.put(currency, address) ;
					}catch(Exception e) {
						System.out.println("Error generating address for "+currency) ;
					}
				}
			}
		}
		for(String currency:addresses.keySet()) {
			String currencyName = Cryptsy.CurrencyNames.get(currency) ;
			System.out.println(currencyName+" ("+currency+"): "+addresses.get(currency)) ;
		}
	}
	
	@Test
	public void testAccountTotalBTC() throws CryptsyException {
		double total = 0 ;
		PublicMarket[] markets = cryptsy.getPublicMarketData();
		InfoReturn info = cryptsy.getInfo();
		System.out.println(info) ;
		for (String currency : info.balances_available.keySet()) {
			double val = info.balances_available.get(currency);
			if (val > 0) {
				double btc_val = 0 ;
				if( currency.equalsIgnoreCase(Currencies.BitCoin) ) btc_val = val ;
				else{
					for(PublicMarket market:markets) { 
						if( market.primarycode.equalsIgnoreCase(currency) && market.secondarycode.equalsIgnoreCase(Currencies.BitCoin) ) {
							btc_val = market.recenttrades[0].price ; 
						}
					}
				}
				System.out.println("    Available " + currency + "=" + val + " BTC val="+btc_val) ;
				total += btc_val ;
			}
		}
		System.out.println("Account Total="+total+" BTC") ;
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

	@Test
	public void testGetMyTransactions() throws CryptsyException {
		Transaction[] transactions = cryptsy.getMyTransactions() ;
		for(Transaction t:transactions) {
			System.out.println(t) ;
		}
	}

	@Test
	public void testGetMarketTrades() throws CryptsyException {
		Trade[] trades = cryptsy.getMarketTrades(Markets.DOGE_BTC) ;
		for (Trade trade : trades) {
			System.out.println("    " + trade);
		}
	}

	@Test
	public void testGetMarketOrders() throws CryptsyException {
		MarketOrderReturn mo = cryptsy.getMarketOrders(Markets.WDC_BTC) ;
		System.out.println("Sell Orders") ;
		for(MarketSellOrder so:mo.sellorders) {
			System.out.println(so) ; 
		}
		
		System.out.println("Buy Orders") ;
		for(MarketBuyOrder bo:mo.buyorders) {
			System.out.println(bo) ; 
		}

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

	@Test
	public void testGenerateNewAddress() throws CryptsyException {
		String results = cryptsy.generateNewAddress(Currencies.AlphaCoin) ;
		System.out.println(results) ;
	}
}
