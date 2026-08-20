package com.huawei.fin.ai.material.followup.facade;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huawei.fin.ai.material.followup.service.FollowUpService;
import com.huawei.fin.ai.material.followup.vo.FollowUpVO;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public List<FollowUpVO> list() {
        return followUpService.list();
    }

    @PostMapping
    public FollowUpVO create(@RequestBody FollowUpVO vo) {
        return followUpService.create(vo);
    }

    @PutMapping("/{id}/status")
    public FollowUpVO updateStatus(@PathVariable Long id, @RequestParam String status) {
        return followUpService.updateStatus(id, status);
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        int created = followUpService.syncFromAnalysis();
        return Map.of("created", created);
    }
}
