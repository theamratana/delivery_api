package com.delivery.deliveryapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        res.put("message", "pong");
        res.put("time", OffsetDateTime.now().toString());
        return res;
    }

    @GetMapping("/db/ping")
    public Map<String, Object> dbPing() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> res = new HashMap<>();
        res.put("status", one != null && one == 1 ? "ok" : "fail");
        res.put("db", one);
        res.put("time", OffsetDateTime.now().toString());
        return res;
    }
}
