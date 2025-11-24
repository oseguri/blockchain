package blockchain.cli.command;

import blockchain.node.Node;
import blockchain.transaction.Mempool;

public class CommandContext {
    private Node node;
    private Mempool mempool;
    private boolean running;

    public CommandContext(){
        this.running = true;
    }

    public Node getNode() {
        return node;
    }
    public void setNode(Node node) {
        this.node = node;
    }

    public Mempool getMempool() {
        return mempool;
    }
    public void setMempool(Mempool mempool) {
        this.mempool = mempool;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isNodeInitialized() {
        return node != null;
    }

    public void stop() {
        this.running = false;
    }
}
