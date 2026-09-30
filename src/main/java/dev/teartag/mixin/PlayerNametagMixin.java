package dev.teartag.mixin;

import com.mojang.serialization.JsonOps;
import dev.teartag.state.NametagEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerNametagMixin implements NametagEntityData {
    @Unique private static final EntityDataAccessor<String> TEARTAG_TEXT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
    @Unique private static final EntityDataAccessor<Integer> TEARTAG_TEARS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Boolean> TEARTAG_ELIMINATED = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> TEARTAG_ENABLED = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void teartag$defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(TEARTAG_TEXT, "");
        builder.define(TEARTAG_TEARS, 0);
        builder.define(TEARTAG_ELIMINATED, false);
        builder.define(TEARTAG_ENABLED, false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void teartag$save(ValueOutput output, CallbackInfo ci) {
        ValueOutput value = output.child("teartag_nametag");
        value.putString("text", teartag$getNametagText());
        value.putInt("tears", teartag$getNametagTears());
        value.putBoolean("eliminated", teartag$isNametagEliminated());
        value.putBoolean("enabled", teartag$isNametagEnabled());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void teartag$load(ValueInput input, CallbackInfo ci) {
        ValueInput value = input.childOrEmpty("teartag_nametag");
        teartag$setNametagRaw(value.getStringOr("text", ""), value.getIntOr("tears", 0),
            value.getBooleanOr("eliminated", false), value.getBooleanOr("enabled", false));
    }

    @Override public String teartag$getNametagText() { return ((Player) (Object) this).getEntityData().get(TEARTAG_TEXT); }
    @Override public int teartag$getNametagTears() { return ((Player) (Object) this).getEntityData().get(TEARTAG_TEARS); }
    @Override public boolean teartag$isNametagEliminated() { return ((Player) (Object) this).getEntityData().get(TEARTAG_ELIMINATED); }
    @Override public boolean teartag$isNametagEnabled() { return ((Player) (Object) this).getEntityData().get(TEARTAG_ENABLED); }

    @Override
    public void teartag$setNametagState(Component text, int tears, boolean eliminated, boolean enabled) {
        String encoded = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, text).result().map(Object::toString).orElse(text.getString());
        teartag$setNametagRaw(encoded, tears, eliminated, enabled);
    }

    @Unique
    private void teartag$setNametagRaw(String text, int tears, boolean eliminated, boolean enabled) {
        var data = ((Player) (Object) this).getEntityData();
        data.set(TEARTAG_TEXT, text);
        data.set(TEARTAG_TEARS, tears);
        data.set(TEARTAG_ELIMINATED, eliminated);
        data.set(TEARTAG_ENABLED, enabled);
    }
}
