package net.danh.storage.Utils;

import com.cryptomorin.xseries.XMaterial;
import net.danh.storage.NMS.NMSAssistant;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

public final class MaterialUtils {

    private static final NMSAssistant NMS = new NMSAssistant();

    private MaterialUtils() {
    }

    public static Material matchMaterial(String value) {
        String materialName = getMaterialName(value);
        if (materialName.isEmpty()) {
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material != null) {
            return material;
        }

        if (NMS.isVersionGreaterThanOrEqualTo(13)) {
            return null;
        }

        try {
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
            return xMaterial.map(XMaterial::parseMaterial).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static ItemStack createItem(String value) {
        Material material = matchMaterial(value);
        if (material == null || material == Material.AIR) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        if (NMS.isVersionLessThanOrEqualTo(12) && value != null && value.contains(";")) {
            item.setDurability((short) Number.getInteger(value.split(";", 2)[1]));
        }
        return item;
    }

    private static String getMaterialName(String value) {
        if (value == null) {
            return "";
        }
        String material = value.trim();
        if (material.contains(";")) {
            material = material.split(";", 2)[0];
        }
        if (material.contains(":")) {
            material = material.substring(material.lastIndexOf(':') + 1);
        }
        return material.toUpperCase(Locale.ENGLISH);
    }
}
