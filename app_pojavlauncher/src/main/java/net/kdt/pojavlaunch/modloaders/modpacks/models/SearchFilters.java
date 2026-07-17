package net.kdt.pojavlaunch.modloaders.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public static final int TYPE_MODPACK = 0;
    public static final int TYPE_MOD = 1;
    public static final int TYPE_RESOURCE_PACK = 2;
    public static final int TYPE_SHADER = 3;

    public int contentType = TYPE_MODPACK;
    public String name;
    @Nullable public String mcVersion;

    /** @deprecated Use {@link #contentType} instead */
    @Deprecated
    public boolean isModpack;

    public void applyContentType() {
        isModpack = (contentType == TYPE_MODPACK);
    }
}
