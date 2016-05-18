
			<% // loginPage is created in initialize.jsp %>

			<dl>
				<dt>Jobs</dt>
				<%=loginPage.showMenuItem("<dd><a href='add_job.jsp'>Add Job</a></dd>", "add_job.jsp")%>
				<%=loginPage.showMenuItem("<dd><a href='view_jobs.jsp'>View Jobs</a></dd>", "view_jobs.jsp")%>
			</dl>

			<dl>
				<dt>Server</dt>
				<%=loginPage.showMenuItem("<dd><a href='users.jsp'>Users</a></dd>", "users.jsp")%>
				<%=loginPage.showMenuItem("<dd><a href='log_viewer.jsp'>Log Viewer</a></dd>", "log_viewer.jsp")%>
				<%=loginPage.showMenuItem("<dd><a href='server_stats.jsp'>Server Statistics</a></dd>", "server_stats.jsp")%>
				<%=loginPage.showMenuItem("<dd><a href='server_properties.jsp'>Server Properties</a></dd>", "server_properties.jsp")%>
				<dd><a href="login.jsp?logout=logout">Logout</a></dd>
			</dl>

			<dl>
				<dt>Documentation</dt>
				<dd><a href="http://openpipeline.org/docs">Developers Guide</a></dd>
				<dd><a href="http://openpipeline.org/docs">Javadoc</a></dd>
			</dl>
