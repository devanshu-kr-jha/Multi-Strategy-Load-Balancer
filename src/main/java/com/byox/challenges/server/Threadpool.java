package com.byox.challenges.server;

import java.util.LinkedList;
import java.util.Queue;

public class Threadpool {
    private final WorkerThread[] workers;

    private final Queue<Runnable> queue;

    private final String threadpoolContext;

    public Threadpool(int poolSize, String context) {
        this.threadpoolContext = context;
        this.workers = new WorkerThread[poolSize];

        this.queue = new LinkedList<>();

        for(int i = 0; i < poolSize; i++){
            workers[i] = new WorkerThread();
            workers[i].setName(threadpoolContext + " - Worker " + (i+1));
            workers[i].start();
        }
        System.out.println("[" + threadpoolContext + "] Threadpool initialized with " + poolSize + " worker threads");
    }

    public void addAndExecute(Runnable task) {
        synchronized (queue) {
            queue.add(task);
            System.out.println("[" + threadpoolContext + "] Task added to queue. Total tasks in queue: " + queue.size());
            queue.notify();
        }
    }

    class WorkerThread extends Thread {
        @Override
        public void run() {
            Runnable task;
            while(true) {
                synchronized (queue){
                    while (queue.isEmpty()) {
                        try {
                            System.out.println("[" + getName() + "] Waiting for tasks");
                            queue.wait();
                        } catch (InterruptedException e) {
                            System.out.println("[" + getName() + "] Error in waiting for queue " + e.getMessage());
                        }
                    }
                    task = queue.poll();
                    System.out.println("[" + getName() + "] Task retrieved from queue. Executing task...");
                }
                try {
                    task.run();
                } catch (RuntimeException e) {
                    System.out.println("[" + getName() + "] Error in running a task: " + e.getMessage());
                }
            }
        }
    }
}

