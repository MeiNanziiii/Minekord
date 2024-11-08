package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import net.luckperms.api.LuckPermsProvider;
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
    public abstract void disconnect(Text text);

    @Shadow
    public abstract void acceptPlayer();

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    private void minekord$trueUuids(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getEnabled())) {
            UUID uuid = null;

            if (loginHelloC2SPacket.comp_907().isPresent()) {
                uuid = loginHelloC2SPacket.comp_907().get();
            } else {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            if (this.server.isOnlineMode() && !MinekordConfigKt.getConfig().get(ExperimentalSpec.DiscordSpec.INSTANCE.getAllowOfflinePlayers()) && !ExperimentalUtils.INSTANCE.premiumPlayer(uuid)) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            UUID trueUuid = ExperimentalUtils.INSTANCE.generateFromNickname(loginHelloC2SPacket.comp_765());

            LuckPermsProvider.get().getUserManager().loadUser(trueUuid).thenAcceptAsync(user -> {
                if (user != null) {
                    this.profile = new GameProfile(trueUuid, loginHelloC2SPacket.comp_765());
                    this.acceptPlayer();
                } else {
                    this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                }
            });

            ci.cancel();
        }
    }
}
