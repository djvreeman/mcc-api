package com.cognitive.nih.niddk.mccapi.controllers;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.cognitive.nih.niddk.mccapi.data.Context;
import com.cognitive.nih.niddk.mccapi.data.MccPatient;
import com.cognitive.nih.niddk.mccapi.managers.ContextManager;
import com.cognitive.nih.niddk.mccapi.managers.QueryManager;
import com.cognitive.nih.niddk.mccapi.mappers.PatientMapper;
import com.cognitive.nih.niddk.mccapi.services.FHIRServices;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class PatientController {

    private final QueryManager queryManager;

    public PatientController(QueryManager queryManager)
    {
        this.queryManager = queryManager;
    }

    @GetMapping("/patient")
    public MccPatient[] getPatients(@RequestParam(required=true) String name, @RequestHeader Map<String, String> headers, WebRequest webRequest)
    {
        ArrayList<MccPatient> out = new ArrayList<>();
        //Create a dummy patient for the moment
        MccPatient p;
        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);

        Map<String, String> values = new HashMap<>();
        String callUrl = queryManager.setupQuery("Patient.QueryByName", values, webRequest);

        if (callUrl != null) {
            Bundle results = client.fetchResourceFromUrl(Bundle.class, callUrl);

            // Bundle results = client.search().forResource(Patient.class).where(Patient.NAME.matches().value(name)).returnBundle(Bundle.class).execute();

            for (Bundle.BundleEntryComponent e : results.getEntry()) {
                if (e.getResource().fhirType().compareTo("Patient") == 0) {
                    Patient bp = (Patient) e.getResource();
                    Context ctx = ContextManager.getManager().findContextForSubject(bp.hasIdentifier() ? bp.getIdentifierFirstRep().getValue() : "Unknown", headers);
                    ctx.setClient(client);
                    p = PatientMapper.fhir2local(bp, ctx);
                    out.add(p);
                }
            }
        }

        MccPatient[] outA = new MccPatient[out.size()];
        outA = out.toArray(outA);
        return outA;

    }

    @GetMapping("/patient/{id}")
    public MccPatient getPatient(@PathVariable(value="id") String id, @RequestHeader Map<String, String> headers, WebRequest webRequest)
    {
        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);
        MccPatient p;


        Map<String, String> values = new HashMap<>();
        values.put("id",id);
        String callUrl = queryManager.setupQuery("Patient.Lookup", values, webRequest);

        if (callUrl != null) {
            Patient fp = client.fetchResourceFromUrl(Patient.class, callUrl);

            //Patient fp = client.read().resource(Patient.class).withId(id).execute();
            Context ctx = ContextManager.getManager().findContextForSubject(id, headers);
            ctx.setClient(client);
            p = PatientMapper.fhir2local(fp, ctx);
        }
        else
        {
            //Lookup suppressed
            //TODO:  Use the stock empty patient
            p = new MccPatient();
        }
        return p;
    }

}
