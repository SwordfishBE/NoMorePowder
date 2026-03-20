package com.nomorepowder.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercepts SurfaceRules$StateRule.tryApply() — the method that surface rules
 * use to decide which block to place at a given position during terrain generation.
 *
 * Powder snow in frozen biomes (Frozen Peaks, Snowy Slopes, Grove, etc.) is placed
 * via surface rules, NOT via features. The surface system calls tryApply() which
 * returns the BlockState to place, then writes it directly into the chunk — completely
 * bypassing WorldGenRegion.setBlock() or any feature placement path.
 *
 * By intercepting the return value, we replace POWDER_SNOW with SNOW_BLOCK
 * before it ever reaches the chunk. No other placement path is touched.
 *
 * Note: Trial Chamber powder snow is placed via StructureTemplate after generation,
 * which does NOT go through SurfaceRules at all — so it remains unaffected.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$StateRule")
public class SurfaceRulesStateRuleMixin {

    @ModifyReturnValue(method = "tryApply", at = @At("RETURN"))
    private @Nullable BlockState nomorepowder$replacePowderSnow(@Nullable BlockState state) {
        if (state != null && state.is(Blocks.POWDER_SNOW)) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        return state;
    }
}
