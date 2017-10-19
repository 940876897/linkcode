需求

卫计委申请了一个公众号，牵头五家医院，开发一个含有五家医院的挂号预约等含有支付功能的微信公众号，但五家医院希望支付的钱付到各自
自己的商户号里。
以下统一称卫计委申请的公众号为“卫计委公众号 ”。五家医院的商户号分别为商户号001、商户号002、商户号003、商户号004、商户号005。

解决方法

1、部署get-weixin-code.html至五家医院的商户号的微信授权回调域名的目录下， 不一定是根目录，例如：http://www.001.com/xxx/get-weixin-code.html

2、卫计委公众号请求各医院get-weixin-code.html 带上收钱商户号appid和回调地址等信息例如： https://www.001.com/get-weixin-code.html?appid=wx001&scope=snsapi_base&state=业务参数&redirect_uri=https://www.xyz.com/back，各收钱商户号从微信那里拿到code之后会重新跳转回redirect_uri里面填写的回调url，并且在url后面带上code和state


解决方法分析

开发公众号支付时，在统一下单接口中要求必传用户openid，而获取openid则需要您在公众平台设置获取openid的域名（网页授权域名 ），
这里的五家医院网页授权域名各不相同。
为了方便理解对于redirect_uri这里未做urlEncode处理，实际开发请对redirect_uri做urlEncode处理

先看两个获取openID的例子

商户号001
页面回调域名：www.001.com
appid:wx001

https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx001&redirect_uri=https://www.001.com/get-weixin-code.html&response_type=code&scope=snsapi_base&state=hello-world&connect_redirect=1#wechat_redirect
访问这个地址时， 如果用户同意授权，页面将跳转至 https://www.001.com/get-weixin-code.html ?code=CODE&state=STATE
此时可以通过code和appid获取用户在商户号001的openID


商户号002
页面回调域名：www.002.com
appid:wx002

https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx002&redirect_uri=https://www.002.com/get-weixin-code.html&response_type=code&scope=snsapi_base&state=hello-world&connect_redirect=1#wechat_redirect
访问这个地址时， 如果用户同意授权，页面将跳转至 https://www.002.com/get-weixin-code.html ?code=CODE&state=STATE
此时可以通过code和appid获取用户在商户号002的openID

通过上面的两个例子，可以发现如果用户同意授权，最终会访问get-weixin-code.html 页面，那么get-weixin-code.html 页面可以将
code等信息location.href请求到某个回调地址去。回调地址可以接受code从而获取openID

那么可以得到解决办法如下：

卫计委公众号
接收商户号回调地址：https://www.xyz.com/back
各收钱商户号的appid和页面回调域名


卫计委公众号发起商户号001的get-weixin-code.html请求
https://www.001.com/get-weixin-code.html?appid=wx001&scope=snsapi_base&state=业务参数&redirect_uri=https://www.xyz.com/back
此时商户号001的get-weixin-code.html接受卫计委公众号的请求会将其转换为
https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx001&redirect_uri='https://www.001.com/get-weixin-code.html?appid=wx001&scope=snsapi_base&state=业务参数&redirect_uri=https://www.xyz.com/back '&response_type=code&scope=snsapi_base&state=hello-world&connect_redirect=1#wechat_redirect
 如果用户同意授权，页面将跳转至 'https://www.001.com/get-weixin-code.html?appid=wx001&scope=snsapi_base&state=业务参数&redirect_uri=https://www.xyz.com/back '?code=CODE&state=STATE,get-weixin-code.html判断如果code获取到了就location.href请求卫计委公众号的回调地址



get-weixin-code.html代码

<!DOCTYPE html>
<html lang="en">

    <head>
        <meta charset="UTF-8">
        <title>微信登录</title>
    </head>

    <body>
        <script>
            var GWC = {
                version: '1.1.1',
                urlParams: {},
                appendParams: function(url, params) {
                    if (params) {
                        var baseWithSearch = url.split('#')[0];
                        var hash = url.split('#')[1];
                        for (var key in params) {
                            var attrValue = params[key];
                            if (attrValue !== undefined) {
                                var newParam = key + "=" + attrValue;
                                if (baseWithSearch.indexOf('?') > 0) {
                                    var oldParamReg = new RegExp('^' + key + '=[-%.!~*\'\(\)\\w]*', 'g');
                                    if (oldParamReg.test(baseWithSearch)) {
                                        baseWithSearch = baseWithSearch.replace(oldParamReg, newParam);
                                    } else {
                                        baseWithSearch += "&" + newParam;
                                    }
                                } else {
                                    baseWithSearch += "?" + newParam;
                                }
                            }
                        }

                        if (hash) {
                            url = baseWithSearch + '#' + hash;
                        } else {
                            url = baseWithSearch;
                        }
                    }
                    return url;
                },
                getUrlParams: function() {
                    var pairs = location.search.substring(1).split('&');
                    for (var i = 0; i < pairs.length; i++) {
                        var pos = pairs[i].indexOf('=');
                        if (pos === -1) {
                            continue;
                        }
                        GWC.urlParams[pairs[i].substring(0, pos)] = decodeURIComponent(pairs[i].substring(pos + 1));
                    }
                },
                doRedirect: function() {
                    var code = GWC.urlParams['code'];
                    var appId = GWC.urlParams['appid'];
                    var scope = GWC.urlParams['scope'] || 'snsapi_base';
                    var state = GWC.urlParams['state'];
                    var isMp = GWC.urlParams['isMp']; //isMp为true时使用开放平台作授权登录，false为网页扫码登录
                    var baseUrl;
                    var redirectUri;

                    if (!code) {
                        baseUrl = "https://open.weixin.qq.com/connect/oauth2/authorize#wechat_redirect";
                        if(scope == 'snsapi_login' && !isMp){
                            baseUrl = "https://open.weixin.qq.com/connect/qrconnect";
                        }
                        //第一步，没有拿到code，跳转至微信授权页面获取code
                        redirectUri = GWC.appendParams(baseUrl, {
                            'appid': appId,
                            'redirect_uri': encodeURIComponent(location.href),
                            'response_type': 'code',
                            'scope': scope,
                            'state': state,
                        });
                    } else {
                        //第二步，从微信授权页面跳转回来，已经获取到了code，再次跳转到实际所需页面
                        redirectUri = GWC.appendParams(GWC.urlParams['redirect_uri'], {
                            'code': code,
                            'state': state
                        });
                    }

                    location.href = redirectUri;
                }
            };

            GWC.getUrlParams();
            GWC.doRedirect();
        </script>
    </body>

</html>



