package blockchain.network;

import java.io.*;
import java.net.Socket;

/**
 * 연결된 피어 노드
 */
public class Peer {
    private final String address;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected;

    public Peer(String address, int port) {
        this.address = address;
        this.port = port;
        this.connected = false;
    }

    public Peer(Socket socket) throws IOException {
        this.socket = socket;
        this.address = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        this.connected = true;

        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public boolean connect() {
        try {
            socket = new Socket(address, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            System.out.println("✓ Connected to peer: " + address + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("✗ Failed to connect to " + address + ":" + port);
            connected = false;
            return false;
        }
    }

    public synchronized void sendMessage(Message message) throws IOException {
        if (!connected || out == null) {
            throw new IOException("Not connected to peer");
        }

        out.writeObject(message);
        out.flush();
    }

    public Message receiveMessage() throws IOException, ClassNotFoundException {
        if (!connected || in == null) {
            throw new IOException("Not connected to peer");
        }

        return (Message) in.readObject();
    }

    public void disconnect() {
        try {
            connected = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();

            System.out.println("Disconnected from peer: " + address + ":" + port);
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public String getAddress() {
        return address;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getId() {
        return address + ":" + port;
    }

    @Override
    public String toString() {
        return address + ":" + port + (connected ? " [Connected]" : " [Disconnected]");
    }
}
