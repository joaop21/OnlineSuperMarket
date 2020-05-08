package middleware.server;

import java.util.LinkedList;
import java.util.Queue;

public class ConcurrentQueue<T> {
    Queue<T> queue = new LinkedList<>();

    /**
     * Blocking method that consumes the head of the queue when its available.
     *
     * @return Object Object that is inside of the queue.
     * */
    synchronized T poll(){
        while(this.queue.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return this.queue.poll();
    }

    /**
     *  Method that adds an object to the queue.
     *
     * @param obj Object to be inserted in the queue.
     * */
    synchronized void add(T obj){
        this.queue.add(obj);
        notify();
    }

}
