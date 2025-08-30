package com.example.installation.db;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.jdbc.core.RowMapper; import org.springframework.stereotype.Service;
import java.sql.*; import java.util.List;

@Service
public class DbOrderService {
  private final JdbcTemplate jdbc;
  public DbOrderService(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  private static class Mapper implements RowMapper<DbOrder> {
    @Override public DbOrder mapRow(ResultSet rs, int rowNum) throws SQLException{
      DbOrder o=new DbOrder(); o.setId(rs.getLong("id"));
      o.setMachineName(rs.getString("machine_name"));
      o.setDueDate(rs.getDate("due_date").toLocalDate());
      Date eta=rs.getDate("eta_date"); o.setEtaDate(eta==null?null:eta.toLocalDate());
      o.setStatus(rs.getString("status")); return o;
    }
  }
  public List<DbOrder> list(){ return jdbc.query("SELECT id,machine_name,due_date,eta_date,status FROM orders ORDER BY due_date,id", new Mapper()); }
}
