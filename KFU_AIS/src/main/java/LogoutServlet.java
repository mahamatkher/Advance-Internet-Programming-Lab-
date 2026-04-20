import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * King Faisal University – Chad Academic Information System v2.0 Invalidates
 * the server session and confirms logout.
 */
@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doPost(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		// Invalidate the session on the server
		HttpSession session = req.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		// Redirect to login page
		res.sendRedirect(req.getContextPath() + "/login.html");
	}
}
