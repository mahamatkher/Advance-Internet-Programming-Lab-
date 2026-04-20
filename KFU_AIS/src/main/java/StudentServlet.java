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
 * Manages student registration, profiles, and deletion.
 */
@WebServlet("/StudentServlet")
public class StudentServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();
        String idParam = req.getParameter("id");

        try (Connection con = DBConnection.getConnection()) {
            String base = "SELECT s.*,u.full_name,u.email,u.phone,u.uid,d.dept_name " +
                          "FROM students s " +
                          "JOIN users u ON s.user_id=u.user_id " +
                          "LEFT JOIN departments d ON s.dept_id=d.dept_id";
            ResultSet rs;
            // FIX #1: PreparedStatement for id filter
            if (idParam != null && !idParam.trim().isEmpty()) {
                int id;
                try { id = Integer.parseInt(idParam.trim()); }
                catch (NumberFormatException e) {
                    res.setStatus(400); out.print("{\"error\":\"id must be a number\"}"); return;
                }
                PreparedStatement ps = con.prepareStatement(base + " WHERE s.student_id=? ORDER BY s.student_id DESC");
                ps.setInt(1, id);
                rs = ps.executeQuery();
            } else {
                rs = con.createStatement().executeQuery(base + " ORDER BY s.student_id DESC");
            }

            StringBuilder sb = new StringBuilder("[");
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("{")
                  .append("\"student_id\":").append(rs.getInt("student_id")).append(",")
                  .append("\"user_id\":").append(rs.getInt("user_id")).append(",")
                  .append("\"name\":\"").append(safe(rs.getString("full_name"))).append("\",")
                  .append("\"uid\":\"").append(safe(rs.getString("uid"))).append("\",")
                  .append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
                  .append("\"email\":\"").append(safe(rs.getString("email"))).append("\",")
                  .append("\"phone\":\"").append(safe(rs.getString("phone"))).append("\",")
                  .append("\"dept_name\":\"").append(safe(rs.getString("dept_name"))).append("\",")
                  .append("\"dept_id\":").append(rs.getInt("dept_id")).append(",")
                  .append("\"semester\":").append(rs.getInt("semester")).append(",")
                  .append("\"section\":\"").append(safe(rs.getString("section"))).append("\",")
                  .append("\"batch\":\"").append(safe(rs.getString("batch"))).append("\",")
                  .append("\"gender\":\"").append(safe(rs.getString("gender"))).append("\",")
                  .append("\"dob\":\"").append(safe(rs.getString("dob"))).append("\"")
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
        String rollNo    = req.getParameter("roll_number");
        String deptParam = req.getParameter("dept_id");
        String semParam  = req.getParameter("semester");

        if (uid == null || uid.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            fullName == null || fullName.trim().isEmpty() ||
            rollNo == null || rollNo.trim().isEmpty() ||
            deptParam == null || deptParam.trim().isEmpty() ||
            semParam == null || semParam.trim().isEmpty()) {
            res.setStatus(400);
            out.print("{\"error\":\"uid, password, full_name, roll_number, dept_id, semester are required\"}");
            return;
        }
        int deptId, semester;
        try {
            deptId   = Integer.parseInt(deptParam.trim());
            semester = Integer.parseInt(semParam.trim());
        } catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"dept_id and semester must be numbers\"}"); return;
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            PreparedStatement pu = con.prepareStatement(
                "INSERT INTO users(uid,password,role,full_name,email,phone) VALUES(?,?,'student',?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, uid.trim());
            pu.setString(2, password);
            pu.setString(3, fullName.trim());
            pu.setString(4, req.getParameter("email") != null ? req.getParameter("email") : "");
            pu.setString(5, req.getParameter("phone") != null ? req.getParameter("phone") : "");
            pu.executeUpdate();

            ResultSet gk = pu.getGeneratedKeys();
            if (!gk.next()) throw new SQLException("Failed to obtain generated user_id");
            int newUserId = gk.getInt(1);

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO students(user_id,roll_number,dept_id,semester,section,batch,dob,gender) VALUES(?,?,?,?,?,?,?,?)");
            ps.setInt   (1, newUserId);
            ps.setString(2, rollNo.trim());
            ps.setInt   (3, deptId);
            ps.setInt   (4, semester);
            ps.setString(5, req.getParameter("section") != null ? req.getParameter("section") : "");
            ps.setString(6, req.getParameter("batch")   != null ? req.getParameter("batch")   : "");
            String dob = req.getParameter("dob");
            if (dob != null && !dob.trim().isEmpty()) ps.setString(7, dob.trim()); else ps.setNull(7, Types.DATE);
            ps.setString(8, req.getParameter("gender") != null ? req.getParameter("gender") : "Male");
            ps.executeUpdate();

            con.commit();
            out.print("{\"status\":\"success\",\"message\":\"Student registered successfully\"}");
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (Exception ignored) {}
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        } finally {
            DBConnection.close(con);
        }
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        String sidParam  = req.getParameter("student_id");
        String deptParam = req.getParameter("dept_id");
        String semParam  = req.getParameter("semester");
        if (sidParam == null || sidParam.trim().isEmpty() ||
            deptParam == null || deptParam.trim().isEmpty() ||
            semParam == null || semParam.trim().isEmpty()) {
            res.setStatus(400); out.print("{\"error\":\"student_id, dept_id, semester are required\"}"); return;
        }
        int sid, deptId, semester;
        try {
            sid      = Integer.parseInt(sidParam.trim());
            deptId   = Integer.parseInt(deptParam.trim());
            semester = Integer.parseInt(semParam.trim());
        } catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"student_id, dept_id, semester must be numbers\"}"); return;
        }

        // FIX #21: wrap PUT in a transaction
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            PreparedStatement pu = con.prepareStatement(
                "UPDATE users SET full_name=?,email=?,phone=? " +
                "WHERE user_id=(SELECT user_id FROM students WHERE student_id=?)");
            pu.setString(1, req.getParameter("full_name") != null ? req.getParameter("full_name") : "");
            pu.setString(2, req.getParameter("email")     != null ? req.getParameter("email")     : "");
            pu.setString(3, req.getParameter("phone")     != null ? req.getParameter("phone")     : "");
            pu.setInt   (4, sid);
            pu.executeUpdate();

            PreparedStatement ps = con.prepareStatement(
                "UPDATE students SET dept_id=?,semester=?,section=?,batch=? WHERE student_id=?");
            ps.setInt   (1, deptId);
            ps.setInt   (2, semester);
            ps.setString(3, req.getParameter("section") != null ? req.getParameter("section") : "");
            ps.setString(4, req.getParameter("batch")   != null ? req.getParameter("batch")   : "");
            ps.setInt   (5, sid);
            ps.executeUpdate();

            con.commit();
            out.print("{\"status\":\"success\",\"message\":\"Student updated\"}");
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
            res.setStatus(400); out.print("{\"error\":\"id is required\"}"); return;
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

            PreparedStatement find = con.prepareStatement(
                "SELECT user_id FROM students WHERE student_id=?");
            find.setInt(1, id);
            ResultSet rs = find.executeQuery();
            if (!rs.next()) {
                res.setStatus(404); out.print("{\"error\":\"Student not found\"}");
                con.rollback(); return;
            }
            int userId = rs.getInt("user_id");

            // Delete all child records (ordered by FK dependency)
            PreparedStatement[] deletes = {
                prepared(con, "DELETE FROM hostel_allotments WHERE student_id=?", id),
                prepared(con, "DELETE FROM submissions WHERE student_id=?",        id),
                prepared(con, "DELETE FROM attendance  WHERE student_id=?",        id),
                prepared(con, "DELETE FROM results     WHERE student_id=?",        id),
                prepared(con, "DELETE FROM fees        WHERE student_id=?",        id),
                // FIX #22: also clean library_issues linked through user_id
                prepared(con, "DELETE FROM library_issues WHERE user_id=?",        userId),
                prepared(con, "DELETE FROM students WHERE student_id=?",           id),
                prepared(con, "DELETE FROM users    WHERE user_id=?",              userId)
            };
            for (PreparedStatement d : deletes) d.executeUpdate();

            con.commit();
            out.print("{\"status\":\"success\",\"message\":\"Student deleted\"}");
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (Exception ignored) {}
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        } finally {
            DBConnection.close(con);
        }
    }

    private PreparedStatement prepared(Connection con, String sql, int param) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, param);
        return ps;
    }

    private String safe(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
