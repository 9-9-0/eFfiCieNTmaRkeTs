package constants;

public enum Exchange {
    COINBASE_PRO("CoinbasePro"),
    KRAKEN("Kraken"),
    BITFINEX("Bitfinex"),
    GEMINI("Gemini");

    private String str;
    Exchange(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return this.str;
    }
}