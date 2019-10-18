package com.ms.silverking.cloud.dht.common;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.ms.silverking.cloud.dht.NamespaceOptions;
import com.ms.silverking.log.Log;
import com.ms.silverking.text.ObjectDefParser2;

/**
 * Internal metadata for a Namespace.  
 */
public class NamespaceProperties {
    private final NamespaceOptions  options;
    private final String            name;   // optional field
    private final String            parent;
    private final long              minVersion;
    private final long              creationTime; // optional field

    private static final NamespaceProperties    templateProperties = new NamespaceProperties();
    private static final Set<String> optionalFields = ImmutableSet.of("name", "creationTime");
    // For backward compatibility only (we drop "creationTime" and "name", since its not in the old data)
    private static final Set<String> legacyExclusionFields = ImmutableSet.of("creationTime", "name");

    public static final long    noCreationTime = 0;
    public static final String  noName = "__namespace_name_is_omitted_in_this_version_of_codes__";

    static {
        ObjectDefParser2.addParserWithOptionalFields(templateProperties, optionalFields);
    }
    
    public static void initParser() { 
    }
    
    
    public NamespaceProperties(NamespaceOptions options, String name, String parent, long minVersion, long creationTime) {
        assert options != null;
        this.options = options;
        this.name = name;
        this.parent = parent;
        this.minVersion = minVersion;
        this.creationTime = creationTime;
    }

    public NamespaceProperties(NamespaceOptions options, String name, String parent, long minVersion) {
        this(options, name, parent, minVersion, noCreationTime);
    }

    public NamespaceProperties(NamespaceOptions options, String parent, long minVersion) {
        this(options, noName, parent, minVersion);
    }
    
    public NamespaceProperties(NamespaceOptions options) {
        this(options, null, Long.MIN_VALUE);
    }
    
    private NamespaceProperties() {
        this(DHTConstants.defaultNamespaceOptions);
    }
    
    public NamespaceOptions getOptions() {
        return options;
    }
    
    public String getParent() {
        return parent;
    }
    
    public long getMinVersion() {
        return minVersion;
    }

    public String getName() {
        return name;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean hasCreationTime() {
        return creationTime != noCreationTime;
    }

    public boolean hasName() {
        return !name.equals(noName);
    }

    public NamespaceProperties creationTime(long creationTime) {
        return new NamespaceProperties(options, name, parent, minVersion, creationTime);
    }
    
    public NamespaceProperties options(NamespaceOptions options) {
        return new NamespaceProperties(options, name, parent, minVersion, creationTime);
    }

    public NamespaceProperties name(String name) {
        return new NamespaceProperties(options, name, parent, minVersion, creationTime);
    }

    
    @Override
    public int hashCode() {
        return options.hashCode()
                ^ (parent == null ? 0 : parent.hashCode())
                ^ (hasName() ? name.hashCode() : 0)
                ^ (hasCreationTime() ? Long.hashCode(creationTime) : 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof NamespaceProperties) {
            NamespaceProperties oProperties;

            oProperties = (NamespaceProperties)o;
            return this.options.equals(oProperties.options)
                    && Objects.equals(this.parent, oProperties.parent)
                    && Objects.equals(this.name, oProperties.name)
                    && this.creationTime == oProperties.creationTime;
        } else {
            return  false;
        }
    }

    public boolean canBeReplacedBy(NamespaceProperties other) {
        if (this == other) {
            return true;
        }

        return NamespaceUtil.canMutateWith(options, other.options)
                && Objects.equals(this.parent, other.parent)
                && Objects.equals(this.name, other.name);
    }

    public void debugEquals(Object o) {
        NamespaceProperties oProperties;
        
        oProperties = (NamespaceProperties)o;
        this.options.debugEquality(oProperties.options);
    }

    // For backward compatibility only
    public String toLegacySKDef() {
        //  We drop "creationTime" and "name", since its not in the old data
        return ObjectDefParser2.objectToStringWithExclusions(this, legacyExclusionFields);
    }

    // This method shall be entry-point for reflection
    public String toSKDef() {
        return ObjectDefParser2.objectToString(this);
    }

    @Override
    public String toString() {
        return toSKDef();
    }

    /**
     * @param def NamespaceProperties reflection string
     * @return NamespaceProperties object whose creationTime needs to be checked
     */
    public static NamespaceProperties parse(String def) {
        // Caller needs to check creation of parsed result
        return ObjectDefParser2.parse(NamespaceProperties.class, def);
    }

    /**
     * @param def NamespaceProperties reflection string, the NamespaceProperties is assumed to has NO creation time
     * @param creationTime the creation time to assign to this NamespaceProperties
     * @return NamespaceProperties object
     */
    public static NamespaceProperties parse(String def, long creationTime) {
        NamespaceProperties nsProperties = ObjectDefParser2.parse(NamespaceProperties.class, def);
        if (nsProperties.hasCreationTime()) {
            Log.warning("The parsed NamespaceProperties already has creationTime [" + nsProperties.getCreationTime() + "] when trying to parse it with creationTime [" + creationTime + "]");
        }
        return nsProperties.creationTime(creationTime);
    }
}
