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
 * student fee records and payments.
 */
@WebServlet("/FeeServlet")
public class FeeServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		String sidParam = req.getParameter("student_id");

		try (Connection con = DBConnection.getConnection()) {
			String base = "SELECT f.*,u.full_name,s.roll_number " + "FROM fees f "
					+ "JOIN students s ON f.student_id=s.student_id " + "JOIN users u ON s.user_id=u.user_id";

			ResultSet rs;
			// FIX #1: PreparedStatement for student_id filter
			if (sidParam != null && !sidParam.trim().isEmpty()) {
				int sid;
				try {
					sid = Integer.parseInt(sidParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"student_id must be a number\"}");
					return;
				}
				PreparedStatement ps = con.prepareStatement(base + " WHERE f.student_id=? ORDER BY f.fee_id DESC");
				ps.setInt(1, sid);
				rs = ps.executeQuery();
			} else {
				rs = con.createStatement().executeQuery(base + " ORDER BY f.fee_id DESC");
			}

			StringBuilder sb = new StringBuilder("[");
			while (rs.next()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("{").append("\"fee_id\":").append(rs.getInt("fee_id")).append(",")
						.append("\"student_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
						.append("\"roll_number\":\"").append(safe(rs.getString("roll_number"))).append("\",")
						.append("\"semester\":").append(rs.getInt("semester")).append(",").append("\"fee_type\":\"")
						.append(safe(rs.getString("fee_type"))).append("\",").append("\"amount\":")
						.append(rs.getDouble("amount")).append(",").append("\"due_date\":\"")
						.append(safe(rs.getString("due_date"))).append("\",").append("\"paid_date\":\"")
						.append(safe(rs.getString("paid_date"))).append("\",").append("\"status\":\"")
						.append(safe(rs.getString("status"))).append("\"").append("}");
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
		String action = req.getParameter("action");

		try (Connection con = DBConnection.getConnection()) {
			if ("pay".equals(action)) {
				String feeIdParam = req.getParameter("fee_id");
				if (feeIdParam == null || feeIdParam.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"fee_id is required\"}");
					return;
				}
				int feeId;
				try {
					feeId = Integer.parseInt(feeIdParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"fee_id must be a number\"}");
					return;
				}
				PreparedStatement ps = con
						.prepareStatement("UPDATE fees SET status='Paid', paid_date=CURDATE() WHERE fee_id=?");
				ps.setInt(1, feeId);
				int updated = ps.executeUpdate();
				if (updated == 0) {
					res.setStatus(404);
					out.print("{\"error\":\"Fee record not found\"}");
					return;
				}
				out.print("{\"status\":\"success\",\"message\":\"Fee marked as paid\"}");

			} else {
				// FIX #13: null-check all required fields
				String sidParam = req.getParameter("student_id");
				String semParam = req.getParameter("semester");
				String feeType = req.getParameter("fee_type");
				String amountParam = req.getParameter("amount");
				String dueDate = req.getParameter("due_date");

				if (sidParam == null || sidParam.trim().isEmpty() || semParam == null || semParam.trim().isEmpty()
						|| feeType == null || feeType.trim().isEmpty() || amountParam == null
						|| amountParam.trim().isEmpty() || dueDate == null || dueDate.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"student_id, semester, fee_type, amount, due_date are required\"}");
					return;
				}
				int sid, sem;
				double amount;
				try {
					sid = Integer.parseInt(sidParam.trim());
					sem = Integer.parseInt(semParam.trim());
					amount = Double.parseDouble(amountParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"student_id, semester must be integers; amount must be a number\"}");
					return;
				}
				PreparedStatement ps = con.prepareStatement(
						"INSERT INTO fees(student_id,semester,fee_type,amount,due_date,status) VALUES(?,?,?,?,?,'Pending')");
				ps.setInt(1, sid);
				ps.setInt(2, sem);
				ps.setString(3, feeType.trim());
				ps.setDouble(4, amount);
				ps.setString(5, dueDate.trim());
				ps.executeUpdate();
				out.print("{\"status\":\"success\",\"message\":\"Fee record added\"}");
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
