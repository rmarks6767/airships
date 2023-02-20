package com.rmarks.airships.entity;

import com.rmarks.airships.AirshipsMod;
import ckathode.archimedes.network.MsgFarInteract;
import net.minecraft.world.entity.player.Player;

public class ShipHandlerClient extends ShipHandlerCommon
{
	public ShipHandlerClient(Ship ship) {
		super(ship);
	}
	
	@Override
	public boolean interact(Player player)
	{
		if (player.getDistanceSqToEntity(ship) >= 36D)
		{
			MsgFarInteract msg = new MsgFarInteract(ship);
			AirshipsMod.instance.pipeline.sendToServer(msg);
		}
		
		return super.interact(player);
	}
	
	@Override
	public void onChunkUpdate()
	{
		super.onChunkUpdate();
	}
}
