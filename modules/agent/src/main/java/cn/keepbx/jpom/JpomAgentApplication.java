package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.JpomApplicationEvent;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.AuthorizeInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * jpom 启动类
 * Created by jiangzeyin on 2017/9/14.
 *
 * @author jiangzeyin
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomAgentApplication extends BaseJpomApplication {

    public JpomAgentApplication() {
        super(Type.Agent, JpomAgentApplication.class);
    }

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomAgentApplication.args = args;
        ApplicationBuilder.createBuilder(JpomAgentApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8))
                // 拦截器
                .addInterceptor(AuthorizeInterceptor.class)
                //
                .addApplicationEventClient(new JpomApplicationEvent())
                .run(args);
    }

}
