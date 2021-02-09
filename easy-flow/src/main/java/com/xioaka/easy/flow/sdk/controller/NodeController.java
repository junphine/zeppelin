package com.xioaka.easy.flow.sdk.controller;

import com.xioaka.easy.flow.sdk.entity.NodeEntity;
import com.xioaka.easy.flow.sdk.service.INodeService;
import com.xioaka.easy.flow.sdk.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liuchengbiao
 * @date 2020-05-23 20:34
 */
@RestController
@RequestMapping("/node")
public class NodeController extends SuperController<NodeEntity> {

    @Autowired
    private INodeService nodeService;

    @GetMapping("/{id}")
    public R save(@PathVariable("id") String id) {
        NodeEntity nodeEntity = nodeService.getById(id);
        return success(nodeEntity);
    }

    /**
     * 新增
     *
     * @param node
     * @return
     */
    @PostMapping
    public R save(@RequestBody NodeEntity node) {
        nodeService.save(node);
        return success();
    }

    /**
     * 更新
     *
     * @param node
     * @return
     */
    @PutMapping
    public R update(@RequestBody NodeEntity node) {
        nodeService.updateById(node);
        return success();
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public R delete(@PathVariable("id") String id) {
        nodeService.delete(id);
        return success();
    }

    /**
     * 更改位置坐标
     *
     * @param node
     * @return
     */
    @PutMapping("/changeSite")
    public R changeSite(@RequestBody NodeEntity node) {
        nodeService.changeSite(node.getId(), node.getLeft(), node.getTop());
        return success();
    }
}
