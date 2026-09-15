package net.nomorepowder.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.rule.BlockRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces powder snow selected by terrain material rules without affecting
 * structure or feature placement.
 */
@Mixin(BlockRule.class)
public class MaterialBlockRuleMixin {

    @ModifyReturnValue(method = "tryApply", at = @At("RETURN"))
    private BlockState nomorepowder$replacePowderSnow(BlockState state) {
        if (state.is(Blocks.POWDER_SNOW)) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }

        return state;
    }
}
