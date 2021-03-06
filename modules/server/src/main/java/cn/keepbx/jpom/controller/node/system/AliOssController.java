package cn.keepbx.jpom.controller.node.system;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 阿里云oss 配置
 *
 * @author jiangzeyin
 * @date 2019/3/5
 */
@Controller
@RequestMapping(value = "/node/system")
public class AliOssController extends BaseServerController {


    /**
     * 页面
     */
    @RequestMapping(value = "alioss", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String whitelistDirectory() {
        UserModel userModel = getUserModel();
        if (userModel.isSystemUser()) {
            JSONObject item = NodeForward.requestData(getNode(), NodeUrl.System_alioss_config, getRequest(), JSONObject.class);
            setAttribute("item", item);
        }
        return "node/system/alioss";
    }

    @RequestMapping(value = "alioss_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditAliOss)
    public String aliOssSubmit() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_alioss_submit).toString();
    }
}
