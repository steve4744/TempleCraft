package com.msingleton.templecraft.games;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import com.msingleton.templecraft.TCMobHandler;
import com.msingleton.templecraft.TCUtils;
import com.msingleton.templecraft.Temple;
import com.msingleton.templecraft.TempleCraft;
import com.msingleton.templecraft.TempleManager;
import com.msingleton.templecraft.TemplePlayer;
//import com.msingleton.templecraft.custommobs.CustomMob;
import com.msingleton.templecraft.util.MobArenaClasses;
import com.msingleton.templecraft.util.Translation;

/**
* Adventure.java
* This work is dedicated to the public domain.
* 
* @author Xarinor
* @author bootscreen
* @author msingleton
*/
public class Adventure extends Game {
	/**
	 * Constructor
	 * 
	 * @param name	 -Adventure name
	 * @param temple -Temple name
	 * @param world	 -Temple world
	 */
	public Adventure(String name, Temple temple, World world) {
		super(name, temple, world);
		world.setPVP(false);
	}

	/**
	 * End this Adventure
	 */
	public void endGame() {
		super.endGame();
	}

	/**
	 * Player dies to the adventure
	 * 
	 * @param player
	 */
	public void playerDeath(Player player) {
		if(TempleCraft.economy != null) {
			String s = TempleCraft.economy.format(2.0);
			String currencyName = s.substring(s.indexOf(" ") + 1);

			if(TempleCraft.economy.has(player.getName(),rejoinCost)) {		
				if(TempleCraft.economy != null && rejoinCost > 0) {
					TempleManager.tellPlayer(player, Translation.tr("adventure.rejoin1", rejoinCost, currencyName));
					TempleManager.tellPlayer(player, Translation.tr("adventure.rejoin2"));
				}
			} else {
				TempleManager.tellPlayer(player, Translation.tr("adventure.rejoinFail1", currencyName));
				TempleManager.tellPlayer(player, Translation.tr("adventure.rejoinFail2"));
			}
		}
		lobbyLoc.getChunk().load(true);
		player.teleport(lobbyLoc);
		super.playerDeath(player);
	}

	/**
	 * Handle Templecraft signs in this Adventure
	 * 
	 * @param sign
	 */
	protected void handleSign(Sign sign)
	{
		String[] Lines = sign.getLines();

		if(!Lines[0].equals("[TempleCraft]") && !Lines[0].equals("[TC]") && !Lines[0].equals("[TCB]") &&!Lines[0].equals("[TempleCraftM]") &&
				!Lines[0].equals("[TCM]") && !Lines[0].equals("[TempleCraftML]") && !Lines[0].equals("[TCML]")) {
			return;
		}

		if(Lines[1].toLowerCase().equals("classes")) {
			if(MobArenaClasses.enabled) {
				usingClasses = true;
				MobArenaClasses.generateClassSigns(sign);
			}
		}
		super.handleSign(sign);
	}

	/**
	 * End block hit
	 * 
	 * @param player
	 */
	public void hitEndBlock(Player player) {
		TCUtils.debugMessage("Player " + player.getName() + " hit EndBlock in Game " + gameName + "(" + gameType + ")");
		TemplePlayer tp = TempleManager.templePlayerMap.get(player);
		if (playerSet.contains(player)) {				
			TCUtils.debugMessage("Player " + player.getName() + " contains PlayerSet.");
			readySet.add(player);
			rewardSet.add(player);
			tp.rewards = rewards;
			int totalTime = (int)(System.currentTimeMillis()-startTime)/1000;
			tellPlayer(player, Translation.tr("game.finishTime", ""+totalTime));

			// Update ScoreBoards
			List<String> scores = new ArrayList<String>();
			scores.add(player.getName());
			scores.add(tp.roundMobsKilled + "");
			scores.add(tp.roundGold + "");
			scores.add(tp.roundDeaths + "");
			scores.add(totalTime + "");
			TempleManager.SBManager.updateScoreBoards(this, scores);

			if(readySet.equals(playerSet)) {
				endGame();
			} else {
				tellPlayer(player, Translation.tr("game.readyToLeave"));
				tp.currentCheckpoint = null;
			}
		} else {
			TCUtils.debugMessage("Player " + player.getName() + " not contains PlayerSet but hit end block.", Level.WARNING);
		}
	}

	/**
	 * Player moving
	 * 
	 * @param event
	 */
	public void onPlayerMove(PlayerMoveEvent event) {
		super.onPlayerMove(event);		
		Player p = event.getPlayer();

		if(!isRunning) {
			return;
		}

		Set<Location> tempLocs = new HashSet<Location>();
		for(Location loc : mobSpawnpointMap.keySet()) {
			//TODO Check
			//Could not pass event PlayerMoveEvent : loc was being passed a null
			if (loc != null) {
				if(p.getLocation().distance(loc) < mobSpawnpointMap.get(loc).getRange()) {
					tempLocs.add(loc);
				}
		    }
			//--------------------------------------------------------			
		}

		for(Location loc : tempLocs) {
			try {
				TCMobHandler.SpawnMobs(this, loc, mobSpawnpointMap.get(loc));
				mobSpawnpointMap.remove(loc);
			} catch (Exception e) {
				e.printStackTrace();
				mobSpawnpointMap.remove(loc);
			}
		}

		//I assume this is an attempt to respawn non-dead mobs after player death -Tim
		//This is the source of the continuous spawn.  Commenting out for now -Tim
		// Correct ;)  out for now - Xari
		/*
		Set<Location> tempLocs2 = new HashSet<Location>();
		List<CustomMob> cmobs = customMobManager.getMobs();
		for(Location loc : mobSpawnpointConstantMap.keySet()) {
			if(p.getLocation().distance(loc) < mobSpawnpointConstantMap.get(loc).getRange()) {
				if(!tempLocs.contains(loc)) {
					for(CustomMob cm : cmobs) {
						if(!cm.isDead() && cm.isEntityDead()) {
							tempLocs2.add(loc);
						}
					}
				}
			}
		}		
		
		for(Location loc : tempLocs2) {
			try {
				TCMobHandler.SpawnMobs(this, loc, mobSpawnpointConstantMap.get(loc));				
				//debug
				//System.out.println("spawnmobs2");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		} 
		*/
	}

	/**
	 * Entity killer kills entity killed
	 *
	 * @param killed
	 * @param killer
	 */
	public void onEntityKilledByEntity(LivingEntity killed, Entity killer)
	{

		super.onEntityKilledByEntity(killed, killer);
		TCUtils.sendDeathMessage(this, killed, killer);

		if(killer instanceof Player) {
			if(mobGoldMap != null && mobGoldMap.containsKey(killed.getEntityId())) {
				for(Player p : playerSet) {
					int gold = mobGoldMap.get(killed.getEntityId())/playerSet.size();
					TempleManager.templePlayerMap.get(p).roundGold += gold;
					if(TempleCraft.economy != null) {
						TempleCraft.economy.depositPlayer(p.getName(), gold);
					}
				}
			}
		}
	}
}
