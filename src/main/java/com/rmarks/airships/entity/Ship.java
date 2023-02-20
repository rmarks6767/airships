package com.rmarks.airships.entity;

import com.rmarks.airships.chunk.MobileChunkServer;
import com.rmarks.airships.control.ShipControllerClient;
import com.rmarks.airships.control.ShipControllerCommon;
import com.rmarks.airships.chunk.ChunkDisassembler;
import com.rmarks.airships.chunk.MobileChunk;
import com.rmarks.airships.chunk.MobileChunkClient;
import com.rmarks.airships.util.AABBRotator;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

import java.io.*;

  public class Ship extends Boat implements IEntityAdditionalSpawnData {
    public static final float BASE_FORWARD_SPEED = 0.005F;
    public static final float BASE_TURN_SPEED = 0.5F;
    public static final float BASE_LIFT_SPEED = 0.004F;

    public static boolean isBBInLiquidNotFall(Level level, BoundingBox bb) {
        int minX = (int) Math.floor(bb.minX());
        int maxX = (int) Math.floor(bb.maxX() + 1D);
        int minY = (int) Math.floor(bb.minY());
        int maxY = (int) Math.floor(bb.maxY() + 1D);
        int minZ = (int) Math.floor(bb.minZ());
        int maxZ = (int) Math.floor(bb.maxZ() + 1D);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockEntity block = level.getBlockEntity(new BlockPos(x, y, z));
                    if(block != null) {
                        BlockState state = block.getBlockState();
                        Material material = block.getBlockState().getMaterial();

                        if ((material == Material.WATER || material == Material.LAVA)) {
                            int waterAmount = state.getFluidState().getAmount();

                            if (waterAmount < 8) {
                                double d0 = y + 1 - waterAmount / 8.0D;
                                if (d0 >= bb.minY()) return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public float motionYaw;
    public int frontDirection;
    public int seatX, seatY, seatZ;

    protected boolean isFlying;
    protected float groundFriction, horFriction, vertFriction;
    protected int[] layeredBlockVolumeCount;

    private MobileChunk shipChunk;
    private ShipCapabilities capabilities;
    private ShipControllerCommon controller;
    private ShipHandlerCommon handler;
    private ShipInfo info;
    private ChunkDisassembler disassembler;
    private Entity prevRiddenByEntity;
    private boolean boatIsEmpty;
    private boolean	syncPosWithServer;
    @OnlyIn(Dist.CLIENT) private int boatPosRotationIncrements;
    @OnlyIn(Dist.CLIENT) private double	boatX, boatY, boatZ;
    @OnlyIn(Dist.CLIENT) private double	boatPitch, boatYaw;
    @OnlyIn(Dist.CLIENT) private double	boatVelX, boatVelY, boatVelZ;

    public Ship(Level level) {
        super(level, 0D, 0D, 0D);

        info = new ShipInfo();
        capabilities = new ShipCapabilities(this);

        if (level.isClientSide) initClient();
        else initCommon();

        motionYaw = 0F;

        layeredBlockVolumeCount = null;
        frontDirection = 0;
        groundFriction = 0.9F;
        horFriction = 0.994F;
        vertFriction = 0.95F;
        prevRiddenByEntity = null;
        isFlying = false;
        boatIsEmpty = false;
        syncPosWithServer = true;

        if (level.isClientSide) {
            boatPosRotationIncrements = 0;
            boatX = boatY = boatZ = 0D;
            boatPitch = boatYaw = 0D;
            boatVelX = boatVelY = boatVelZ = 0D;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void initClient()
    {

        shipChunk = new MobileChunkClient(level, this);
        handler = new ShipHandlerClient(this);
        controller = new ShipControllerClient();
    }

    private void initCommon()
    {
        shipChunk = new MobileChunkServer(level, this);
        handler = new ShipHandlerServer(this);
        controller = new ShipControllerCommon();
    }

    // @Override
    protected void entityInit()
    {
        dataWatcher.addObject(30, (byte) 0);
    }

    public MobileChunk getShipChunk()
    {
        return shipChunk;
    }


    // Possibly not needed IDK?
    // public ShipCapabilities getCapabilities() {
    //        return capabilities;
    //    }

    public ShipControllerCommon getController()
    {
        return controller;
    }

    public ChunkDisassembler getDisassembler() {
        return disassembler == null ? new ChunkDisassembler(this) : disassembler;
    }

    public void setInfo(ShipInfo shipinfo) {
        if (shipinfo == null) throw new NullPointerException("Cannot set null ship info");
        info = shipinfo;
    }

    public ShipInfo getInfo() {
        return info;
    }

    public void setPilotSeat(int dir, int seatX, int seatY, int seatZ) {
        frontDirection = dir;
        this.seatX = seatX;
        this.seatY = seatY;
        this.seatZ = seatZ;
    }

    public void setDead() {
        shipChunk.onChunkUnload();
        capabilities.clear();
    }

    public void onEntityUpdate() {
        if (shipChunk.isModified) {
            shipChunk.isModified = false;
            handler.onChunkUpdate();
        }
    }

    public void setRotatedBoundingBox() {
        if (shipChunk == null) {
            float hw = getBbWidth() / 2F;
            int x = getBlockX(), y = getBlockY(), z = getBlockZ();

            super.setBoundingBox(
                new AABB(x - hw, y, z - hw, x + hw, y + getEyeHeight(), z + hw)
            );
        }
        else {
            int x = getBlockX(), y = getBlockY(), z = getBlockZ();

            super.setBoundingBox(
                new AABB(
                    x - shipChunk.getCenterX(),
                    y,
                    z - shipChunk.getCenterZ(),
                    x + shipChunk.getCenterX(),
                    y + getEyeHeight(),
                    z + shipChunk.getCenterZ()
                )
            );

            AABBRotator.rotateAABBAroundY(getBoundingBox(), x, z, (float) Math.toRadians(getYRot()));
        }
    }

    public void setBounds(float w, float h) {
        float width = getBbWidth();
        float height = getBbHeight();

        if (w != width || h != height) {
            width = w;
            height = h;
            float hw = w / 2F;
            boundingBox.setBounds(posX - hw, posY, posZ - hw, posX + hw, posY + height, posZ + hw);
        }

        float f = w % 2.0F;
        if (f < 0.375D) {
            myEntitySize = EnumEntitySize.SIZE_1;
        }
        else if (f < 0.75D) {
            myEntitySize = EnumEntitySize.SIZE_2;
        }
        else if (f < 1.0D) {
            myEntitySize = EnumEntitySize.SIZE_3;
        }
        else if (f < 1.375D) {
            myEntitySize = EnumEntitySize.SIZE_4;
        }
        else if (f < 1.75D) {
            myEntitySize = EnumEntitySize.SIZE_5;
        }
        else {
            myEntitySize = EnumEntitySize.SIZE_6;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int incr)
    {
        if (boatIsEmpty)
        {
            boatPosRotationIncrements = incr + 5;
        } else
        {
            double dx = x - posX;
            double dy = y - posY;
            double dz = z - posZ;
            double d = dx * dx + dy * dy + dz * dz;

            if (d < 0.3D)
            {
                return;
            }

            syncPosWithServer = true;
            boatPosRotationIncrements = incr;
        }

        boatX = x;
        boatY = y;
        boatZ = z;
        boatYaw = yaw;
        boatPitch = pitch;
        motionX = boatVelX;
        motionY = boatVelY;
        motionZ = boatVelZ;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setVelocity(double x, double y, double z)
    {
        boatVelX = motionX = x;
        boatVelY = motionY = y;
        boatVelZ = motionZ = z;
    }

    @Override
    public void onUpdate()
    {
        onEntityUpdate();
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        double horvel = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (worldObj.isRemote)
        {
            if (riddenByEntity == null)
            {
                setIsBoatEmpty(true);
            }
            spawnParticles(horvel);
        }

        if (worldObj.isRemote && (boatIsEmpty || syncPosWithServer))
        {
            handleClientUpdate();
            if (boatPosRotationIncrements == 0)
            {
                syncPosWithServer = false;
            }
        } else
        {
            handleServerUpdate(horvel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void handleClientUpdate()
    {
        if (boatPosRotationIncrements > 0)
        {
            double dx = posX + (boatX - posX) / boatPosRotationIncrements;
            double dy = posY + (boatY - posY) / boatPosRotationIncrements;
            double dz = posZ + (boatZ - posZ) / boatPosRotationIncrements;
            double ang = MathHelper.wrapAngleTo180_double(boatYaw - rotationYaw);
            rotationYaw = (float) (rotationYaw + ang / boatPosRotationIncrements);
            rotationPitch = (float) (rotationPitch + (boatPitch - rotationPitch) / boatPosRotationIncrements);
            boatPosRotationIncrements--;
            setPosition(dx, dy, dz);
            setRotation(rotationYaw, rotationPitch);
        } else
        {
            setPosition(posX + motionX, posY + motionY, posZ + motionZ);

            if (onGround)
            {
                motionX *= groundFriction;
                motionY *= groundFriction;
                motionZ *= groundFriction;
            }

            motionX *= horFriction;
            motionY *= vertFriction;
            motionZ *= horFriction;
        }
        setRotatedBoundingBox();
    }

    protected void handleServerUpdate(double horvel)
    {
        boolean underControl = false;

        //START outer forces
        byte b0 = 5;
        int bpermeter = (int) (b0 * (boundingBox.maxY - boundingBox.minY));
        float watervolume = 0F;
        AxisAlignedBB axisalignedbb = AxisAlignedBB.getBoundingBox(0D, 0D, 0D, 0D, 0D, 0D);
        int belowwater = 0;
        for (; belowwater < bpermeter; belowwater++)
        {
            double d1 = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * belowwater / bpermeter;
            double d2 = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * (belowwater + 1) / bpermeter;
            axisalignedbb.setBounds(boundingBox.minX, d1, boundingBox.minZ, boundingBox.maxX, d2, boundingBox.maxZ);

            if (!isAABBInLiquidNotFall(worldObj, axisalignedbb))
            {
                break;
            }
        }
        if (belowwater > 0 && layeredBlockVolumeCount != null)
        {
            int k = belowwater / b0;
            for (int y = 0; y <= k && y < layeredBlockVolumeCount.length; y++)
            {
                if (y == k)
                {
                    watervolume += layeredBlockVolumeCount[y] * (belowwater % b0) * MaterialDensity.WATER_DENSITY / b0;
                } else
                {
                    watervolume += layeredBlockVolumeCount[y] * MaterialDensity.WATER_DENSITY;
                }
            }
        }

        if (onGround)
        {
            isFlying = false;
        }

        float gravity = 0.05F;
        if (watervolume > 0F)
        {
            isFlying = false;
            float buoyancyforce = MaterialDensity.WATER_DENSITY * watervolume * gravity; //F = rho * V * g (Archimedes' law)
            float mass = capabilities.getMass();
            motionY += buoyancyforce / mass;
        }
        if (!isFlying())
        {
            motionY -= gravity;
        }
        capabilities.updateEngines();
        //END outer forces

        //START player input
        if (riddenByEntity == null)
        {
            if (prevRiddenByEntity != null)
            {
                if (ArchimedesShipMod.instance.modConfig.disassembleOnDismount)
                {
                    alignToGrid();
                    updateRiderPosition(prevRiddenByEntity, seatX, seatY, seatZ, 1);
                    disassemble(false);
                } else
                {
                    if (!worldObj.isRemote && isFlying())
                    {
                        EntityParachute parachute = new EntityParachute(worldObj, this, seatX, seatY, seatZ);
                        if (worldObj.spawnEntityInWorld(parachute))
                        {
                            prevRiddenByEntity.mountEntity(parachute);
                            prevRiddenByEntity.setSneaking(false);
                        }
                    }
                }
                prevRiddenByEntity = null;
            }
        }

        if (riddenByEntity == null)
        {
            if (isFlying())
            {
                motionY -= BASE_LIFT_SPEED * 0.2F;
            }
        } else
        {
            underControl = handlePlayerControl();
            prevRiddenByEntity = riddenByEntity;
        }
        if( !underControl ) driftToGrid();
        //END player input

        //START limit motion
        double newhorvel = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double maxvel = ArchimedesShipMod.instance.modConfig.speedLimit;
        if (newhorvel > maxvel)
        {
            double d = maxvel / newhorvel;
            motionX *= d;
            motionZ *= d;
            newhorvel = maxvel;
        }
        motionY = MathHelperMod.clamp_double(motionY, -maxvel, maxvel);
        //END limit motion

        if (onGround)
        {
            motionX *= groundFriction;
            motionY *= groundFriction;
            motionZ *= groundFriction;
        }
        rotationPitch = rotationPitch + (motionYaw * ArchimedesShipMod.instance.modConfig.bankingMultiplier - rotationPitch) * 0.15f;
        motionYaw *= 0.7F;
        //motionYaw = MathHelper.clamp_float(motionYaw, -BASE_TURN_SPEED * ShipMod.instance.modConfig.turnSpeed, BASE_TURN_SPEED * ShipMod.instance.modConfig.turnSpeed);
        rotationYaw += motionYaw;
        setRotatedBoundingBox();
        moveEntity(motionX, motionY, motionZ);
        posY = Math.min(posY, worldObj.getHeight());
        motionX *= horFriction;
        motionY *= vertFriction;
        motionZ *= horFriction;

        if (ArchimedesShipMod.instance.modConfig.shipControlType == ArchimedesConfig.CONTROL_TYPE_VANILLA)
        {
            double newyaw = rotationYaw;
            double dx = prevPosX - posX;
            double dz = prevPosZ - posZ;

            if (riddenByEntity != null && !isBraking() && dx * dx + dz * dz > 0.01D)
            {
                newyaw = 270F - Math.toDegrees(Math.atan2(dz, dx)) + frontDirection * 90F;
            }

            double deltayaw = MathHelper.wrapAngleTo180_double(newyaw - rotationYaw);
            double maxyawspeed = 2D;
            if (deltayaw > maxyawspeed)
            {
                deltayaw = maxyawspeed;
            }
            if (deltayaw < -maxyawspeed)
            {
                deltayaw = -maxyawspeed;
            }

            rotationYaw = (float) (rotationYaw + deltayaw);
        }
        setRotation(rotationYaw, rotationPitch);

        //START Collision
        if (!worldObj.isRemote)
        {
            @SuppressWarnings("unchecked")
            List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(0.2D, 0.0D, 0.2D));
            if (list != null && !list.isEmpty())
            {
                for (Entity entity : list)
                {
                    if (entity != riddenByEntity && entity.canBePushed())
                    {
                        if (entity instanceof EntityShip)
                        {
                            entity.applyEntityCollision(this);
                        } else if (entity instanceof EntityBoat)
                        {
                            double d0 = this.posX - entity.posX;
                            double d1 = this.posZ - entity.posZ;
                            double d2 = MathHelper.abs_max(d0, d1);

                            if (d2 >= 0.01D)
                            {
                                d2 = MathHelper.sqrt_double(d2);
                                d0 /= d2;
                                d1 /= d2;
                                double d3 = 1.0D / d2;

                                if (d3 > 1.0D)
                                {
                                    d3 = 1.0D;
                                }

                                d0 *= d3;
                                d1 *= d3;
                                d0 *= 0.05D;
                                d1 *= 0.05D;
                                d0 *= 1.0F - entity.entityCollisionReduction;
                                d1 *= 1.0F - entity.entityCollisionReduction;
                                entity.addVelocity(-d0, 0.0D, -d1);
                            }
                        }
                    }
                }
            }

            for (int l = 0; l < 4; ++l)
            {
                int i1 = MathHelper.floor_double(posX + ((l % 2) - 0.5D) * 0.8D);
                int j1 = MathHelper.floor_double(posZ + ((l / 2) - 0.5D) * 0.8D);

                for (int k1 = 0; k1 < 2; ++k1)
                {
                    int l1 = MathHelper.floor_double(posY) + k1;
                    Block block = worldObj.getBlock(i1, l1, j1);

                    if (block == Blocks.snow)
                    {
                        worldObj.setBlockToAir(i1, l1, j1);
                        isCollidedHorizontally = false;
                    } else if (block == Blocks.waterlily)
                    {
                        worldObj.func_147480_a(i1, l1, j1, true);
                        isCollidedHorizontally = false;
                    }
                }
            }
        }
        //END Collision
    }

    private boolean handlePlayerControl()
    {
        boolean underControl = false;

        if (riddenByEntity instanceof EntityLivingBase)
        {
            double throttle = ((EntityLivingBase) riddenByEntity).moveForward;
            if (isFlying())
            {
                throttle *= 0.5D;
            }
            if( throttle > 0.0D ) underControl = true;

            if (ArchimedesShipMod.instance.modConfig.shipControlType == ArchimedesConfig.CONTROL_TYPE_ARCHIMEDES)
            {
                Vec3 vec = Vec3.createVectorHelper(riddenByEntity.motionX, 0D, riddenByEntity.motionZ);
                vec.rotateAroundY((float) Math.toRadians(riddenByEntity.rotationYaw));

                double steer = ((EntityLivingBase) riddenByEntity).moveStrafing;
                if( steer != 0.0D ) underControl = true;

                motionYaw += steer * BASE_TURN_SPEED * capabilities.getPoweredRotationMult() * ArchimedesShipMod.instance.modConfig.turnSpeed;

                float yaw = (float) Math.toRadians(180F - rotationYaw + frontDirection * 90F);
                vec.xCoord = motionX;
                vec.zCoord = motionZ;
                vec.rotateAroundY(yaw);
                vec.xCoord *= 0.9D;
                vec.zCoord -= throttle * BASE_FORWARD_SPEED * capabilities.getPoweredSpeedMult();
                vec.rotateAroundY(-yaw);

                motionX = vec.xCoord;
                motionZ = vec.zCoord;
            } else if (ArchimedesShipMod.instance.modConfig.shipControlType == ArchimedesConfig.CONTROL_TYPE_VANILLA)
            {
                if (throttle > 0.0D)
                {
                    double dsin = -Math.sin(Math.toRadians(riddenByEntity.rotationYaw));
                    double dcos = Math.cos(Math.toRadians(riddenByEntity.rotationYaw));
                    motionX += dsin * BASE_FORWARD_SPEED * capabilities.speedMultiplier;
                    motionZ += dcos * BASE_FORWARD_SPEED * capabilities.speedMultiplier;
                }
            }
        }

        if (controller.getShipControl() != 0)
        {
            if (controller.getShipControl() == 4)
            {
                alignToGrid();
            } else if (isBraking())
            {
                motionX *= capabilities.brakeMult;
                motionZ *= capabilities.brakeMult;
                if (isFlying())
                {
                    motionY *= capabilities.brakeMult;
                }
            } else if (controller.getShipControl() < 3 && capabilities.canFly())
            {
                int i;
                if (controller.getShipControl() == 2)
                {
                    isFlying = true;
                    i = 1;
                } else
                {
                    i = -1;
                }
                motionY += i * BASE_LIFT_SPEED * capabilities.getPoweredLiftMult();
            }
            underControl = true;
        }
        return underControl;
    }

    @Override
    public boolean handleWaterMovement()
    {
        float f = width;
        width = 0F;
        boolean ret = super.handleWaterMovement();
        width = f;
        return ret;
    }

    public boolean isFlying()
    {
        return capabilities.canFly() && (isFlying || controller.getShipControl() == 2);
    }

    public boolean isBraking()
    {
        return controller.getShipControl() == 3;
    }

    /**
     * Determines whether the entity should be pushed by fluids
     */
    @Override
    public boolean isPushedByWater()
    {
        return ticksExisted > 60;
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles(double horvel)
    {
		/*if (isInWater() && horvel > 0.1625D)
		{
			/*double yaw = Math.toRadians(rotationYaw);
			double cosyaw = Math.cos(yaw);
			double sinyaw = Math.sin(yaw);*//*
											
											for (int j = 0; j < 1D + horvel * 60D; j++)
											{
											worldObj.spawnParticle("splash", posX + (rand.nextFloat() - 0.5F) * width, posY, posZ + (rand.nextFloat() - 0.5F) * width, motionX, motionY + 1F, motionZ);
											}
											for (int j = 0; j < 1D + horvel * 20D; j++)
											{
											worldObj.spawnParticle("bubble", posX + rand.nextFloat() - 0.5F, posY - 0.2D, posZ + rand.nextFloat() - 0.5F, 0D, 0D, 0D);
											}
											}*/
        if (capabilities.getEngines() != null)
        {
            Vec3 vec = Vec3.createVectorHelper(0d, 0d, 0d);
            float yaw = (float) Math.toRadians(rotationYaw);
            for (TileEntityEngine engine : capabilities.getEngines())
            {
                if (engine.isRunning())
                {
                    vec.xCoord = engine.xCoord - shipChunk.getCenterX() + 0.5f;
                    vec.yCoord = engine.yCoord;
                    vec.zCoord = engine.zCoord - shipChunk.getCenterZ() + 0.5f;
                    vec.rotateAroundY(yaw);
                    worldObj.spawnParticle("smoke", posX + vec.xCoord, posY + vec.yCoord + 1d, posZ + vec.zCoord, 0d, 0d, 0d);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setIsBoatEmpty(boolean flag)
    {
        boatIsEmpty = flag;
    }

    @Override
    public boolean shouldRiderSit()
    {
        return true;
    }

    public void updateRiderPosition()
    {
        updateRiderPosition(getFirstPassenger(), seatX, seatY, seatZ, 1);
    }

    public void updateRiderPosition(Entity entity, int seatx, int seaty, int seatz, int flags) {
        if (entity != null) {
            float yaw = (float) Math.toRadians(rotationYaw);
            float pitch = (float) Math.toRadians(rotationPitch);

            int x1 = seatx, y1 = seaty, z1 = seatz;
            if ((flags & 1) == 1)
            {
                if (frontDirection == 0)
                {
                    z1 -= 1;
                } else if (frontDirection == 1)
                {
                    x1 += 1;
                } else if (frontDirection == 2)
                {
                    z1 += 1;
                } else if (frontDirection == 3)
                {
                    x1 -= 1;
                }

                Block block = shipChunk.getBlock(x1, MathHelper.floor_double(y1 + getMountedYOffset() + entity.getYOffset()), z1);
                if (block.isOpaqueCube())
                {
                    x1 = seatx;
                    y1 = seaty;
                    z1 = seatz;
                }
            }

            double yoff = (flags & 2) == 2 ? 0d : getMountedYOffset();
            Vec3 vec = Vec3.createVectorHelper(x1 - shipChunk.getCenterX() + 0.5d, y1 - shipChunk.minY() + yoff, z1 - shipChunk.getCenterZ() + 0.5d);
            switch (frontDirection)
            {
                case 0:
                    vec.rotateAroundZ(-pitch);
                    break;
                case 1:
                    vec.rotateAroundX(pitch);
                    break;
                case 2:
                    vec.rotateAroundZ(pitch);
                    break;
                case 3:
                    vec.rotateAroundX(-pitch);
                    break;
            }
            vec.rotateAroundY(yaw);

            entity.setPosition(posX + vec.xCoord, posY + vec.yCoord + entity.getYOffset(), posZ + vec.zCoord);
        }
    }

    @Override
    public double getPassengersRidingOffset() {
      return 0.5D;
    }

    @Override
    protected boolean canTriggerWalking()
    {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity)
    {
        return entity instanceof EntitySeat || entity.ridingEntity instanceof EntitySeat || entity instanceof EntityLiving ? null : entity.boundingBox;
        //return null;
    }

    @Override
    public boolean isPushable()
    {
        return onGround && !isInWater() && riddenByEntity == null;
    }

    @Override
    public boolean canBeCollidedWith()
    {
        return !isDead;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage)
    {
		/*if (source.isExplosion())
		{
			if (source.getEntity() != null && source.getEntity().getClass().getName().equals("ckathode.weaponmod.entity.projectile.EntityCannonBall"))
			{
				double dx = source.getEntity().posX - posX;
				double dy = source.getEntity().posY - posY;
				double dz = source.getEntity().posZ - posZ;
				
				Vec3 vec = worldObj.getWorldVec3Pool().getVecFromPool(dx, dy, dz);
				vec.rotateAroundY((float) Math.toRadians(-rotationYaw));
				
				worldObj.createExplosion(source.getEntity(), source.getEntity().posX, source.getEntity().posY, source.getEntity().posZ, 4F, false);
				source.getEntity().setDead();
			}
		}*/
        return false;
    }

    public float getHorizontalVelocity()
    {
        return (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    @Override
    public boolean interact(Player player, InteractionHand hand)
    {
        return handler.interact(player);
    }

    public void alignToGrid()
    {
        rotationYaw = Math.round(rotationYaw / 90F) * 90F;
        rotationPitch = 0F;

        Vec3 vec = Vec3.createVectorHelper(-shipChunk.getCenterX(), -shipChunk.minY(), -shipChunk.getCenterZ());
        vec.rotateAroundY((float) Math.toRadians(rotationYaw));

        int ix = MathHelperMod.round_double(vec.xCoord + posX);
        int iy = MathHelperMod.round_double(vec.yCoord + posY);
        int iz = MathHelperMod.round_double(vec.zCoord + posZ);

        posX = ix - vec.xCoord;
        posY = iy - vec.yCoord;
        posZ = iz - vec.zCoord;

        motionX = motionY = motionZ = 0D;
    }

    public void driftToGrid()
    {
        if( Math.abs( motionYaw ) < BASE_TURN_SPEED  * 0.25f )
        {
            float targetYaw = Math.round(rotationYaw / 90F) * 90F - rotationYaw;
            float targetDir = Math.min( Math.abs( targetYaw ), BASE_TURN_SPEED * 0.25f ) * Math.signum( targetYaw );
            motionYaw = targetDir;
        }

        if( Math.abs( motionX ) < BASE_FORWARD_SPEED * 0.25f && Math.abs( motionZ ) < BASE_FORWARD_SPEED * 0.25f )
        {
            Vec3 size = Vec3.createVectorHelper(shipChunk.getSizeX(), shipChunk.getSizeY(), shipChunk.getSizeZ());
            size.rotateAroundY((float) Math.toRadians(rotationYaw));

            Vec3 target = Vec3.createVectorHelper(getBlockAt(posX, size.xCoord), getBlockAt(posY, size.yCoord), getBlockAt(posZ, size.zCoord));
            double ix = target.xCoord - posX;
            double iy = target.yCoord - posY;
            double iz = target.zCoord - posZ;

            double targetX = Math.min( Math.abs( ix ), BASE_FORWARD_SPEED * 0.25f ) * Math.signum( ix );
            double targetY = Math.min( Math.abs( iy ), BASE_FORWARD_SPEED * 0.25f ) * Math.signum( iy );
            double targetZ = Math.min( Math.abs( iz ), BASE_FORWARD_SPEED * 0.25f ) * Math.signum( iz );

            motionX = targetX;
            motionZ = targetZ;
        }
    }

    public double getBlockAt( double x, double width)
    {
        return (double)((int)x) +(width % 2) * 0.5;
    }

    public boolean disassemble(boolean overwrite)
    {
        if (worldObj.isRemote) return true;

        updateRiderPosition();

        ChunkDisassembler disassembler = getDisassembler();
        disassembler.overwrite = overwrite;

        if (!disassembler.canDisassemble())
        {
            if (prevRiddenByEntity instanceof EntityPlayer)
            {
                ChatComponentText c = new ChatComponentText("Cannot disassemble ship here");
                ((EntityPlayer) prevRiddenByEntity).addChatMessage(c);
            }
            return false;
        }

        AssembleResult result = disassembler.doDisassemble();
        if (result.getShipMarker() != null)
        {
            TileEntity te = result.getShipMarker().tileEntity;
            if (te instanceof TileEntityHelm)
            {
                ((TileEntityHelm) te).setAssembleResult(result);
                ((TileEntityHelm) te).setShipInfo(info);
            }
        }

        return true;
    }

    public void dropAsItems()
    {
        TileEntity tileentity;
        Block block;
        for (int i = shipChunk.minX(); i < shipChunk.maxX(); i++)
        {
            for (int j = shipChunk.minY(); j < shipChunk.maxY(); j++)
            {
                for (int k = shipChunk.minZ(); k < shipChunk.maxZ(); k++)
                {
                    tileentity = shipChunk.getTileEntity(i, j, k);
                    if (tileentity instanceof IInventory)
                    {
                        IInventory inv = (IInventory) tileentity;
                        for (int it = 0; it < inv.getSizeInventory(); it++)
                        {
                            ItemStack is = inv.getStackInSlot(it);
                            if (is != null)
                            {
                                entityDropItem(is, 0F);
                            }
                        }
                    }
                    block = shipChunk.getBlock(i, j, k);

                    if (block != Blocks.air)
                    {
                        int meta = shipChunk.getBlockMetadata(i, j, k);
                        block.dropBlockAsItem(worldObj, MathHelper.floor_double(posX), MathHelper.floor_double(posY), MathHelper.floor_double(posZ), meta, 0);
                    }
                }
            }
        }
    }

    void fillAirBlocks(Set<ChunkPosition> set, int x, int y, int z)
    {
        if (x < shipChunk.minX() - 1 || x > shipChunk.maxX() || y < shipChunk.minY() - 1 || y > shipChunk.maxY() || z < shipChunk.minZ() - 1 || z > shipChunk.maxZ()) return;
        ChunkPosition pos = new ChunkPosition(x, y, z);
        if (set.contains(pos)) return;

        set.add(pos);
        if (shipChunk.setBlockAsFilledAir(x, y, z))
        {
            fillAirBlocks(set, x, y + 1, z);
            //fillAirBlocks(set, x, y - 1, z);
            fillAirBlocks(set, x - 1, y, z);
            fillAirBlocks(set, x, y, z - 1);
            fillAirBlocks(set, x + 1, y, z);
            fillAirBlocks(set, x, y, z + 1);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(shipChunk.getMemoryUsage());
        DataOutputStream out = new DataOutputStream(baos);
        try
        {
            ChunkIO.writeAll(out, shipChunk);
            out.flush();
            out.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        compound.setByteArray("chunk", baos.toByteArray());
        compound.setByte("seatX", (byte) seatX);
        compound.setByte("seatY", (byte) seatY);
        compound.setByte("seatZ", (byte) seatZ);
        compound.setByte("front", (byte) frontDirection);

        if (!shipChunk.chunkTileEntityMap.isEmpty())
        {
            NBTTagList tileentities = new NBTTagList();
            for (TileEntity tileentity : shipChunk.chunkTileEntityMap.values())
            {
                NBTTagCompound comp = new NBTTagCompound();
                tileentity.writeToNBT(comp);
                tileentities.appendTag(comp);
            }
            compound.setTag("tileent", tileentities);
        }

        compound.setString("name", info.shipName);
        if (info.owner != null)
        {
            compound.setString("owner", info.owner);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        byte[] ab = compound.getByteArray("chunk");
        ByteArrayInputStream bais = new ByteArrayInputStream(ab);
        DataInputStream in = new DataInputStream(bais);
        try
        {
            ChunkIO.read(in, shipChunk);
            in.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        if (compound.hasKey("seat"))
        {
            short s = compound.getShort("seat");
            seatX = s & 0xF;
            seatY = s >>> 4 & 0xF;
            seatZ = s >>> 8 & 0xF;
            frontDirection = s >>> 12 & 3;
        } else
        {
            seatX = compound.getByte("seatX") & 0xFF;
            seatY = compound.getByte("seatY") & 0xFF;
            seatZ = compound.getByte("seatZ") & 0xFF;
            frontDirection = compound.getByte("front") & 3;
        }

        NBTTagList tileentities = compound.getTagList("tileent", 10);
        if (tileentities != null)
        {
            for (int i = 0; i < tileentities.tagCount(); i++)
            {
                NBTTagCompound comp = tileentities.getCompoundTagAt(i);
                TileEntity tileentity = TileEntity.createAndLoadEntity(comp);
                shipChunk.setTileEntity(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord, tileentity);
            }
        }

        info = new ShipInfo();
        info.shipName = compound.getString("name");
        if (compound.hasKey("owner"))
        {
            info.shipName = compound.getString("owner");
        }
    }

    @Override
    public void writeSpawnData(ByteBuf data)
    {
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
    public void readSpawnData(ByteBuf data)
    {
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