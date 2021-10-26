package org.academiadecodigo.altcatras65;

public class Main {

    public static void main(String[] args) {
        final int PORT_NUMBER = 8080;

        ChatServer chatServer = new ChatServer(PORT_NUMBER);
        chatServer.listen();
    }

}
