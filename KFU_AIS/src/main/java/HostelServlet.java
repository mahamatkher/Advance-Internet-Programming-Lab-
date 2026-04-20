import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Manages hostel rooms and student allotments.
 */
@WebServlet("/HostelServlet")
public class HostelServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("allotments".equals(action)) {
                ResultSet rs = con.createStatement().executeQuery(
                    "SELECT ha.*,u.full_name,s.roll_number,r.room_number,r.block " +
                    "FROM hostel_allotments ha " +
                    "JOIN students s ON ha.student_id=s.student_id " +
                    "JOIN users u ON s.user_id=u.user_id " +
                    "JOIN hostel_rooms r ON ha.room_id=r.room_id " +
                    "WHERE ha.status='Active' ORDER BY ha.allot_id DESC");
                StringBuilder sb = new StringBuilder("[");
                while (rs.next()) {
                    if (sb.length() > 1) sb.append(",");
                    sb.append("{")
                      .append("\"allot_id\":").append(rs.getInt("allot_id")).append(",")
                      .append("\"student_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
                      .append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
                      .append("\"room_number\":\"").append(safe(rs.getString("room_number"))).append("\",")
                      .append("\"block\":\"").append(safe(rs.getString("block"))).append("\",")
                      .append("\"allot_date\":\"").append(safe(rs.getString("allot_date"))).append("\",")
                      .append("\"status\":\"").append(safe(rs.getString("status"))).append("\"")
                      .append("}");
                }
                sb.append("]");
                out.print(sb);
            } else {
                ResultSet rs = con.createStatement().executeQuery(
                    "SELECT * FROM hostel_rooms ORDER BY block, room_number");
                StringBuilder sb = new StringBuilder("[");
                while (rs.next()) {
                    if (sb.length() > 1) sb.append(",");
                    sb.append("{")
                      .append("\"room_id\":").append(rs.getInt("room_id")).append(",")
                      .append("\"room_number\":\"").append(safe(rs.getString("room_number"))).append("\",")
                      .append("\"block\":\"").append(safe(rs.getString("block"))).append("\",")
                      .append("\"room_type\":\"").append(safe(rs.getString("room_type"))).append("\",")
                      .append("\"floor\":").append(rs.getInt("floor")).append(",")
                      .append("\"capacity\":").append(rs.getInt("capacity")).append(",")
                      .append("\"occupied\":").append(rs.getInt("occupied")).append(",")
                      .append("\"monthly_fee\":").append(rs.getDouble("monthly_fee"))
                      .append("}");
                }
                sb.append("]");
                out.print(sb);
            }
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("allot".equals(action)) {
                String sidParam    = req.getParameter("student_id");
                String roomParam   = req.getParameter("room_id");
                if (sidParam == null || sidParam.trim().isEmpty() ||
                    roomParam == null || roomParam.trim().isEmpty()) {
                    res.setStatus(400); out.print("{\"error\":\"student_id and room_id are required\"}"); return;
                }
                int sid, roomId;
                try {
                    sid    = Integer.parseInt(sidParam.trim());
                    roomId = Integer.parseInt(roomParam.trim());
                } catch (NumberFormatException e) {
                    res.setStatus(400); out.print("{\"error\":\"student_id and room_id must be numbers\"}"); return;
                }

                // FIX #14: explicit check if room exists before capacity check
                PreparedStatement chk = con.prepareStatement(
                    "SELECT capacity, occupied FROM hostel_rooms WHERE room_id=?");
                chk.setInt(1, roomId);
                ResultSet rc = chk.executeQuery();
                if (!rc.next()) {
                    res.setStatus(404); out.print("{\"error\":\"Room not found\"}"); return;
                }
                if (rc.getInt("occupied") >= rc.getInt("capacity")) {
                    res.setStatus(409); out.print("{\"error\":\"Room is full\"}"); return;
                }

                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO hostel_allotments(student_id,room_id,allot_date,status) VALUES(?,?,CURDATE(),'Active')");
                ps.setInt(1, sid);
                ps.setInt(2, roomId);
                ps.executeUpdate();

                PreparedStatement upd = con.prepareStatement(
                    "UPDATE hostel_rooms SET occupied=occupied+1 WHERE room_id=?");
                upd.setInt(1, roomId);
                upd.executeUpdate();

                out.print("{\"status\":\"success\",\"message\":\"Room allotted successfully\"}");

            } else {
                // Add new room
                String numParam     = req.getParameter("room_number");
                String blockParam   = req.getParameter("block");
                String typeParam    = req.getParameter("room_type");
                String floorParam   = req.getParameter("floor");
                String capParam     = req.getParameter("capacity");
                String feeParam     = req.getParameter("monthly_fee");

                if (numParam == null || numParam.trim().isEmpty() ||
                    floorParam == null || floorParam.trim().isEmpty() ||
                    capParam == null || capParam.trim().isEmpty() ||
                    feeParam == null || feeParam.trim().isEmpty()) {
                    res.setStatus(400);
                    out.print("{\"error\":\"room_number, floor, capacity, monthly_fee are required\"}");
                    return;
                }
                int floor, capacity;
                double fee;
                try {
                    floor    = Integer.parseInt(floorParam.trim());
                    capacity = Integer.parseInt(capParam.trim());
                    fee      = Double.parseDouble(feeParam.trim());
                } catch (NumberFormatException e) {
                    res.setStatus(400); out.print("{\"error\":\"floor, capacity must be integers; monthly_fee must be a number\"}"); return;
                }
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO hostel_rooms(room_number,block,room_type,floor,capacity,occupied,monthly_fee) VALUES(?,?,?,?,?,0,?)");
                ps.setString(1, numParam.trim());
                ps.setString(2, blockParam != null ? blockParam.trim() : "");
                ps.setString(3, typeParam  != null ? typeParam.trim()  : "Double");
                ps.setInt   (4, floor);
                ps.setInt   (5, capacity);
                ps.setDouble(6, fee);
                ps.executeUpdate();
                out.print("{\"status\":\"success\",\"message\":\"Room added\"}");
            }
        } catch (SQLException e) {
            res.setStatus(500);
            out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
        }
    }

    private String safe(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
