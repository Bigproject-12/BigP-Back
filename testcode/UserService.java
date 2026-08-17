import java.sql.*;
import java.util.*;

public class UserService {

    private Connection connection;

    public UserService() throws Exception {
        connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/test",
            "root",
            "password123"
        );
    }

    public User findUser(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            User user = new User();
            user.id = rs.getInt("id");
            user.name = rs.getString("username");
            user.email = rs.getString("email");
            user.password = rs.getString("password");
            return user;
        }
        return null;
    }

    public boolean login(String username, String password) throws Exception {
        String sql = "SELECT * FROM users WHERE username = '" + username
                + "' AND password = '" + password + "'";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            System.out.println("Login success");
            return true;
        } else {
            System.out.println("Login failed");
            return false;
        }
    }

    public List<User> getUsers() throws Exception {
        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            User user = new User();
            user.id = rs.getInt("id");
            user.name = rs.getString("username");
            user.email = rs.getString("email");
            user.password = rs.getString("password");
            users.add(user);
        }

        return users;
    }

    public List<User> searchUsers(String keyword) throws Exception {
        List<User> result = new ArrayList<>();

        String sql = "SELECT * FROM users";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            String name = rs.getString("username");

            if (name != null && name.toLowerCase().contains(keyword.toLowerCase())) {
                User user = new User();
                user.id = rs.getInt("id");
                user.name = rs.getString("username");
                user.email = rs.getString("email");
                user.password = rs.getString("password");
                result.add(user);
            }
        }

        return result;
    }

    public void deleteUser(int id) throws Exception {
        String sql = "DELETE FROM users WHERE id = " + id;
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql);
        System.out.println("User deleted");
    }

    public void printUsers() throws Exception {
        List<User> users = getUsers();

        for (User user : users) {
            System.out.println("ID: " + user.id);
            System.out.println("Name: " + user.name);
            System.out.println("Email: " + user.email);
            System.out.println("Password: " + user.password);
        }
    }

    public void backupUsers() throws Exception {
        List<User> users = getUsers();

        for (User user : users) {
            System.out.println(
                user.id + "," + user.name + "," +
                user.email + "," + user.password
            );
        }
    }

    static class User {
        int id;
        String name;
        String email;
        String password;
    }
}