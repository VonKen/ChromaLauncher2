package net.kdt.pojavlaunch.modloaders.modpacks.models;

public class ModDependency {
    public static final int TYPE_REQUIRED = 0;
    public static final int TYPE_OPTIONAL = 1;
    public static final int TYPE_INCOMPATIBLE = 2;
    public static final int TYPE_EMBEDDED = 3;

    public final String projectId;
    public final String versionId;
    public final String fileName;
    public final int type;

    public ModDependency(String projectId, String versionId, String fileName, int type) {
        this.projectId = projectId;
        this.versionId = versionId;
        this.fileName = fileName;
        this.type = type;
    }
}
