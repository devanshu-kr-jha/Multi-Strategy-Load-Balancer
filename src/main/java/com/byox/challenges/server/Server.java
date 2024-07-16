package com.byox.challenges.server;

import com.byox.challenges.loadbalancer.LoadBalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable{
    private int port;
    private Threadpool threadpool;
    private LoadBalancer loadBalancer;

    /* Constructor for a Load Balancer*/
    public Server(int port, int poolSize, LoadBalancer loadBalancer) {
        this.port = port;
        this.threadpool = new Threadpool(poolSize, "Server on port " + port);
        this.loadBalancer = loadBalancer;
        System.out.println("Server initialized with load balancer on port " + port);
    }

    /* Constructor for a standalone server instance */
    public Server(int port, int poolSize) {
        this.port = port;
        this.threadpool = new Threadpool(poolSize, "Server on port " + port);
        this.loadBalancer = null;
        System.out.println("Server initialized without load balancer on port " + port);
    }

    private void log(String message) {
        System.out.println("[" + getServerType() + ":" + (port) + "] : " + message);
    }

    private void logError(String action, Exception e) {
        System.out.println("Error in " + getServerType() + " : " + action + " : " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            log("Server listening");

            while(!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                log("Client connected");

                /* Decide how to handle the client based on whether a load balancer is present */
                if(getServerType().equals("Server")) {
                    log("Handling client directly");
                    this.threadpool.addAndExecute(new ClientHandler(clientSocket, port));
                } else {
                    this.threadpool.addAndExecute(new ClientHandler(clientSocket, loadBalancer, port));
                }
            }
        } catch (IOException e) {
            log("[" + getServerType() + " on port " + port + "] Server Exception: " + e.getMessage());
        }
    }

    private String getServerType() {
        return (loadBalancer == null) ? "Server" : "Load Balancer";
    }

    public class ClientHandler implements Runnable {
        private Socket clientSocket;
        private LoadBalancer loadBalancer;
        private int port;


        //Constructor for handling client requests directly w/o loadBalancer
        public ClientHandler(Socket clientSocket, int serverPort) {
            this.clientSocket = clientSocket;
            this.loadBalancer = null;
            this.port = serverPort;
            log("Client handler created without consistent hasher");
        }

        // Constructor for handling client requests via load balancer
        public ClientHandler(Socket clientSocket, LoadBalancer loadBalancer, int serverPort) {
            this.clientSocket = clientSocket;
            this.loadBalancer = loadBalancer;
            this.port = serverPort;
            log("Client handler created with consistent hasher");
        }

        @Override
        public void run() {
            log("ClientHandler started..");
            if(loadBalancer != null) {
                handleRequestViaLoadBalancer();
            } else {
                handleDirectClientRequest();
            }
        }

        private void handleRequestViaLoadBalancer(){
            try(BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))){
                log("Reading request from client");
                String requestLine = clientReader.readLine();
                log("Request line: " + requestLine);
                String targetServer = loadBalancer.getServer(requestLine);

                forwardRequest(targetServer, requestLine, clientReader);
            } catch (IOException e) {
                logError("handling request via load balancer", e);
            } finally {
                closeSocket();
            }
        }
        private void handleDirectClientRequest(){
            log("Handling direct client request...");
            try(BufferedReader clientReader= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                String request = readRequest(clientReader);
                String responseMessage = "Hello from the server " + (port);
                sendResponse(clientWriter, responseMessage, "text/plain; charset=utf-8");

                log("Sent response: " + responseMessage);

            } catch (IOException e) {
                logError("handling direct client response", e);
            } finally {
                closeSocket();
            }
        }

        private void forwardRequest(String serverAddress, String initialRequestLine, BufferedReader clientReader) {
            log("Forwarding request to: " + serverAddress);

            String[] addressParts = serverAddress.split(":");
            if(addressParts.length < 2) {
                log("Invalid server address: " + serverAddress);
                return;
            }
            int targetPort  = Integer.parseInt(addressParts[1]);
            try(Socket forwardSocket = new Socket(addressParts[0], targetPort)) {
                PrintWriter serverWriter = new PrintWriter(forwardSocket.getOutputStream(), true); /* LoadBalancer -> server */
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(forwardSocket.getInputStream())); /* Server -> LoadBalancer */
                PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true); /* LoadBalancer -> Client */

                forwardInitialRequest(initialRequestLine, serverWriter);
                readAndForwardHeaders(clientReader, serverWriter);
                loadBalancer.relieveServer(serverAddress);
                readAndForwardResponse(serverReader, clientWriter);

                log("Responses forwarded successfully to client");
            } catch (IOException e) {
                logError("forwarding request", e);
            }
            finally {
                closeSocket();
            }
        }

        private void forwardInitialRequest(String initialRequestLine, PrintWriter serverWriter) {
            serverWriter.println(initialRequestLine);
        }

        private void readAndForwardHeaders(BufferedReader clientReader, PrintWriter serverWriter) throws IOException {
            String line;
            while((line = clientReader.readLine()) != null) {
                serverWriter.println(line);
                if(line.isEmpty()) break;
            }
            serverWriter.println();
        }

        private void readAndForwardResponse(BufferedReader serverReader, PrintWriter clientWriter) throws  IOException {
            String line;
            while((line = serverReader.readLine()) != null) {
                clientWriter.println(line);
            }
        }

        private String readRequest(BufferedReader reader) throws IOException {
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while(!(line = reader.readLine()).isEmpty()) {
                requestBuilder.append(line).append("\n");
                log("Received request line: "+line);
            }
            return requestBuilder.toString();
        }

        private void sendResponse(PrintWriter writer, String response, String contentType) {
            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + response.length());
            writer.println();
            writer.println(response);
        }

        private void closeSocket(){
            try {
                if(clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    log("Client socket is closed");
                }
            } catch (IOException e) {
                logError("closing client socket", e);
            }
        }
    }
}
