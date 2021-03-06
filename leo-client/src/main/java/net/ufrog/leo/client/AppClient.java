package net.ufrog.leo.client;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.ufrog.aries.common.contract.ListResponse;
import net.ufrog.aries.common.contract.PageResponse;
import net.ufrog.leo.client.contracts.AppResponse;
import net.ufrog.leo.client.contracts.AppSecretResponse;
import net.ufrog.leo.client.fallbackfactories.AppClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 应用业务客户端
 *
 * @author ultrafrog, ufrog.net@gmail.com
 * @version 3.0.0, 2018-03-27
 * @since 3.0.0
 */
@FeignClient(name = Client.NAME, fallbackFactory = AppClientFallbackFactory.class)
@RequestMapping("/apps")
@Api(value = "用户服务")
public interface AppClient {

    /**
     * 查询应用
     *
     * @param id 编号
     * @return 单个应用响应
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "按编号查询应用", notes = "查询单个应用")
    AppResponse read(@PathVariable("id") String id);

    /**
     * 查询所有应用
     *
     * @return 应用列表
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "查询所有应用", notes = "分页查询应用列表")
    PageResponse<AppResponse> read();

    /**
     * 查询所有应用密钥
     *
     * @return 应用密钥列表
     */
    @RequestMapping(value = "/secrets", method = RequestMethod.GET)
    @ApiOperation(value = "查询所有应用密钥", notes = "查询所有应用密钥")
    ListResponse<AppSecretResponse> readSecrets();
}
