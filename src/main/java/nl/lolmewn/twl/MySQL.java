package nl.lolmewn.twl;

import java.sql.*;

public class MySQL {

    private String host, username, password, database, prefix;
    private int port;
    private boolean fault;
    private Statement st;
    private Connection con;
    private Main plugin;

    public MySQL(Main main, String host, int port, String username, String password, String database, String prefix) {
        this.plugin = main;
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.prefix = prefix;
        this.port = port;
        this.connect();
        this.setupTables();
    }

    private void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database;
            this.plugin.getLogger().info("Connecting to database on " + url);
            this.con = DriverManager.getConnection(url, this.username, this.password);
            this.st = con.createStatement();
            this.plugin.getLogger().info("MySQL initiated succesfully!");
        } catch (ClassNotFoundException e) {
            this.plugin.getLogger().warning(e.toString());
            this.setFault(true);
        } catch (SQLException e) {
            this.plugin.getLogger().warning(e.toString());
            this.setFault(true);
        } finally {
            if (this.fault) {
                this.plugin.getLogger().info("MySQL initialisation failed!");
            }
        }
    }

    private void setupTables() {
        if (this.isFault()) {
            return;
        }
        this.executeStatement("CREATE TABLE IF NOT EXISTS " + this.prefix
                + "(counter int PRIMARY KEY NOT NULL AUTO_INCREMENT, "
                + "player varchar(255) NOT NULL)");
    }

    public boolean isFault() {
        return fault;
    }

    private void setFault(boolean fault) {
        this.fault = fault;
    }

    public int executeStatement(String statement) {
        if (isFault()) {
            this.plugin.getLogger().info("Can't execute statement, something wrong with connection");
            return 0;
        }
        try {
            this.st = this.con.createStatement();
            int re = this.st.executeUpdate(statement);
            this.st.close();
            return re;
        } catch (SQLException e) {
            if(!e.getClass().getSimpleName().equals("MysqlDataTruncation")){
                this.plugin.getLogger().warning(e.toString());                
                this.plugin.getLogger().warning("Attempted query, failed with exception above. Query: " + statement);
            }
        }
        return 0;
    }

    public ResultSet executeQuery(String statement) {
        if (isFault()) {
            this.plugin.getLogger().info("Can't execute query, something wrong with connection");
            return null;
        }
        if (statement.toLowerCase().startsWith("update") || statement.toLowerCase().startsWith("insert") || statement.toLowerCase().startsWith("delete")) {
            this.executeStatement(statement);
            return null;
        }
        try {
            this.st = this.con.createStatement();
            ResultSet set = this.st.executeQuery(statement);
            return set;
        } catch (SQLException e) {
            if(!e.getClass().getSimpleName().equals("MysqlDataTruncation")){
                this.plugin.getLogger().warning(e.toString());
                this.plugin.getLogger().warning("Attempted query, failed with exception above. Query: " + statement);
            }
        }
        return null;
    }

    public void close() {
        if (isFault()) {
            this.plugin.getLogger().info("Can't close connection, something wrong with it");
            return;
        }
        try {
            this.con.close();
        } catch (SQLException e) {
            this.plugin.getLogger().warning(e.toString());
        }
    }
}