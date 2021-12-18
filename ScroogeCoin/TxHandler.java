import java.util.ArrayList;
public class TxHandler {

    private UTXOPool uPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        uPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double sumi = 0;
        double sumo = 0;
        ArrayList<UTXO> usedUTXO = new ArrayList<UTXO>();

        for(int i=0; i<tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            byte[] prevTxHash = input.prevTxHash;
            int outputIndex = input.outputIndex;
            byte[] sig = input.signature;

            UTXO ut = new UTXO(prevTxHash, outputIndex);

            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            if (!uPool.contains(ut)) {
                return false;
            }

            Transaction.Output output = uPool.getTxOutput(ut);
            byte[] message = tx.getRawDataToSign(i);

            // (2) the signatures on each input of {@code tx} are valid

            if (!Crypto.verifySignature(output.address, message, sig)) {
                return false;
            }

            sumi += output.value;

            // (3) no UTXO is claimed multiple times by {@code tx}
            if (usedUTXO.contains(ut)) {
                return false;
            }

            usedUTXO.add(ut);
        }

        for(int i=0; i<tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);
            // (4) all of {@code tx}s output values are non-negative, 
            if (output.value < 0) {
                return false;
            }
            sumo += output.value;

        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values
        if(sumi < sumo) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTx = new ArrayList<Transaction>();

        for(Transaction tx: possibleTxs) {
            if(isValidTx(tx)) {
                validTx.add(tx);

                // remove UTXO
                for(int i=0; i<tx.numInputs(); i++) {
                    Transaction.Input input = tx.getInput(i);
                    byte[] prevTxHash = input.prevTxHash;
                    int outputIndex = input.outputIndex;

                    UTXO ut = new UTXO(prevTxHash, outputIndex);
                    uPool.removeUTXO(ut);
                }

                // Add UTXO
                byte[] hash = tx.getHash();
                for(int i=0; i<tx.numOutputs(); i++) {
                   UTXO ut = new UTXO(hash, i);
                   uPool.addUTXO(ut, tx.getOutput(i));
                }
            }

        }
        Transaction[] validTxn = new Transaction[validTx.size()];
        validTxn = validTx.toArray(validTxn);
        return validTxn;
    }

}
