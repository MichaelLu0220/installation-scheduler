package com.example.installation.web;
import com.example.installation.db.DbOrderService;
import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class DbPageController {
  private final DbOrderService svc;
  public DbPageController(DbOrderService svc){ this.svc = svc; }
  @GetMapping("/db/orders") public String orders(Model model){ model.addAttribute("orders", svc.list()); return "db-orders"; }
}
