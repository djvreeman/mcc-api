package com.cognitive.nih.niddk.mccapi.controllers;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.cognitive.nih.niddk.mccapi.data.Context;
import com.cognitive.nih.niddk.mccapi.data.GoalLists;
import com.cognitive.nih.niddk.mccapi.data.GoalSummary;
import com.cognitive.nih.niddk.mccapi.data.MccGoal;
import com.cognitive.nih.niddk.mccapi.managers.ContextManager;
import com.cognitive.nih.niddk.mccapi.managers.QueryManager;
import com.cognitive.nih.niddk.mccapi.mappers.GoalMapper;
import com.cognitive.nih.niddk.mccapi.services.FHIRServices;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class GoalController {
    private final QueryManager queryManager;

    public GoalController(QueryManager queryManager) {
        this.queryManager = queryManager;
    }

    @GetMapping("/goalsummary")
    public GoalLists getGoalSummary(@RequestParam(required = true, name = "subject") String subjectId, @RequestParam(required = false, name = "careplan") String careplanId, @RequestHeader Map<String, String> headers, WebRequest webRequest) {
        GoalLists out = new GoalLists();

        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);
        Map<String,String> values = new HashMap<>();
        String callUrl=queryManager.setupQuery("Goal.Query",values,webRequest);

        if (callUrl != null) {
            Bundle results = client.fetchResourceFromUrl(Bundle.class, callUrl);
            // Bundle results = client.search().forResource(Goal.class).where(Goal.SUBJECT.hasId(subjectId))
            //         .returnBundle(Bundle.class).execute();
            Context ctx = ContextManager.getManager().findContextForSubject(subjectId, headers);
            ctx.setClient(client);
            for (Bundle.BundleEntryComponent e : results.getEntry()) {
                if (e.getResource().fhirType().compareTo("Goal")==0){
                    Goal g = (Goal) e.getResource();
                    GoalSummary gs = GoalMapper.summaryfhir2local(g, ctx);
                    out.addSummary(gs);
                }
            }
        }
        return out;
    }

    @GetMapping("/goal")
    public MccGoal[] getGoals(@RequestParam(required = true, name = "subject") String subjectId, @RequestHeader Map<String, String> headers, WebRequest webRequest) {
        ArrayList<MccGoal> out = new ArrayList<>();
        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);
        Map<String,String> values = new HashMap<>();
        String callUrl=queryManager.setupQuery("Goal.Query",values,webRequest);

        if (callUrl != null) {
            Bundle results = client.fetchResourceFromUrl(Bundle.class, callUrl);
            //Bundle results = client.search().forResource(Goal.class).where(Goal.SUBJECT.hasId(subjectId))
            //        .returnBundle(Bundle.class).execute();
            Context ctx = ContextManager.getManager().findContextForSubject(subjectId, headers);
            ctx.setClient(client);
            for (Bundle.BundleEntryComponent e : results.getEntry()) {
                if (e.getResource().fhirType().compareTo("Goal") == 0) {
                    Goal g = (Goal) e.getResource();
                    out.add(GoalMapper.fhir2local(g, ctx));
                }
            }
        }
        MccGoal[] outA = new MccGoal[out.size()];
        outA = out.toArray(outA);
        return outA;
    }

    @GetMapping("/goal/{id}")
    public MccGoal getGoal(@PathVariable(value = "id") String id, @RequestHeader Map<String, String> headers, WebRequest webRequest) {
        MccGoal g;
        FHIRServices fhirSrv = FHIRServices.getFhirServices();
        IGenericClient client = fhirSrv.getClient(headers);
        Map<String,String> values = new HashMap<>();
        values.put("id",id);
        String callUrl=queryManager.setupQuery("Goal.Lookup",values,webRequest);

        if (callUrl != null) {
            Goal fg = client.fetchResourceFromUrl(Goal.class, callUrl);

            //Goal fg = client.read().resource(Goal.class).withId(id).execute();
            String subjectId = fg.getSubject().getId();
            Context ctx = ContextManager.getManager().findContextForSubject(subjectId, headers);
            g = GoalMapper.fhir2local(fg, ctx);
        }
        else
        {
            //TODO: Return unavailable goal
            g = new MccGoal();
            log.warn("Goal Lookup disabled, Goal "+id+" Not found");
        }
        return g;
    }


}
