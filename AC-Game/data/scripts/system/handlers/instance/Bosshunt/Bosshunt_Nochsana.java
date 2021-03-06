package instance.Bosshunt;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 
 * @author Ghostfur
 * @author Nimwey
 */

@InstanceID(300030000)
public class Bosshunt_Nochsana extends GeneralInstanceHandler {
	@SuppressWarnings("unused")
    private long startTime;
    @SuppressWarnings("unused")
	private boolean isStop;
	private Future<?> TWDTask;
	private Future<?> TWD1Task;
	private Future<?> TWD2Task;
    private Future<?> instanceTimer;
    protected boolean isInstanceDestroyed = false;
    
	/**
     * Spawn Boss On Position
     */

        @Override
        public void onInstanceCreate(WorldMapInstance instance) {
            super.onInstanceCreate(instance);
            spawn(233740, 330.60318f, 269.97272f, 384.55338f, (byte) 15); // Dynatoum Boss ID + Position
            if (instanceTimer == null) {
                startTime = System.currentTimeMillis();
                instanceTimer = ThreadPoolManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        sendMsg(1402193);
                    }
                }, 60000);
            }
        }
        
        /**
         * You have 6 minutes to finish the boss.
         */
        

        @Override
        public void onEnterZone(Player player, ZoneInstance zone) {
    				TWD1Task = ThreadPoolManager.getInstance().schedule(new Runnable() {
    								@Override
    								public void run() {
    									instance.doOnAllPlayers(new Visitor<Player>() {
    										@Override
    										public void visit(Player player) {		
    											PacketSendUtility.sendMessage(player, "Dynatoum Boss will go off in 5 min!");
    										}
    									});
    								}
    							}, 60000); //1 Minutes.
    							
    							TWD2Task = ThreadPoolManager.getInstance().schedule(new Runnable() {
    								@Override
    								public void run() {
    									instance.doOnAllPlayers(new Visitor<Player>() {
    										@Override
    										public void visit(Player player) {
    											PacketSendUtility.sendMessage(player, "Dynatoum Boss will go off in 1 min!");
    										}
    									});
    								}
    							}, 300000); //5 Minutes.
    							
    				instance.doOnAllPlayers(new Visitor<Player>() {
    				    @Override
    					public void visit(Player player) {
    						if (player.isOnline()) {
    							PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 360)); //6 Minutes.
    							PacketSendUtility.sendSpecMessage("BOSSHUNT", "Your 6 min to kill this boss starts Now, so team up!!!");
    						}
    					}
    				});
    							TWDTask = ThreadPoolManager.getInstance().schedule(new Runnable() {
    								@Override
    								public void run() {
    									instance.doOnAllPlayers(new Visitor<Player>() {
    										@Override
    										public void visit(Player player) {
    											PacketSendUtility.sendSpecMessage("BOSSHUNT", "Sorry, you failed, better luck next time!!");
    											onExitInstance(player);
    										}
    									});
    									onInstanceDestroy();
    								}
    							}, 360000); //6 Minutes.
    			}		

		/**
         * Reward Group When Boss Is Death And Stop Timer
         */

		 @Override
	    public void onDie(Npc npc) {
	        Player player = npc.getAggroList().getMostPlayerDamage();
	        switch (npc.getObjectTemplate().getTemplateId()) {   
	            case 233740: // Dynatoum Boss ID     	
	              	for (Player p: player.getPlayerGroup2().getMembers()){	
	            		TWDTask.cancel(true);
	   					TWD1Task.cancel(true);
	   					TWD2Task.cancel(true);
	   					stopInstanceTask();
	   					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0)); //cancel timer
						ItemService.addItem(p, 187000148, 1);	//Special Elite Ambassador Divine Wings Special					
		            	ItemService.addItem(p, 188053083, 3);	//Tempering Solution Chest
						ItemService.addItem(p, 166020000, 5);	//Omega enchantment stone 
		    			InGameShopEn.getInstance().addToll(p, 20);
		    			PacketSendUtility.sendMessage(p, "You have receive 20 tolls. Totals: "+ p.getPlayerAccount().getToll());
						 }
						PacketSendUtility.sendSpecMessage("BOSSHUNT", "To exit bosshunt typ .bosshunt");
	                break;
	        }
	    }
	   		
		 	/**
		     * 
		     * Bosshunt Methods
		     * 
		     */
		    
		    protected List<Npc> getNpcs(int npcId) {
		        if (!isInstanceDestroyed) {
		            return instance.getNpcs(npcId);
		        }
		        return null;
		    }
		      
			private void stopInstanceTask() {
				if (instanceTimer != null) {
					instanceTimer.cancel(true);
				}
			}

		    public void onExitInstance(Player player) {
		    	TeleportService2.moveToBindLocation(player, false);
		    }
		    
		    @Override
		    public void onLeaveInstance(Player player) {
				stopInstanceTask();
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0)); //cancel timer
			}
		    
		    @Override
		    public boolean onDie(final Player player, Creature lastAttacker) {
		        PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
		        PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), player.haveSelfRezItem(), 0, 8));
		        return true;
		    }
		
		    @Override
		    public void onPlayerLogOut(Player player) {
				 TeleportService2.moveToBindLocation(player, false);
		    }
		    
	    @Override
	    public boolean onReviveEvent(Player player) {
	        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
	        PlayerReviveService.revive(player, 100, 100, false, 0);
	        player.getGameStats().updateStatsAndSpeedVisually();
	        return TeleportService2.teleportTo(player, mapId, instanceId, 342.3624f, 332.31348f, 380.01395f, (byte) 75);
	    }
}