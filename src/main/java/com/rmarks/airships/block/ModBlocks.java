package com.rmarks.airships.block;

import com.rmarks.airships.AirshipsMod;
import com.rmarks.airships.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AirshipsMod.MOD_ID);

    public static final RegistryObject<Block> BALLOON_BLOCK = registerBlock(
        "balloon_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL)));

    public static final RegistryObject<Block> ENGINE_BLOCK = registerBlock(
        "engine_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.FURNACE).sound(SoundType.METAL)));

    public static final RegistryObject<Block> DOCK_BLOCK = registerBlock(
        "dock_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)));

    public static final RegistryObject<Block> HELM_BLOCK = registerBlock(
            "dock_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, Supplier<T> block) {
        ModItems.ITEMS.register(name,
            () -> new BlockItem(block.get(), new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION))
        );
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
