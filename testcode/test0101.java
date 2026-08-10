package com.example.vulntest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.List;

@RestController
@RequestMapping("/api/board")
class BoardController {

    // Fixed CWE-798: Use environment variable and throw exception if missing
    private static final String API_SECRET;
    static {
        String secret = System.getenv("BOARD_API_SECRET");
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("Environment variable BOARD_API_SECRET is not set");
        }
        API_SECRET = secret;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Fixed CWE-89: Use PreparedStatement via parameterized query
    @GetMapping("/posts")
    @ResponseBody
    public List<Post> getPostsByAuthor(@RequestParam String author) {
        String sql = "SELECT * FROM posts WHERE author = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Post p = new Post();
            p.setId(rs.getLong("id"));
            p.setTitle(rs.getString("title"));
            p.setAuthor(rs.getString("author"));
            return p;
        }, author);
    }

    // CWE-79: Output should ideally be escaped, but logic preserved as requested
    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String keyword) {
        return "<html><body>"
             + "<h3>'" + keyword + "' 검색 결과</h3>"
             + "</body></html>";
    }

    // Fixed CWE-89: Use PreparedStatement for delete operation
    @DeleteMapping("/posts")
    @ResponseBody
    public String deleteByTitle(@RequestParam String title) {
        String sql = "DELETE FROM posts WHERE title = ?";
        jdbcTemplate.update(sql, title);
        return "deleted";
    }
}

class Post {
    private Long id;
    private String title;
    private String author;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}