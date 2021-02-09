## 说明

easy-flow是基于vue+elementUI实现的一款前端流程绘制的组件，在使用过程中除了前端需要进行开发，还涉及到数据如何存储以及加载的问题，为了方便快速的进行开发，easy-flow-sdk提供了基本的操作

## 目录结构说明

案例是基于SpringBoot + MyBatisPlus 开发，使用的是MySQL数据库，脚本在src/main/resources/sql/easy-flow.sql中。

## 如何启动

找到 com.xioaka.easy.flow.sdk.App 类，然后执行里面的main方法即可

## 获取流程数据接口

**请求接口：** /project/data/{projectId}

**请求方式:**   get

**返回数据样例:** 

```json
{
  "code": 0,
  "desc": null,
  "data": {
    "name": "测试",
    "nodeList": [
      {
        "id": "1264194018334486530",
        "projectId": "1264186026780315650",
        "name": "节点B",
        "type": "task",
        "left": "200px",
        "top": "100px",
        "ico": "task",
        "state": "success"
      },
      {
        "id": "1264194078472417282",
        "projectId": "1264186026780315650",
        "name": "节点A",
        "type": "task",
        "left": "100px",
        "top": "300px",
        "ico": "task",
        "state": "success"
      }
    ],
    "lineList": [
      {
        "id": "1264194173825724417",
        "projectId": "1264186026780315650",
        "from": "1264194078472417282",
        "to": "1264194018334486530",
        "label": "hello"
      }
    ]
  }
}
```

上面的返回数据中的data即easy-flow 页面需要的数据结构，在页面中调用 this.dataReload(data) 即可。



## SQL脚本

通过看下数据库的脚本(src/main/resources/sql/easy-flow.sql)应该就知道是什么意思了，和前端的数据结构几乎一样。

```sql
DROP TABLE IF EXISTS `flow_project`;
CREATE TABLE `flow_project` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `name` varchar(100) NOT NULL COMMENT '项目名称',
  PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `flow_node`;
CREATE TABLE `flow_node` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `type` varchar(100) NOT NULL COMMENT '类型',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `left` varchar(100) NOT NULL COMMENT '坐标',
  `top` varchar(100) NOT NULL COMMENT '坐标',
  `ico` varchar(100) NOT NULL COMMENT '图标',
  `state` varchar(100) NOT NULL COMMENT '状态',
  PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `flow_line`;
CREATE TABLE `flow_line` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `project_id` varchar(64) NOT NULL COMMENT '项目ID',
  `from` varchar(64) NOT NULL COMMENT '开始节点ID',
  `to` varchar(64) NOT NULL COMMENT '结束节点ID',
  `label` varchar(100) NOT NULL COMMENT '条件',
  PRIMARY KEY (`id`),
  unique  key  `uni_line`(`from`,`to`)
);
```

* flow_project： 不同的节点组成的一个流程这里被称之为项目。一个项目下可以包含多种节点和连线。
* flow_node： 通过project_id和项目进行关联。
* flow_line: 通过project_id和项目进行关联。同时设置了约束（两个节点之间不能存在多个相同的连线）。

## 业务扩展

上面的表结构只是easy-flow需要的一些必须的字段信息，在实际业务中还需要存储其他的业务信息，比如Node中除了需要记录名称之外，可能还需要记录一些调用的接口信息、流程处理人信息等。针对这种需求可以分为2种方法解决。

> 不改变上面的表结构的条件下，将新增的字段放入新建的另一个表中，通过node_id进行关联。

> 将上诉的表结构进行下改变、将新增的字段放入到上面的node表中。



