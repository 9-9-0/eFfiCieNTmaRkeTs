package rest;

import config.Configuration;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.service.KrakenAccountService;
import org.knowm.xchange.kraken.service.KrakenMarketDataService;
import org.knowm.xchange.kraken.service.KrakenTradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.MetadataAggregator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static constants.Exchange.KRAKEN;

public class KrakenExchangeRestAPI implements ExchangeRestAPI {
    private static final Logger LOG = LoggerFactory.getLogger(KrakenExchangeRestAPI.class);

    private final constants.Exchange exchangeName = KRAKEN;
    private Exchange exchangeInstance;
    private KrakenAccountService accountService;
    private KrakenTradeService tradeService;
    private KrakenMarketDataService marketDataService;

    private MetadataAggregator metadataAggregator;

    //Cached Info
    Map<CurrencyPair, CurrencyPairMetaData> metadataMap;
    Map<CurrencyPair, Fee> feeMap;
    AccountInfo accountInfo;

    public KrakenExchangeRestAPI(Configuration cfg,
                                   MetadataAggregator metadataAggregator) throws IOException {
        if (cfg.getKrakenConfig().isEnabled()) {
            ExchangeSpecification exSpec = new KrakenExchange().getDefaultExchangeSpecification();

            exSpec.setSecretKey(cfg.getKrakenConfig().getSecretKey());
            exSpec.setApiKey(cfg.getKrakenConfig().getApiKey());

            exchangeInstance = ExchangeFactory.INSTANCE.createExchange(exSpec);
            accountService = (KrakenAccountService)exchangeInstance.getAccountService();
            tradeService = (KrakenTradeService)exchangeInstance.getTradeService();
            marketDataService = (KrakenMarketDataService)exchangeInstance.getMarketDataService();

            this.metadataAggregator = metadataAggregator;

            //Get status details
            /*
            for (Kraken product : marketDataService.getStatus()) {
                LOG.info(product.toString());
            }

             */

            //Cache initial calls
            refreshProducts();
            refreshFees();
            refreshAccountInfo();
        } else {
            LOG.info("KrakenRestAPI is disabled"); //TODO: Replace with exception?
        }
    }

    @Override
    public constants.Exchange getExchangeName() {
        return exchangeName;
    }

    @Override
    public void refreshProducts() throws IOException {
        exchangeInstance.remoteInit();
        metadataMap = exchangeInstance.getExchangeMetaData().getCurrencyPairs(); //NOTE: trading fees might be static for Kraken
        metadataAggregator.upsertMetadata(KRAKEN, metadataMap);
    }

    @Override
    public void refreshFees() throws IOException {
//        feeMap = accountService.getDynamicTradingFees(); //TODO: XChange to implement getDynamicTradingFees
        feeMap = new HashMap<>();
        exchangeInstance.remoteInit();
        //TODO: Double check whether the fees in the exchangeMetaData are accurate... 0.26% looks ok for now
        exchangeInstance.getExchangeMetaData().getCurrencyPairs().forEach((currencyPair, currencyPairMetaData) -> {
            feeMap.put(currencyPair, new Fee(currencyPairMetaData.getTradingFee(), currencyPairMetaData.getTradingFee()));
        });
        metadataAggregator.upsertFeeMap(KRAKEN, feeMap);
    }

    @Override
    public void refreshAccountInfo() throws IOException {
        accountInfo = accountService.getAccountInfo();
        metadataAggregator.upsertAccountInfo(KRAKEN, accountInfo);
    }

    public Map<CurrencyPair, Fee> getFees() throws Exception {
        return feeMap;
    }

    public Balance getBalance(Currency currency) throws Exception {
        return accountInfo.getWallet().getBalance(currency);
    }
}
