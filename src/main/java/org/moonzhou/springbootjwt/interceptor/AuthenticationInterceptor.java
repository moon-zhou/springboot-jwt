package org.moonzhou.springbootjwt.interceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.*;
import org.moonzhou.springbootjwt.annotation.TokenRequired;
import org.moonzhou.springbootjwt.entity.User;
import org.moonzhou.springbootjwt.service.UserService;
import org.moonzhou.springbootjwt.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * jwt验证处理拦截器
 *
 * 先验证token，通过之后才能获取数据查库，减少数据库的压力，查库放在最后操作
 * TODO 但是此处的验证签名使用的是密码，后续方案上可进行优化
 * @author moon-zhou <ayimin1989@163.com>
 * @version V1.0.0
 * @description
 * @date 2020/5/5 21:14
 * @since 1.0
 */
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        // 从 http 请求头中取出 token
        String token = httpServletRequest.getHeader("token");
        // 如果不是映射到方法直接通过
        if (!(object instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) object;
        Method method = handlerMethod.getMethod();
        //检查有没有需要用户权限的注解
        if (method.isAnnotationPresent(TokenRequired.class)) {
            TokenRequired userLoginToken = method.getAnnotation(TokenRequired.class);
            if (userLoginToken.required()) {
                // 执行认证的参数合法性校验
                if (token == null) {
                    // ResultDTO.failure(new ResultError(UserError.TOKEN_IS_NOT_EXIT));
                    throw new RuntimeException("无token，请重新登录");
                }

                // 获取 token 中的 user id
                String userId;
                try {
                    userId = JWT.decode(token).getClaim("userId").asString();
                } catch (JWTDecodeException j) {
                    throw new RuntimeException("401");
                }
                User user = userService.findUserById(userId);
                if (user == null) {
                    // ResultDTO.failure(new ResultError(UserError.EMP_IS_NULL_EXIT));
                    throw new RuntimeException("用户不存在，请重新登录");
                }

                // 验证 token
                try {
                    if (!JwtUtil.verify(token, user.getPassword())) {
                        //  ResultDTO.failure(new ResultError(UserError.TOKEN_IS_VERITYED));
                        throw new RuntimeException("无效的令牌");
                    }
                } catch (SignatureVerificationException e) {
                    throw new RuntimeException("签名不一致");
                } catch (TokenExpiredException e) {
                    throw new RuntimeException("令牌过期");
                } catch (AlgorithmMismatchException e) {
                    throw new RuntimeException("算法不匹配");
                } catch (InvalidClaimException e) {
                    throw new RuntimeException("失效的payload");
                } catch (JWTVerificationException e) {
                    throw new RuntimeException("401");
                } catch (Exception e) {
                    throw new RuntimeException("token无效");
                }

                return true;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
