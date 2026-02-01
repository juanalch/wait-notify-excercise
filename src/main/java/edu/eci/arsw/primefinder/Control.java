package edu.eci.arsw.primefinder;

import java.util.Scanner;

public class Control extends Thread {
    
    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread pft[];
    
    private final Object monitor = new Object(); // shared monitor 
    private volatile boolean shouldPause = false;
    private int threadsPaused = 0;
    private int activeThreads = 0;
    
    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];

        int i;
        for(i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i*NDATA, (i+1)*NDATA, monitor, this);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i*NDATA, MAXVALUE + 1, monitor, this);
    }
    
    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        // all workerthread start
        for(int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }
        activeThreads = NTHREADS;
        
        // Boss thread 
        try {
            while (activeThreads > 0) {
                synchronized(monitor) {
                    monitor.wait(TMILISECONDS);
                }
                // if thereis no active threads break
                if (activeThreads == 0) break;
                
                System.out.println("\n=== INICIANDO PAUSA PROGRAMADA ===");
                
                // Start synchronized pause
                synchronized(monitor) {
                    shouldPause = true;
                    threadsPaused = 0;
                    monitor.notifyAll(); // Wake up all threads
                    
                    // Wait for all threads to pause
                    while (threadsPaused < activeThreads) {
                        System.out.println("Waiting for paused threads: " + threadsPaused + "/" + activeThreads);
                        monitor.wait();
                    }
                    
                    System.out.println("All threads are paused.");
                }
                
                // Show statistics
                showStatistics();
                
                // Wait for ENTER to continue
                System.out.println("Press ENTER to continue...");
                new Scanner(System.in).nextLine();
                
                // Resume
                synchronized(monitor) {
                    System.out.println("Resuming threads...");
                    shouldPause = false;
                    monitor.notifyAll(); // Notify all threads to continue
                }
                
                System.out.println("Search resumed.\n");
            }
            
            // Show final results
            showFinalStatistics();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void threadPaused() {
        synchronized(monitor) {
            threadsPaused++;
            System.out.println("Thread paused. Total: " + threadsPaused);
            if (threadsPaused >= activeThreads) {
                monitor.notify(); // notify the contrller that all are paused
            }
        }
    }
    
    public void threadFinished() {
        synchronized(monitor) {
            activeThreads--;
            System.out.println("Thread finished. Remaining active threads: " + activeThreads);
            if (threadsPaused >= activeThreads && activeThreads > 0) {
                monitor.notify(); // Notify if all remaining threads are paused
            }
        }
    }
    
    private void showStatistics() {
        int totalPrimesFound = 0;
        for (PrimeFinderThread thread : pft) {
            totalPrimesFound += thread.getPrimesCount();
        }
        System.out.println("\n=== PAUSE ===");
        System.out.println("Total prime numbers found so far: " + totalPrimesFound);
        System.out.println("Active threads: " + activeThreads);
        System.out.println("Paused threads: " + threadsPaused);
        System.out.println("=================\n");
    }
    
    private void showFinalStatistics() {
        int totalPrimesFound = 0;
        for (PrimeFinderThread thread : pft) {
            totalPrimesFound += thread.getPrimesCount();
        }
        System.out.println("\n=== FINAL RESULTS ===");
        System.out.println("Total prime numbers found: " + totalPrimesFound);
        System.out.println("Search completed.");
        System.out.println("==========================\n");
    }
    
    public boolean shouldPause() {
        return shouldPause;
    }
}