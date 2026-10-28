package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.fml.ModList;
import net.parkabird.changedsynergy.ChangedSynergyMod;

/** Uses the installed mod's meshes without requiring or redistributing it. */
public final class PartialSkinLayersCompat {
    private static final String[] PARTS = {"Hat", "Jacket", "LeftSleeve", "RightSleeve", "LeftPants", "RightPants"};
    private static final String[] MESHES = {"getHeadMesh", "getTorsoMesh", "getLeftArmMesh", "getRightArmMesh", "getLeftLegMesh", "getRightLegMesh"};
    private static final String[] OPTIONS = {"enableHat", "enableJacket", "enableLeftSleeve", "enableRightSleeve", "enableLeftPants", "enableRightPants"};
    private static final String[] OFFSETS = {"HEAD", "BODY", "LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG"};
    private static final Map<AbstractClientPlayer, Map<Boolean, CroppedSkin>> CROPPED = new WeakHashMap<>();
    private static Api api;
    private static boolean unavailable;

    private PartialSkinLayersCompat() {}

    public static void apply(Object model, AbstractClientPlayer player) {
        if (unavailable || !ModList.get().isLoaded("skinlayers3d")) return;
        try {
            if (api == null) api = new Api();
            ModelPart[] parts = new ModelPart[PARTS.length];
            boolean partial = !model.getClass().getSimpleName().equals("LatexHumanModel");
            for (int i = 0; i < parts.length; i++) {
                var field = model.getClass().getDeclaredField(PARTS[i]);field.setAccessible(true);
                ModelPart part = (ModelPart)field.get(model);
                if (partial && i >= 4) {
                    String child = i == 4 ? "LeftThighLayer_r1" : "RightThighLayer_r1";
                    if (!part.hasChild(child)) continue;
                    part = part.getChild(child);
                }
                parts[i] = part;
                api.inject.invoke(part, null, null);
            }
            if (player == null || player.isInvisible()) return;
            Object config = api.config.get(null);
            int distance = config.getClass().getField("renderDistanceLOD").getInt(config);
            if (player.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > (double)distance * distance) return;
            boolean slim = player.getModelName().equals("slim");
            if (!(boolean)api.setup.invoke(null, player, player, slim)) return;
            Object[] cropped = partial ? api.cropped(player, slim) : null;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] == null || !config.getClass().getField(OPTIONS[i]).getBoolean(config)) continue;
                if (i == 0 && ((java.util.Set<?>)api.hiddenHats.get(null)).contains(player.getItemBySlot(EquipmentSlot.HEAD).getItem())) continue;
                Object mesh = partial && i >= 2 ? cropped == null ? null : cropped[i - 2] : api.getters[i].invoke(player);
                if (mesh != null) api.inject.invoke(parts[i], mesh, api.offsets.getField(OFFSETS[i] + (slim && (i == 2 || i == 3) ? "_SLIM" : "")).get(null));
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            ChangedSynergyMod.LOGGER.warn("3D skin layer compatibility unavailable; retaining native skin rendering", exception);
        }
    }

    private record CroppedSkin(ResourceLocation skin, Object[] meshes) {}

    private static final class Api {
        final Class<?> offsets = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
        final Class<?> mesh = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
        final Class<?> settings = Class.forName("dev.tr7zw.skinlayers.accessor.PlayerSettings");
        final Class<?> util = Class.forName("dev.tr7zw.skinlayers.SkinUtil");
        final Class<?> base = Class.forName("dev.tr7zw.skinlayers.SkinLayersModBase");
        final java.lang.reflect.Field config = base.getField("config"), hiddenHats = base.getField("hideHeadLayers");
        final Method inject = Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector").getMethod("setInjectedMesh", mesh, offsets);
        final Method setup = util.getMethod("setup3dLayers", AbstractClientPlayer.class, settings, boolean.class);
        final Method texture = util.getMethod("getTexture", ResourceLocation.class, Class.forName("dev.tr7zw.skinlayers.accessor.SkullSettings"));
        final Object helper = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI").getMethod("getMeshHelper").invoke(null);
        final Method create = Class.forName("dev.tr7zw.skinlayers.api.MeshHelper").getMethod("create3DMesh", NativeImage.class,
                int.class, int.class, int.class, int.class, int.class, boolean.class, float.class);
        final Method[] getters = new Method[MESHES.length];

        Api() throws ReflectiveOperationException {
            Class<?> data = Class.forName("dev.tr7zw.skinlayers.api.PlayerData");
            for (int i = 0; i < getters.length; i++) getters[i] = data.getMethod(MESHES[i]);
        }

        Object[] cropped(AbstractClientPlayer player, boolean slim) throws ReflectiveOperationException {
            ResourceLocation skin = player.getSkinTextureLocation();
            var cache = CROPPED.computeIfAbsent(player, ignored -> new java.util.HashMap<>());
            var previous = cache.get(slim);
            if (previous != null && previous.skin.equals(skin)) return previous.meshes;
            NativeImage source = (NativeImage)texture.invoke(null, skin, null);
            if (source == null || source.getWidth() != 64 || source.getHeight() != 64) return null;
            Object[] meshes = new Object[4];
            int[][] dimensions = {{slim ? 3 : 4,9,48,48}, {slim ? 3 : 4,9,40,32}, {4,7,0,48}, {4,7,0,32}};
            for (int i = 0; i < meshes.length; i++) {
                int[] size = dimensions[i];
                try (NativeImage image = new NativeImage(64,64,true)) {
                    image.copyFrom(source);
                    // The native partial models omit the lower cap at the latex boundary.
                    for (int x = size[2]+4+size[0]; x < size[2]+4+2*size[0]; x++)
                        for (int y = size[3]; y < size[3]+4; y++) image.setPixelRGBA(x,y,0);
                    meshes[i] = create.invoke(helper,image,size[0],size[1],4,size[2],size[3],true,i < 2 ? -2F : 0F);
                }
            }
            cache.put(slim,new CroppedSkin(skin,meshes));
            return meshes;
        }
    }
}
