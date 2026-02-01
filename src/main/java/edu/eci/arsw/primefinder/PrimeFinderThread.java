package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread {
    
    int a, b;
    private List<Integer> primes;
    private final Object monitor; // MISMO MONITOR QUE CONTROL
    private Control control;
    private volatile boolean paused = false;
    private volatile boolean finished = false;
    
    public PrimeFinderThread(int a, int b, Object monitor, Control control) {
        super();
        this.primes = new LinkedList<>();
        this.a = a;
        this.b = b;
        this.monitor = monitor; // Recibe el monitor compartido
        this.control = control;
    }
    
    @Override
    public void run() {
        try {
            for (int i = a; i < b && !finished; i++) {
                // Verificar si debemos pausar (EN EL MONITOR COMPARTIDO)
                synchronized(monitor) {
                    while (control.shouldPause() && !paused) {
                        paused = true;
                        control.threadPaused(); // Notificar al controlador
                        
                        // Esperar hasta que se reanude
                        while (control.shouldPause()) {
                            monitor.wait();
                        }
                        paused = false;
                    }
                }
                
                if (isPrime(i)) {
                    primes.add(i);
                    System.out.println(i);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            finished = true;
            control.threadFinished(); // Notificar que terminó
        }
    }
    
    boolean isPrime(int n) {
        boolean ans;
        if (n > 2) {
            ans = n % 2 != 0;
            for (int i = 3; ans && i * i <= n; i += 2) {
                ans = n % i != 0;
            }
        } else {
            ans = n == 2;
        }
        return ans;
    }
    
    public List<Integer> getPrimes() {
        return primes;
    }
    
    public int getPrimesCount() {
        return primes.size();
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public boolean isFinished() {
        return finished;
    }
}