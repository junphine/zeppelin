package com.xioaka.easy.flow.sdk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xioaka.easy.flow.sdk.dao.ProjectDao;
import com.xioaka.easy.flow.sdk.entity.LineEntity;
import com.xioaka.easy.flow.sdk.entity.NodeEntity;
import com.xioaka.easy.flow.sdk.entity.ProjectEntity;
import com.xioaka.easy.flow.sdk.service.ILineService;
import com.xioaka.easy.flow.sdk.service.INodeService;
import com.xioaka.easy.flow.sdk.service.IProjectService;
import com.xioaka.easy.flow.sdk.vo.ProjectVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liuchengbiao
 * @date 2020-05-22 21:30
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectDao, ProjectEntity> implements IProjectService {
	
	ObjectMapper om = new ObjectMapper();

    @Autowired
    private INodeService nodeService;
    @Autowired
    private ILineService lineService;

    @Override
    public ProjectVo queryData(String projectId) {
        ProjectEntity projectEntity = getById(projectId);
        if (projectEntity == null) {        	
        	throw new IllegalArgumentException("项目不存在");        	
        }
        List<NodeEntity> nodeList = nodeService.queryByProjectId(projectId);
        List<LineEntity> lineList = lineService.queryByProjectId(projectId);
        ProjectVo projectVo = new ProjectVo();
        projectVo.setName(projectEntity.getName());
        projectVo.setNodeList(nodeList);
        projectVo.setLineList(lineList);
        return projectVo;
    }

    @Override
    public void delete(String projectId) {
        removeById(projectId);
        nodeService.deleteByProjectId(projectId);
        lineService.deleteByProjectId(projectId);
    }
    
    public void saveData(String projectId,ProjectVo data) {
    	URL path = this.getClass().getClassLoader().getResource("/WEB-INF");
    	
		try {
			String pathStr = ".";
			if(path!= null) {
	    		pathStr = path.getPath();
	    	}
	    	File file = new File(pathStr+"/workflows",projectId+".js");
	    	FileWriter writer;
			writer = new FileWriter(file);
			om.writeValue(writer, data);
	    	writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalArgumentException("保存失败",e);       
		}
    	
    }
    
    public List<ProjectEntity> findAll(String type){
    	if("template".equals(type)) { //template
    		
    		List<ProjectEntity> tplList = new ArrayList<ProjectEntity>();
    		tplList.add(new ProjectEntity("data_A","串行流程A"));
    		tplList.add(new ProjectEntity("data_B","依赖型流程B"));
    		tplList.add(new ProjectEntity("data_C","条件流程C"));
    		tplList.add(new ProjectEntity("data_D","流程D,自定义样式"));
    		return tplList;
    	}
    	List<ProjectEntity> list = this.list();
    	return list;
    }
}
