package com.cognitive.nih.niddk.mccapi.controllers;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.cognitive.nih.niddk.mccapi.data.Contact;
import com.cognitive.nih.niddk.mccapi.data.Context;
import com.cognitive.nih.niddk.mccapi.managers.ContextManager;
import com.cognitive.nih.niddk.mccapi.mappers.CareTeamMapper;
import com.cognitive.nih.niddk.mccapi.mappers.PatientMapper;
import com.cognitive.nih.niddk.mccapi.mappers.PractitionerMapper;
import com.cognitive.nih.niddk.mccapi.services.FHIRServices;
import com.cognitive.nih.niddk.mccapi.util.Helper;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class ContactController {
    @GetMapping("/contact")
    public Contact[] getContacts(@RequestParam(required = true, name = "subject") String subjectId, @RequestParam(required = false, name = "careplan") String carePlanId, @RequestHeader Map<String, String> headers) {
        ArrayList<Contact> out = new ArrayList<>();
        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);
        Context ctx = ContextManager.getManager().findContextForSubject(subjectId, headers);
        ctx.setClient(client);

        Contact contact;
        Patient fp = client.read().resource(Patient.class).withId(subjectId).execute();

        contact = PatientMapper.fhir2Contact(fp, ctx);
        out.add(contact);

        //Find Emergency Contacts
        List<Patient.ContactComponent> contacts = PatientMapper.getActiveContactOfType(fp, "C");
        if (contacts.size() > 0) {
            for (Patient.ContactComponent pc : contacts) {
                contact = PatientMapper.fhir2Contact(pc, ctx);
                contact.setRole(Contact.ROLE_EMERGENCY);
                out.add(contact);
            }
        }
        //Look for primary care
        if (fp.hasGeneralPractitioner()) {
            List<Reference> gp = fp.getGeneralPractitioner();
            for (Reference ref : gp) {
                String type = ref.getType();
                if (Helper.isReferenceOfType(ref, "Practitioner")) {
                    Practitioner p = client.fetchResourceFromUrl(Practitioner.class, ref.getReference());
                    Contact pc = PractitionerMapper.fhir2Contact(p, ctx);
                    pc.setRole(Contact.ROLE_PRIMARY_CARE);
                    out.add(pc);
                }
                //TODO: Handle other types
            }

        }

        //If a care plan is presented then we process it to find the care teams
        if (carePlanId != null) {
            // Fetch Careplan

            try {
                CarePlan fc = client.read().resource(CarePlan.class).withId(carePlanId).execute();

                if (fc != null) {
                    List<Reference> teams = fc.getCareTeam();

                    //TODO: In the future maybe remove duplicate when more then one team is present
                    for (Reference ref : teams) {
                        if (Helper.isReferenceOfType(ref, "CareTeam")) {
                            CareTeam t = client.fetchResourceFromUrl(CareTeam.class, ref.getReference());
                            if (t != null) {
                                out.addAll(CareTeamMapper.fhir2Contacts(t, ctx));
                            }
                        }
                    }
                }
            } catch (Exception exp) {
                log.warn("Error tyring to fetch careplan " + carePlanId + " , " + exp.getMessage());
            }
        }



    //Finally we look for Insurance
    contacts =PatientMapper.getActiveContactOfType(fp,"I");
        if(contacts.size()>0)

    {
        for (Patient.ContactComponent pc : contacts) {
            contact = PatientMapper.fhir2Contact(pc, ctx);
            contact.setRole(Contact.ROLE_INSURANCE);
            out.add(contact);
        }
    }


    Contact[] outA = new Contact[out.size()];
    outA =out.toArray(outA);
        return outA;
}
}
