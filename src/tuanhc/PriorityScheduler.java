/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package tuanhc;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class PriorityScheduler {

    private Process activeProc;
    private boolean preemptive;
    private PriorityQueue<Process> pq;
    
    public PriorityScheduler(boolean isPreemptive) {
        preemptive = isPreemptive;
        pq = new PriorityQueue<>(new Comparator<Process>() {
            @Override
            public int compare(Process o1, Process o2) {
                return (o1.getPriority() >= o2.getPriority()) ? 1 : -1;
            }
        });
    }
    
    public void addProc(Process p) {
        pq.add(p);
    }
    
    public boolean removeProc(Process p) {
        return pq.remove(p);
    }
    
    public void setScheduler(PriorityScheduler method) {
        Iterator<Process> itr = pq.iterator();
        while(itr.hasNext()){
            method.addProc(itr.next());
            itr.remove();
        }
    }

    public Process getNextProc(double currentTime) {
        if (((isPreemptive() && pq.peek().isIsArrived()) || activeProc == null || activeProc.isIsFinished())) {
            activeProc = pq.peek();
        }
        return activeProc;
    }
    
    public String getName() {
        return !isPreemptive() ? "Priority" : "Premetive Priority";
    }
    
    public boolean isPreemptive() {
        return preemptive;
    }
    
    public boolean isProcLeft() {
        return !pq.isEmpty();
    }
    
}
