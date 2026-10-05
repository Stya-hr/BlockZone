package dev.stya.blockzone.equipment;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Carrier protection comes from plates, so it adds no second vanilla damage reduction. */
public final class TacticalArmorItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        public int getDurabilityForType(Type type) { return 0; }
        public int getDefenseForType(Type type) { return 0; }
        public int getEnchantmentValue() { return 0; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        public String getName() { return "blockzone:tactical"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    };
    private final String model;
    private final int plateSlots;

    public TacticalArmorItem(Type type, String model, int plateSlots) {
        super(MATERIAL, type, new Item.Properties().stacksTo(1));
        this.model = model;
        this.plateSlots = plateSlots;
    }
    public int plateSlots() { return plateSlots; }
    public String model() { return model; }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new dev.stya.blockzone.client.equipment.TacticalArmorExtension());
    }

    @Override public String getArmorTexture(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, String type) {
        return "blockzone:textures/models/armor/tactical.png";
    }
}
