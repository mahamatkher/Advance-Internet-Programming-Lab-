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
 * King Faisal University – Chad Academic Information System v2.0 Manages class
 * timetable / schedule slots.
 */
@WebServlet("/TimetableServlet")
public class TimetableServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();

		try (Connection con = DBConnection.getConnection()) {
			ResultSet rs = con.createStatement()
					.executeQuery("SELECT t.*,c.course_name,c.course_code,u.full_name AS faculty_name,d.dept_name "
							+ "FROM timetable t " + "JOIN courses c ON t.course_id=c.course_id "
							+ "JOIN faculty f ON t.faculty_id=f.faculty_id " + "JOIN users u ON f.user_id=u.user_id "
							+ "LEFT JOIN departments d ON t.dept_id=d.dept_id "
							+ "ORDER BY FIELD(t.day_of_week,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'), t.start_time");
			StringBuilder sb = new StringBuilder("[");
			while (rs.next()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("{").append("\"slot_id\":").append(rs.getInt("slot_id")).append(",")
						.append("\"course_name\":\"").append(safe(rs.getString("course_name"))).append("\",")
						.append("\"course_code\":\"").append(safe(rs.getString("course_code"))).append("\",")
						.append("\"faculty_name\":\"").append(safe(rs.getString("faculty_name"))).append("\",")
						.append("\"day\":\"").append(safe(rs.getString("day_of_week"))).append("\",")
						.append("\"start_time\":\"").append(safe(rs.getString("start_time"))).append("\",")
						.append("\"end_time\":\"").append(safe(rs.getString("end_time"))).append("\",")
						.append("\"room\":\"").append(safe(rs.getString("room"))).append("\",").append("\"semester\":")
						.append(rs.getInt("semester")).append(",").append("\"section\":\"")
						.append(safe(rs.getString("section"))).append("\"").append("}");
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

		String cidParam = req.getParameter("course_id");
		String fidParam = req.getParameter("faculty_id");
		String didParam = req.getParameter("dept_id");
		String semParam = req.getParameter("semester");
		String dayParam = req.getParameter("day_of_week");
		String startParam = req.getParameter("start_time");
		String endParam = req.getParameter("end_time");

		if (cidParam == null || cidParam.trim().isEmpty() || fidParam == null || fidParam.trim().isEmpty()
				|| didParam == null || didParam.trim().isEmpty() || semParam == null || semParam.trim().isEmpty()
				|| dayParam == null || dayParam.trim().isEmpty() || startParam == null || startParam.trim().isEmpty()
				|| endParam == null || endParam.trim().isEmpty()) {
			res.setStatus(400);
			out.print(
					"{\"error\":\"course_id, faculty_id, dept_id, semester, day_of_week, start_time, end_time are required\"}");
			return;
		}

		int courseId, facultyId, deptId, semester;
		try {
			courseId = Integer.parseInt(cidParam.trim());
			facultyId = Integer.parseInt(fidParam.trim());
			deptId = Integer.parseInt(didParam.trim());
			semester = Integer.parseInt(semParam.trim());
		} catch (NumberFormatException e) {
			res.setStatus(400);
			out.print("{\"error\":\"course_id, faculty_id, dept_id, semester must be numbers\"}");
			return;
		}

		try (Connection con = DBConnection.getConnection()) {
			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO timetable(course_id,faculty_id,dept_id,semester,section,day_of_week,start_time,end_time,room) "
							+ "VALUES(?,?,?,?,?,?,?,?,?)");
			ps.setInt(1, courseId);
			ps.setInt(2, facultyId);
			ps.setInt(3, deptId);
			ps.setInt(4, semester);
			ps.setString(5, req.getParameter("section") != null ? req.getParameter("section") : "");
			ps.setString(6, dayParam.trim());
			ps.setString(7, startParam.trim());
			ps.setString(8, endParam.trim());
			ps.setString(9, req.getParameter("room") != null ? req.getParameter("room") : "");
			ps.executeUpdate();
			out.print("{\"status\":\"success\",\"message\":\"Timetable slot added\"}");
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();

		// FIX #23: null-check id before parseInt
		String idParam = req.getParameter("id");
		if (idParam == null || idParam.trim().isEmpty()) {
			res.setStatus(400);
			out.print("{\"error\":\"id is required\"}");
			return;
		}
		int id;
		try {
			id = Integer.parseInt(idParam.trim());
		} catch (NumberFormatException e) {
			res.setStatus(400);
			out.print("{\"error\":\"id must be a number\"}");
			return;
		}

		try (Connection con = DBConnection.getConnection()) {
			PreparedStatement ps = con.prepareStatement("DELETE FROM timetable WHERE slot_id=?");
			ps.setInt(1, id);
			int deleted = ps.executeUpdate();
			if (deleted == 0) {
				res.setStatus(404);
				out.print("{\"error\":\"Slot not found\"}");
				return;
			}
			out.print("{\"status\":\"success\",\"message\":\"Slot deleted\"}");
		} catch (SQLException e) {
			res.setStatus(500);
			out.print("{\"error\":\"" + DBConnection.errMsg(e) + "\"}");
		}
	}

	private String safe(String s) {
		return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
	}
}
