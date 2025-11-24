package main;

import blockchain.cli.CLI;


/**
 * P2P 네트워크 테스트
 * 3개 노드가 P2P로 연결되어 블록을 동기화
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CLI cli = new CLI();
        cli.start();
    }

}

