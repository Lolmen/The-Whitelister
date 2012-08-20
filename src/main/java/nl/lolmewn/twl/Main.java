/*
 *  Copyright 2012 Lolmewn <info@lolmewn.nl>.
 */
package nl.lolmewn.twl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Lolmewn <info@lolmewn.nl>
 */
public class Main extends JavaPlugin {

    private Settings settings;
    private Set<String> whitelisted;
    private boolean whitelistEnabled = true;
    
    private MySQL mysql;
    
    private boolean updateAvailable;
    private double newVersion;

    @Override
    public void onEnable() {
        if (this.getServer().getOnlineMode()) {
            this.getServer().getPluginManager().registerEvents(new OnlineMode(this), this);
        } else {
            this.getServer().getPluginManager().registerEvents(new OfflineMode(this), this);
        }
        loadConfig();
        loadPlayers();
        if(this.getSettings().isUpdate()){
            this.checkUpdate();
        }
    }

    @Override
    public void onDisable() {
        this.savePlayers();
        if(this.updateAvailable){
            this.downloadFile("https://dl.dropbox.com/u/7365249/TheWhiteLister.jar");
        }
    }
    
    public Settings getSettings(){
        return this.settings;
    }
    
    protected synchronized MySQL getMySQL(){
        return this.mysql;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public boolean hasPlayer(String player) {
        return this.whitelisted.contains(player);
    }

    private void loadConfig() {
        this.getDataFolder().mkdirs();
        File config = new File(this.getDataFolder(), "config.yml");
        if (!config.exists()) {
            //first run
            this.saveResource("config.yml", false);
            int world = loadWorld(this.getServer().getWorlds().get(0));
            int wl = loadVanillaWhitelist();
            this.getLogger().info("Added " + world + " from default world and " + wl + " from previous whitelist");
            this.settings = new Settings(this.getConfig());
            this.savePlayers();
        }else{
            this.settings = new Settings(this.getConfig());
            this.whitelistEnabled = this.getSettings().isEnableOnStartup();
        }
    }

    private void loadPlayers() {
        if(this.settings.isUseMySQL()){
            this.loadMySQL();
            if(this.getMySQL().isFault()){
                this.settings.setIsUseMySQL(false);
                this.loadPlayers();
                return;
            }
            ResultSet set = this.getMySQL().executeQuery("SELECT * FROM " + this.getSettings().getDbTable());
            if(set == null){
                this.getLogger().warning("Something wrong with resultset, using flatfile.");
                this.settings.setIsUseMySQL(false);
                this.loadPlayers();
                return;
            }
            try {
                while(set.next()){
                    this.whitelisted.add(set.getString("player"));
                }
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            File file = new File(this.getDataFolder(), "players.yml");
            YamlConfiguration f = YamlConfiguration.loadConfiguration(file);
            this.whitelisted = new HashSet<String>(f.getStringList("players"));
        }
    }

    private int loadWorld(World world) {
        File folder = new File(world.getName() + File.separator + "players");
        if (!folder.exists()) {
            return -1;
        }
        int added = 0;
        for (File f : folder.listFiles()) {
            String player = f.getName().substring(0, f.getName().lastIndexOf("."));
            if (this.whitelisted.add(player)) {
                added++;
            }
        }
        return added;
    }

    private int loadVanillaWhitelist() {
        int added = 0;
        for(OfflinePlayer player : this.getServer().getWhitelistedPlayers()){
            if(this.whitelisted.add(player.getName())){
                added++;
            }
        }
        return added;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(cmd.getName().equalsIgnoreCase("wl")){
            if(args.length == 0){
                sender.sendMessage("=================");
                sender.sendMessage("The Whitelister");
                sender.sendMessage("Version: " + this.getDescription().getVersion());
                sender.sendMessage("Active: " + (this.whitelistEnabled ? ChatColor.GREEN + "True" : ChatColor.RED + "False") );
                sender.sendMessage("=================");
                return true;
            }
            if(args[0].equals("list")){
                if(sender.hasPermission("wl.seelist")){
                    sender.sendMessage("Whitelisted: " + this.whitelisted.size());
                    sender.sendMessage(Arrays.toString(this.whitelisted.toArray()));
                    return true;
                }else{
                    sender.sendMessage("You don't have permissions to do this!");
                    return true;
                }
            }
            if(args[0].equals("toggle")){
                if(!sender.hasPermission("wl.toggle")){
                    sender.sendMessage("You don't have permissions to do this!");
                    return true;
                }
                this.whitelistEnabled = !this.whitelistEnabled;
                sender.sendMessage("The whitelist is now " + (this.whitelistEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
                return true;
            }
            if(args[0].equals("remove") || args[0].equals("delete")){
                if(!sender.hasPermission("wl.remove")){
                    sender.sendMessage("You don't have permissions to do this!");
                    return true;
                }
                if(args.length == 1){
                    sender.sendMessage("Usage: /wl remove <player1> <player2> ...");
                    return true;
                }
                for(int i = 1; i < args.length; i++){
                    sender.sendMessage(this.removePlayer(args[i]));
                }
                return true;
            }
            if(!sender.hasPermission("wl.add")){
                sender.sendMessage("You don't have permissions to do this!");
                return true;
            }
            for(int i = 0; i < args.length; i++){
                sender.sendMessage(this.addPlayer(args[i]));
            }
            return true;
        }
        return false;
    }
    
    protected String removePlayer(String player){
        if(!this.whitelisted.contains(player)){
            return player + " isn't whitelisted!";
        }
        this.whitelisted.remove(player);
        if(this.settings.isUseMySQL()){
            this.getMySQL().executeStatement("DELETE FROM " + this.getSettings().getDbTable() + " WHERE player='" + player + "'");
        }
        return player + " unwhitelisted!";
    }
    
    protected String addPlayer(String player){
        if(this.whitelisted.contains(player)){
            return player + " is already whitelisted!";
        }
        this.whitelisted.add(player);
        if(this.settings.isUseMySQL()){
            this.getMySQL().executeStatement("INSERT INTO " + this.getSettings().getDbTable() + "(player) VALUES ('" + player + "')");
        }
        return player + " whitelisted!";
    }

    private void savePlayers() {
        if(this.whitelisted.isEmpty()){
            return;
        }
        if(!this.settings.isUseMySQL()){ //no need to save them if MySQL is used, that gets done automatically.
            File file = new File(this.getDataFolder(), "players.yml");
            if(!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            YamlConfiguration f = YamlConfiguration.loadConfiguration(file);
            try {
                f.set("players", this.whitelisted);
                f.save(file);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void loadMySQL() {
        this.mysql = new MySQL(this, this.getSettings().getDbHost(), 
                this.getSettings().getDbPort(), this.getSettings().getDbUser(),
                this.getSettings().getDbPass(), this.getSettings().getDbDatabase(),
                this.getSettings().getDbTable());
    }
    
    private void checkUpdate() {
        try {
            URL url = new URL("http://dl.dropbox.com/u/7365249/twl.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                if (this.getSettings().getVersion() < Double.parseDouble(str)) {
                    this.updateAvailable = true;
                    this.getLogger().info("An update is available! Will be downloaded on Disable! Old version: " + this.getSettings().getVersion() + " New version: " + str);
                    this.newVersion = Double.parseDouble(str);
                }
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(String location) {
        try {
            this.getLogger().info("Updating The Whitelister.. Please wait.");
            BufferedInputStream in = new BufferedInputStream(new URL(location).openStream());
            FileOutputStream fout = new FileOutputStream(nl.lolmewn.twl.Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            byte data[] = new byte[1024]; //Download 1 KB at a time
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
            this.getLogger().info("Update complete!");
            in.close();
            fout.close();
            this.getConfig().set("version", newVersion);
            this.saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
