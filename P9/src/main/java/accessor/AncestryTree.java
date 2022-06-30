package accessor;

import constants.Constants;
import controller.ConnectionRequest;
import model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.TransactionFlow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class AncestryTree {

    public final Map<String, Integer> parentCount = new HashMap<>();
    public final Map<String, Integer> parentAncestorsCount = new HashMap<>();
    public final Map<String, List<TransactionFlow>> parentToChild = new HashMap<>();
    public Set<String> rootNodes = new HashSet<>();

    /**
     * fetch() for given offset, get all the elements and add in map
     * @throws IOException
     */

    private void fetch() throws IOException {
        String key = getKeyForBlockHeight(Constants.BLOCK);
        int index = 0;

        while(index <= 2874) {
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
    }

    /**
     * we will be adding direct parent and inDirect parents in this case.
     * @return top 10 transaction with most ancestors
     * @throws IOException
     */

    public List<String> getTopTenTransactions() throws IOException {
        //start with rootNode and add indirect ancestors.
        fetch();
        while(rootNodes.size() > 0) {
            List<String> currNodes = new ArrayList<>();
            for (String root : rootNodes) {
                List<TransactionFlow> transactionFlows = parentToChild.getOrDefault(root, new ArrayList<>());
                for (TransactionFlow transactionFlow : transactionFlows) {
                    Integer count = parentAncestorsCount.getOrDefault(transactionFlow.getTxid(), 0);
                    parentAncestorsCount.put(transactionFlow.getTxid(), ++count);
                    parentCount.put(transactionFlow.getTxid(), parentCount.get(transactionFlow.getTxid()));
                    if (parentCount.get(transactionFlow.getTxid()) == 0) {
                        parentCount.remove(transactionFlow.getTxid());
                        currNodes.add(transactionFlow.getTxid());
                    }
                }

            }
            rootNodes.addAll(currNodes);
        }

        Map<String, Integer> sortedMap = parentAncestorsCount.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new
                ));

        int n = 10;
        List<String> result = new ArrayList<>();

        for(String res : sortedMap.keySet()) {
            System.out.println(res);
            if(n == 0) {
                break;
            }

            result.add(res);
        }

        return result;


    }

    /**
     * get the hash for a given block number
     * @param block block number
     * @return hash value for given block
     * @throws IOException
     */
    public static String getKeyForBlockHeight(String block) throws IOException {
        return ConnectionRequest.getResponse(Constants.BLOCK_URL + block);
    }
}
