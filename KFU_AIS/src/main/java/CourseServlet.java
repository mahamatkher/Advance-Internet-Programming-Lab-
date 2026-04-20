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
 * King Faisal University – Chad Academic Information System v2.0 Manages course
 * catalogue.
 */
@WebServlet("/CourseServlet")
public class CourseServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();

		try (Connection con = DBConnection.getConnection()) {
			ResultSet rs = con.createStatement().executeQuery("SELECT c.*,d.dept_name FROM courses c "
					+ "LEFT JOIN departments d ON c.dept_id=d.dept_id " + "ORDER BY c.course_name");
			StringBuilder sb = new StringBuilder("[");
			while (rs.next()) {
				if (sb.length() > 1)
					sb.append(",");
				// FIX #7: all string fields run through safe()
				sb.append("{").append("\"course_id\":").append(rs.getInt("course_id")).append(",")
						.append("\"course_name\":\"").append(safe(rs.getString("course_name"))).append("\",")
						.append("\"course_code\":\"").append(safe(rs.getString("course_code"))).append("\",")
						.append("\"credits\":").append(rs.getInt("credits")).append(",").append("\"semester\":")
						.append(rs.getInt("semester")).append(",").append("\"course_type\":\"")
						.append(safe(rs.getString("course_type"))).append("\",").append("\"dept_name\":\"")
						.append(safe(rs.getString("dept_name"))).append("\"").append("}");
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

		// FIX #8: null-check and parse-guard all numeric fields
		String nameParam = req.getParameter("course_name");
		String codeParam = req.getParameter("course_code");
		String creditsParam = req.getParameter("credits");
		String deptParam = req.getParameter("dept_id");
		String semParam = req.getParameter("semester");

		if (nameParam == null || nameParam.trim().isEmpty() || codeParam == null || codeParam.trim().isEmpty()
				|| creditsParam == null || creditsParam.trim().isEmpty() || deptParam == null
				|| deptParam.trim().isEmpty() || semParam == null || semParam.trim().isEmpty()) {
			res.setStatus(400);
			out.print("{\"error\":\"course_name, course_code, credits, dept_id, semester are required\"}");
			return;
		}

		int credits, deptId, semester;
		try {
			credits = Integer.parseInt(creditsParam.trim());
			deptId = Integer.parseInt(deptParam.trim());
			semester = Integer.parseInt(semParam.trim());
		} catch (NumberFormatException e) {
			res.setStatus(400);
			out.print("{\"error\":\"credits, dept_id, semester must be numbers\"}");
			return;
		}

		try (Connection con = DBConnection.getConnection()) {
			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO courses(course_name,course_code,credits,dept_id,semester,course_type) VALUES(?,?,?,?,?,?)");
			ps.setString(1, nameParam.trim());
			ps.setString(2, codeParam.trim());
			ps.setInt(3, credits);
			ps.setInt(4, deptId);
			ps.setInt(5, semester);
			ps.setString(6, req.getParameter("course_type") != null ? req.getParameter("course_type") : "Core");
			ps.executeUpdate();
			out.print("{\"status\":\"success\",\"message\":\"Course added\"}");
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	private String safe(String s) {
		return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
	}
}
