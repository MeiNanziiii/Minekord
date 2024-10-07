package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.mei.minekord.auth.SnowflakeToUUID;

import java.util.UUID;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow public abstract void disconnect(Text text);

    @Shadow @Nullable private GameProfile profile;

    @Shadow protected abstract void sendSuccessPacket(GameProfile gameProfile);

    @Shadow @Final MinecraftServer server;

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    private void minekord$trueUuids(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (SnowflakeToUUID.INSTANCE.enabled()) {
            if (this.server.isOnlineMode() && !SnowflakeToUUID.INSTANCE.allowOfflinePlayers() && !SnowflakeToUUID.INSTANCE.premiumPlayer(loginHelloC2SPacket.comp_907())) {
                this.disconnect(Text.literal("test1"));
                ci.cancel();
            }

            UUID trueUuid = SnowflakeToUUID.INSTANCE.generateFromNickname(loginHelloC2SPacket.comp_765());

            if (trueUuid != null) {
                this.profile = new GameProfile(trueUuid, loginHelloC2SPacket.comp_765());
                this.sendSuccessPacket(this.profile);
            } else {
                this.disconnect(Text.literal("test2"));
            }

            ci.cancel();
        }
    }
}
