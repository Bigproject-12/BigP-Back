package com.example.vulntest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.sql.*;
import java.util.List;

/**
 * 패치된 코드 - 보안 취약점 수정
 * 1. 하드코딩된 시크릿 제거 (환경 변수 사용)
 * 2. SQL Injection 방지 (PreparedStatement 사용)
 * 3. Reflected XSS 방지 (HtmlUtils 이스케이프 처리)
 */
@RestController
@RequestMapping("/api/board")
class BoardController {

    // 수정: 환경 변수에서 시크릿을 로드하고, 없을 경우 예외 발생
    private static final String API_SECRET = System.getenv("BOARD_API_SECRET");
    
    static {
        if (API_SECRET == null || API_SECRET.isEmpty()) {
            throw new IllegalStateException("BOARD_API_SECRET environment variable is not set");
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 수정: PreparedStatement를 사용하여 SQL Injection 방지
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

    // 수정: HTML 이스케이프를 통해 XSS 방지
    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String keyword) {
        String escapedKeyword = HtmlUtils.htmlEscape(keyword);
        return "<html><body>"
             + "<h3>'" + escapedKeyword + "' 검색 결과</h3>"
             + "</body></html>";
    }

    // 수정: PreparedStatement를 사용하여 DELETE 쿼리 SQL 인젝션 방지
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