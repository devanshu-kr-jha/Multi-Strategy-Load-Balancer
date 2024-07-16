package com.byox.challenges.loadbalancer;

public interface LoadBalancer {
    void addServer(String serverAddress);
    void removeServer(String serverAddress);
    String getServer(String request);
    void relieveServer(String serverAddress);
}
