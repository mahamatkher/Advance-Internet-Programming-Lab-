import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Manages student attendance records.
 */
@WebServlet("/AttendanceServlet")
public class AttendanceServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();
        String sidParam = req.getParameter("student_id");

        try (Connection con = DBConnection.getConnection()) {
            // FIX #1: use PreparedStatement for optional student_id filter
            String sql = "SELECT a.*,u.full_name,s.roll_number,c.course_name " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id=s.student_id " +
                "JOIN users u ON s.user_id=u.user_id " +
                "JOIN courses c ON a.course_id=c.course_id";

            ResultSet rs;
            if (sidParam != null && !sidParam.trim().isEmpty()) {
                int sid;
                try { sid = Integer.parseInt(sidParam.trim()); }
                catch (NumberFormatException e) {
                    res.setStatus(400); out.print("{\"error\":\"student_id must be a number\"}"); return;
                }
                PreparedStatement ps = con.prepareStatement(sql + " WHERE a.student_id=? ORDER BY a.att_date DESC LIMIT 200");
                ps.setInt(1, sid);
                rs = ps.executeQuery();
            } else {
                rs = con.createStatement().executeQuery(sql + " ORDER BY a.att_date DESC LIMIT 200");
            }

            StringBuilder sb = new StringBuilder("[");
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("{")
                  .append("\"att_id\":").append(rs.getInt("att_id")).append(",")
                  .append("\"student_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
                  .append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
                  .append("\"course_name\":\"").append(safe(rs.getString("course_name"))).append("\",")
                  .append("\"date\":\"").append(safe(rs.getString("att_date"))).append("\",")
                  .append("\"status\":\"").append(safe(rs.getString("status"))).append("\"")
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

        // FIX #6: validate all required parameters before parsing
        String sidParam    = req.getParameter("student_id");
        String cidParam    = req.getParameter("course_id");
        String dateParam   = req.getParameter("date");

        if (sidParam == null || sidParam.trim().isEmpty() ||
            cidParam == null || cidParam.trim().isEmpty() ||
            dateParam == null || dateParam.trim().isEmpty()) {
            res.setStatus(400);
            out.print("{\"error\":\"student_id, course_id, and date are required\"}");
            return;
        }

        int sid, cid;
        try {
            sid = Integer.parseInt(sidParam.trim());
            cid = Integer.parseInt(cidParam.trim());
        } catch (NumberFormatException e) {
            res.setStatus(400);
            out.print("{\"error\":\"student_id and course_id must be numbers\"}");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO attendance(student_id, course_id, att_date, status) VALUES(?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE status=VALUES(status)");
            ps.setInt   (1, sid);
            ps.setInt   (2, cid);
            ps.setString(3, dateParam.trim());
            ps.setString(4, req.getParameter("status") != null ? req.getParameter("status") : "Present");
            ps.executeUpdate();
            out.print("{\"status\":\"success\",\"message\":\"Attendance marked\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    private String safe(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
