import accessor.AncestryTree;

import java.io.IOException;

public class Main{

    public static void main(String[] args) throws IOException {
        AncestryTree ancestryTree = new AncestryTree();
       System.out.println(ancestryTree.getTopTenTransactions());
    }
}
