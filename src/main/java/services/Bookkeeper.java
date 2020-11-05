package services;

import constants.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Bookkeeper {
    private static Logger LOG = LoggerFactory.getLogger(Bookkeeper.class);

    //TODO: Ensure the nested map is thread safe
    private final Map<Exchange, Map<CurrencyPair, OrderBook>> orderBooks = new ConcurrentHashMap<>();

    public Bookkeeper() {
    }

    public void upsertOrderBook(Exchange exchange, CurrencyPair currencyPair, OrderBook orderBook) {
        LOG.info("This is my upsert thread");
        orderBooks.computeIfAbsent(exchange, (k) -> {
            return new HashMap<>();
        });
        orderBooks.get(exchange).put(currencyPair, orderBook);
    }

    public OrderBook getOrderBook(Exchange exchange, CurrencyPair currencyPair) {
        LOG.info("This is my get thread");
        return orderBooks.get(exchange).get(currencyPair);
    }
}
