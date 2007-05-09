package net.sf.jxls;

import junit.framework.TestCase;
import net.sf.jxls.report.ReportManager;
import net.sf.jxls.report.ReportManagerImpl;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leonid Vysochyn
 */
public class SQLReportingTest extends TestCase {

    public static final String report = "/templates/report.xls";
    public static final String reportDest = "target/report_output.xls";

    public static final String CREATE_EMPLOYEE_TABLE = "CREATE TABLE employee (\n" +
            "  name varchar(20) default NULL,\n" +
            "  age int default NULL,\n" +
            "  payment double default NULL,\n" +
            "  bonus double default NULL,\n" +
            "  birthDate date default NULL,\n" +
            "  id int NOT NULL PRIMARY KEY, \n" +
            "  depid int,  FOREIGN KEY (depid) REFERENCES department (id) " +
            ");";

    public static final String CREATE_DEPARTMENT_TABLE = "CREATE TABLE department (\n" +
            "  name varchar(20) NOT NULL, " +
            "id int NOT NULL PRIMARY KEY );";

    public static final String INSERT_EMPLOYEE = "INSERT INTO employee\n" +
            "  (name, age, payment, bonus, birthDate, depid, id)\n" +
            "VALUES\n" +
            "  (?, ?, ?, ?, ?, ?, ? );";
    public static final String INSERT_DEPARTMENT = "INSERT INTO department (name, id) VALUES (?, ?)";

    String[] depNames = new String []{"IT", "HR", "BA"};
    String[][] employeeNames = new String[][]{{"Elsa", "Oleg", "Neil", "Maria", "John"},
            {"Olga", "Helen", "Keith", "Cat"},
            {"Denise", "LeAnn", "Natali"}};
    int[][] employeeAges = new int[][]{ {25, 30, 34, 25, 35},
            {26, 24, 27, 28},
            {30, 29, 26}};
    double[][] employeePayments = new double[][]{{3000, 1500, 2300, 2400, 1800},
            {1400, 2100, 1800, 1900},
            {2400, 2200, 1700}};
    double[][] employeeBonuses = new double[][]{ {0.3, 0.25, 0.25, 0.1, 0.2},
            {0.15, 0.05, 0.2, 0.1},
            {0.2, 0.1, 0.15}};
    String[][] employeeBirthDates = new String[][]{
            {"1970-12-02", "1980-02-15", "1976-07-20", "1974-10-24", "1972-06-05"},
            {"1968-08-22", "1971-10-16", "1979-03-21", "1974-12-05"},
            {"1976-12-02", "1981-05-25", "1983-06-17"}
            };

    Connection conn;

    protected void setUp() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        conn = DriverManager.getConnection("jdbc:hsqldb:mem:jxls", "sa", "");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate( CREATE_DEPARTMENT_TABLE );
        stmt.executeUpdate( CREATE_EMPLOYEE_TABLE );
        PreparedStatement insertDep = conn.prepareStatement( INSERT_DEPARTMENT );
        PreparedStatement insertStmt = conn.prepareStatement( INSERT_EMPLOYEE );
        int k = 1;
        int n = 1;
        for (int i = 0; i < depNames.length; i++) {
            String depName = depNames[i];
            insertDep.setString(1, depName);
            insertDep.setInt(2, n++);
            insertDep.executeUpdate();
            for (int j = 0; j < employeeNames[i].length; j++) {
                insertStmt.setString(1, employeeNames[i][j]);
                insertStmt.setInt(2, employeeAges[i][j]);
                insertStmt.setDouble(3, employeePayments[i][j]);
                insertStmt.setDouble(4, employeeBonuses[i][j]);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
                insertStmt.setDate(5, new Date( sdf.parse( employeeBirthDates[i][j]).getTime() ) );
                insertStmt.setInt(6, n - 1);
                insertStmt.setInt(7, k++);
                insertStmt.executeUpdate();
            }
        }

        stmt.close();
        insertStmt.close();
    }

    protected void tearDown() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE employee");
        stmt.executeUpdate("DROP TABLE department");
        stmt.close();
    }

    public void testSelect() throws IOException {
        Map beans = new HashMap();
        ReportManager rm = new ReportManagerImpl( conn, beans );
        beans.put("rm", rm);
        beans.put("minDate", "1979-01-01");

        InputStream is = new BufferedInputStream(getClass().getResourceAsStream(report));
        XLSTransformer transformer = new XLSTransformer();
//        transformer.setJexlInnerCollectionsAccess( true );
        HSSFWorkbook resultWorkbook = transformer.transformXLS(is, beans);
        is.close();
        // todo
        saveWorkbook( resultWorkbook, reportDest );
    }


    public void atestHSQLDBConnect() throws SQLException {
        Statement stmt = conn.createStatement();
        String query = "SELECT name, age, payment, bonus, birthDate FROM employee order by age desc";
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
           String name = rs.getString("name");
           int age = rs.getInt("age");
           double payment = rs.getDouble("payment");
           double bonus = rs.getDouble("bonus");
           Date date = rs.getDate("birthDate");
           System.out.println("Name: " + name);
            System.out.println("Age: " + age);
            System.out.println("Payment: " + payment);
            System.out.println("Bonus: " + bonus);
            System.out.println("BirthDate: " + date);
        }
        rs.close();
        stmt.close();
    }

    private void saveWorkbook(HSSFWorkbook resultWorkbook, String fileName) throws IOException {
        String saveResultsProp = System.getProperty("saveResults");
        if( "true".equalsIgnoreCase(saveResultsProp) ){
            OutputStream os = new BufferedOutputStream(new FileOutputStream(fileName));
            resultWorkbook.write(os);
            os.flush();
            os.close();
        }
    }



}