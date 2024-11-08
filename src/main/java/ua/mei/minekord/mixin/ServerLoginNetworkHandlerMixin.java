package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
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
import ua.mei.minekord.config.MinekordConfigKt;
import ua.mei.minekord.config.spec.ExperimentalSpec;
import ua.mei.minekord.utils.ExperimentalUtils;

import java.util.UUID;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable GameProfile profile;
    @Shadow
    ServerLoginNetworkHandler.State state;
    @Shadow
    @Final
    ClientConnection connection;

    @Shadow
    public abstract void disconnect(Text text);

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    private void minekord$trueUuids(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getEnabled())) {
            if (this.server.isOnlineMode() && !MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getAllowOfflinePlayers()) && !ExperimentalUtils.INSTANCE.premiumPlayer(loginHelloC2SPacket.comp_907().get())) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            UUID trueUuid = ExperimentalUtils.INSTANCE.generateFromNickname(loginHelloC2SPacket.comp_765());

            if (trueUuid != null) {
                this.state = ServerLoginNetworkHandler.State.KEY;
                this.connection.send(new LoginSuccessS2CPacket(this.profile));
            } else {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
            }

            ci.cancel();
        }
    }
}
