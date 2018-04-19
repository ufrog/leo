package net.ufrog.leo.server.controllers;

import net.ufrog.aries.common.contract.PageResp;
import net.ufrog.leo.client.AppClient;
import net.ufrog.leo.client.contract.AppResp;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用控制器
 *
 * @author ultrafrog, ufrog.net@gmail.com
 * @version 3.0.0, 2018-04-11
 * @since 3.0.0
 */
@RestController
@CrossOrigin
public class AppController implements AppClient {

    @Override
    public AppResp read(String id) {
        return null;
    }

    @Override
    public PageResp<AppResp> read() {
        return null;
    }
}