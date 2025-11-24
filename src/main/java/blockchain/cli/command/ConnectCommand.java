package blockchain.cli.command;

public class ConnectCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if(!context.isNodeInitialized()) {
            System.out.println("[Error] : Node need to be initialized");
            return;
        }

        if(args.length < 2) {
            System.out.println("Usage: connect <host> <port>");
            return;
        }

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            System.out.println("  Connecting to peer " + host + ":" + port + "...");

            boolean success = context.getNode().connectToPeer(host, port);

            if (success) {
                System.out.println("  Connected successfully!");
            } else {
                System.out.println("  Connection failed!");
            }

        } catch (NumberFormatException e) {
            System.out.println("  Invalid port number: " + args[1]);
        } catch (Exception e) {
            System.out.println("  Connection error: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "connect <host> <port> - Connect to a peer node";
    }

    @Override
    public String getName() {
        return "connect";
    }
}
