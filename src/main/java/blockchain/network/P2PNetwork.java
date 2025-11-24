package blockchain.network;

import blockchain.block.Block;
import blockchain.node.Node;
import blockchain.transaction.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * P2P 네트워크 관리자
 */
public class P2PNetwork {

    private final Node node;
    private final P2PServer server;
    private final List<Peer> peers;
    private final ConcurrentHashMap<String, Peer> peerMap;
    private String nodeId;

    public P2PNetwork(Node node, int port) {
        this.node = node;
        this.server = new P2PServer(port, this);
        this.peers = new CopyOnWriteArrayList<>();
        this.peerMap = new ConcurrentHashMap<>();

        try {
            this.nodeId = node.getAddress();
        } catch (Exception e) {
            this.nodeId = "Node-" + port;
        }
    }

    public void start() {
        server.start();
        System.out.println("P2P Network started for node: " + nodeId);
    }

    public boolean connectToPeer(String address, int port) {
        String peerId = address + ":" + port;

        if (peerMap.containsKey(peerId)) {
            System.out.println("Already connected to peer: " + peerId);
            return true;
        }

        Peer peer = new Peer(address, port);

        if (peer.connect()) {
            addPeer(peer);
            new Thread(() -> handlePeerMessages(peer)).start();
            return true;
        }

        return false;
    }

    public void addPeer(Peer peer) {
        if (!peerMap.containsKey(peer.getId())) {
            peers.add(peer);
            peerMap.put(peer.getId(), peer);

            System.out.println("✓ Peer added: " + peer.getId());
            System.out.println("Total peers: " + peers.size());
        }
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
        peerMap.remove(peer.getId());
        peer.disconnect();

        System.out.println("Peer removed: " + peer.getId());
    }

    public void handlePeerMessages(Peer peer) {
        try {
            while (peer.isConnected()) {
                Message message = peer.receiveMessage();

                System.out.println("Received: " + message.getType() +
                        " from " + peer.getId());

                handleMessage(message, peer);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling messages from " + peer.getId());
            removePeer(peer);
        }
    }

    private void handleMessage(Message message, Peer sender) {
        try {
            switch (message.getType()) {
                case NEW_BLOCK:
                    handleNewBlock((Block) message.getPayload());
                    break;

                case NEW_TRANSACTION:
                    handleNewTransaction((Transaction) message.getPayload());
                    break;

                case REQUEST_CHAIN:
                    handleChainRequest(sender);
                    break;

                case RESPONSE_CHAIN:
                    handleChainResponse((List<Block>) message.getPayload());
                    break;

                case PING:
                    handlePing(sender);
                    break;

                default:
                    System.out.println("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNewBlock(Block block) {
        System.out.println("\n=== Received New Block ===");
        System.out.println("Block Hash: " + bytesToHex(block.getBlockHash()).substring(0, 16) + "...");

        boolean added = node.receiveBlock(block);

        if (added) {
            System.out.println("✓ Block added to local chain");
        } else {
            System.out.println("✗ Block rejected");
        }
        System.out.println("=========================\n");
    }

    private void handleNewTransaction(Transaction tx) {
        System.out.println("Received transaction: " +
                bytesToHex(tx.getTxid()).substring(0, 16) + "...");
    }

    private void handleChainRequest(Peer sender) {
        System.out.println("Chain request from: " + sender.getId());

        try {
            List<Block> chain = node.getBlockList();
            Message response = new Message(
                    Message.MessageType.RESPONSE_CHAIN,
                    chain,
                    nodeId
            );

            sender.sendMessage(response);
        } catch (IOException e) {
            System.err.println("Failed to send chain: " + e.getMessage());
        }
    }

    private void handleChainResponse(List<Block> receivedChain) {
        System.out.println("\n=== Received Blockchain ===");
        System.out.println("Received chain length: " + receivedChain.size());
        System.out.println("Current chain length: " + node.getChainLength());

        if (receivedChain.size() > node.getChainLength()) {
            node.replaceChain(receivedChain);
        }

        System.out.println("==========================\n");
    }

    private void handlePing(Peer sender) {
        try {
            Message pong = new Message(Message.MessageType.PONG, null, nodeId);
            sender.sendMessage(pong);
        } catch (IOException e) {
            System.err.println("Failed to send pong: " + e.getMessage());
        }
    }

    public void broadcastBlock(Block block) {
        System.out.println("\n=== Broadcasting Block ===");
        System.out.println("To " + peers.size() + " peers");

        Message message = new Message(Message.MessageType.NEW_BLOCK, block, nodeId);
        broadcast(message);

        System.out.println("=========================\n");
    }

    public boolean requestChainSync() {
        if (peers.isEmpty()) {
            System.out.println("No peers to sync with");
            return false;
        }

        System.out.println("Requesting blockchain from peers...");

        Message message = new Message(Message.MessageType.REQUEST_CHAIN, null, nodeId);
        broadcast(message);
        return true;
    }

    private void broadcast(Message message) {
        List<Peer> failedPeers = new ArrayList<>();

        for (Peer peer : peers) {
            try {
                if (peer.isConnected()) {
                    peer.sendMessage(message);
                    System.out.println("  ✓ Sent to " + peer.getId());
                } else {
                    failedPeers.add(peer);
                }
            } catch (IOException e) {
                System.err.println("Failed to send to " + peer.getId() + ": " + e.getMessage());
                e.printStackTrace(); // ← 상세 에러 출력
                failedPeers.add(peer);
            }
        }

        for (Peer peer : failedPeers) {
            removePeer(peer);
        }
    }


    public void shutdown() {
        System.out.println("Shutting down P2P network...");

        for (Peer peer : peers) {
            peer.disconnect();
        }

        server.stop();
        System.out.println("P2P network shutdown complete");
    }

    public int getPeerCount() {
        return peers.size();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 트랜잭션 브로드캐스트
     * @param tx 브로드캐스트할 트랜잭션
     */
    public void broadcastTransaction(Transaction tx) {
        System.out.println("📡 Broadcasting Transaction");
        System.out.println("   TXID: " + bytesToHex(tx.getTxid()).substring(0, 16) + "...");
        System.out.println("   To " + peers.size() + " peers");

        Message message = new Message(Message.MessageType.NEW_TRANSACTION, tx, nodeId);
        broadcast(message);

        System.out.println("✓ Transaction broadcast complete\n");
    }
}
