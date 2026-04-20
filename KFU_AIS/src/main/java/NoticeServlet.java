import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Manages university notices and announcements.
 */
@WebServlet("/NoticeServlet")
public class NoticeServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT n.*,u.full_name AS posted_by_name " +
                "FROM notices n " +
                "JOIN users u ON n.posted_by=u.user_id " +
                "ORDER BY n.is_urgent DESC, n.created_at DESC");
            StringBuilder sb = new StringBuilder("[");
            while (rs.next()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("{")
                  .append("\"notice_id\":").append(rs.getInt("notice_id")).append(",")
                  .append("\"title\":\"").append(safe(rs.getString("title"))).append("\",")
                  .append("\"content\":\"").append(safe(rs.getString("content"))).append("\",")
                  .append("\"target_role\":\"").append(safe(rs.getString("target_role"))).append("\",")
                  .append("\"posted_by\":\"").append(safe(rs.getString("posted_by_name"))).append("\",")
                  .append("\"is_urgent\":").append(rs.getInt("is_urgent")).append(",")
                  .append("\"created_at\":\"").append(safe(rs.getString("created_at"))).append("\"")
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

        String title       = req.getParameter("title");
        String content     = req.getParameter("content");
        String postedParam = req.getParameter("posted_by");

        // FIX #17: null-check posted_by and title before parseInt
        if (title == null || title.trim().isEmpty()) {
            res.setStatus(400); out.print("{\"error\":\"title is required\"}"); return;
        }
        if (postedParam == null || postedParam.trim().isEmpty()) {
            res.setStatus(400); out.print("{\"error\":\"posted_by is required\"}"); return;
        }
        int postedBy;
        try { postedBy = Integer.parseInt(postedParam.trim()); }
        catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"posted_by must be a number\"}"); return;
        }

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO notices(title,content,target_role,posted_by,is_urgent) VALUES(?,?,?,?,?)");
            ps.setString(1, title.trim());
            ps.setString(2, content != null ? content : "");
            ps.setString(3, req.getParameter("target_role") != null ? req.getParameter("target_role") : "all");
            ps.setInt   (4, postedBy);
            ps.setInt   (5, "on".equals(req.getParameter("is_urgent")) || "1".equals(req.getParameter("is_urgent")) ? 1 : 0);
            ps.executeUpdate();
            out.print("{\"status\":\"success\",\"message\":\"Notice posted\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        // FIX #18: null-check id before parseInt
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            res.setStatus(400); out.print("{\"error\":\"id is required\"}"); return;
        }
        int id;
        try { id = Integer.parseInt(idParam.trim()); }
        catch (NumberFormatException e) {
            res.setStatus(400); out.print("{\"error\":\"id must be a number\"}"); return;
        }

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM notices WHERE notice_id=?");
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                res.setStatus(404); out.print("{\"error\":\"Notice not found\"}"); return;
            }
            out.print("{\"status\":\"success\",\"message\":\"Notice deleted\"}");
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    private String safe(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
