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
import jakarta.servlet.http.HttpSession;

/**
 * King Faisal University – Chad Academic Information System v2.0 Returns
 * dashboard summary counts and full session info including role-specific IDs
 * (student_id / faculty_id) for use by frontend pages.
 */
@WebServlet("/DashboardServlet")
public class DashboardServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();

		HttpSession session = req.getSession(false);
		if (session == null) {
			res.setStatus(401);
			out.print("{\"error\":\"Not logged in\"}");
			return;
		}

		String role = session.getAttribute("role") instanceof String ? (String) session.getAttribute("role")
				: "unknown";
		String fullName = session.getAttribute("full_name") instanceof String
				? (String) session.getAttribute("full_name")
				: "";
		String uid = session.getAttribute("uid") instanceof String ? (String) session.getAttribute("uid") : "";
		int userId = session.getAttribute("user_id") instanceof Integer ? (Integer) session.getAttribute("user_id") : 0;

		try (Connection con = DBConnection.getConnection()) {
			StringBuilder sb = new StringBuilder("{");
			sb.append("\"role\":\"").append(safe(role)).append("\",");
			sb.append("\"name\":\"").append(safe(fullName)).append("\",");
			sb.append("\"uid\":\"").append(safe(uid)).append("\",");
			sb.append("\"user_id\":").append(userId).append(",");

			if ("student".equals(role)) {
				PreparedStatement ps = con
						.prepareStatement("SELECT student_id, roll_number, semester FROM students WHERE user_id=?");
				ps.setInt(1, userId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					sb.append("\"student_id\":").append(rs.getInt("student_id")).append(",");
					sb.append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",");
					sb.append("\"semester\":").append(rs.getInt("semester")).append(",");
				} else {
					sb.append("\"student_id\":0,\"roll_number\":\"\",\"semester\":1,");
				}
			} else if ("faculty".equals(role)) {
				PreparedStatement ps = con.prepareStatement(
						"SELECT f.faculty_id, f.employee_id, f.designation, f.qualification, d.dept_name "
								+ "FROM faculty f LEFT JOIN departments d ON f.dept_id=d.dept_id WHERE f.user_id=?");
				ps.setInt(1, userId);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					sb.append("\"faculty_id\":").append(rs.getInt("faculty_id")).append(",");
					sb.append("\"employee_id\":\"").append(safe(rs.getString("employee_id"))).append("\",");
					sb.append("\"designation\":\"").append(safe(rs.getString("designation"))).append("\",");
					sb.append("\"qualification\":\"").append(safe(rs.getString("qualification"))).append("\",");
					sb.append("\"dept_name\":\"").append(safe(rs.getString("dept_name"))).append("\",");
				} else {
					sb.append(
							"\"faculty_id\":0,\"employee_id\":\"\",\"designation\":\"\",\"qualification\":\"\",\"dept_name\":\"\",");
				}
			}

			ResultSet r1 = con.createStatement().executeQuery("SELECT COUNT(*) c FROM students");
			r1.next();
			sb.append("\"total_students\":").append(r1.getInt("c")).append(",");
			ResultSet r2 = con.createStatement().executeQuery("SELECT COUNT(*) c FROM faculty");
			r2.next();
			sb.append("\"total_faculty\":").append(r2.getInt("c")).append(",");
			ResultSet r3 = con.createStatement().executeQuery("SELECT COUNT(*) c FROM courses");
			r3.next();
			sb.append("\"total_courses\":").append(r3.getInt("c")).append(",");
			ResultSet r4 = con.createStatement().executeQuery("SELECT COUNT(*) c FROM notices WHERE is_urgent=1");
			r4.next();
			sb.append("\"urgent_notices\":").append(r4.getInt("c"));

			sb.append("}");
			out.print(sb);
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	private String safe(String s) {
		return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
	}
}