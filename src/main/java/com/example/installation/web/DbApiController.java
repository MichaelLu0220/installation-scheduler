package com.example.installation.web;
import com.example.installation.db.DbOrder; import com.example.installation.db.DbOrderService;
import org.springframework.web.bind.annotation.*; import java.util.List;

@RestController @RequestMapping("/api/db")
public class DbApiController {
  private final DbOrderService svc;
  public DbApiController(DbOrderService svc){ this.svc = svc; }
  @GetMapping("/orders") public List<DbOrder> orders(){ return svc.list(); }
}
