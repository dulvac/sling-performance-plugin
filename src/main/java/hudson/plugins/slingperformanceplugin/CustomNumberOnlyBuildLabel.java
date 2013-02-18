package hudson.plugins.slingperformanceplugin;

import hudson.model.AbstractBuild;

public final class CustomNumberOnlyBuildLabel implements Comparable<CustomNumberOnlyBuildLabel> {
    public final AbstractBuild build;

    public CustomNumberOnlyBuildLabel(AbstractBuild build) {
        this.build = build;
    }

    public int compareTo(CustomNumberOnlyBuildLabel that) {
        return this.build.number - that.build.number;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof CustomNumberOnlyBuildLabel))    return false;
        CustomNumberOnlyBuildLabel that = (CustomNumberOnlyBuildLabel) o;
        return build.number == that.build.number;
    }

    @Override
    public int hashCode() {
        return build.number;
    }

    @Override
    public String toString() {
        return Integer.toString(build.number);
    }
}
