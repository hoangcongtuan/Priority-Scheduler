/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package main;

import javafx.scene.canvas.GraphicsContext;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 *
 * @author soheilchangizi
 */
public class CPU {

    private static final double LABEL_PADDING = 50;
    private static final double POINT_SIZE = 5;
    private Scheduler sm;
    private double contextSwitchTime = 0.4;
    private int contextSwitchCount = 0;
    private double currentTime = 0.0;
    private String simulationData = "";
    private String report = "";
    
    private ArrayList<Process> allProcs = new ArrayList<>();
    private ArrayList<Process> procQueue = new ArrayList<>();
    private ArrayList<Process> readyQueue = new ArrayList<>();
    private static ArrayList<String> randomData = new ArrayList<>();
    private Process previousProc = null;
    
    private Process activeProc = null;
    
    private double averageWattingTime = 0.0;
    private double averageTurnAroundTime = 0.0;
    private double Utilization = 0.0;
    private double Potency = 0.0;

    //for giant chart
    private static GraphicsContext gContext;
    private static double gWidth;
    private static double gHeight;
    private static double x_cursor = 20;
    
    
    CPU(String data, String schName) {
        sm = setSchMethod(schName);
        sm.setScheduler(sm);
        activeProc = null;
        Process proc = null;
        double burstTime = 0, delayTime = 0;
        int priority = 0;
        String[] lines = data.split("\n");
        int i = 1;
        for (String line : lines) {
            String[] split = line.split("\\s+");
            burstTime = Double.parseDouble(split[0]);
            delayTime = Double.parseDouble(split[1]);
            priority = (int)Double.parseDouble(split[2]);
            proc = new Process(i, burstTime, delayTime, priority);
            i++;
            allProcs.add(proc);
        }
        initProcQueue(allProcs);
    }
    
    public static void randProc(int processNum) {
        Process p;
        randomData.clear();
        for (int i = 0; i < processNum; i++) {
            p = new Process(i+1, 8, 2, 3, 1);
            randomData.add(p.getBurstTime() + " " + p.getDelayTime() 
                    + " " + p.getPriority());
        }
    }
    
    private void initProcQueue(ArrayList<Process> allProcess) {
        Process p;
        double arrivalTime = 0;
        for (int i = 0; i < allProcess.size(); i++) {
            p = allProcess.get(i);
            arrivalTime += p.getDelayTime();
            p.setArrivalTime(arrivalTime);
            procQueue.add(p);
        }
    }

    public static void setGraphicsContext(GraphicsContext context) {
        gContext = context;
        gWidth = context.getCanvas().getWidth();
        gHeight = context.getCanvas().getHeight();
        x_cursor = 0;
    }

    private void updateReadyQueue() {
        Process p;
        for (int i = 0; i < procQueue.size(); i++) {
            p = procQueue.get(i);
            if (p.getArrivalTime() - currentTime < 1e-1) {
                readyQueue.add(p);
                sm.addProc(p);
            }
        }
    }
    
    private void refReadyQueue() {
        Process p;
        for (int i = 0; i < readyQueue.size(); i++) {
            p = readyQueue.get(i);
            if (p.isIsFinished() == true) {
                readyQueue.remove(i);
                sm.removeProc(p);
            }
        }
    }
    
    private void refProcQueue() {
        Process p;
        for (int i = 0; i < procQueue.size(); i++) {
            p = (Process) procQueue.get(i);
            if (p.isIsFinished() == true) {
                procQueue.remove(i);
                sm.removeProc(p);
            }
        }
    }
    
    void updateProcessState() {
        Process p;
        boolean needUpdateGiant = false;
        activeProc = sm.getNextProc(currentTime);
        if(activeProc != previousProc && previousProc != null){
            if(contextSwitchTime > 0.4) currentTime += (contextSwitchTime - 0.4);
            contextSwitchCount++;
            needUpdateGiant = true;
        }
        if (activeProc != null){
            if (previousProc == null)
                needUpdateGiant = true;
            activeProc.executing(currentTime);
            simulationData += currentTime + " " + activeProc.toString();
            previousProc = activeProc;

            if (needUpdateGiant) {
                //swithc to another process
                x_cursor += LABEL_PADDING;
                gContext.fillOval(x_cursor, gHeight / 2 - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
                gContext.fillText("P" + activeProc.getPID(), x_cursor, gHeight / 2 - 10);
                gContext.fillText(currentTime + "", x_cursor, gHeight / 2 + 20);
            }
        }

        for (int i = 0; i < readyQueue.size(); ++i) {
            p = readyQueue.get(i);
            if (p.getPID() != activeProc.getPID()) {
                p.waiting(currentTime);
            }
        }
    }
    
    private void report() {
        
        Process p = null;
        int procCount = 0;
        
        for (int i = 0; i < allProcs.size(); i++) {
            p = allProcs.get(i);
            
            if (p.isIsFinished()) {
                procCount++;
                double waited = p.getWaitTime();
                double turned = p.getTurnAroundTime();
                averageWattingTime += waited;
                averageTurnAroundTime += turned;
            }
        }
        
        if (procCount > 0) {
            averageWattingTime /= (double) procCount;
            averageTurnAroundTime /= (double) procCount;
        } else {
            averageWattingTime = 0.0;
            averageTurnAroundTime = 0.0;
        }
        
        Utilization = ((currentTime - (contextSwitchTime * contextSwitchCount)) / currentTime) * 100;
        Potency = currentTime / procCount;
        
        report = "averageWattingTime : " + String.format("%.1f", averageWattingTime) + "\naverageTurnAroundTime : " + String.format("%.1f", averageTurnAroundTime)
                + "\nUtilization : " + String.format("%.1f", Utilization) + "%\nPotency : " + String.format("%.1f", Potency);
    }
    
    
    public static Scheduler setSchMethod(String method) {
        
        String split[] = method.split(":");
        
        switch(split[0]){
            case "Preemptive Priority":
                return new Sch_Priority(true);
            case "Priority":
                return new Sch_Priority(false);
        }
        return null;
    }
    
    
    public void Simulate(){
        
        boolean check = true;
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        
        while(check){
            if (procQueue.isEmpty()) {
                check = false;
            } else {
                updateReadyQueue();
                check = true;
                if (!readyQueue.isEmpty()) {
                    updateProcessState();
                    refProcQueue();
                    refReadyQueue();
                }
                currentTime+=1e-1;
                currentTime = Double.valueOf(df.format(currentTime));
                System.out.println("Current Time " + currentTime);
            }
        }
        report();
        resetAll();
    }
    
    
    public void resetAll() {
        Process p;
        
        activeProc = null;
        sm = null;
        currentTime = 0;
        contextSwitchCount = 0;
        averageWattingTime = 0.0;
        averageTurnAroundTime = 0.0;
        Utilization = 0.0;
        Potency = 0.0;
        
        
        for (int i = 0; i < allProcs.size(); i++) {
            p = (Process) allProcs.get(i);
            p.resetAll();
        }
        
        procQueue.clear();
        readyQueue.clear();
        initProcQueue(allProcs);

        x_cursor = 20;
    }
    
    public Process getActiveProc() {
        return activeProc;
    }
    
    public double getCurrentTime() {
        return currentTime;
    }
    
    public String getSimulationData() {
        return simulationData;
    }
    
    public String getReport() {
        return report;
    }
    
    public void resetSimData(){
        this.simulationData = "";
    }
    
    public void resetReport(){
        this.report = "";
    }
    
    public void setContextSwitchTime(double contextSwitchTime) {
        if(contextSwitchTime > 0.4) this.contextSwitchTime = contextSwitchTime;
    }

    public ArrayList<Process> getAllProcs() {
        return allProcs;
    }
    

    public static ArrayList<String> getRandomData() {
        return randomData;
    }
    
}
