package net.ufrog.leo.server.controllers;

import net.ufrog.common.Logger;
import net.ufrog.common.Result;
import net.ufrog.common.ThreadPools;
import net.ufrog.common.app.App;
import net.ufrog.common.app.AppUser;
import net.ufrog.common.exception.ServiceException;
import net.ufrog.common.utils.Passwords;
import net.ufrog.common.utils.Strings;
import net.ufrog.common.web.app.WebApp;
import net.ufrog.leo.client.LeoInterception;
import net.ufrog.leo.domain.models.User;
import net.ufrog.leo.domain.models.UserSignLog;
import net.ufrog.leo.server.AccessToken;
import net.ufrog.leo.server.AccessTokenManager;
import net.ufrog.leo.service.AppService;
import net.ufrog.leo.service.SecurityService;
import net.ufrog.leo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 索引控制器
 *
 * @author ultrafrog, ufrog.net@gmail.com
 * @version 0.1, 2017-02-26
 * @since 0.1
 */
@Controller
public class IndexController {

    private static final String SESSION_ACCESS_TOKENS   = "access_tokens";

    /** 应用业务接口 */
    private AppService appService;

    /** 权限业务接口 */
    private SecurityService securityService;

    /** 用户业务接口 */
    private UserService userService;

    /**
     * 构造函数
     *
     * @param appService 应用业务接口
     * @param securityService 权限业务接口
     * @param userService 用户业务接口
     */
    @Autowired
    public IndexController(AppService appService, SecurityService securityService, UserService userService) {
        this.appService = appService;
        this.securityService = securityService;
        this.userService = userService;
    }

    /**
     * 索引
     *
     * @return view for index
     */
    @GetMapping({"", "/", "/index"})
    public String index() {
        return "index";
    }

    /**
     * 视图
     *
     * @return view for {view}
     */
    @GetMapping("/view/{view}")
    public String view(@PathVariable("view") String view) {
        return view.replaceAll("@", "/");
    }

    /**
     * 登录表单
     *
     * @return view for sign_in
     */
    @GetMapping("/sign_in")
    public String signIn() {
        return "sign_in";
    }

    /**
     * 注销
     *
     * @return view for sign_in
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/sign_out")
    public String signOut() {
        AppUser appUser = App.current().getUser();
        if (appUser != null) {
            WebApp app = App.current(WebApp.class);
            List<AccessToken> lAccessToken = App.current(WebApp.class).session(SESSION_ACCESS_TOKENS, List.class);
            if (lAccessToken != null) {
                lAccessToken.forEach(accessToken -> {
                    AccessTokenManager.get().offline(appUser.getId(), accessToken.getAppId(), accessToken.getToken());
                    ThreadPools.execute(() -> userService.createSignLog(UserSignLog.Type.SIGN_OUT, UserSignLog.Mode.GATEWAY, accessToken.getAppId(), accessToken.getUserId(), null, null));
                });
            }
            app.setUser(null);
            app.session(SESSION_ACCESS_TOKENS, new ArrayList<>());
        }
        return signIn();
    }

    /**
     * 用户认证
     *
     * @param account 账号
     * @param password 密码
     * @return 用户认证结果
     */
    @PostMapping("/authenticate")
    @ResponseBody
    public Result<AppUser> authenticate(String account, String password) {
        try {
            User user = userService.authenticate(account, password, UserService.UserType.STAFF, UserService.UserType.ROOT);
            AppUser appUser = new AppUser(user.getId(), user.getAccount(), user.getName());
            App.current().setUser(appUser);
            securityService.clearResourceMapping(appUser.getId());
            return Result.success(appUser, App.message("authenticate.success"));
        } catch (ServiceException e) {
            return Result.failure(App.message(e.getKey()));
        }
    }

    /**
     * 跳转应用
     *
     * @param appId 应用编号
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/redirect/{appId}")
    public String redirect(@PathVariable("appId") String appId, HttpServletRequest request) {
        net.ufrog.leo.domain.models.App app = appService.getById(appId);
        List<AccessToken> lAccessToken = App.current(WebApp.class).session(SESSION_ACCESS_TOKENS, List.class);
        if (lAccessToken == null) lAccessToken = new ArrayList<>();

        AccessToken accessToken;
        Optional<AccessToken> oAccessToken = lAccessToken.stream().filter(at -> Strings.equals(appId, at.getAppId())).findFirst();
        if (!oAccessToken.isPresent()) {
            accessToken = AccessTokenManager.get().online(App.user(), app, request.getRemoteAddr());
            lAccessToken.add(accessToken);
            App.current(WebApp.class).session(SESSION_ACCESS_TOKENS, lAccessToken);
            ThreadPools.execute(() -> userService.createSignLog(UserSignLog.Type.SIGN_IN, UserSignLog.Mode.GATEWAY, accessToken.getAppId(), accessToken.getUserId(), null, null));
            Logger.debug("create access token '%s' for app '%s'", accessToken.getToken(), accessToken.getAppId());
        } else {
            accessToken = oAccessToken.get();
        }
        return "redirect:" + app.getUrl() + (app.getUrl().contains("?") ? "&" : "?") + LeoInterception.DEFAULT_PARAM_KEY + "=" + accessToken.getToken();
    }

    /**
     * 查询应用
     *
     * @return 应用列表
     */
    @GetMapping("/apps")
    @ResponseBody
    public List<net.ufrog.leo.domain.models.App> findApps() {
        return securityService.filter(appService.findAll(), App.user().getId());
    }

    /**
     * 检查是否需要强制修改密码
     *
     * @return 检查结果
     */
    @GetMapping("/check_forced")
    @ResponseBody
    public Result<User> checkForced() {
        User user = userService.findById(App.user().getId());
        if (Strings.equals(User.Forced.TRUE, user.getForced())) {
            return Result.warning(user, App.message("reset.password.forced"));
        }
        return Result.success(user);
    }

    /**
     * 重置密码
     *
     * @param prev 原密码
     * @param next 新密码
     * @param confirm 确认密码
     * @return 重置结果
     */
    @PostMapping("/reset_password")
    @ResponseBody
    public Result<User> resetPassword(String prev, String next, String confirm) {
        if (Strings.equals(next, confirm)) {
            User user = userService.findById(App.user().getId());
            if (Passwords.match(prev, user.getPassword())) {
                user.setPassword(Passwords.encode(next));
                user.setForced(User.Forced.FALSE);
                userService.update(user);
                signOut();
                return Result.success(user, App.message("reset.password.success"));
            }
            return Result.failure(App.message("reset.password.failure.prev"));
        }
        return Result.failure(App.message("reset.password.failure.confirm"));
    }
}
