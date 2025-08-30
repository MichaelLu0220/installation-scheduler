package com.example.installation.web;
import com.example.installation.model.InstallationJob;
import com.example.installation.service.JobService;
import org.springframework.web.bind.annotation.*; import java.util.List;

@RestController @RequestMapping("/api/jobs")
public class ApiController {
  private final JobService jobService;
  public ApiController(JobService jobService){ this.jobService=jobService; }
  @GetMapping public List<InstallationJob> list(@RequestParam(value="q", required=false) String q){ return jobService.search(q); }
  @GetMapping("/{id}") public InstallationJob detail(@PathVariable String id){ return jobService.find(id).orElse(null); }
}
