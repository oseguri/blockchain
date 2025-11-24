package blockchain.cli;

import blockchain.cli.command.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CLI {
    private final CommandContext context;
    private final Map<String, Command> commands;
    private final Scanner scanner;

    public CLI() {
        this.context = new CommandContext();
        this.commands = new HashMap<>();
        this.scanner = new Scanner(System.in);

        registerCommands();
    }

    /**
     * 명령어 등록
     */
    private void registerCommands() {
        // 기본 명령어
        registerCommand(new HelpCommand(commands));
        registerCommand(new ExitCommand());

        // 노드 관리
        registerCommand(new StartCommand());
        registerCommand(new StatusCommand());

        // 네트워크
        registerCommand(new ConnectCommand());
        registerCommand(new PeersCommand());
        registerCommand(new SyncCommand());

        // 트랜잭션
        registerCommand(new BalanceCommand());
        registerCommand(new SendCommand());
        registerCommand(new MempoolCommand());

        // 채굴
        registerCommand(new MineCommand());

        // 블록체인 조회
        registerCommand(new ListCommand());
        registerCommand(new BlockCommand());
    }

    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
    }

    /**
     * CLI 시작
     */
    public void start() {
        printBanner();

        System.out.println("Type 'help' for available commands\n");

        // REPL 루프
        while (context.isRunning()) {
            System.out.print("blockchain> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        scanner.close();
    }

    /**
     * 명령어 처리
     */
    private void processCommand(String input) {
        String[] tokens = input.split("\\s+");
        String commandName = tokens[0].toLowerCase();
        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);

        Command command = commands.get(commandName);

        if (command != null) {
            try {
                command.execute(context, args);
            } catch (Exception e) {
                System.out.println("  Command execution error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("  Unknown command: " + commandName);
            System.out.println("   Type 'help' for available commands");
        }
    }

    /**
     * 배너 출력
     */
    private void printBanner() {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║            🔗  BLOCKCHAIN CLI v1.0  ⛓️                    ║");
        System.out.println("║                                                           ║");
        System.out.println("║         A Simple P2P Blockchain Implementation            ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

}
