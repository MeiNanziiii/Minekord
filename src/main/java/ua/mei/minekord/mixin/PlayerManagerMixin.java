package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.mei.minekord.bot.DiscordUtils;
import ua.mei.minekord.cache.IPCache;
import ua.mei.minekord.config.AuthSpec;
import ua.mei.minekord.config.MinekordConfigKt;
import ua.mei.minekord.event.IPCheckEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin", at = @At("RETURN"), cancellable = true)
    private void minekord$checkRoles(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() == null) {
            boolean uuidFromSnowflake = MinekordConfigKt.getConfig().get(AuthSpec.INSTANCE.getUuidFromSnowflake());
            boolean requiredRoles = !MinekordConfigKt.getConfig().get(AuthSpec.INSTANCE.getRequiredRoles()).isEmpty();
            boolean loginByIp = MinekordConfigKt.getConfig().get(AuthSpec.INSTANCE.getLoginByIp());
            String playerName = gameProfile.getName();
            String cachedIp = IPCache.INSTANCE.getFromCache(playerName);

            if (!uuidFromSnowflake && requiredRoles) {
                if (DiscordUtils.INSTANCE.getPlayer(playerName) == null) {
                    cir.setReturnValue(Text.translatable("multiplayer.disconnect.generic"));
                }
            }

            if (loginByIp && socketAddress instanceof InetSocketAddress inet && !cachedIp.equals(inet.getHostName())) {
                IPCheckEvent.Companion.getEvent().invoker().request(socketAddress, gameProfile);
                cir.setReturnValue(Text.translatable("multiplayer.disconnect.generic"));
            }
        }
    }
}
