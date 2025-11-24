package blockchain.cli.command;

import blockchain.node.Node;
import blockchain.transaction.Transaction;

public class SendCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Usage: send <to_address> <amount>");
            return;
        }

        Node node = context.getNode();

        try {
            String toAddress = args[0];
            long amount = Long.parseLong(args[1]);

            // 잔액 확인
            long myBalance = node.getBalance(node.getAddress()); // 오버로드된 메서드 사용
            if (myBalance < amount) {
                System.out.println("   Insufficient balance!");
                System.out.println("   Your balance: " + myBalance + " satoshi");
                System.out.println("   Required: " + amount + " satoshi");
                return;
            }

            System.out.println("   Creating transaction...");
            System.out.println("   From: " + node.getAddress());
            System.out.println("   To: " + toAddress);
            System.out.println("   Amount: " + amount + " satoshi");

            // 트랜잭션 생성 (Node에 새로 추가한 메서드)
            Transaction tx = node.createTransaction(toAddress, amount);

            if (tx == null) {
                System.out.println("   Failed to create transaction");
                return;
            }

            // Mempool에 추가
            boolean added = context.getMempool().addTransaction(tx);

            if (added) {
                System.out.println("   Transaction created successfully!");
                System.out.println("   TX ID: " + bytesToHex(tx.getTxid()));
                System.out.println("   Status: Pending in mempool");

                // P2P 브로드캐스트 (Node에 새로 추가한 메서드)
                node.broadcastTransaction(tx);
                System.out.println("   Broadcasted to peers");
            } else {
                System.out.println("   Transaction validation failed");
            }

        } catch (NumberFormatException e) {
            System.out.println("   Invalid amount: " + args[1]);
        } catch (Exception e) {
            System.out.println("   Transaction error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getHelp() {
        return "send <to_address> <amount> - Send coins to an address";
    }

    @Override
    public String getName() {
        return "send";
    }
}
