package com.byox.challenges.loadbalancer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConsistentHasher implements LoadBalancer{
    private final SortedMap<BigInteger, String> circle = new ConcurrentSkipListMap<>();

    @Override
    public void addServer(String serverAddress) {
        BigInteger hash = getHash(serverAddress);
        circle.put(hash, serverAddress);
    }

    @Override
    public void removeServer(String serverAddress) {
        BigInteger hash = getHash(serverAddress);
        circle.remove(hash);
    }

    @Override
    public String getServer(String request) {
        if(circle.isEmpty()){
            return  null;
        }
        BigInteger hash = getHash(request);
        if(!circle.containsKey(hash)) {
            SortedMap<BigInteger, String>  tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    @Override
    public void relieveServer(String serverAddress) {
        System.out.println("Relieving server: " + serverAddress);
    }

    private BigInteger getHash(String key){
        String salt = "quertyuuid";
        try {
            key = salt + key;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[]  messageDigest = md.digest(key.getBytes());
            return new BigInteger(1, messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
