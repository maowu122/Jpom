package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.system.AgentAutoUser;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.util.JvmUtil;
import com.alibaba.fastjson.JSONObject;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import java.io.File;
import java.util.List;

/**
 * 自动导入本机节点
 *
 * @author jiangzeyin
 * @date 2019/4/18
 */
@PreLoadClass
public class AutoImportLocalNode {

    private static final String AGENT_MAIN_CLASS = "cn.keepbx.jpom.JpomAgentApplication";
    private static NodeService nodeService;

    @PreLoadMethod
    private static void loadAgent() {
        nodeService = SpringUtil.getBean(NodeService.class);
        List<NodeModel> list = nodeService.list();
        if (list != null && !list.isEmpty()) {
            return;
        }
        //
        try {
            List<MonitoredVm> monitoredVms = JvmUtil.listMainClass(AGENT_MAIN_CLASS);
            monitoredVms.forEach(monitoredVm -> {
                VmIdentifier vmIdentifier = monitoredVm.getVmIdentifier();
                findPid(vmIdentifier.getUserInfo());
            });
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("自动添加本机节点错误", e);
        }
    }

    private static void findPid(String pid) {
        File file = ConfigBean.getInstance().getApplicationJpomInfo(Type.Agent);
        if (!file.exists() || file.isDirectory()) {
            return;
        }
        // 比较进程id
        String json = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
        JpomManifest jpomManifest = JSONObject.parseObject(json, JpomManifest.class);
        if (!pid.equals(String.valueOf(jpomManifest.getPid()))) {
            return;
        }
        // 判断自动授权文件是否存在
        String path = ConfigBean.getInstance().getAgentAutoAuthorizeFile(jpomManifest.getDataPath());
        if (!FileUtil.exist(path)) {
            return;
        }
        json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
        AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
        // 判断授权信息
        //
        NodeModel nodeModel = new NodeModel();
        nodeModel.setUrl(StrUtil.format("127.0.0.1:{}", jpomManifest.getPort()));
        nodeModel.setName("本机");
        nodeModel.setId("localhost");
        //
        nodeModel.setLoginPwd(autoUser.getAgentPwd());
        nodeModel.setLoginName(autoUser.getAgentName());
        //
        nodeModel.setOpenStatus(true);
        nodeService.addItem(nodeModel);
        DefaultSystemLog.LOG().info("自动添加本机节点成功：" + nodeModel.getId());
    }
}
