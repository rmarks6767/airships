package com.rmarks.airships.chunk;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.rmarks.airships.entity.Ship;
import net.minecraft.block.Block;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import ckathode.archimedes.entity.EntityShip;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class MobileChunkServer extends MobileChunk
{
    private Set<ChunkPos>	sendQueue;

    public MobileChunkServer(Level level, Ship ship)
    {
        super(world, entityship);
        sendQueue = new HashSet<ChunkPosition>();
    }

    public Collection<ChunkPosition> getSendQueue()
    {
        return sendQueue;
    }

    @Override
    public boolean setBlockIDWithMetadata(int x, int y, int z, Block block, int meta)
    {
        if (super.setBlockIDWithMetadata(x, y, z, block, meta))
        {
            sendQueue.add(new ChunkPosition(x, y, z));
            return true;
        }
        return false;
    }

    @Override
    public boolean setBlockMetadata(int x, int y, int z, int meta)
    {
        if (super.setBlockMetadata(x, y, z, meta))
        {
            sendQueue.add(new ChunkPosition(x, y, z));
            return true;
        }
        return false;
    }

    @Override
    protected void onSetBlockAsFilledAir(int x, int y, int z)
    {
    }
}
