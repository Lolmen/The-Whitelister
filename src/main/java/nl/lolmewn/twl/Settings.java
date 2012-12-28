/*
 * Copyright 2012 Lolmewn <info@lolmewn.nl>.
 */
package nl.lolmewn.twl;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * @author Lolmewn <info@lolmewn.nl>
 */
public class Settings {
    
    private FileConfiguration config;
    
    //settings
    private boolean useMySQL;
    private String dbUser, dbPass, dbDatabase, dbHost, dbTable;
    private int dbPort;
    
    private String kickMessage;
    private boolean enableOnStartup;
    
    private boolean update;
    
    public Settings(FileConfiguration config){
        this.config = config;
        this.loadSettings();
    }

    protected String getDbDatabase() {
        return dbDatabase;
    }

    protected String getDbHost() {
        return dbHost;
    }

    protected String getDbPass() {
        return dbPass;
    }

    protected int getDbPort() {
        return dbPort;
    }

    protected String getDbTable() {
        return dbTable;
    }

    protected String getDbUser() {
        return dbUser;
    }

    protected boolean isUseMySQL() {
        return useMySQL;
    }
    
    protected void setIsUseMySQL(boolean value){
        this.useMySQL = value;
    }

    public boolean isEnableOnStartup() {
        return enableOnStartup;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public boolean isUpdate() {
        return update;
    }

    private void loadSettings() {
        this.useMySQL = this.config.getBoolean("useMySQL", false);
        if(useMySQL){
            this.dbHost = this.config.getString("MySQL.host");
            this.dbDatabase = this.config.getString("MySQL.database");
            this.dbUser = this.config.getString("MySQL.username");
            this.dbPass = this.config.getString("MySQL.password");
            this.dbTable = this.config.getString("MySQL.table");
            this.dbPort = this.config.getInt("MySQL.port");
        }
        this.kickMessage = ChatColor.translateAlternateColorCodes('&', this.config.getString("kickMessage", "You are not on the whitelist!"));
        this.enableOnStartup = this.config.getBoolean("enableOnStartup", true);
        this.update = this.config.getBoolean("auto-update", true);
    }
    
}
