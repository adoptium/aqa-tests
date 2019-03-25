/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

import java.sql.*;
import java.util.*;
import java.io.*;

public class jdbc41droptb {
    static String getenv(String key) {
        String val = System.getenv(key);
        if (null == val)
           val = System.getProperty(key);
        return val;
    }
	public static void main(String[] args) throws SQLException, IOException {
		String env1 = getenv("TEST_STRING");
		String tb = getenv("JDBC41_TABLE_NAME");
		String driver = "jdbc:derby:javadb/"+ env1;

		Properties info = new Properties();
		info.setProperty("create", "true");
		info.setProperty("user", "userid");
		info.setProperty("password", "password");
		Connection conn = DriverManager.getConnection(driver, info);
		System.out.println("Execute DROP TABLE.");
		try(Statement stat = conn.createStatement()) {
			stat.executeUpdate("DROP TABLE " + tb);
		} catch(SQLException e){
		}
	}
}
