package model;

import java.util.List;


public class Transaction {
    String txid;
    List<TransactionFlow> vin;

    public String getTxid() {
        return txid;
    }

    public List<TransactionFlow> getVin() {
        return vin;
    }
}
