package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.mei.minekord.cache.IPCache;
import ua.mei.minekord.config.MinekordConfig;
import ua.mei.minekord.event.IPCheckEvent;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "checkCanJoin", at = @At("RETURN"), cancellable = true)
    public void minekord$checkCanJoin(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() == null) {
            if (MinekordConfig.Auth.INSTANCE.getIpBasedLogin() && !IPCache.INSTANCE.containsInCache(socketAddress, gameProfile) && !server.getPlayerManager().getIpBanList().isBanned(socketAddress)) {
                if (!IPCache.INSTANCE.isRequested(socketAddress, gameProfile)) {
                    IPCheckEvent.Companion.getEVENT().invoker().check(socketAddress, gameProfile);
                }
                cir.setReturnValue(Text.literal(MinekordConfig.Messages.INSTANCE.getIpKickMessage()));
            }
        }
    }
}
