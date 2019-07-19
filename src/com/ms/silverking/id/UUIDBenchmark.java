package com.ms.silverking.id;

import java.util.concurrent.atomic.AtomicLong;

import com.ms.silverking.time.SimpleStopwatch;
import com.ms.silverking.time.Stopwatch;

public class UUIDBenchmark implements Runnable {
    private final int   iterations;
    private final int   threads;
    
    private static final AtomicLong    l1;
    private static final AtomicLong    l2;
    
    static {
        l1 = new AtomicLong();
        l2 = new AtomicLong();
    }
    
    public UUIDBenchmark(int iterations, int threads) {
        this.iterations = iterations;
        this.threads = threads;
        new IDThread(this).start();
    }

    @Override
    public void run() {
        Stopwatch   sw;
        int         sum;
        double      uuidsPerSecond;
        double      usPerUUID;
        
        sum = 0;
        sw = new SimpleStopwatch();
        for (int i = 0; i < iterations; i++) {
            UUIDBase uuid;
            //UUID    uuid;
            
            //uuid = UUID.randomUUID();
            //uuid = new UUID(0, l2.getAndIncrement());
            uuid = new UUIDBase();
            sum += uuid.hashCode();
        }
        sw.stop();
        uuidsPerSecond = (double)iterations / sw.getElapsedSeconds();
        System.out.println(sum +"\n\n");
        System.out.println(uuidsPerSecond);
        usPerUUID = sw.getElapsedSeconds() / (double)(iterations * threads) * 1000000.0;
        System.out.println(usPerUUID);        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            int iterations;
            int threads;
            
            if (args.length != 2) {
                System.out.println("args: <iterations> <threads>");
                return;
            }
            iterations = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
            for (int i = 0; i < threads; i++) {
                new UUIDBenchmark(iterations, threads);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
