import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Handles user authentication and session creation.
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        String uid  = req.getParameter("uid");
        String pass = req.getParameter("password");

        // FIX #24: never log the plain password
        System.out.println("[KFU] Login attempt – UID: [" + uid + "]");

        if (uid == null || uid.trim().isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"UID is required\"}");
            return;
        }
        if (pass == null || pass.isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"Password is required\"}");
            return;
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                "SELECT user_id, uid, full_name, role, email FROM users " +
                "WHERE uid = ? AND password = ? AND is_active = 1");
            ps.setString(1, uid.trim());
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HttpSession session = req.getSession(true);
                int    userId   = rs.getInt("user_id");
                String fullName = rs.getString("full_name");
                String role     = rs.getString("role");
                String email    = rs.getString("email");

                session.setAttribute("user_id",  userId);
                session.setAttribute("uid",       uid.trim());
                session.setAttribute("full_name", fullName);
                session.setAttribute("role",      role);
                session.setAttribute("email",     email);

                System.out.println("[KFU] Login SUCCESS – " + fullName + " (" + role + ")");
                out.print("{\"status\":\"success\",\"role\":\"" + role +
                          "\",\"name\":\"" + fullName +
                          "\",\"user_id\":" + userId + "}");
            } else {
                // Check whether UID exists at all (without exposing stored password)
                PreparedStatement chk = con.prepareStatement(
                    "SELECT is_active FROM users WHERE uid = ?");
                chk.setString(1, uid.trim());
                ResultSet rc = chk.executeQuery();
                if (rc.next()) {
                    int active = rc.getInt("is_active");
                    if (active == 0) {
                        out.print("{\"status\":\"error\",\"message\":\"Account is inactive. Contact administration.\"}");
                    } else {
                        out.print("{\"status\":\"error\",\"message\":\"Incorrect password.\"}");
                    }
                } else {
                    out.print("{\"status\":\"error\",\"message\":\"UID not found in King Faisal University database.\"}");
                }
            }

        } catch (SQLException e) {
            System.out.println("[KFU] SQL ERROR: " + DBConnection.errMsg(e));
            res.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"" + DBConnection.errMsg(e) + "\"}");
        } finally {
            DBConnection.close(con);
        }
    }
}
