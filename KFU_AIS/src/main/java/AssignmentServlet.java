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
 * assignments and student submissions.
 */
@WebServlet("/AssignmentServlet")
public class AssignmentServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		String action = req.getParameter("action");

		try (Connection con = DBConnection.getConnection()) {
			if ("submissions".equals(action)) {
				// FIX #4: null-check assign_id before use
				String aidParam = req.getParameter("assign_id");
				if (aidParam == null || aidParam.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"assign_id is required\"}");
					return;
				}
				int aid;
				try {
					aid = Integer.parseInt(aidParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"assign_id must be a number\"}");
					return;
				}
				// FIX #1: use PreparedStatement instead of string concatenation
				PreparedStatement ps = con
						.prepareStatement("SELECT sub.*,u.full_name,s.roll_number FROM submissions sub "
								+ "JOIN students s ON sub.student_id=s.student_id "
								+ "JOIN users u ON s.user_id=u.user_id " + "WHERE sub.assign_id=?");
				ps.setInt(1, aid);
				ResultSet rs = ps.executeQuery();
				StringBuilder sb = new StringBuilder("[");
				while (rs.next()) {
					if (sb.length() > 1)
						sb.append(",");
					sb.append("{").append("\"sub_id\":").append(rs.getInt("sub_id")).append(",")
							.append("\"student_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
							.append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
							.append("\"submitted_at\":\"").append(safe(rs.getString("submitted_at"))).append("\",")
							.append("\"status\":\"").append(safe(rs.getString("status"))).append("\",")
							.append("\"marks\":").append(rs.getDouble("marks")).append("}");
				}
				sb.append("]");
				out.print(sb);

			} else {
				ResultSet rs = con.createStatement()
						.executeQuery("SELECT a.*,c.course_name,u.full_name AS faculty_name FROM assignments a "
								+ "JOIN courses c ON a.course_id=c.course_id "
								+ "JOIN faculty f ON a.faculty_id=f.faculty_id "
								+ "JOIN users u ON f.user_id=u.user_id " + "ORDER BY a.created_at DESC");
				StringBuilder sb = new StringBuilder("[");
				while (rs.next()) {
					if (sb.length() > 1)
						sb.append(",");
					sb.append("{").append("\"assign_id\":").append(rs.getInt("assign_id")).append(",")
							.append("\"title\":\"").append(safe(rs.getString("title"))).append("\",")
							.append("\"description\":\"").append(safe(rs.getString("description"))).append("\",")
							.append("\"course_name\":\"").append(safe(rs.getString("course_name"))).append("\",")
							.append("\"faculty_name\":\"").append(safe(rs.getString("faculty_name"))).append("\",")
							.append("\"due_date\":\"").append(safe(rs.getString("due_date"))).append("\",")
							.append("\"max_marks\":").append(rs.getInt("max_marks")).append("}");
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
			if ("submit".equals(action)) {
				// FIX #2: null-check all required params
				if (!hasParams(req, "assign_id", "student_id")) {
					res.setStatus(400);
					out.print("{\"error\":\"assign_id and student_id are required\"}");
					return;
				}
				int assignId, studentId;
				try {
					assignId = Integer.parseInt(req.getParameter("assign_id").trim());
					studentId = Integer.parseInt(req.getParameter("student_id").trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"assign_id and student_id must be numbers\"}");
					return;
				}
				PreparedStatement ps = con.prepareStatement(
						"INSERT INTO submissions(assign_id, student_id, remarks, status) VALUES(?,?,?,'Submitted')");
				ps.setInt(1, assignId);
				ps.setInt(2, studentId);
				ps.setString(3, req.getParameter("remarks") != null ? req.getParameter("remarks") : "");
				ps.executeUpdate();
				out.print("{\"status\":\"success\",\"message\":\"Assignment submitted successfully\"}");

			} else {
				// FIX #5: null-check title and other required params
				if (!hasParams(req, "course_id", "faculty_id", "title", "due_date", "max_marks")) {
					res.setStatus(400);
					out.print("{\"error\":\"course_id, faculty_id, title, due_date, max_marks are required\"}");
					return;
				}
				int courseId, facultyId, maxMarks;
				try {
					courseId = Integer.parseInt(req.getParameter("course_id").trim());
					facultyId = Integer.parseInt(req.getParameter("faculty_id").trim());
					maxMarks = Integer.parseInt(req.getParameter("max_marks").trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"course_id, faculty_id, max_marks must be numbers\"}");
					return;
				}
				PreparedStatement ps = con.prepareStatement(
						"INSERT INTO assignments(course_id, faculty_id, title, description, due_date, max_marks) VALUES(?,?,?,?,?,?)");
				ps.setInt(1, courseId);
				ps.setInt(2, facultyId);
				ps.setString(3, req.getParameter("title").trim());
				ps.setString(4, req.getParameter("description") != null ? req.getParameter("description") : "");
				ps.setString(5, req.getParameter("due_date").trim());
				ps.setInt(6, maxMarks);
				ps.executeUpdate();
				out.print("{\"status\":\"success\",\"message\":\"Assignment posted\"}");
			}
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	private boolean hasParams(HttpServletRequest req, String... names) {
		for (String n : names) {
			String v = req.getParameter(n);
			if (v == null || v.trim().isEmpty())
				return false;
		}
		return true;
	}

	private String safe(String s) {
		return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
	}
}
