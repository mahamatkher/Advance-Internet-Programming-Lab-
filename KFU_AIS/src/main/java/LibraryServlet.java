import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * King Faisal University – Chad Academic Information System v2.0 Manages
 * library books, issues and returns.
 */
@WebServlet("/LibraryServlet")
public class LibraryServlet extends HttpServlet {

	private static final double FINE_PER_DAY = 50.0; // CFA Francs per overdue day

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		String action = req.getParameter("action");

		try (Connection con = DBConnection.getConnection()) {
			if ("issues".equals(action)) {
				ResultSet rs = con.createStatement()
						.executeQuery("SELECT li.*,b.title,u.full_name FROM library_issues li "
								+ "JOIN library_books b ON li.book_id=b.book_id "
								+ "JOIN users u ON li.user_id=u.user_id " + "ORDER BY li.issue_id DESC");
				StringBuilder sb = new StringBuilder("[");
				while (rs.next()) {
					if (sb.length() > 1)
						sb.append(",");
					sb.append("{").append("\"issue_id\":").append(rs.getInt("issue_id")).append(",")
							.append("\"book_title\":\"").append(safe(rs.getString("title"))).append("\",")
							.append("\"user_name\":\"").append(safe(rs.getString("full_name"))).append("\",")
							.append("\"issue_date\":\"").append(safe(rs.getString("issue_date"))).append("\",")
							.append("\"due_date\":\"").append(safe(rs.getString("due_date"))).append("\",")
							.append("\"return_date\":\"").append(safe(rs.getString("return_date"))).append("\",")
							.append("\"status\":\"").append(safe(rs.getString("status"))).append("\",")
							.append("\"fine\":").append(rs.getDouble("fine")).append("}");
				}
				sb.append("]");
				out.print(sb);
			} else {
				ResultSet rs = con.createStatement().executeQuery("SELECT * FROM library_books ORDER BY title");
				StringBuilder sb = new StringBuilder("[");
				while (rs.next()) {
					if (sb.length() > 1)
						sb.append(",");
					sb.append("{").append("\"book_id\":").append(rs.getInt("book_id")).append(",")
							.append("\"title\":\"").append(safe(rs.getString("title"))).append("\",")
							.append("\"author\":\"").append(safe(rs.getString("author"))).append("\",")
							.append("\"isbn\":\"").append(safe(rs.getString("isbn"))).append("\",")
							.append("\"category\":\"").append(safe(rs.getString("category"))).append("\",")
							.append("\"total_copies\":").append(rs.getInt("total_copies")).append(",")
							.append("\"available\":").append(rs.getInt("available")).append(",")
							.append("\"rack_no\":\"").append(safe(rs.getString("rack_no"))).append("\"").append("}");
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
			if ("issue".equals(action)) {
				String bookParam = req.getParameter("book_id");
				String userParam = req.getParameter("user_id");
				if (bookParam == null || bookParam.trim().isEmpty() || userParam == null
						|| userParam.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"book_id and user_id are required\"}");
					return;
				}
				int bookId, userId;
				try {
					bookId = Integer.parseInt(bookParam.trim());
					userId = Integer.parseInt(userParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"book_id and user_id must be numbers\"}");
					return;
				}

				// FIX #15: explicit exists + availability check
				PreparedStatement chk = con.prepareStatement("SELECT available FROM library_books WHERE book_id=?");
				chk.setInt(1, bookId);
				ResultSet rc = chk.executeQuery();
				if (!rc.next()) {
					res.setStatus(404);
					out.print("{\"error\":\"Book not found\"}");
					return;
				}
				if (rc.getInt("available") <= 0) {
					res.setStatus(409);
					out.print("{\"error\":\"No copies available\"}");
					return;
				}

				PreparedStatement ps = con
						.prepareStatement("INSERT INTO library_issues(book_id,user_id,issue_date,due_date,status) "
								+ "VALUES(?,?,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 14 DAY),'Issued')");
				ps.setInt(1, bookId);
				ps.setInt(2, userId);
				ps.executeUpdate();

				PreparedStatement upd = con
						.prepareStatement("UPDATE library_books SET available=available-1 WHERE book_id=?");
				upd.setInt(1, bookId);
				upd.executeUpdate();
				out.print("{\"status\":\"success\",\"message\":\"Book issued for 14 days\"}");

			} else if ("return".equals(action)) {
				String issueParam = req.getParameter("issue_id");
				if (issueParam == null || issueParam.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"issue_id is required\"}");
					return;
				}
				int issueId;
				try {
					issueId = Integer.parseInt(issueParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"issue_id must be a number\"}");
					return;
				}

				PreparedStatement get = con.prepareStatement(
						"SELECT book_id, due_date FROM library_issues WHERE issue_id=? AND status='Issued'");
				get.setInt(1, issueId);
				ResultSet rg = get.executeQuery();
				if (!rg.next()) {
					res.setStatus(404);
					out.print("{\"error\":\"Issue record not found or already returned\"}");
					return;
				}
				int bookId = rg.getInt("book_id");
				Date dueDate = rg.getDate("due_date");

				// FIX #16: calculate fine for overdue books
				double fine = 0.0;
				if (dueDate != null) {
					long today = System.currentTimeMillis();
					long due = dueDate.getTime();
					if (today > due) {
						long overdueDays = (today - due) / (1000 * 60 * 60 * 24);
						fine = overdueDays * FINE_PER_DAY;
					}
				}

				PreparedStatement ps = con.prepareStatement(
						"UPDATE library_issues SET return_date=CURDATE(), status='Returned', fine=? WHERE issue_id=?");
				ps.setDouble(1, fine);
				ps.setInt(2, issueId);
				ps.executeUpdate();

				PreparedStatement upd = con
						.prepareStatement("UPDATE library_books SET available=available+1 WHERE book_id=?");
				upd.setInt(1, bookId);
				upd.executeUpdate();

				out.print("{\"status\":\"success\",\"message\":\"Book returned successfully\",\"fine\":" + fine + "}");

			} else {
				// Add new book
				String titleParam = req.getParameter("title");
				String copiesParam = req.getParameter("total_copies");
				if (titleParam == null || titleParam.trim().isEmpty() || copiesParam == null
						|| copiesParam.trim().isEmpty()) {
					res.setStatus(400);
					out.print("{\"error\":\"title and total_copies are required\"}");
					return;
				}
				int copies;
				try {
					copies = Integer.parseInt(copiesParam.trim());
				} catch (NumberFormatException e) {
					res.setStatus(400);
					out.print("{\"error\":\"total_copies must be a number\"}");
					return;
				}
				PreparedStatement ps = con.prepareStatement(
						"INSERT INTO library_books(title,author,isbn,category,total_copies,available,rack_no) VALUES(?,?,?,?,?,?,?)");
				ps.setString(1, titleParam.trim());
				ps.setString(2, req.getParameter("author") != null ? req.getParameter("author") : "");
				ps.setString(3, req.getParameter("isbn") != null ? req.getParameter("isbn") : null);
				ps.setString(4, req.getParameter("category") != null ? req.getParameter("category") : "");
				ps.setInt(5, copies);
				ps.setInt(6, copies);
				ps.setString(7, req.getParameter("rack_no") != null ? req.getParameter("rack_no") : "");
				ps.executeUpdate();
				out.print("{\"status\":\"success\",\"message\":\"Book added to library\"}");
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
