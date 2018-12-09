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

public class jdbc41autoclose {
	public static void main(String[] args) throws SQLException, IOException {
		String env1 = System.getenv("TEST_STRING");
		//String env2 = System.getenv("TEST_STRINGS");
		String tb = System.getenv("JDBC41_TABLE_NAME");
		String cname = System.getenv("JDBC41_CNAME");

		String driver = "jdbc:derby:javadb/"+ env1;
		Properties info = new Properties();
		info.setProperty("create", "true");
		info.setProperty("user", "userid");
		info.setProperty("password", "password");
		Connection conn0 = null;
		try(Connection conn = DriverManager.getConnection(driver, info)){
			conn0 = conn;
			conn.setAutoCommit(false);
			try(Statement stat = conn.createStatement()) {
				System.out.println("Execute CREATE TABLE.");
				stat.executeUpdate("CREATE TABLE " + tb + " (ID INT GENERATED ALWAYS AS IDENTITY, "+ cname +" CHAR(100))");
				for(int i=0; i<args.length; i++){
					String s = String.format("INSERT INTO " + tb + " (" + cname + ") VALUES ('%s')",args[i]);
					System.out.println(s);
					stat.executeUpdate(s);
				}
				conn.setAutoCommit(true);
			} catch(SQLException e){
				e.printStackTrace();
				conn.rollback();
				System.exit(1);	
			}
		}catch(SQLException f){
                    f.printStackTrace();
                    System.exit(1);	
		}
		if(conn0.isClosed()) System.out.println("=== AutoClose is called. ===");
		try(Connection conn = DriverManager.getConnection(driver, info)){
			try(Statement stat = conn.createStatement()) {
				ResultSet result = stat.executeQuery("SELECT * FROM " + tb + " order by ID");
				System.out.println("Execute SELECT by jdbc41autoclose.");
				while(result.next()){
					int id = result.getInt(1);
					String name = result.getString(2).trim();
					System.out.println(id +","+name);
				}
			} catch(SQLException e){
			}
		}catch(SQLException f){
		}
	}
}
