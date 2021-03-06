package cn.keepbx.jpom.controller.node.manage.file;

import cn.hutool.core.io.FileUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.OperateType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 文件管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/file/")
public class ProjectFileControl extends BaseServerController {
    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 文件管理页面
     *
     * @param id 项目id
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String fileManage(String id) {
        setAttribute("id", id);
        JSONObject projectInfo = projectInfoService.getItem(getNode(), id);
        String lib = projectInfo.getString("lib");
        lib = FileUtil.getAbsolutePath(FileUtil.file(lib));
        setAttribute("absLib", lib);
        return "node/manage/filemanage";
    }

    /**
     * 列出目录下的文件
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    public String getFileList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_GetFileList).toString();
    }


    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkUpload = true)
    @OperateType(UserOperateLogV1.OptType.UploadProjectFile)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Manage_File_Upload).toString();
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.DownloadProjectFile)
    public void download() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_File_Download);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkDelete = true)
    @OperateType(UserOperateLogV1.OptType.DelProjectFile)
    public String deleteFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_DeleteFile).toString();
    }
}
