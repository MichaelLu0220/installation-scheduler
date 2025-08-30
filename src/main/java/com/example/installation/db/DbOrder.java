package com.example.installation.db;
import java.time.LocalDate;
public class DbOrder {
  private Long id; private String machineName; private LocalDate dueDate; private LocalDate etaDate; private String status;
  public Long getId(){return id;} public void setId(Long v){this.id=v;}
  public String getMachineName(){return machineName;} public void setMachineName(String v){this.machineName=v;}
  public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){this.dueDate=v;}
  public LocalDate getEtaDate(){return etaDate;} public void setEtaDate(LocalDate v){this.etaDate=v;}
  public String getStatus(){return status;} public void setStatus(String v){this.status=v;}
}
