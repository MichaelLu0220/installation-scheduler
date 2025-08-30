package com.example.installation.service;
import com.example.installation.model.BomItem;
import com.example.installation.model.InstallationJob;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobService {
  private final Map<String, InstallationJob> store = new LinkedHashMap<>();

  @PostConstruct public void init(){
    InstallationJob j1=new InstallationJob();
    j1.setJobId("JOB-240918-0001"); j1.setStatus("Planned"); j1.setPriority("High");
    j1.setCustomerName("王小明"); j1.setCustomerPhone("0912-345-678");
    j1.setAddress("台北市信義區市府路45號");
    j1.setStart("2025-09-02T09:00:00"); j1.setEnd("2025-09-02T11:00:00"); j1.setSlaDue("2025-09-05T17:00:00");
    j1.getBom().add(new BomItem("AC-12000BTU","分離式冷氣主機",1,1,"TAO-WH1",null));
    j1.getBom().add(new BomItem("PIPE-20M","銅管組20M",1,0,null,"2025-09-01"));
    j1.getAssigned().addAll(Arrays.asList("W001","W015"));

    InstallationJob j2=new InstallationJob();
    j2.setJobId("JOB-240918-0002"); j2.setStatus("Assigned"); j2.setPriority("Normal");
    j2.setCustomerName("陳小姐"); j2.setCustomerPhone("0988-000-000");
    j2.setAddress("新北市板橋區文化路一段100號");
    j2.setStart("2025-09-02T13:30:00"); j2.setEnd("2025-09-02T15:00:00"); j2.setSlaDue("2025-09-04T12:00:00");
    j2.getBom().add(new BomItem("HWH-50L","電熱水器50L",1,1,"TP-WH2",null));
    j2.getAssigned().add("W009");

    InstallationJob j3=new InstallationJob();
    j3.setJobId("JOB-240918-0003"); j3.setStatus("Draft"); j3.setPriority("Low");
    j3.setCustomerName("張先生"); j3.setCustomerPhone("0933-666-999");
    j3.setAddress("桃園市中壢區中大路300號"); j3.setSlaDue("2025-09-10T18:00:00");
    j3.getBom().add(new BomItem("GAS-HOSE","瓦斯軟管",1,1,null,null));
    j3.getBom().add(new BomItem("VALVE-REG","調壓閥",1,1,null,null));

    store.put(j1.getJobId(), j1); store.put(j2.getJobId(), j2); store.put(j3.getJobId(), j3);
  }

  public List<InstallationJob> list(){ return new ArrayList<>(store.values()); }
  public Optional<InstallationJob> find(String id){ return Optional.ofNullable(store.get(id)); }
  public List<InstallationJob> search(String q){
    if(q==null||q.isBlank()) return list();
    String k=q.trim();
    return store.values().stream().filter(j -> j.getJobId().contains(k) || j.getCustomerName().contains(k) || j.getAddress().contains(k)).collect(Collectors.toList());
  }
}
