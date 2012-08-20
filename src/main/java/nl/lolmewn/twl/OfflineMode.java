/*
 *  Copyright 2012 Lolmewn <info@lolmewn.nl>.
 */

package nl.lolmewn.twl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

/**
 * @author Lolmewn <info@lolmewn.nl>
 */
public class OfflineMode implements Listener{

    private Main plugin;
    
    public OfflineMode(Main m) {
        this.plugin = m;
    }
    
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event){
        if(plugin.isWhitelistEnabled()){
            if(!this.plugin.hasPlayer(event.getPlayer().getName())){
                event.disallow(Result.KICK_WHITELIST, this.plugin.getSettings().getKickMessage());
            }
        }
    }

}
