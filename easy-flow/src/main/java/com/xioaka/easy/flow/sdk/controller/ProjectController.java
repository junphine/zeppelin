package com.xioaka.easy.flow.sdk.controller;

import com.xioaka.easy.flow.sdk.entity.ProjectEntity;
import com.xioaka.easy.flow.sdk.service.IProjectService;
import com.xioaka.easy.flow.sdk.vo.ProjectVo;
import com.xioaka.easy.flow.sdk.vo.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liuchengbiao
 * @date 2020-05-23 20:34
 */
@RestController
@RequestMapping("/project")
public class ProjectController extends SuperController<ProjectEntity> {

    @Autowired
    private IProjectService projectService;

    @GetMapping("/{id}")
    public R save(@PathVariable("id") String id) {
        ProjectEntity projectEntity = projectService.getById(id);
        return success(projectEntity);
    }

    @GetMapping("/data/{id}")
    public R<ProjectVo> data(@PathVariable("id") String id) {
        ProjectVo projectVo = projectService.queryData(id);
        return new R<ProjectVo>().setCode(0).setData(projectVo);
    }
    
    @GetMapping(value="/template/{id}",produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String template(@PathVariable("id") String projectId) {
    	InputStream in = this.getClass().getResourceAsStream("/data/"+projectId+".js");
    	if(in!=null) {    	
    		InputStreamReader reader;
			try {
				reader = new InputStreamReader(in,"UTF-8");
				CharBuffer sb = CharBuffer.allocate(10000);
	    		reader.read(sb);
	    		return new String(sb.array(),0,sb.position());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    		
    	}
    	throw new IllegalArgumentException("项目不存在");        	
    }
    
    @GetMapping("/list/{type}")
    public R<List<ProjectEntity>> list(@PathVariable("type") String type) {
        List<ProjectEntity> projectList = projectService.findAll(type);
        return new R<List<ProjectEntity>>().setCode(0).setData(projectList);
    }

    /**
     * 新增
     *
     * @param project
     * @return
     */
    @PostMapping
    public R save(@RequestBody ProjectEntity project) {
        projectService.save(project);
        return success();
    }

    /**
     * 更新
     *
     * @param project
     * @return
     */
    @PutMapping
    public R update(@RequestBody ProjectEntity project) {
        projectService.updateById(project);
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
        projectService.delete(id);
        return success();
    }
    
    @PostMapping("/data/{id}")
    public R saveData(@PathVariable("id") String projectId, @RequestBody ProjectVo project) {
        projectService.saveData(projectId,project);
        return success();
    }

}
