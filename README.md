# cryptsy-api

Small fast (and complete) Java API for the Cryptsy crypto-currency exchange with minimal dependencies.  The only external library needed to use this API is the [Google Gson Library](https://code.google.com/p/google-gson/).

Note: While the API is complete and functional in its current state, I am refining the naming conventions for the API and adding documentation.  This may require small but critical changes to your code if you upgrade to future versions.

## Installation

Add the file Cryptsy.java to your project and include the Gson jar as a reference.  Thats all thats needed.  The unit tests for this library use a properties file for saving the API keys but thats not a requirement.  The use of the properties file is only a requirement of the unit tests and not the API itself.

## Basic Usage

Create a Cryptsy API object using the following code:

```java
cryptsy = new Cryptsy();
cryptsy.setAuthKeys("<api_key>", "<api_secret>");
```

After this, using the API is as simple as calling the appropriate method off of your `cryptsy` object.  It is useful to look at the Cryptsy_Test.java source code since the unit tests in this file contain sample code for all of the APIs.  I'll be adding a complete javadoc in the near future.

## Examples

### Get account info
Obtain account info including account balances and amounts on hold due to open orders:

```java
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
```

### Get account transactions
Get the account transactions.

```java
Transaction[] transactions = cryptsy.getMyTransactions() ;
for(Transaction t:transactions) {
	System.out.println(t) ;
}
```

### Get market orders for a single market
Returns all the orders (order book) for a single currency market.

```java
MarketOrderReturn mo = cryptsy.getMarketOrders(Markets.WDC_BTC) ;
System.out.println("Sell Orders") ;
for(MarketSellOrder so:mo.sellorders) {
	System.out.println(so) ; 
}

System.out.println("Buy Orders") ;
for(MarketBuyOrder bo:mo.buyorders) {
	System.out.println(bo) ; 
}
```

## Donations

If you use this library please donate to any of the following addresses:

Cryptsy Trade Key: 77c1602534b3d7642bcc24891a9dab6198d2f526  
BitCoin (BTC): 15VgKx3fzYVnv4zEiGci7EfRgBRfRRgwzx  
DevCoin (DVC): 19f6Ba23VSNcGu5Kt3CEmPvhEGaKUeW9Bz  
Dogecoin (DOGE): DDEkLaveBR35Jz8oxcajGdeg1marYfaD22  
LiteCoin (LTC): LSjiTrRZRse6ZhPCwzU5YVr9RETVJsnRQU  
MasterCoin (MST): K75zTNe4kZZAQge51irPRzR1rLZU3VgL59  
NameCoin (NMC): NA1LEncYN4JeUixJDyeXjTyw78U5yJisig  
PrimeCoin (XPM): AUGUWwcqA3sA44BPQz6WXsiyRdGi127Tiw  
TerraCoin (TRC): 1D3TNLzM6f9F31yNw9gS4xnYtzyCdHc6Wm  
ZetaCoin (ZET): ZKmFiwsfiQTyzjatQDX1KuGdP8xfeUGrG3  