package accessor;

import constants.Constants;
import controller.ConnectionRequest;
import model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.TransactionFlow;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AncestryTree {



    public final Map<String, Integer> parentCount = new HashMap<>();
    public final Map<String, List<TransactionFlow>> parentToChild = new HashMap<>();
    public Set<String> rootNodes = new HashSet<>();


    public void fetch() throws IOException {
        String key = getKeyForBlockHeight(Constants.BLOCK);
        int index = 0;
        String response = ConnectionRequest.getResponse(Constants.BLOCKSTREAM_URL + key + "/txs/" +
                String.valueOf(index));

        Gson gson = new GsonBuilder().create();
        Transaction[] transaction = gson.fromJson(response, Transaction[].class);
        for(int i = 0; i < transaction.length; ++i) {
            ++index;
            List<TransactionFlow> ancestors = transaction[i].getVin();
            Integer parentAncestorCount = parentCount.getOrDefault(transaction[i].getTxid(), 0);
            if(parentAncestorCount == 0) {
                rootNodes.add(transaction[i].getTxid());
            }
            parentToChild.put(transaction[i].getTxid(), ancestors);
            for(TransactionFlow transactionFlow : ancestors) {
                rootNodes.remove(transactionFlow.getTxid());
                Integer count = parentCount.getOrDefault(transactionFlow.getTxid(), 0);
                parentCount.put(transactionFlow.getTxid(), ++count);
            }
        }
    }

    public List<String> getTopTenTransactions() {

    }

    public static String getKeyForBlockHeight(String block) throws IOException {
        return ConnectionRequest.getResponse(Constants.BLOCK_URL + block);
    }
}
