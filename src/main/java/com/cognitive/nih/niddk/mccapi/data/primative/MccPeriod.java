package com.cognitive.nih.niddk.mccapi.data.primative;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include. NON_NULL)
public @Data
class MccPeriod {

    public static final String fhirType = "Period";

    private MccDate start;
    private MccDate end;
}
