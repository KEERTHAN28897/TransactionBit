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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;


public class AncestryTree {
    
    private final List<Transaction> allTransactions = new ArrayList<>();

    private final Map<String, Integer> directParentCount = new HashMap<>();
    private final Map<String, List<String>> directParentMap = new HashMap<>();
    private final Map<String, Boolean> transactionMap = new HashMap<>();
    
    private final Map<String, Integer> totCount = new HashMap<>();


    /**
     * print top 10 transactions.
     */
    private void printTopTransactions() {
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
     * map all direct and indirect parents
     * total parents would be = direct parents + indirect parents.
     */
    private void mapDirectAndIndirectParents() {
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
    }


    /**
     * add all DirectParents to a child
     */
    private void addAllDirectParents() {
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
    }

    /**
     * fetchRequiredDetails() for given offset, get all the elements and add in map
     * @throws IOException
     */
    private void fetchRequiredDetails() throws IOException {
        String key = getKeyForBlockHeight(Constants.BLOCK);
        int index = 0;

        //TODO: This should not be hardcoded
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
    }



    /**
     * we will be adding direct parent and inDirect parents in this case.
     * @return top 10 transaction with most ancestors
     * @throws IOException
     */
    public void getTopTenTransactions() throws IOException {
        //start with rootNode and add indirect ancestors.
        fetchRequiredDetails();
        addAllDirectParents();
        System.out.println("Length of all transactions: " + directParentMap.size());
        mapDirectAndIndirectParents();
        printTopTransactions();
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
