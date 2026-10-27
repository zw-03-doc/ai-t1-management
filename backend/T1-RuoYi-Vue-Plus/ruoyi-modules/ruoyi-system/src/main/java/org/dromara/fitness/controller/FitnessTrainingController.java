package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.service.impl.FitnessTrainingService;
import org.dromara.fitness.service.impl.FitnessTrainingMediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Shared by the member mini program, coach browser/mini program and administrator. */
@SaCheckLogin
@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness/training")
public class FitnessTrainingController {
    private final FitnessTrainingService service;
    private final FitnessTrainingMediaService mediaService;

    @GetMapping("/templates")
    public R<List<Plan>> templates() { return R.ok(service.templates()); }

    @GetMapping("/plans")
    public R<List<Plan>> plans(@RequestParam(required = false) Long memberId) { return R.ok(service.plans(memberId)); }

    @GetMapping("/plans/{planId}")
    public R<Plan> detail(@PathVariable Long planId) { return R.ok(service.detail(planId)); }

    @RepeatSubmit
    @PostMapping("/plans")
    public R<Plan> create(@RequestBody PlanInput input) { return R.ok(service.create(input)); }

    @RepeatSubmit
    @PutMapping("/plans/{planId}")
    public R<Plan> update(@PathVariable Long planId, @RequestBody PlanInput input) { return R.ok(service.update(planId, input)); }

    @RepeatSubmit
    @PostMapping("/plans/{planId}/start")
    public R<List<Workout>> start(@PathVariable Long planId, @RequestBody StartInput input) { return R.ok(service.start(planId, input)); }

    @RepeatSubmit
    @PostMapping("/plans/{planId}/shift")
    public R<List<Workout>> shift(@PathVariable Long planId, @RequestBody ShiftInput input) { return R.ok(service.shift(planId, input)); }

    @GetMapping("/plans/{planId}/workouts")
    public R<List<Workout>> workouts(@PathVariable Long planId) { return R.ok(service.workouts(planId)); }

    @RepeatSubmit
    @PutMapping("/workouts/{workoutId}/log")
    public R<Workout> log(@PathVariable Long workoutId, @RequestBody LogInput input) { return R.ok(service.saveLog(workoutId, input)); }

    @GetMapping("/plans/{planId}/stats")
    public R<Stats> stats(@PathVariable Long planId) { return R.ok(service.stats(planId)); }

    @GetMapping("/audit")
    public R<List<Audit>> audit(@RequestParam(required = false) Long planId) { return R.ok(service.audit(planId)); }

    @PostMapping("/media")
    public R<Media> upload(@RequestParam Long planId, @RequestPart("file") MultipartFile file) {
        return R.ok(mediaService.upload(planId, file));
    }

    @GetMapping("/media/{assetId}")
    public ResponseEntity<Resource> media(@PathVariable String assetId, HttpServletResponse response) {
        ResponseEntity<Resource> result = mediaService.read(assetId);
        // The authentication interceptor initializes the servlet response as JSON.
        // Override it only after file authorization, including on the embedded servlet server.
        response.setContentType(result.getHeaders().getContentType().toString());
        return result;
    }
}
