package io.github.gaming32.worldhost.common.upnp;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

// Descriptions and codes come from the UPnP spec at http://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v2-Service.pdf
public final class UPnPErrors {
    public static final Int2ObjectMap<AddPortMappingErrors> ADD_PORT_MAPPING_ERROR_CODES = new Int2ObjectLinkedOpenHashMap<>();

    static {
        for (final AddPortMappingErrors error : AddPortMappingErrors.values()) {
            ADD_PORT_MAPPING_ERROR_CODES.put(error.code, error);
        }
        ADD_PORT_MAPPING_ERROR_CODES.defaultReturnValue(AddPortMappingErrors.TBD);
    }

    private UPnPErrors() {
    }

    public enum AddPortMappingErrors {
        TBD(-1),
        Action_not_authorized(606),
        WildCardNotPermittedInSrcIP(715),
        WildCardNotPermittedInExtPort(716),
        ConflictInMappingEntry(718),
        SamePortValuesRequired(724),
        OnlyPermanentLeasesSupported(725),
        RemoteHostOnlySupportsWildcard(726),
        ExternalPortOnlySupportsWildcard(727),
        NoPortMapsAvailable(728),
        ConflictWithOtherMechanisms(729),
        WildCardNotPermittedInIntPort(732)
        ;

        public final int code;

        AddPortMappingErrors(int code) {
            this.code = code;
        }
    }
}
