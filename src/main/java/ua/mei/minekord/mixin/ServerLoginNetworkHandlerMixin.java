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
    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable
    private GameProfile profile;

    @Shadow
    public abstract void disconnect(Text text);

    @Shadow
    protected abstract void sendSuccessPacket(GameProfile gameProfile);

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    private void minekord$trueUuids(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (SnowflakeToUUID.INSTANCE.enabled()) {
            if (this.server.isOnlineMode() && !SnowflakeToUUID.INSTANCE.allowOfflinePlayers() && !SnowflakeToUUID.INSTANCE.premiumPlayer(loginHelloC2SPacket.comp_907())) {
                this.disconnect(Text.translatable("multiplayer.disconnect.generic"));
                ci.cancel();
            }

            UUID trueUuid = SnowflakeToUUID.INSTANCE.generateFromNickname(loginHelloC2SPacket.comp_765());

            if (trueUuid != null) {
                this.profile = new GameProfile(trueUuid, loginHelloC2SPacket.comp_765());
                this.sendSuccessPacket(this.profile);
            } else {
                this.disconnect(Text.translatable("multiplayer.disconnect.generic"));
            }

            ci.cancel();
        }
    }

    @Inject(method = "sendSuccessPacket", at = @At("HEAD"))
    private void minekord$checkRoles(GameProfile gameProfile, CallbackInfo ci) {
        if (!SnowflakeToUUID.INSTANCE.enabled()) {
            if (SnowflakeToUUID.INSTANCE.getPlayer(gameProfile.getName()) == null) {
                this.disconnect(Text.translatable("multiplayer.disconnect.generic"));
            }
        }
    }
}
