package blockchain.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * P2P 서버 (연결 수신)
 */
public class P2PServer {

    private final int port;
    private ServerSocket serverSocket;
    private final P2PNetwork network;
    private final ExecutorService executorService;
    private boolean running;

    public P2PServer(int port, P2PNetwork network) {
        this.port = port;
        this.network = network;
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("✓ P2P Server started on port " + port);

            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        System.out.println("New peer connection from: " +
                                clientSocket.getInetAddress().getHostAddress());

                        Peer peer = new Peer(clientSocket);
                        network.addPeer(peer);

                        executorService.submit(() -> network.handlePeerMessages(peer));

                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            });

            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            System.err.println("Failed to start P2P server: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();

            System.out.println("P2P Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }
}
