package com.company.security.controller.rest;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.apache.commons.codec.binary.Base64;
import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.RestController;

import com.company.security.Const.LoginServiceConst;
import com.company.security.Const.SessionKeyConst;
import com.company.security.domain.AccessContext;
import com.company.security.domain.RequestLogin;
import com.company.security.domain.RequestModifyPassword;
import com.company.security.domain.RequestTokenBody;
import com.company.security.domain.SecurityUser;
import com.company.security.domain.sms.SmsContext;
import com.company.security.domain.sms.AuthCode;
import com.company.security.service.ISmsValidCodeService;
import com.company.security.service.IUserLoginService;
import com.company.security.utils.RSAUtils;
import com.google.gson.Gson;
import com.xinwei.nnl.common.domain.ProcessResult;
import com.xinwei.nnl.common.util.JsonUtil;


@RestController
@RequestMapping("/user")
public class UserLoginController {
	
	@Resource(name="userLoginService")
	private IUserLoginService userLoginService;
	
	@Resource(name="smsValidCodeService")
	private ISmsValidCodeService smsValidCodeService;
	
	/**
	 * 认证码注册
	 * @param request
	 * @param countryCode
	 * @param requestTokenBody
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/{countryCode}/registerByCode")
	public  ProcessResult registerUserByCode(HttpServletRequest request,@PathVariable String countryCode,@RequestBody RequestLogin loginUserSession) {
		ProcessResult processResult =new ProcessResult();
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			//设置秘钥
			PrivateKey rsaPrivateKey = (PrivateKey) request.getSession().getAttribute(SessionKeyConst.Rsa_private_key);
			accessContext.setRsaPrivateKey(rsaPrivateKey);
			accessContext.setTransid(loginUserSession.getTransid());
			//设置电话号码，transid，authcode
			AuthCode authCode = new AuthCode();
			authCode.setTransid(loginUserSession.getTransid());
			authCode.setAuthCode(loginUserSession.getAuthCode());
			authCode.setPhone(loginUserSession.getLoginId());
			accessContext.setLoginUserSession(loginUserSession);
			int iRet= userLoginService.registerUserByCode(accessContext, countryCode, loginUserSession.getLoginId(), loginUserSession.getPassword(),loginUserSession,authCode);
			processResult.setRetCode(iRet);
			processResult.setResponseInfo(accessContext.getLoginUserInfo());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 密码登录
	 * @param countryCode
	 * @param loginUserSession -- phone,password
	 * @return
	 */
	@RequestMapping(method = {RequestMethod.POST,RequestMethod.GET},value = "/{countryCode}/loginByPass")
	public  ProcessResult loginByPass(HttpServletRequest request,@PathVariable String countryCode,@RequestBody RequestLogin loginUserSession) {
		ProcessResult processResult =new ProcessResult();
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			PrivateKey rsaPrivateKey = (PrivateKey) request.getSession().getAttribute(SessionKeyConst.Rsa_private_key);
			accessContext.setRsaPrivateKey(rsaPrivateKey);
			accessContext.setTransid(loginUserSession.getTransid());
			accessContext.setLoginUserSession(loginUserSession);
			int iRet= userLoginService.loginUserManual(accessContext, countryCode, loginUserSession.getLoginId(), loginUserSession.getPassword(),loginUserSession);
			processResult.setRetCode(iRet);
			loginUserSession.setPassword("");
			processResult.setResponseInfo(loginUserSession);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 短信认证码登录流程
	 * @param request
	 * @param userId
	 * @param token
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/{countryCode}/loginByAuthCode")
	public  ProcessResult loginByAuthCode(@PathVariable String countryCode,@RequestBody RequestLogin loginUserSession) {
		ProcessResult processResult =new ProcessResult();
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			//构造短信认证码
			AuthCode authCode = new AuthCode();
			authCode.setPhone(loginUserSession.getLoginId());
			authCode.setTransid(loginUserSession.getTransid());
			authCode.setAuthCode(loginUserSession.getAuthCode());
			accessContext.setLoginUserSession(loginUserSession);
			int iRet= userLoginService.loginUserBySmsCode(accessContext, countryCode, loginUserSession.getLoginId(),loginUserSession,authCode);
			processResult.setRetCode(iRet);
			loginUserSession.setPassword("");
			processResult.setResponseInfo(loginUserSession);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 申请短信认证码，通用的函数,所有发送短信的都可以走这个短信认证码
	 * @param requestTokenBody  -- 申请的可以不走这个流程
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/getSmsValid")
	public  ProcessResult getSmsValidCode(HttpServletRequest request,@RequestBody AuthCode authCode) {
		ProcessResult processResult =new ProcessResult();		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AuthCode smsValidCode =authCode;
			//禁止客户端的transid
			smsValidCode.setTransid("");
			
			SmsContext smsContext  = new SmsContext();
			smsContext.setSmsValidCode(smsValidCode);
			//申请随机数
			int iRet = this.userLoginService.createRandom(smsContext, smsValidCode.getPhone());
			//发送短信认证码
			 iRet = smsValidCodeService.sendValidCodeBySms(smsContext, smsValidCode);
			//构造秘钥
			 String base64PublicKey = getBase64PublicKey(request,smsValidCode.getPhone());
			smsContext.getSmsValidCode().setCrcType(AuthCode.CrcType_RSA);
			smsContext.getSmsValidCode().setPublicKey(base64PublicKey);
			processResult.setRetCode(iRet);
			smsContext.getSmsValidCode().setAuthCode("");
			processResult.setResponseInfo(smsContext.getSmsValidCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 客户端申请随机数，用于加密
	 * @param AuthCode
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/getRandom")
	public  ProcessResult getRandom(@RequestBody AuthCode authCode) {
		ProcessResult processResult =new ProcessResult();		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AuthCode smsValidCode =authCode;
			SmsContext smsContext  = new SmsContext();
			smsContext.setSmsValidCode(smsValidCode);
			int iRet = this.userLoginService.createRandom(smsContext, smsValidCode.getPhone());
			processResult.setRetCode(iRet);
			processResult.setResponseInfo(smsContext.getSmsValidCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 
	 * @param request
	 * @param phone
	 * @return
	 */
	protected String getBase64PublicKey(HttpServletRequest request,String phone)
	{
		try {
			PublicKey publicKey = (PublicKey)request.getSession().getAttribute(SessionKeyConst.Rsa_public_key);
			if(publicKey==null)
			{
				KeyPair keyPair= this.userLoginService.getRsaInfo(phone);
				if(keyPair==null)
				{
					keyPair= RSAUtils.generateKeyPair();
				}
				PrivateKey privateKey = keyPair.getPrivate();
				publicKey = keyPair.getPublic();
				request.getSession().setAttribute(SessionKeyConst.Rsa_private_key, privateKey);
				request.getSession().setAttribute(SessionKeyConst.Rsa_public_key, publicKey);
				
			}
			return Base64.encodeBase64String(publicKey.getEncoded());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * 获取加密的公钥
	 * @param request
	 * @param AuthCode
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/getRsaPubKey")
	public  ProcessResult getRSaPubkey(HttpServletRequest request,@RequestBody AuthCode authCode) {
		ProcessResult processResult =new ProcessResult();		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AuthCode smsValidCode =authCode;
			SmsContext smsContext  = new SmsContext();
			smsContext.setSmsValidCode(authCode);
			int iRet = this.userLoginService.createRandom(smsContext, smsValidCode.getPhone());
			//用于加密的transid和随机数
			//smsValidCode.setTransid(smsContext.getSmsValidCode().getTransid());
			//smsValidCode.setSendSeqno(smsContext.getSmsValidCode().getAuthCode());
			
			String base64PublicKey = getBase64PublicKey(request,smsValidCode.getPhone());
			//加密的公钥
			smsValidCode.setPublicKey(base64PublicKey);
			processResult.setRetCode(LoginServiceConst.RESULT_Success);
			processResult.setResponseInfo(smsValidCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	/**
	 * 短信认证码重置密码
	 * @param requestTokenBody
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/{countryCode}/resetPassByAuthCode")
	public  ProcessResult resetPassByAuthCode(HttpServletRequest request,@PathVariable String countryCode,@RequestBody RequestLogin loginUserSession) {
		ProcessResult processResult =new ProcessResult();
		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext = new AccessContext();
			accessContext.setTransid(loginUserSession.getTransid());
			PrivateKey privateKey=(PrivateKey)request.getSession().getAttribute(SessionKeyConst.Rsa_private_key);
			
			accessContext.setRsaPrivateKey(privateKey);
			//构造短信认证码
			AuthCode authCode = new AuthCode();
			authCode.setPhone(loginUserSession.getLoginId());
			authCode.setTransid(loginUserSession.getTransid());
			authCode.setAuthCode(loginUserSession.getAuthCode());
			
			int iRet= userLoginService.resetPasswrodByPhone(accessContext,countryCode,loginUserSession.getLoginId(),loginUserSession.getPassword(),loginUserSession,authCode);
			
			 processResult.setRetCode(iRet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	/**
	 * 修改密码
	 * @param request
	 * @param countryCode
	 * @param requestTokenBody
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST,value = "/{countryCode}/modifyPassword")
	public  ProcessResult modifyPassword(HttpServletRequest request,@PathVariable String countryCode,@RequestBody RequestModifyPassword requestModifyPassword) {
		ProcessResult processResult =new ProcessResult();
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			
			//RequestModifyPassword requestModifyPassword  =	JsonUtil.fromJson(requestTokenBody.getRequestBody(),RequestModifyPassword.class);
			accessContext.setTransid(requestModifyPassword.getTransid());
			accessContext.setRsaPrivateKey((PrivateKey)request.getSession().getAttribute(SessionKeyConst.Rsa_private_key));
		
			//accessContext.setLoginUserSession(loginUserSession);
			int iRet= userLoginService.modifyPasswrodByPhone(accessContext, requestModifyPassword.getPhone(), requestModifyPassword.getModifyKey(), requestModifyPassword.getNewPassword());
			processResult.setRetCode(iRet);
			processResult.setResponseInfo(accessContext.getLoginUserInfo());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "{countryCode}/getUserInfo")
	public  ProcessResult getUserInfo(HttpServletRequest request,@PathVariable String countryCode,@RequestBody SecurityUser securityUser) {
		ProcessResult processResult =new ProcessResult();		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			
			int iRet = this.userLoginService.getUserInfo(accessContext,securityUser.getPhone());
			processResult.setRetCode(iRet);
			if(LoginServiceConst.RESULT_Success==iRet)
			{
				processResult.setResponseInfo(accessContext.getObject());	
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "{countryCode}/modifyUserInfo")
	public  ProcessResult modifyUserInfo(@PathVariable String countryCode,@RequestBody SecurityUser securityUser) {
		ProcessResult processResult =new ProcessResult();		
		processResult.setRetCode(LoginServiceConst.RESULT_Error_Fail);
		try {
			AccessContext accessContext =new AccessContext();
			accessContext.setObject(securityUser);
			int iRet = this.userLoginService.modifyUserInfo(accessContext, securityUser.getPhone());
			processResult.setRetCode(iRet);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return processResult;
	}
}
