package ua.mei.minekord.mixin;

import com.mojang.authlib.GameProfile;
import com.uchuhimo.konf.Config;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.network.ClientConnection;
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
import ua.mei.minekord.cache.IPCache;
import ua.mei.minekord.config.MinekordConfigKt;
import ua.mei.minekord.config.spec.ExperimentalSpec;
import ua.mei.minekord.config.spec.MessagesSpec;
import ua.mei.minekord.event.IPCheckEvent;
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
    @Final
    ClientConnection connection;

    @Shadow
    public abstract void disconnect(Text text);

    @Shadow
    public abstract void acceptPlayer();

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    private void minekord$trueUuids(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        Config config = MinekordConfigKt.getConfig();
        ExperimentalSpec.DiscordSpec discordSpec = ExperimentalSpec.DiscordSpec.INSTANCE;

        if (config.get(discordSpec.getEnabled())) {
            if (config.get(discordSpec.getLoginByIp()) && IPCache.INSTANCE.isBlockedIp(this.connection.getAddress())) {
                this.disconnect(Text.translatable("multiplayer.disconnect.ip_banned"));
                ci.cancel();
            }

            UUID uuid = loginHelloC2SPacket.comp_907().orElse(null);

            if (uuid == null) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            if (this.server.isOnlineMode() && !config.get(discordSpec.getAllowOfflinePlayers()) && !ExperimentalUtils.INSTANCE.premiumPlayer(uuid)) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            UUID trueUuid = ExperimentalUtils.INSTANCE.generateFromNickname(loginHelloC2SPacket.comp_765());

            if (trueUuid == null) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            }

            this.profile = new GameProfile(trueUuid, loginHelloC2SPacket.comp_765());

            if (config.get(discordSpec.getLoginByIp()) && !IPCache.INSTANCE.containsInCache(this.profile.getName(), this.connection.getAddress())) {
                if (!IPCache.INSTANCE.isIpRequested(this.connection.getAddress(), this.profile)) {
                    IPCheckEvent.Companion.getEVENT().invoker().request(this.connection.getAddress(), this.profile);
                }
                this.disconnect(Text.literal(config.get(MessagesSpec.INSTANCE.getIpKickMessage())));
                ci.cancel();
            }

            LuckPermsProvider.get().getUserManager().loadUser(trueUuid).thenAcceptAsync(user -> {
                if (user != null) {
                    this.acceptPlayer();
                } else {
                    this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                }
            });

            ci.cancel();
        }
    }
}
