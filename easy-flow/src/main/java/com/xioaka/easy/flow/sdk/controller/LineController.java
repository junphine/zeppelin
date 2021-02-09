package com.xioaka.easy.flow.sdk.controller;

import com.xioaka.easy.flow.sdk.entity.LineEntity;
import com.xioaka.easy.flow.sdk.service.ILineService;
import com.xioaka.easy.flow.sdk.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liuchengbiao
 * @date 2020-05-23 20:34
 */
@RestController
@RequestMapping("/line")
public class LineController extends SuperController<LineEntity> {

    @Autowired
    private ILineService lineService;

    /**
     * 新增
     *
     * @param lineEntity
     * @return
     */
    @PostMapping
    public R save(@RequestBody LineEntity lineEntity) {
        lineService.save(lineEntity);
        return success();
    }

    /**
     * 删除
     *
     * @param from
     * @param to
     * @return
     */
    @DeleteMapping("/{from}/{to}")
    public R delete(@PathVariable("from") String from, @PathVariable("to") String to) {
        lineService.delete(from, to);
        return success();
    }

    /**
     * 更改条件
     *
     * @param lineEntity
     * @return
     */
    @PutMapping("/changeLabel")
    public R changeLabel(@RequestBody LineEntity lineEntity) {
        lineService.changeLabel(lineEntity.getFrom(), lineEntity.getTo(), lineEntity.getLabel());
        return success();
    }

}
