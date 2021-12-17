package com.rmarks.airships.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;


public class Ship extends Boat implements IEntityAdditionalSpawnData {

    public Ship(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf data) {
        data.writeByte(seatX);
        data.writeByte(seatY);
        data.writeByte(seatZ);
        data.writeByte(frontDirection);

        data.writeShort(info.shipName.length());
        data.writeBytes(info.shipName.getBytes());

        try
        {
            ChunkIO.writeAllCompressed(data, shipChunk);
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (ShipSizeOverflowException ssoe)
        {
            disassemble(false);
            ArchimedesShipMod.modLog.warn("Ship is too large to be sent");
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf data) {
        seatX = data.readUnsignedByte();
        seatY = data.readUnsignedByte();
        seatZ = data.readUnsignedByte();
        frontDirection = data.readUnsignedByte();

        byte[] ab = new byte[data.readShort()];
        data.readBytes(ab);
        info.shipName = new String(ab);
        try
        {
            ChunkIO.readCompressed(data, shipChunk);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        shipChunk.onChunkLoad();
    }
}