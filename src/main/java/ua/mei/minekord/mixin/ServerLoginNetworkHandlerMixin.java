package ua.mei.minekord.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.kord.core.entity.Member;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.mei.minekord.config.MinekordConfig;
import ua.mei.minekord.utils.AuthUtils;

import java.util.concurrent.atomic.AtomicInteger;

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
    GameProfile profile;

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    ServerLoginNetworkHandler.State state;

    @Shadow
    public abstract void disconnect(Text text);

    @Inject(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"), cancellable = true)
    public void minekord$replaceUuid(LoginHelloC2SPacket loginHelloC2SPacket, CallbackInfo ci) {
        if (MinekordConfig.Auth.INSTANCE.getSnowflakeBasedUuid() && !server.isOnlineMode()) {
            Member member = AuthUtils.INSTANCE.findMember(loginHelloC2SPacket.comp_765());

            if (member == null) {
                this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                ci.cancel();
            } else {
                this.profile = new GameProfile(AuthUtils.INSTANCE.uuidFromMember(member), loginHelloC2SPacket.comp_765());
                LOGGER.info("Snowflake based UUID of player {} is {}", this.profile.getName(), this.profile.getId());
            }
        }
    }

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;setUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V"), cancellable = true)
    public void minekord$replaceThread(LoginKeyC2SPacket loginKeyC2SPacket, CallbackInfo ci, @Local String string) {
        if (MinekordConfig.Auth.INSTANCE.getSnowflakeBasedUuid()) {
            Thread thread = new Thread("User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet()) {
                public void run() {
                    GameProfile gameProfile = ServerLoginNetworkHandlerMixin.this.profile;

                    Member member = AuthUtils.INSTANCE.findMember(gameProfile.getName());

                    if (member != null) {
                        ServerLoginNetworkHandlerMixin.this.profile = new GameProfile(AuthUtils.INSTANCE.uuidFromMember(member), gameProfile.getName());
                    }

                    if (ServerLoginNetworkHandlerMixin.this.profile != null) {
                        LOGGER.info("Snowflake based UUID of player {} is {}", ServerLoginNetworkHandlerMixin.this.profile.getName(), ServerLoginNetworkHandlerMixin.this.profile.getId());
                        ServerLoginNetworkHandlerMixin.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else if (ServerLoginNetworkHandlerMixin.this.server.isSingleplayer()) {
                        LOGGER.warn("Failed to verify username but will let them in anyway!");
                        ServerLoginNetworkHandlerMixin.this.profile = gameProfile;
                        ServerLoginNetworkHandlerMixin.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else {
                        ServerLoginNetworkHandlerMixin.this.disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                        LOGGER.error("Username '{}' tried to join with an invalid session", gameProfile.getName());
                    }
                }
            };
            thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
            thread.start();
            ci.cancel();
        }
    }
}
