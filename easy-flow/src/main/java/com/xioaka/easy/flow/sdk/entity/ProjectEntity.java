package com.xioaka.easy.flow.sdk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author liuchengbiao
 * @date 2020-05-22 21:22
 */
@Data
@TableName("hosp.flow_project")
public class ProjectEntity {
    @TableId(type = IdType.ID_WORKER_STR)
    private String id;
    private String name;
    
    ProjectEntity(){
    	
    }
    
    public ProjectEntity(String id,String name){
    	this.id = id;
    	this.name = name;
    }
}
