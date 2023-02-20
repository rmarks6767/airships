package com.rmarks.airships.control;

import com.rmarks.airships.AirshipsMod;
import com.rmarks.airships.control.ShipControllerCommon;
import com.rmarks.airships.entity.Ship;
import ckathode.archimedes.network.MsgControlInput;
import net.minecraft.world.entity.player.Player;

public class ShipControllerClient extends ShipControllerCommon
{
    @Override
    public void updateControl(Ship ship, Player player, int i)
    {
        super.updateControl(ship, player, i);
        MsgControlInput msg = new MsgControlInput(ship, i);
        AirshipsMod.instance.pipeline.sendToServer(msg);
    }
}
