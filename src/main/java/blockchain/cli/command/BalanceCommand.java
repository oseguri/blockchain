package blockchain.cli.command;

import blockchain.node.Node;

public class BalanceCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("  Node not started. Use 'start' command first.");
            return;
        }

        Node node = context.getNode();

        try {
            if (args.length == 0) {
                // 내 잔액 조회
                long balance = node.getBalance(node.getAddress());
                System.out.println("  Your Balance: " + balance + " guri");
                System.out.println("   Address: " + node.getAddress());
            } else {
                // 특정 주소 잔액 조회
                String address = args[0];
                long balance = node.getBalance(address);
                System.out.println("  Balance of " + address + ": " + balance + " guri");
            }
        } catch (Exception e) {
            System.out.println("  Error fetching balance: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "balance [address] - Check balance (yours or specific address)";
    }

    @Override
    public String getName() {
        return "balance";
    }
}
