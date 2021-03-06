package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.RunMode;
import cn.keepbx.jpom.model.data.*;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.service.system.ServerWhitelistServer;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * 节点分发编辑项目
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
public class OutGivingProjectEditController extends BaseServerController {
    @Resource
    private ServerWhitelistServer serverWhitelistServer;

    @Resource
    private OutGivingServer outGivingServer;

    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String editProject(String id) throws IOException {
        setAttribute("type", "add");
        OutGivingModel outGivingModel = null;
        if (StrUtil.isNotEmpty(id)) {
            outGivingModel = outGivingServer.getItem(id);
            if (outGivingModel != null) {
                setAttribute("item", outGivingModel);
                setAttribute("type", "edit");
            }
        }
        // 运行模式
        JSONArray runModes = (JSONArray) JSONArray.toJSON(RunMode.values());
        setAttribute("runModes", runModes);
        // 权限
        UserModel userModel = getUser();
        if (userModel.isServerManager()) {
            List<NodeModel> nodeModels = nodeService.listAndProject();
            setAttribute("nodeModels", nodeModels);
            //
            String reqId = nodeService.cacheNodeList(nodeModels);
            setAttribute("reqId", reqId);
        }

        // 白名单
        List<String> jsonArray = serverWhitelistServer.getOutGiving();
        setAttribute("whitelistDirectory", jsonArray);
        //
        if (outGivingModel != null && jsonArray != null) {
            JSONObject projectInfo = outGivingModel.getFirstNodeProject(true);
            if (projectInfo != null) {
                for (Object obj : jsonArray) {
                    String itemWhitelistDirectory = AgentWhitelist.getItemWhitelistDirectory(projectInfo, obj.toString());
                    if (itemWhitelistDirectory != null) {
                        setAttribute("itemWhitelistDirectory", itemWhitelistDirectory);
                        break;
                    }
                }
                setAttribute("firstData", projectInfo);
            }
        }
        return "outgiving/editProject";
    }

    /**
     * 保存节点分发项目
     *
     * @param id   id
     * @param type 类型
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "save_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.SaveOutgivingProject)
    public String save(String id, String type) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            if (!Validator.isGeneral(id, 2, 20)) {
                return JsonMessage.getString(401, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            }
            return addOutGiving(id);
        } else {
            return updateGiving(id);
        }
    }

    /**
     * 删除分发项目
     *
     * @param id 项目id
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "delete_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.DeleteOutgivingProject)
    public String delete(String id) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(200, "没有对应的分发项目");
        }
        if (!outGivingModel.isOutGivingProject()) {
            return JsonMessage.getString(405, "改项目不是节点分发项目,不能在此次删除");
        }
        UserModel userModel = getUser();
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getOutGivingNodeProjectList();
        if (deleteNodeProject != null) {
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = outGivingNodeProject1.getNodeData(true);
                JsonMessage jsonMessage = deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }
        outGivingServer.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    private String addOutGiving(String id) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel != null) {
            return JsonMessage.getString(405, "分发id已经存在啦");
        }
        outGivingModel = new OutGivingModel();
        outGivingModel.setOutGivingProject(true);
        outGivingModel.setId(id);
        //
        String error = doData(outGivingModel, false);
        if (error != null) {
            return error;
        }
        outGivingServer.addItem(outGivingModel);
        error = saveNodeData(outGivingModel, false);
        if (error != null) {
            return error;
        }
        return JsonMessage.getString(200, "添加成功");
    }


    private String updateGiving(String id) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(405, "没有找到对应的分发id");
        }
        String error = doData(outGivingModel, true);
        if (error != null) {
            return error;
        }
        outGivingServer.updateItem(outGivingModel);
        error = saveNodeData(outGivingModel, true);
        if (error != null) {
            return error;
        }
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 保存节点项目数据
     *
     * @param outGivingModel 节点分发项目
     * @param edit           是否为编辑模式
     * @return 错误信息
     */
    private String saveNodeData(OutGivingModel outGivingModel, boolean edit) {
        Map<NodeModel, JSONObject> map = outGivingModel.getTempCacheMap();
        if (map == null) {
            if (!edit) {
                outGivingServer.deleteItem(outGivingModel.getId());
            }
            return JsonMessage.getString(405, "数据异常");
        }
        UserModel userModel = getUser();
        List<Map.Entry<NodeModel, JSONObject>> success = new ArrayList<>();
        boolean fail = false;
        try {
            Set<Map.Entry<NodeModel, JSONObject>> entries = map.entrySet();
            JsonMessage jsonMessage;
            for (Map.Entry<NodeModel, JSONObject> entry : entries) {
                NodeModel nodeModel = entry.getKey();
                jsonMessage = sendData(nodeModel, userModel, entry.getValue(), true);
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    if (!edit) {
                        fail = true;
                        outGivingServer.deleteItem(outGivingModel.getId());
                    }
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
                success.add(entry);
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("保存分发项目失败", e);
            if (!edit) {
                fail = true;
                outGivingServer.deleteItem(outGivingModel.getId());
            }
            return JsonMessage.getString(500, "保存节点数据失败:" + e.getMessage());
        } finally {
            if (fail) {
                try {
                    for (Map.Entry<NodeModel, JSONObject> entry : success) {
                        deleteNodeProject(entry.getKey(), userModel, outGivingModel.getId());
                    }
                } catch (Exception e) {
                    DefaultSystemLog.ERROR().error("还原项目失败", e);
                }
            }
        }
        return null;
    }

    /**
     * 删除项目
     *
     * @param nodeModel 节点
     * @param userModel 用户
     * @param project   判断id
     * @return json
     */
    private JsonMessage deleteNodeProject(NodeModel nodeModel, UserModel userModel, String project) {
        // 发起预检查数据
        String url = nodeModel.getRealUrl(NodeUrl.Manage_DeleteProject);
        HttpRequest request = HttpUtil.createPost(url);
        // 授权信息
        NodeForward.addUser(request, nodeModel, userModel);
        JSONObject data = new JSONObject();
        data.put("id", project);
        request.form(data);
        //
        String body = request.execute()
                .body();
        return NodeForward.toJsonMessage(body);
    }

    /**
     * 创建项目管理的默认数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return String为有异常
     */
    private Object getDefData(OutGivingModel outGivingModel, boolean edit) {
        JSONObject defData = new JSONObject();
        defData.put("id", outGivingModel.getId());
        defData.put("name", outGivingModel.getName());
        //
        // 运行模式
        String runMode = getParameter("runMode");
        RunMode runMode1 = RunMode.ClassPath;
        try {
            runMode1 = RunMode.valueOf(runMode);
        } catch (Exception ignored) {
        }
        defData.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath) {
            String mainClass = getParameter("mainClass");
            defData.put("mainClass", mainClass);
        }
        String whitelistDirectory = getParameter("whitelistDirectory");
        List<String> whitelistServerOutGiving = serverWhitelistServer.getOutGiving();
        if (!AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory)) {
            return JsonMessage.getString(401, "请选择正确的项目路径,或者还没有配置白名单");
        }
        defData.put("whitelistDirectory", whitelistDirectory);
        String lib = getParameter("lib");
        defData.put("lib", lib);
        defData.put("group", "节点分发");
        if (edit) {
            // 编辑模式
            defData.put("edit", "on");
        }
        defData.put("previewData", true);
        return defData;
    }

    /**
     * 处理页面数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return json
     * @throws IOException IO
     */
    private String doData(OutGivingModel outGivingModel, boolean edit) throws IOException {
        outGivingModel.setName(getParameter("name"));
        if (StrUtil.isEmpty(outGivingModel.getName())) {
            return JsonMessage.getString(405, "分发名称不能为空");
        }
        String reqId = getParameter("reqId");
        List<NodeModel> nodeModelList = nodeService.getNodeModel(reqId);
        if (nodeModelList == null) {
            return JsonMessage.getString(401, "当前页面请求超时");
        }
        Object object = getDefData(outGivingModel, edit);
        if (object instanceof String) {
            return object.toString();
        }
        JSONObject defData = (JSONObject) object;
        UserModel userModel = getUser();
        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;
        //
        Iterator<NodeModel> iterator = nodeModelList.iterator();
        Map<NodeModel, JSONObject> cache = new HashMap<>(nodeModelList.size());
        while (iterator.hasNext()) {
            NodeModel nodeModel = iterator.next();
            String add = getParameter("add_" + nodeModel.getId());
            if (!nodeModel.getId().equals(add)) {
                iterator.remove();
                continue;
            }
            // 判断项目是否已经被使用过啦
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel1 : outGivingModels) {
                    if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                        continue;
                    }
                    if (outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId())) {
                        return JsonMessage.getString(405, "已经存在相同的分发项目:" + outGivingModel.getId());
                    }
                }
            }
            outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), outGivingModel.getId());
            if (outGivingNodeProject == null) {
                outGivingNodeProject = new OutGivingNodeProject();
            }
            outGivingNodeProject.setNodeId(nodeModel.getId());
            outGivingNodeProject.setProjectId(outGivingModel.getId());
            outGivingNodeProjects.add(outGivingNodeProject);
            // 检查数据
            JSONObject allData = (JSONObject) defData.clone();
            String token = getParameter(StrUtil.format("{}_token", nodeModel.getId()));
            allData.put("token", token);
            String jvm = getParameter(StrUtil.format("{}_jvm", nodeModel.getId()));
            allData.put("jvm", jvm);
            String args = getParameter(StrUtil.format("{}_args", nodeModel.getId()));
            allData.put("args", args);
            JsonMessage jsonMessage = sendData(nodeModel, userModel, allData, false);
            if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            }
            cache.put(nodeModel, allData);
        }
        // 删除已经删除的项目
        String error = deleteProject(outGivingModel, outGivingNodeProjects, userModel);
        if (error != null) {
            return error;
        }
        outGivingModel.setOutGivingNodeProjectList(outGivingNodeProjects);
        outGivingModel.setTempCacheMap(cache);
        return null;
    }

    /**
     * 删除已经删除过的项目
     *
     * @param outGivingModel        分发项目
     * @param outGivingNodeProjects 新的节点项目
     * @param userModel             用户
     * @return 错误信息
     */
    private String deleteProject(OutGivingModel outGivingModel, List<OutGivingNodeProject> outGivingNodeProjects, UserModel userModel) {
        if (outGivingNodeProjects.size() < 2) {
            return JsonMessage.getString(406, "至少选择两个节点及以上");
        }
        // 删除
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getDelete(outGivingNodeProjects);
        if (deleteNodeProject != null) {
            JsonMessage jsonMessage;
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = outGivingNodeProject1.getNodeData(true);
                jsonMessage = deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }
        return null;
    }

    private JsonMessage sendData(NodeModel nodeModel, UserModel userModel, JSONObject data, boolean save) {
        if (save) {
            data.remove("previewData");
        }
        data.put("outGivingProject", true);
        // 发起预检查数据
        String url = nodeModel.getRealUrl(NodeUrl.Manage_SaveProject);
        HttpRequest request = HttpUtil.createPost(url);
        // 授权信息
        NodeForward.addUser(request, nodeModel, userModel);
        request.form(data);
        //
        String body = request.execute()
                .body();
        return NodeForward.toJsonMessage(body);
    }
}
