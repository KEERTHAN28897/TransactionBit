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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


public class AncestryTree {

    private final Map<String, Integer> parentCount = new HashMap<>();
    private final Map<String, Integer> parentAncestorsCount = new HashMap<>();
    private final Map<String, List<TransactionFlow>> parentToChild = new HashMap<>();

    private final List<Transaction> allTransactions = new ArrayList<>();

    private final Map<String, Integer> directParentCount = new HashMap<>();
    private final Map<String, List<String>> directParentMap = new HashMap<>();
    private final Map<String, Boolean> transactionMap = new HashMap<>();

    private Set<String> rootNodes = new HashSet<>();

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
            allTransactions.addAll(new ArrayList<>(List.of(transaction)));
            for(int i = 0; i < transaction.length; ++i) {
                ++index;
                transactionMap.put(transaction[i].getTxid(), true);
            }
        }

        for(Transaction transaction : allTransactions) {
            List<TransactionFlow> children = transaction.getVin();
            for(TransactionFlow child : children) {
                if(transactionMap.containsKey(child.getTxid()) &&
                        transactionMap.get(child.getTxid()) &&
                        transaction.getTxid() != child.getTxid()) {
                    int count = directParentCount.getOrDefault(transaction.getTxid(), 0);
                    directParentCount.put(transaction.getTxid(), ++count);
                    List<String> transactions = directParentMap.getOrDefault(transaction.getTxid(), new ArrayList<>());
                    transactions.add(child.getTxid());
                    directParentMap.put(transaction.getTxid(), transactions);
                }
            }
        }
        System.out.println("Length of all transactions: " + directParentMap.size());


        Map<String, Integer> totCount = new HashMap<>();

        for(String child : directParentMap.keySet()) {

            Queue<String> searchQueue = new LinkedList<>();
            searchQueue.add(child);
            while (searchQueue.isEmpty() == false) {
                totCount.put(child, totCount.getOrDefault(child, 0) + directParentCount.getOrDefault(searchQueue.peek(), 0));
                List<String> parents = directParentMap.getOrDefault(searchQueue.peek(), new ArrayList<>());
                for (String next : parents) {
                    searchQueue.add(next);
                }

                searchQueue.remove();
            }
        }

        Map<String, Integer> sortedMap = totCount.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new
                ));

        int n = 10;
        for(String res : sortedMap.keySet()) {
            System.out.println(res + " " + sortedMap.get(res));n--;
            if(n == 0) {
                break;
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
