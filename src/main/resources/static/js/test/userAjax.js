
/**
 * email 登录
 */
let LoginIdType_email = 1;
	/**
	 * 电话登录
	 */
let LoginIdType_phone = 2;
	/**
	 * id登录
	 */
let LoginIdType_userid = 3;
	

/**
 * pcweb登录
 */
let loginType_web = 1;
/**
 * 移动端登录
 */
let loginType_mobile = 2;
/**
 * 
 */
let loginType_pad = 3;

/**
 * pc客户端
 */
let loginType_pc = 4;


/**
 * 返回错误
 */
/**
 * 用户名或者密码错误
 */
let RESULT_Error_PasswordError = 3001;
/**
 * 更新缓存错误
 */
let RESULT_Error_putSession = 3002;
/**
 * 电话号码已经注册
 */
let RESULT_Error_PhoneHaveRegister = 3003;
/**
 * 认证码错误
 */
let RESULT_Error_ValidCode = 3004;

/**
 * 对应服务器的 RequestLogin 类
 * @param countryCode -- 国家码，以00开头
 * @param phone  -- 包括国家码的全号码
 * @param password
 * @param successCallback
 * @param errorCallback
 * @returns
 */
function ajaxLoginWithPass(countryCode,phone,password,successCallback,errorCallback)
{
	let serverUrl = getRootPath() + "/user/" + countryCode + "/loginByPass";
    let data = {
    		"loginIdType":LoginIdType_phone,
    		"loginId":phone,
    		"countryCode":countryCode,
    		"password":password,
    		"loginType":loginType_web
    		};	
    console.log(JSON.stringify(data));
	ajaxPost(serverUrl,data,function successLogin(data,textStatus){
		console.log(data);
		//successCallback(loginUserSession);
	},
	function errorLogin(xhr,testStatus){
		//errorCallback(xhr,testStatus);
	});
}
/**
 * 
 * @param countryCode
 * @param phone
 * @param password
 * @param successCallback
 * @param errorCallback
 * @returns
 */
function ajaxRegisterWithAuth(countryCode,phone,password,authTransid,authCode,successCallback,errorCallback)
{
	let serverUrl = getRootPath() + "/user/" + countryCode + "/registerByCode";
    let data = {
    		"transid":authTransid,
    		 "authCode":authCode,
    		"loginIdType":LoginIdType_phone,
    		"loginId":phone,
    		"countryCode":countryCode,
    		"password":password,
    		"loginType":loginType_web
    		};	
    console.log(JSON.stringify(data));
	ajaxPost(serverUrl,data,function successLogin(data,textStatus){
		console.log(data);
		
		//successCallback(loginUserSession);
	},
	function errorLogin(xhr,testStatus){
		//errorCallback(xhr,testStatus);
	});
}
/**
 * 请求短信认证码，
 * @param countryCode
 * @param phone
 * @param password
 * @param successCallback
 * @param errorCallback
 * @returns 返回transid，提交的时候需要输入transid和用户输入的authCode
 */
function ajaxGetAuthCode(countryCode,phone,successCallback,errorCallback)
{
	let serverUrl = getRootPath() + "/user/"+ "/getSmsValid";
    let data = {
    		"phone":phone
    		};	
    console.log(JSON.stringify(data));
	ajaxPost(serverUrl,data,function successLogin(data,textStatus){
		console.log(data);
		successCallback(data.retCode,data.responseInfo);
	},
	function errorLogin(xhr,testStatus){
		//errorCallback(xhr,testStatus);
	});
}

