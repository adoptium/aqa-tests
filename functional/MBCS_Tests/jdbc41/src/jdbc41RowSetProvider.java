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
//import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.*;

public class jdbc41RowSetProvider {
	public static void main(String[] args) throws Exception {
		String env1 = System.getenv("TEST_STRING");
		String driver = "jdbc:derby:javadb/"+ env1;
		String tb = System.getenv("JDBC41_TABLE_NAME");
		RowSetFactory myRowSetFactory = null;
    		JdbcRowSet jdbcRs = null;

		try {
      			myRowSetFactory = RowSetProvider.newFactory();
      			jdbcRs = myRowSetFactory.createJdbcRowSet();

			jdbcRs.setUrl(driver);
			jdbcRs.setUsername("userid");
			jdbcRs.setPassword("password");
			jdbcRs.setCommand("SELECT * FROM " + tb + " order by ID");
			jdbcRs.execute();
			System.out.println("Execute SELECT by jdbc41RowSetProvider.");
			while(jdbcRs.next()){
				System.out.println(jdbcRs.getString(1)+ "," + jdbcRs.getString(2).trim());
			}
		} catch(SQLException e){
			e.printStackTrace();
		} finally {
    			try {
      				jdbcRs.close();
    			} catch (SQLException e2) {
      				e2.printStackTrace();
    			}
		}
	}
}
