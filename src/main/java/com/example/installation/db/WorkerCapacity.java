package com.example.installation.db;

import java.time.LocalDate;

public class WorkerCapacity {
    private LocalDate workDate;
    private int workerCount;
    private int hoursPerWorker;
    private int unitsPerHour;
    private int totalUnitsPerDay;
    
    // 建構子
    public WorkerCapacity() {}
    
    public WorkerCapacity(LocalDate workDate, int workerCount, int hoursPerWorker, int unitsPerHour) {
        this.workDate = workDate;
        this.workerCount = workerCount;
        this.hoursPerWorker = hoursPerWorker;
        this.unitsPerHour = unitsPerHour;
        this.totalUnitsPerDay = workerCount * hoursPerWorker * unitsPerHour;
    }
    
    // Getters and Setters
    public LocalDate getWorkDate() {
        return workDate;
    }
    
    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }
    
    public int getWorkerCount() {
        return workerCount;
    }
    
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }
    
    public int getHoursPerWorker() {
        return hoursPerWorker;
    }
    
    public void setHoursPerWorker(int hoursPerWorker) {
        this.hoursPerWorker = hoursPerWorker;
    }
    
    public int getUnitsPerHour() {
        return unitsPerHour;
    }
    
    public void setUnitsPerHour(int unitsPerHour) {
        this.unitsPerHour = unitsPerHour;
    }
    
    public int getTotalUnitsPerDay() {
        return totalUnitsPerDay;
    }
    
    public void setTotalUnitsPerDay(int totalUnitsPerDay) {
        this.totalUnitsPerDay = totalUnitsPerDay;
    }
}