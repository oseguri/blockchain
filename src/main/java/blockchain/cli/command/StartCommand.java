package blockchain.cli.command;

import blockchain.node.Node;
import blockchain.transaction.Mempool;

public class StartCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if(context.isNodeInitialized()) {
            System.out.println("[Error] : Node already started");
            return;
        }

        if(args.length < 1) {
            System.out.println("Usage: start <port> <storage_path>");
            return;
        }

        try{
            int port = Integer.parseInt(args[0]);
            System.out.println("Starting node on port: " + port
            + " storage path: " + args[1]);
            //노드 생성
            Node node = new Node(args[1], port);
            context.setNode(node);
            //Mempool 초기화
            Mempool mempool = new Mempool(node.getValidator());
            context.setMempool(mempool);
            //네트워크 시작
            node.startP2P();

            System.out.println("   Node started successfully!");
            System.out.println("   Address: " + node.getAddress());
            System.out.println("   Balance: " + node.getBalance(node.getAddress()));
            System.out.println("   Chain height: " + (node.getBlockList().size() - 1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getHelp() {
        return "start <port> [storage_path] - Start a blockchain node";
    }

    @Override
    public String getName() {
        return "start";
    }
}
