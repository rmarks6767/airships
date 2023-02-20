package com.rmarks.airships.control;

import com.rmarks.airships.entity.Ship;
import net.minecraft.world.entity.player.Player;

public class ShipControllerCommon {
    private int	shipControl	= 0;

    public void updateControl(Ship ship, Player player, int i) {
        shipControl = i;
    }

    public int getShipControl() {
        return shipControl;
    }
}
