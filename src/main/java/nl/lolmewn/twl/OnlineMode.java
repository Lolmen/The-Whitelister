/*
 *  Copyright 2012 Lolmewn <info@lolmewn.nl>.
 */

package nl.lolmewn.twl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;

/**
 * @author Lolmewn <info@lolmewn.nl>
 */
public class OnlineMode implements Listener{

    private Main plugin;
    
    public OnlineMode(Main m) {
        this.plugin = m;
    }
    
    @EventHandler
    public void join(AsyncPlayerPreLoginEvent event){
        if(plugin.isWhitelistEnabled()){
            if(!this.plugin.hasPlayer(event.getName())){
                event.disallow(Result.KICK_WHITELIST, this.plugin.getSettings().getKickMessage());
            }
        }
    }

}
