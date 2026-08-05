package com.example.vulntest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.sql.*;
import java.util.List;

/*
 * 테스트용 샘플 - 보안 취약점이 패치된 버전
 */
@RestController
@RequestMapping("/api/board")
class BoardController {

    // 취약점 1 수정: 하드코딩 대신 환경 변수에서 로드
    private final String API_SECRET = System.getenv("BOARD_API_SECRET_KEY");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 취약점 2 수정: Prepared를 사용하여 SQL Injection 방지
    @GetMapping("/posts")
    @ResponseBody
    public List<Post> getPostsByAuthor(@RequestParam String author) {
        String sql = "SELECT * FROM posts WHERE author = ?";
        return jdbcTemplate.query(sql, author, (rs, rowNum) -> {
            Post p = new Post();
            p.setId(rs.getLong("id"));
            p.setTitle(rs.getString("title"));
            p.setAuthor(rs.getString("author"));
            return p;
        });
    }

    // 취약점 3 수정: HTML 이스케이프를 통해 Reflected XSS 방지
    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String keyword) {
        String escapedKeyword = HtmlUtils.htmlEscape(keyword);
        return "<html><body>" +
               + "<h3>'" + escapedKeyword + "' 검색 결과</h3>" +
               + "</body></html>";
    }

    // 취약점 2-b 수정: Prepared를 사용하여 SQL Injection 방지
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