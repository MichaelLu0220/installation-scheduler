package com.example.installation.model;
public class BomItem {
  private String itemCode; private String name; private int qty;
  private Integer reservedQty; private String location; private String eta;
  public BomItem() {}
  public BomItem(String itemCode, String name, int qty, Integer reservedQty, String location, String eta){
    this.itemCode=itemCode; this.name=name; this.qty=qty; this.reservedQty=reservedQty; this.location=location; this.eta=eta;
  }
  public String getItemCode(){return itemCode;} public void setItemCode(String v){this.itemCode=v;}
  public String getName(){return name;} public void setName(String v){this.name=v;}
  public int getQty(){return qty;} public void setQty(int v){this.qty=v;}
  public Integer getReservedQty(){return reservedQty;} public void setReservedQty(Integer v){this.reservedQty=v;}
  public String getLocation(){return location;} public void setLocation(String v){this.location=v;}
  public String getEta(){return eta;} public void setEta(String v){this.eta=v;}
}
