package ua.bonfiremc.minekord.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.bonfiremc.minekord.event.AdvancementGrantEvent;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "lambda$award$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void invokeEvent(AdvancementHolder holder, DisplayInfo display, CallbackInfo ci) {
        AdvancementGrantEvent.EVENT.invoker().onAdvancementGrant(this.player, holder);
    }
}
