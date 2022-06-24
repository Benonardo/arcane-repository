package net.messer.mystical_index.mixin;

import net.messer.mystical_index.client.render.InterceptionWidget;
import net.messer.mystical_index.util.ChatInterception;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(
            method = "render",
            at = @At(value = "HEAD")
    )
    private void renderInterceptionWidget(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var interception = ChatInterception.shouldIntercept(MinecraftClient.getInstance().player, "");

        if (interception != null) {
            var thisScreen = (ChatScreen) (Object) this;

            InterceptionWidget.render(matrices, thisScreen.width, thisScreen.height, delta, interception);
        }
    }
}
