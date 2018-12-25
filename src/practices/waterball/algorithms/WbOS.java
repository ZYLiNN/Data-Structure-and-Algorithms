package practices.waterball.algorithms;

import dsa.algorithms.OS;

import java.util.LinkedList;

import static dsa.Utils.*;

public class WbOS implements OS {

    public static class Banker implements OS.Banker{

        @Override
        public int[] safetyAlgorithm(int n, int m, int[] available, int[][] max, int[][] allocation, int[][] need) {
            int finishCount = 0;
            int[] safeSequence = new int[n];
            boolean[] finish = new boolean[n];
            boolean hasFoundNextSafeAllocation;

            do {  // this loop has [n + (n-1) + ... + 1] = n(n+1)/2 = O(n^2)
                hasFoundNextSafeAllocation = false;
                // found through process
                for (int i = 0; i < n; i ++)
                {
                    // find the next unfinished process which its need is less then available amount
                    if (!finish[i] && arrayLessOrEqual(need[i], available))
                    {
                        // this process can finish its work and return those resources
                        available = arrayPlus(available, allocation[i]);  //O(m)
                        finish[i] = true;
                        hasFoundNextSafeAllocation = true;
                        safeSequence[finishCount++] = i;
                    }
                }
            } while (hasFoundNextSafeAllocation);

            for (int i = 0; i < finish.length; i ++)
                if (!finish[i])
                    return null;
            return safeSequence; // this algorithm has O(m*n^2)
        }


        @Override
        public RequestGrantState resourceRequestAlgorithm(int n, int m, int[] available, int[][] max, int[][] allocation, int[][] need, int[][] request) {
            for(int i = 0; i < n; i ++)
            {
                if (arrayLessOrEqual(request[i], need[i]))
                {
                    if (arrayLessOrEqual(request[i] , available))
                    {
                        // have the system pretend to have allocated the requested resources to process
                        available = arraySub(available, request[i]);
                        allocation[i] = arrayPlus(allocation[i], request[i]);
                        need[i] = arraySub(need[i], request[i]);
                    }
                    else
                    {
                        System.out.println("The process " + i + " must wait, since the resources are not available.");
                        return RequestGrantState.mustWait(i);
                    }
                }
                else
                    return RequestGrantState.nonAvailable();
            }

            return safetyAlgorithm(n, m, available, max, allocation, need) == null ?
                    RequestGrantState.unsafeNotGranted() : RequestGrantState.safeGranted();
        }

    }

    public static class Semaphore{
        private int value;

        public Semaphore(int value) {
            this.value = value;
        }

        public synchronized void sWait(){
            value --;
            if (value < 0) {
                try {
                    this.wait();
                } catch (InterruptedException ignored) { }
            }
        }

        public synchronized void sSignal(){
            value ++;
            if (value <= 0)
                this.notify();
        }
    }

    /**
     * Call P as signaling processes, Q as waiting processes.
     * The monitor conforms the Hoare's monitor property:
     * Any signaling process P waits Q until Q's completion or Q is blocked again
     */
    public static class Monitor {
        private Semaphore mutex = new Semaphore(1);  // monitor's mutual exclusion
        private Semaphore next = new Semaphore(0); // Used to block P
        private int nextCount = 0;  // number of P

        public void doProcedure(Runnable runnable){
            mutex.sWait();
            runnable.run();
            if (nextCount > 0)
                next.sSignal();  // Q finished, wake P up
            else
                mutex.sSignal();  // No P waiting, return mutex
        }

        public class Condition {
            private int value;  // number of Q
            private Semaphore sem = new Semaphore(0); // Used to block Q

            void cWait(){
                value ++;
                if (nextCount > 0)
                    next.sSignal();  // Q is blocked again, wake P up to exit
                else
                    mutex.sSignal();  // Q is blocked again while no P waiting, return mutex
                sem.sWait();
                value --;
            }

            void cSignal(){
                if (value > 0) // there exists Q, do signaling process
                {
                    nextCount ++;
                    sem.sSignal();  // wake one Q up
                    next.sWait();  // wait the waked Q until Q's completion or Q's blocked again
                    nextCount --;
                }
            }
        }
    }

    public static class DiningMonitor extends Monitor implements OS.DiningMonitor {
        public  enum State{ EATING, HUNGRY, THINKING}
        private final int n;
        private Monitor.Condition[] philosophers;
        private State[] states;

        public DiningMonitor(int n) {
            this.n = n;
            states = new State[n];
            philosophers = new Monitor.Condition[n];
            for (int i = 0; i < n; i++) {
                philosophers[i] = this.new Condition();
            }
        }

        @Override
        public void pick(int i) throws InterruptedException {
            doProcedure(()->{
                states[i] = State.HUNGRY;
                if (!test(i))
                    philosophers[i].cWait();
                System.out.println("Philosopher " + i + " is eating.");
                states[i] = State.EATING;
            });
        }

        @Override
        public void putDown(int i) throws InterruptedException{
            doProcedure(()->{
                System.out.println("Philosopher " + i + " is putting down.");
                states[i] = State.THINKING;
                if (test((i+1) % n))
                    philosophers[(i+1) % n].cSignal();
                if (test((i+n-1) % n))
                    philosophers[(i+n-1) % n].cSignal();
            });
        }

        private boolean test(int i){  //test right and left and see if he's hungry
            return states[(i+n-1) % n] != State.EATING &&
                    states[(i+1) % n] != State.EATING &&
                    states[i] == State.HUNGRY;
        }
    }
}
