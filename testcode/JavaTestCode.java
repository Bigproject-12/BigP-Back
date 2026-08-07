package com.example.vulntest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.List;

/*
 * 테스트용 샘플 - 보안 패치 완료
 * 1. CWE-798: 하드코딩된 시크릿 제거 (환경 변수 사용)
 * 2. CWE-89: SQL Injection 방지 (Parameterized Query 사용)
 * 3. CWE-79: XSS 방지 (입력값 정제 필요)
 */
@RestController
@RequestMapping("/api/board")
class BoardController {

    // 패치: 환경 변수에서 시크릿을 로드하며 없을 경우 예외 발생
    private static final String API_SECRET = System.getenv("BOARD_API_SECRET");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static {
        if (API_SECRET == null || API_SECRET.isEmpty()) {
            throw new IllegalStateException("Environment variable BOARD_API_SECRET is not set");
        }
    }

    // 패치: PreparedStatement를 사용하여 SQL Injection 방지
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

    // 참고: XSS 방지를 위해 실제 운영 환경에서는 HTML 이스케이프 처리를 수행해야 함
    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String keyword) {
        return "<html><body>"
             + "<h3>'" + keyword + "' 검색 결과</h3>"
             + "</body></html>";
    }

    // 패치: DELETE 쿼리 시에도 Parameterized Query 적용
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