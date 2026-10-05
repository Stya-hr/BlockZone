package dev.stya.blockzone.client.equipment;

import com.google.gson.JsonParser;
import dev.stya.blockzone.equipment.TacticalArmorItem;
import dev.stya.blockzone.equipment.PlateCapacity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class TacticalArmorExtension implements IClientItemExtensions {
    private static final Map<String, HumanoidModel<?>> MODELS = new HashMap<>();
    public static void clearCache() { MODELS.clear(); }

    @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
            EquipmentSlot slot, HumanoidModel<?> original) {
        String name = ((TacticalArmorItem) stack.getItem()).model();
        if (name.equals("carrier") && PlateCapacity.expanded(stack)) name = "advanced_carrier";
        return MODELS.computeIfAbsent(name, TacticalArmorExtension::load);
    }

    private static HumanoidModel<?> load(String name) {
        try (var reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(
                ResourceLocation.fromNamespaceAndPath("blockzone", "models/equipment/" + name + ".json")).open(), StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var mesh = new MeshDefinition();
            var root = mesh.getRoot();
            for (String bone : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                var cubes = CubeListBuilder.create();
                for (var element : json.getAsJsonArray("cubes")) {
                    var cube = element.getAsJsonObject();
                    if (!cube.get("bone").getAsString().equals(bone)) continue;
                    var from = cube.getAsJsonArray("from");
                    var size = cube.getAsJsonArray("size");
                    cubes.texOffs(cube.get("u").getAsInt(), cube.get("v").getAsInt()).addBox(
                            from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                            size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());
                }
                var pose = switch (bone) {
                    case "right_arm" -> PartPose.offset(-5, 2, 0); case "left_arm" -> PartPose.offset(5, 2, 0);
                    case "right_leg" -> PartPose.offset(-1.9F, 12, 0); case "left_leg" -> PartPose.offset(1.9F, 12, 0);
                    default -> PartPose.ZERO;
                };
                root.addOrReplaceChild(bone, cubes, pose);
            }
            return new HumanoidModel<>(LayerDefinition.create(mesh, 128, 128).bakeRoot());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load tactical armor model " + name, exception);
        }
    }
}
