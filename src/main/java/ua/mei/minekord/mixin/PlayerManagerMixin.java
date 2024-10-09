package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.mei.minekord.cache.IPCache;
import ua.mei.minekord.config.ExperimentalSpec;
import ua.mei.minekord.config.MinekordConfigKt;
import ua.mei.minekord.event.IPCheckEvent;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin", at = @At("RETURN"), cancellable = true)
    private void minekord$checkRoles(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() == null && MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getEnabled()) && MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getLoginByIp())) {
            if (!IPCache.INSTANCE.haveInCache(gameProfile.getName(), socketAddress)) {
                IPCheckEvent.Companion.getEvent().invoker().request(socketAddress, gameProfile);
                cir.setReturnValue(Text.translatable("multiplayer.disconnect.generic"));
            }
        }
    }
}
