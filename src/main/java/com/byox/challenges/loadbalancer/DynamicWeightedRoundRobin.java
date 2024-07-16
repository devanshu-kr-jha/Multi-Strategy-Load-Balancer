package com.byox.challenges.loadbalancer;

import jakarta.annotation.Priority;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DynamicWeightedRoundRobin implements LoadBalancer {
    private static class Server {
        String address;
        AtomicInteger load;

        Server(String address) {
            this.address = address;
            this.load = new AtomicInteger(0);
        }

        void increaseLoad() {
            this.load.incrementAndGet();
        }

        void decreaseLoad() {
            this.load.decrementAndGet();
        }

        int getLoad() {
            return this.load.get();
        }
    }

    private PriorityQueue<Server> serverQueue;

    private Map<String, Server> serverMap;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public DynamicWeightedRoundRobin() {
        serverQueue = new PriorityQueue<>(Comparator.comparingInt(Server::getLoad));
        serverMap = new HashMap<>();
    }

    @Override
    public void addServer(String serverAddress) {
        lock.writeLock().lock();
        try {
            Server server = new Server(serverAddress);
            serverQueue.offer(server);
            serverMap.put(serverAddress, server);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeServer(String serverAddress) {
        lock.writeLock().lock();
        try {
            Server server  = serverMap.get(serverAddress);
            if(server != null) {
                serverQueue.remove(server);
                serverMap.remove(serverAddress);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getServer(String request) {
        lock.writeLock().lock();
        if (serverQueue.isEmpty()) {
            return null;
        }
        Server server = serverQueue.poll();
        server.increaseLoad();
        serverQueue.offer(server);
        lock.writeLock().unlock();
        return server.address;
    }

    @Override
    public void relieveServer(String serverAddress) {
        lock.writeLock().lock();
        try {
            Server server = serverMap.get(serverAddress);
            if(server != null) {
                serverQueue.remove(server);
                server.decreaseLoad();
                serverQueue.offer(server);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
