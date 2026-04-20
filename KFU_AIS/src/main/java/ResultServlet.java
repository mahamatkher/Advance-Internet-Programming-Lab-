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
 * King Faisal University – Chad Academic Information System v2.0 Manages
 * student exam results and grade calculation.
 */
@WebServlet("/ResultServlet")
public class ResultServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		String sidParam = req.getParameter("student_id");

		try (Connection con = DBConnection.getConnection()) {
			// FIX #1/#20: use PreparedStatement for student_id filter
			String base = "SELECT r.*,u.full_name,s.roll_number,c.course_name,c.course_code,c.credits "
					+ "FROM results r " + "JOIN students s ON r.student_id=s.student_id "
					+ "JOIN users u ON s.user_id=u.user_id " + "JOIN courses c ON r.course_id=c.course_id";

			ResultSet rs;
			if (sidParam != null && !sidParam.trim().isEmpty()) {
				int sid;
				try {
					sid = Integer.parseInt(sidParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"student_id must be a number\"}");
					return;
				}
				PreparedStatement ps = con
						.prepareStatement(base + " WHERE r.student_id=? ORDER BY r.semester, c.course_name");
				ps.setInt(1, sid);
				rs = ps.executeQuery();
			} else {
				rs = con.createStatement().executeQuery(base + " ORDER BY r.semester, c.course_name");
			}

			StringBuilder sb = new StringBuilder("[");
			while (rs.next()) {
				if (sb.length() > 1)
					sb.append(",");
				// FIX #20: read total from the DB generated column directly
				sb.append("{").append("\"result_id\":").append(rs.getInt("result_id")).append(",")
						.append("\"student_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
						.append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
						.append("\"course_name\":\"").append(safe(rs.getString("course_name"))).append("\",")
						.append("\"course_code\":\"").append(safe(rs.getString("course_code"))).append("\",")
						.append("\"credits\":").append(rs.getInt("credits")).append(",").append("\"semester\":")
						.append(rs.getInt("semester")).append(",").append("\"internal\":")
						.append(rs.getDouble("internal")).append(",").append("\"external\":")
						.append(rs.getDouble("external")).append(",").append("\"total\":").append(rs.getDouble("total"))
						.append(",").append("\"grade\":\"").append(safe(rs.getString("grade"))).append("\",")
						.append("\"grade_point\":").append(rs.getDouble("grade_point")).append("}");
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

		String sidParam = req.getParameter("student_id");
		String cidParam = req.getParameter("course_id");
		String semParam = req.getParameter("semester");
		String internalParam = req.getParameter("internal");
		String externalParam = req.getParameter("external");

		if (sidParam == null || sidParam.trim().isEmpty() || cidParam == null || cidParam.trim().isEmpty()
				|| semParam == null || semParam.trim().isEmpty() || internalParam == null
				|| internalParam.trim().isEmpty() || externalParam == null || externalParam.trim().isEmpty()) {
			res.setStatus(400);
			out.print("{\"error\":\"student_id, course_id, semester, internal, external are required\"}");
			return;
		}

		int sid, cid, sem;
		double internal, external;
		try {
			sid = Integer.parseInt(sidParam.trim());
			cid = Integer.parseInt(cidParam.trim());
			sem = Integer.parseInt(semParam.trim());
			internal = Double.parseDouble(internalParam.trim());
			external = Double.parseDouble(externalParam.trim());
		} catch (NumberFormatException e) {
			res.setStatus(400);
			out.print(
					"{\"error\":\"student_id, course_id, semester must be integers; internal and external must be numbers\"}");
			return;
		}

		// FIX #19: validate marks ranges
		if (internal < 0 || internal > 50) {
			res.setStatus(400);
			out.print("{\"error\":\"internal marks must be between 0 and 50\"}");
			return;
		}
		if (external < 0 || external > 50) {
			res.setStatus(400);
			out.print("{\"error\":\"external marks must be between 0 and 50\"}");
			return;
		}

		double total = internal + external;
		String grade = calcGrade(total);
		double gradePoint = calcGP(total);

		try (Connection con = DBConnection.getConnection()) {
			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO results(student_id,course_id,semester,internal,external,grade,grade_point,exam_year) "
							+ "VALUES(?,?,?,?,?,?,?,YEAR(CURDATE()))");
			ps.setInt(1, sid);
			ps.setInt(2, cid);
			ps.setInt(3, sem);
			ps.setDouble(4, internal);
			ps.setDouble(5, external);
			ps.setString(6, grade);
			ps.setDouble(7, gradePoint);
			ps.executeUpdate();

			out.print("{\"status\":\"success\",\"message\":\"Marks saved\"," + "\"grade\":\"" + grade + "\","
					+ "\"grade_point\":" + gradePoint + "," + "\"total\":" + total + "}");
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	private String calcGrade(double t) {
		if (t >= 91)
			return "O";
		if (t >= 81)
			return "A+";
		if (t >= 71)
			return "A";
		if (t >= 61)
			return "B+";
		if (t >= 51)
			return "B";
		if (t >= 41)
			return "C";
		return "F";
	}

	private double calcGP(double t) {
		if (t >= 91)
			return 10;
		if (t >= 81)
			return 9;
		if (t >= 71)
			return 8;
		if (t >= 61)
			return 7;
		if (t >= 51)
			return 6;
		if (t >= 41)
			return 5;
		return 0;
	}

	private String safe(String s) {
		return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
	}
}
