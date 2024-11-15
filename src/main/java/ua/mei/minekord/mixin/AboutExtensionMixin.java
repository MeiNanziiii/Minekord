package ua.mei.minekord.mixin;

import dev.kordex.core.extensions.impl.AboutExtension;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AboutExtension.class)
public abstract class AboutExtensionMixin {
    @Inject(method = "setup", at = @At("HEAD"), cancellable = true, remap = false)
    public void minekord$disableAbout(Continuation<? super Unit> continuation, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(null);
    }
}
