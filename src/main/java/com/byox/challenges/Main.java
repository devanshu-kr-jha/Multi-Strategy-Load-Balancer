package com.byox.challenges;

import com.byox.challenges.loadbalancer.ConsistentHasher;
import com.byox.challenges.loadbalancer.DynamicWeightedRoundRobin;
import com.byox.challenges.server.Server;

public class Main {
	public static void main(String[] args) {
		Server[] servers = new Server[10];
		int portRange = 1004;

		/* Consistent hashing strategy*/
//		ConsistentHasher consistentHasher= new ConsistentHasher();
//		for (int i = 0; i < 10; i++) {
//			servers[i] = new Server(portRange + i, 10);
//			new Thread(servers[i]).start();
//			consistentHasher.addServer("localhost:" + (portRange + i));
//		}
//		Server loadBalancerServer = new Server(1003, 10, consistentHasher);


		/* Weighted round-robin strategy */
		DynamicWeightedRoundRobin weightedRoundRobin = new DynamicWeightedRoundRobin();
		for(int i = 0; i < 10; i++) {
			servers[i] = new Server(portRange + i, 10);
			new Thread(servers[i]).start();
			weightedRoundRobin.addServer("localhost:" + (portRange + i));
		}
		Server loadBalancerServer = new Server(1003, 10, weightedRoundRobin);

		Thread loadBalancerThread = new Thread(loadBalancerServer);
		loadBalancerThread.start();
	}
}
