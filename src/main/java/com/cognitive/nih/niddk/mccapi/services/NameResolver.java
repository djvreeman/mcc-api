package com.cognitive.nih.niddk.mccapi.services;

import com.cognitive.nih.niddk.mccapi.data.Context;
import com.cognitive.nih.niddk.mccapi.util.Helper;
import lombok.NonNull;
import org.hl7.fhir.r4.model.*;

import java.util.List;

@SuppressWarnings("unused")
public class NameResolver {
    public static String getName(@NonNull Condition c, Context ctx) {
        return Helper.getConceptDisplayString(c.getCode());
    }

    public static String getName(@NonNull Practitioner p, Context ctx) {
        return Helper.getBestName(p.getName());
    }

    public static String getName(@NonNull Patient p, Context ctx) {
        return Helper.getBestName(p.getName());
    }

    public static String getName(@NonNull Medication x, Context ctx) {
        return x.getCode().getText();
    }

    public static String getName(@NonNull Organization o, Context ctx) {
        return o.getName();
    }

    public static String getName(@NonNull RelatedPerson o, Context ctx) {
        return Helper.getBestName(o.getName());
    }

    public static String getName(@NonNull PractitionerRole o, Context ctx) {
        return o.getCodeFirstRep().getText();
    }

    public static String getName(@NonNull NutritionOrder o, Context ctx) {
        if (o.hasIdentifier()) {
            return "NutritionOrder " + o.getIdentifierFirstRep().getValue();
        } else {
            return "NutritionOrder, " + o.getIdElement().toString();
        }
    }

    public static String getName(@NonNull ServiceRequest s, Context ctx) {
        StringBuilder out = new StringBuilder();
        out.append(s.getIntent().getDisplay());
        if (s.hasCode()) {
            out.append(" for ");
            out.append(Helper.getConceptDisplayString(s.getCode()));
        } else {
            out.append(" ");
            if (s.hasIdentifier()) {
                return "RiskAssessment " + s.getIdentifierFirstRep().getValue();
            } else {
                return "RiskAssessment, " + s.getIdElement().toString();
            }
        }
        return out.toString();
    }

    public static String getName(@NonNull RiskAssessment r, Context ctx) {
        if (r.hasCode()) {
            return Helper.getConceptDisplayString(r.getCode());
        }
        if (r.hasCondition()) {
            return getReferenceName(r.getCondition(), ctx);
        }
        if (r.hasIdentifier()) {
            return "RiskAssessment " + r.getIdentifierFirstRep().getValue();
        } else {
            return "RiskAssessment, " + r.getIdElement().toString();
        }
    }

    public static String getName(@NonNull MedicationStatement in, Context ctx) {
        String out = "Unresolvable Medication Statement";
        if (in.hasMedication()) {
            if (in.hasMedicationCodeableConcept()) {
                out = in.getMedicationCodeableConcept().getText();
            }
            if (in.hasMedicationReference()) {
                out = getReferenceName(in.getMedicationReference(), ctx);
            }
        }
        return out;
    }

    public static String getReferenceName(Reference ref, Context ctx) {

        if (ref.hasDisplay()) {
            return ref.getDisplay();
        }
        String out = null;
        String type = Helper.getReferenceType(ref);
        if (type != null) {
            switch (type) {
                case "Condition": {
                    Condition x = ReferenceResolver.findCondition(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "Medication": {
                    Medication x = ReferenceResolver.findMedication(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "MedicationStatement": {
                    MedicationStatement x = ReferenceResolver.findMedicationStatement(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "NutritionOrder": {
                    NutritionOrder x = ReferenceResolver.findNutritionOrder(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "Organization": {
                    Organization x = ReferenceResolver.findOrganization(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "Patient": {
                    Patient x = ReferenceResolver.findPatient(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "Practitioner": {
                    Practitioner x = ReferenceResolver.findPractitioner(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "PractitionerRole": {
                    PractitionerRole x = ReferenceResolver.findPractitionerRole(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "RiskAssessment": {
                    RiskAssessment x = ReferenceResolver.findRiskAssessment(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                case "ServiceRequest": {
                    ServiceRequest x = ReferenceResolver.findServiceRequest(ref, ctx);
                    out = x != null ? getName(x, ctx) : "Missing " + type;
                    break;
                }
                default: {
                    out = type + " Not Yet Supported";
                    break;
                }
            }
        }

        return out;
    }

    public static String getReferenceNames(List<Reference> refs, Context ctx) {
        StringBuilder out = new StringBuilder();
        for (Reference ref : refs) {
            Helper.addStringToBufferWithSep(out, getReferenceName(ref, ctx), ",");
        }
        return out.toString();
    }
}
