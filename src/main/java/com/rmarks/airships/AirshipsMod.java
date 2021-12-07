package com.rmarks.airships;

import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AirshipsMod.MOD_ID)
public class AirshipsMod
{
    public static final String MOD_ID		= "airships";
    public static final String MOD_VERSION	= "0.0.1 v0.0.1";
    public static final String MOD_NAME	    = "Airships";
    public static final String ASSETS       = "archimedes";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public AirshipsMod() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }
}
