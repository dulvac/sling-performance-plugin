package hudson.plugins.slingperformanceplugin;

import hudson.model.AbstractBuild;

public final class CustomNumberOnlyBuildLabel implements Comparable<CustomNumberOnlyBuildLabel> {
    public final AbstractBuild build;
    private String customSuffix = "";

    public String getCustomSuffix() {
        return customSuffix;
    }

    public void setCustomSuffix(String customSuffix) {
        this.customSuffix = customSuffix;
    }

    public CustomNumberOnlyBuildLabel(AbstractBuild build) {
        this.build = build;
    }

    public int compareTo(CustomNumberOnlyBuildLabel that) {
        int buildDiff = this.build.number - that.build.number;
        if (buildDiff == 0)
            return this.getCustomSuffix().compareTo(that.getCustomSuffix());
        return buildDiff;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof CustomNumberOnlyBuildLabel))    return false;
        CustomNumberOnlyBuildLabel that = (CustomNumberOnlyBuildLabel) o;
        return build.number == that.build.number && this.getCustomSuffix() == that.getCustomSuffix();
    }

    @Override
    public int hashCode() {
        return build.number;
    }

    @Override
    public String toString() {
        return Integer.toString(build.number) + this.getCustomSuffix();
    }
}
