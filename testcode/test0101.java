import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/board")
class BoardController {

    // Fixed: Load secret from environment variable
    private static final String API_SECRET = System.getenv("BOARD_API_SECRET");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Fixed: Use PreparedStatement for SQL injection protection
    @GetMapping("/posts")
    @ResponseBody
    public List<Post> getPostsByAuthor(@RequestParam String author) {
        String sql = "SELECT * FROM posts WHERE author = ?";
        return jdbcTemplate.query(sql, new Object[]{author}, (rs, rowNum) -> {
            Post p = new Post();
            p.setId(rs.getLong("id"));
            p.setTitle(rs.getString("title"));
            p.setAuthor(rs.getString("author"));
            return p;
        });
    }

    // Fixed: Escape HTML output to prevent XSS
    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String keyword) {
        return "<html><body>"
             + "<h3>'" + escapeHtml(keyword) + "' 검색 결과</h3>"
             + "</body></html>";
    }

    // Fixed: Use PreparedStatement for DELETE operation
    @DeleteMapping("/posts")
    @ResponseBody
    public String deleteByTitle(@RequestParam String title) {
        String sql = "DELETE FROM posts WHERE title = ?";
        jdbcTemplate.update(sql, title);
        return "deleted";
    }

    // Helper method to escape HTML
    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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