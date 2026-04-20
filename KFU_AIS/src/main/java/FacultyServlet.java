import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Manages faculty records.
 */
@WebServlet("/FacultyServlet")
public class FacultyServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT f.*,u.full_name,u.email,u.phone,u.uid,d.dept_name " +
                "FROM faculty f " +
                "JOIN users u ON f.user_id=u.user_id " +
                "LEFT JOIN departments d ON f.dept_id=d.dept_id " +
                "ORDER BY f.faculty_id DESC");
            StringBuilder sb = new StringBuilder("[");
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("{")
                  .append("\"faculty_id\":").append(rs.getInt("faculty_id")).append(",")
                  .append("\"name\":\"").append(safe(rs.getString("full_name"))).append("\",")
                  .append("\"uid\":\"").append(safe(rs.getString("uid"))).append("\",")
                  .append("\"employee_id\":\"").append(safe(rs.getString("employee_id"))).append("\",")
                  .append("\"email\":\"").append(safe(rs.getString("email"))).append("\",")
                  .append("\"phone\":\"").append(safe(rs.getString("phone"))).append("\",")
                  .append("\"designation\":\"").append(safe(rs.getString("designation"))).append("\",")
                  .append("\"qualification\":\"").append(safe(rs.getString("qualification"))).append("\",")
                  .append("\"dept_name\":\"").append(safe(rs.getString("dept_name"))).append("\",")
                  .append("\"dept_id\":").append(rs.getInt("dept_id")).append(",")
                  .append("\"joining_date\":\"").append(safe(rs.getString("joining_date"))).append("\"")
                  .append("}");
            }
            sb.append("]");
            out.print(sb);
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        String uid       = req.getParameter("uid");
        String password  = req.getParameter("password");
        String fullName  = req.getParameter("full_name");
        String deptParam = req.getParameter("dept_id");

        if (uid == null || uid.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            fullName == null || fullName.trim().isEmpty() ||
            deptParam == null || deptParam.trim().isEmpty()) {
            res.setStatus(400);
            out.print("{\"error\":\"uid, password, full_name, dept_id are required\"}");
            return;
        }

        int deptId;
        try { deptId = Integer.parseInt(deptParam.trim()); }
        catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"dept_id must be a number\"}"); return;
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            PreparedStatement pu = con.prepareStatement(
                "INSERT INTO users(uid,password,role,full_name,email,phone) VALUES(?,?,'faculty',?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, uid.trim());
            pu.setString(2, password);
            pu.setString(3, fullName.trim());
            pu.setString(4, req.getParameter("email") != null ? req.getParameter("email") : "");
            pu.setString(5, req.getParameter("phone") != null ? req.getParameter("phone") : "");
            pu.executeUpdate();

            ResultSet gk = pu.getGeneratedKeys();
            if (!gk.next()) throw new SQLException("Failed to obtain generated user_id");
            int newId = gk.getInt(1);

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO faculty(user_id,employee_id,dept_id,designation,qualification,joining_date) VALUES(?,?,?,?,?,?)");
            ps.setInt   (1, newId);
            ps.setString(2, req.getParameter("employee_id") != null ? req.getParameter("employee_id") : "");
            ps.setInt   (3, deptId);
            ps.setString(4, req.getParameter("designation") != null ? req.getParameter("designation") : "");
            ps.setString(5, req.getParameter("qualification") != null ? req.getParameter("qualification") : "");
            String jd = req.getParameter("joining_date");
            if (jd != null && !jd.trim().isEmpty()) ps.setString(6, jd.trim()); else ps.setNull(6, Types.DATE);
            ps.executeUpdate();

            con.commit();
            out.print("{\"status\":\"success\",\"message\":\"Faculty added successfully\"}");
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (Exception ignored) {}
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        } finally {
            DBConnection.close(con);
        }
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            res.setStatus(400); out.print("{\"error\":\"Faculty id is required\"}"); return;
        }
        int id;
        try { id = Integer.parseInt(idParam.trim()); }
        catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"id must be a number\"}"); return;
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            PreparedStatement find = con.prepareStatement("SELECT user_id FROM faculty WHERE faculty_id=?");
            find.setInt(1, id);
            ResultSet rs = find.executeQuery();
            // FIX #11 #12: return inside finally so connection is always closed
            if (!rs.next()) {
                res.setStatus(404);
                out.print("{\"error\":\"Faculty not found\"}");
                con.rollback();
                return;
            }
            int userId = rs.getInt("user_id");

            // Delete child records first
            PreparedStatement d1 = con.prepareStatement("DELETE FROM timetable WHERE faculty_id=?");
            d1.setInt(1, id); d1.executeUpdate();
            PreparedStatement d2 = con.prepareStatement("DELETE FROM assignments WHERE faculty_id=?");
            d2.setInt(1, id); d2.executeUpdate();
            PreparedStatement d3 = con.prepareStatement("DELETE FROM faculty WHERE faculty_id=?");
            d3.setInt(1, id); d3.executeUpdate();
            PreparedStatement d4 = con.prepareStatement("DELETE FROM users WHERE user_id=?");
            d4.setInt(1, userId); d4.executeUpdate();

            con.commit();
            out.print("{\"status\":\"success\",\"message\":\"Faculty deleted\"}");
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (Exception ignored) {}
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        } finally {
            DBConnection.close(con);
        }
    }

    private String safe(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
