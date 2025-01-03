package ua.mei.minekord.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.kord.core.entity.Member;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.mei.minekord.cache.IPCache;
import ua.mei.minekord.config.MinekordConfig;
import ua.mei.minekord.event.IPCheckEvent;
import ua.mei.minekord.utils.AuthUtils;

import java.util.concurrent.atomic.AtomicInteger;

// TODO: improve this fuckin mixin
@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    static Logger LOGGER;

    @Shadow
    @Final
    private static AtomicInteger NEXT_AUTHENTICATOR_THREAD_ID;

    @Shadow
    @Nullable
    private GameProfile profile;

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    private ServerLoginNetworkHandler.State state;

    @Shadow
    @Final
    ClientConnection connection;

    @Unique
    Member member = null;

    @Shadow
    public abstract void disconnect(Text text);

    @Shadow @Nullable String profileName;

    @Shadow abstract void startVerify(GameProfile gameProfile);

    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    public void minekord$checkIp(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (MinekordConfig.Auth.INSTANCE.getIpBasedLogin() && IPCache.INSTANCE.isBlocked(this.connection.getAddress())) {
            this.disconnect(Text.translatable("multiplayer.disconnect.ip_banned"));
            ci.cancel();
        }
    }

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    public void minekord$replaceUuid(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (MinekordConfig.Auth.INSTANCE.getSnowflakeBasedUuid() || MinekordConfig.Auth.INSTANCE.getIpBasedLogin() || !MinekordConfig.Auth.INSTANCE.getRequiredRoles().isEmpty()) {
            member = AuthUtils.INSTANCE.findMember(loginHelloC2SPacket.comp_765());
            if (member == null) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
                return;
            }
        }

        if (MinekordConfig.Auth.INSTANCE.getSnowflakeBasedUuid() && !server.isOnlineMode()) {
            this.profile = new GameProfile(AuthUtils.INSTANCE.uuidFromMember(member), loginHelloC2SPacket.comp_765());
            LOGGER.info("Snowflake based UUID of player {} is {}", this.profile.getName(), this.profile.getId());
        }

        if (MinekordConfig.Auth.INSTANCE.getIpBasedLogin() && this.profile != null) {
            if (!IPCache.INSTANCE.containsInCache(this.connection.getAddress(), this.profile)) {
                if (!IPCache.INSTANCE.isRequested(this.connection.getAddress(), this.profile)) {
                    IPCheckEvent.Companion.getEVENT().invoker().check(this.connection.getAddress(), this.profile);
                }
                this.disconnect(Text.literal(MinekordConfig.Messages.INSTANCE.getIpKickMessage()));
                ci.cancel();
            }
        }
    }

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;setUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V"), cancellable = true)
    public void minekord$replaceThread(LoginKeyC2SPacket loginKeyC2SPacket, CallbackInfo ci, @Local String string) {
        if (MinekordConfig.Auth.INSTANCE.getSnowflakeBasedUuid()) {
            Thread thread = new Thread("Minekord User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet()) {
                public void run() {
                    assert ServerLoginNetworkHandlerMixin.this.profileName != null;

                    if (member == null) {
                        member = AuthUtils.INSTANCE.findMember(ServerLoginNetworkHandlerMixin.this.profileName);
                    }

                    if (member != null) {
                        ServerLoginNetworkHandlerMixin.this.profile = new GameProfile(AuthUtils.INSTANCE.uuidFromMember(member), ServerLoginNetworkHandlerMixin.this.profileName);
                    }

                    if (ServerLoginNetworkHandlerMixin.this.profile != null) {
                        LOGGER.info("Snowflake based UUID of player {} is {}", ServerLoginNetworkHandlerMixin.this.profile.getName(), ServerLoginNetworkHandlerMixin.this.profile.getId());
                        ServerLoginNetworkHandlerMixin.this.startVerify(ServerLoginNetworkHandlerMixin.this.profile);
                    } else {
                        ServerLoginNetworkHandlerMixin.this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                        LOGGER.error("Username '{}' tried to join with an invalid session", ServerLoginNetworkHandlerMixin.this.profileName);
                    }
                }
            };
            thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
            thread.start();
            ci.cancel();
        }
    }
}
