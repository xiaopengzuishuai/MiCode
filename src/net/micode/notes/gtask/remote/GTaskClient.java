/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// 声明该类所在的包
package net.micode.notes.gtask.remote;

// 导入 Android 账户相关类
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
// 导入 Android 活动类
import android.app.Activity;
// 导入 Android 系统的 Bundle 类，用于在不同组件间传递数据
import android.os.Bundle;
// 导入 Android 文本工具类
import android.text.TextUtils;
// 导入 Android 日志工具类
import android.util.Log;

// 导入自定义的 Google 任务数据相关类
import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
// 导入自定义的异常类
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
// 导入自定义的 Google 任务字符串工具类
import net.micode.notes.tool.GTaskStringUtils;
// 导入自定义的笔记偏好设置活动类
import net.micode.notes.ui.NotesPreferenceActivity;

// 导入 Apache HTTP 客户端相关类
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
// 导入 JSON 处理相关类
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// 导入 Java 输入输出和压缩相关类
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * GTaskClient 类用于与 Google 任务服务进行交互，包括登录、创建任务和任务列表等操作。
 * 采用单例模式确保只有一个实例。
 */
public class GTaskClient {
    // 日志标签，用于在日志中标识该类的输出
    private static final String TAG = GTaskClient.class.getSimpleName();
    // Google 任务的基础 URL
    private static final String GTASK_URL = "https://mail.google.com/tasks/";
    // 用于获取 Google 任务数据的 URL
    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig";
    // 用于向 Google 任务服务发送 POST 请求的 URL
    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig";
    // GTaskClient 的单例实例
    private static GTaskClient mInstance = null;
    // Apache HTTP 客户端对象，用于发送 HTTP 请求
    private DefaultHttpClient mHttpClient;
    // 当前使用的 GET 请求 URL
    private String mGetUrl;
    // 当前使用的 POST 请求 URL
    private String mPostUrl;
    // 客户端版本号
    private long mClientVersion;
    // 登录状态标识
    private boolean mLoggedin;
    // 上次登录时间
    private long mLastLoginTime;
    // 操作 ID，用于标识不同的操作
    private int mActionId;
    // 当前使用的 Google 账户
    private Account mAccount;
    // 更新操作的 JSON 数组
    private JSONArray mUpdateArray;

    /**
     * 私有构造函数，确保只能通过 getInstance 方法获取实例
     */
    private GTaskClient() {
        mHttpClient = null;
        mGetUrl = GTASK_GET_URL;
        mPostUrl = GTASK_POST_URL;
        mClientVersion = -1;
        mLoggedin = false;
        mLastLoginTime = 0;
        mActionId = 1;
        mAccount = null;
        mUpdateArray = null;
    }

    /**
     * 获取 GTaskClient 的单例实例
     *
     * @return GTaskClient 的单例实例
     */
    public static synchronized GTaskClient getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskClient();
        }
        return mInstance;
    }

    /**
     * 登录 Google 任务服务
     *
     * @param activity 当前活动
     * @return 登录成功返回 true，失败返回 false
     */
    public boolean login(Activity activity) {
        // 假设 cookie 有效期为 5 分钟，超过则需要重新登录
        final long interval = 1000 * 60 * 5;
        if (mLastLoginTime + interval < System.currentTimeMillis()) {
            mLoggedin = false;
        }

        // 切换账户后需要重新登录
        if (mLoggedin
                && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity
                        .getSyncAccountName(activity))) {
            mLoggedin = false;
        }

        if (mLoggedin) {
            Log.d(TAG, "already logged in");
            return true;
        }

        mLastLoginTime = System.currentTimeMillis();
        // 登录 Google 账户获取认证令牌
        String authToken = loginGoogleAccount(activity, false);
        if (authToken == null) {
            Log.e(TAG, "login google account failed");
            return false;
        }

        // 如果是自定义域名账户，使用自定义 URL 登录
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase()
                .endsWith("googlemail.com"))) {
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/");
            int index = mAccount.name.indexOf('@') + 1;
            String suffix = mAccount.name.substring(index);
            url.append(suffix + "/");
            mGetUrl = url.toString() + "ig";
            mPostUrl = url.toString() + "r/ig";

            if (tryToLoginGtask(activity, authToken)) {
                mLoggedin = true;
            }
        }

        // 如果自定义域名登录失败，尝试使用 Google 官方 URL 登录
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL;
            mPostUrl = GTASK_POST_URL;
            if (!tryToLoginGtask(activity, authToken)) {
                return false;
            }
        }

        mLoggedin = true;
        return true;
    }

    /**
     * 登录 Google 账户并获取认证令牌
     *
     * @param activity        当前活动
     * @param invalidateToken 是否使现有令牌无效
     * @return 认证令牌，失败返回 null
     */
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken;
        AccountManager accountManager = AccountManager.get(activity);
        Account[] accounts = accountManager.getAccountsByType("com.google");

        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account");
            return null;
        }

        String accountName = NotesPreferenceActivity.getSyncAccountName(activity);
        Account account = null;
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                account = a;
                break;
            }
        }
        if (account != null) {
            mAccount = account;
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings");
            return null;
        }

        // 获取认证令牌
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account,
                "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult();
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken);
                loginGoogleAccount(activity, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed");
            authToken = null;
        }

        return authToken;
    }

    /**
     * 尝试登录 Google 任务服务
     *
     * @param activity  当前活动
     * @param authToken 认证令牌
     * @return 登录成功返回 true，失败返回 false
     */
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        if (!loginGtask(authToken)) {
            // 可能认证令牌过期，使令牌无效并重新尝试
            authToken = loginGoogleAccount(activity, true);
            if (authToken == null) {
                Log.e(TAG, "login google account failed");
                return false;
            }

            if (!loginGtask(authToken)) {
                Log.e(TAG, "login gtask failed");
                return false;
            }
        }
        return true;
    }

    /**
     * 使用认证令牌登录 Google 任务服务
     *
     * @param authToken 认证令牌
     * @return 登录成功返回 true，失败返回 false
     */
    private boolean loginGtask(String authToken) {
        int timeoutConnection = 10000;
        int timeoutSocket = 15000;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        mHttpClient = new DefaultHttpClient(httpParameters);
        BasicCookieStore localBasicCookieStore = new BasicCookieStore();
        mHttpClient.setCookieStore(localBasicCookieStore);
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false);

        // 发送登录请求
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken;
            HttpGet httpGet = new HttpGet(loginUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // 检查是否获取到认证 cookie
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) {
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) {
                Log.w(TAG, "it seems that there is no auth cookie");
            }

            // 获取客户端版本号
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            mClientVersion = js.getLong("v");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // 捕获所有异常
            Log.e(TAG, "httpget gtask_url failed");
            return false;
        }

        return true;
    }

    /**
     * 获取操作 ID
     *
     * @return 操作 ID
     */
    private int getActionId() {
        return mActionId++;
    }

    /**
     * 创建 HttpPost 请求对象
     *
     * @return HttpPost 请求对象
     */
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        httpPost.setHeader("AT", "1");
        return httpPost;
    }

    /**
     * 获取 HTTP 响应内容
     *
     * @param entity HTTP 响应实体
     * @return 响应内容
     * @throws IOException 输入输出异常
     */
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null;
        if (entity.getContentEncoding() != null) {
            contentEncoding = entity.getContentEncoding().getValue();
            Log.d(TAG, "encoding: " + contentEncoding);
        }

        InputStream input = entity.getContent();
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            input = new GZIPInputStream(entity.getContent());
        } else if (contentEncoding != null && contentEncoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true);
            input = new InflaterInputStream(entity.getContent(), inflater);
        }

        try {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            while (true) {
                String buff = br.readLine();
                if (buff == null) {
                    return sb.toString();
                }
                sb = sb.append(buff);
            }
        } finally {
            input.close();
        }
    }

    /**
     * 发送 POST 请求
     *
     * @param js 请求的 JSON 对象
     * @return 响应的 JSON 对象
     * @throws NetworkFailureException 网络失败异常
     */
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        HttpPost httpPost = createHttpPost();
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<BasicNameValuePair>();
            list.add(new BasicNameValuePair("r", js.toString()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
            httpPost.setEntity(entity);

            // 执行 POST 请求
            HttpResponse response = mHttpClient.execute(httpPost);
            String jsString = getResponseContent(response.getEntity());
            return new JSONObject(jsString);

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to jsonobject");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    /**
     * 创建新任务
     *
     * @param task 任务对象
     * @throws NetworkFailureException 网络失败异常
     */
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // 添加创建任务的操作到操作列表
            actionList.put(task.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送 POST 请求
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handing jsonobject failed");
        }
    }

    /**
     * 创建新的任务列表
     *
     * @param tasklist 任务列表对象
     * @throws NetworkFailureException 网络失败异常
     */
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // 添加创建任务列表的操作到操作列表
            actionList.put(tasklist.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送 POST 请求
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handing jsonobject failed");
        }
    }

    /**
     * 提交更新操作
     *
     * @throws NetworkFailureException 网络失败异常
     */
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) {
            try {
                JSONObject jsPost = new JSONObject();

                // action_list
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray);

                // client_version
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

                postRequest(jsPost);
                mUpdateArray = null;
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handing jsonobject failed");
            }
        }
    }

    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) {
            // too many update items may result in an error
            // set max to 10 items
            if (mUpdateArray != null && mUpdateArray.length() > 10) {
                commitUpdate();
            }

            if (mUpdateArray == null)
                mUpdateArray = new JSONArray();
            mUpdateArray.put(node.getUpdateAction(getActionId()));
        }
    }

    /**
     * 将任务从一个任务列表移动到另一个任务列表
     *
     * @param task      要移动的任务
     * @param preParent 任务的原父任务列表
     * @param curParent 任务的目标父任务列表
     * @throws NetworkFailureException 网络失败异常
     */
    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        // 提交之前的更新操作
        commitUpdate();
        try {
            // 创建一个新的 JSON 对象，用于构建 POST 请求的内容
            JSONObject jsPost = new JSONObject();
            // 创建一个 JSON 数组，用于存储操作列表
            JSONArray actionList = new JSONArray();
            // 创建一个 JSON 对象，用于存储单个操作
            JSONObject action = new JSONObject();

            // 开始构建操作的 JSON 对象
            // 设置操作类型为移动任务
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE);
            // 设置操作 ID
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            // 设置要移动的任务的 ID
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid());
            // 如果任务在同一个任务列表中移动，并且不是该列表中的第一个任务，则设置前置兄弟任务的 ID
            if (preParent == curParent && task.getPriorSibling() != null) {
                // 仅当在任务列表内移动且不是第一个任务时才设置前置兄弟任务的 ID
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling());
            }
            // 设置源任务列表的 ID
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid());
            // 设置目标父任务列表的 ID
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid());
            // 如果任务是在不同的任务列表之间移动，则设置目标任务列表的 ID
            if (preParent != curParent) {
                // 仅当在不同任务列表之间移动时才设置目标任务列表的 ID
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid());
            }
            // 将操作添加到操作列表中
            actionList.put(action);
            // 将操作列表添加到 POST 请求的 JSON 对象中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 设置客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送 POST 请求
            postRequest(jsPost);

        } catch (JSONException e) {
            // 记录 JSON 处理异常
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("move task: handing jsonobject failed");
        }
    }


    /**
     * 删除指定的节点
     *
     * @param node 要删除的节点
     * @throws NetworkFailureException 网络失败异常
     */
    public void deleteNode(Node node) throws NetworkFailureException {
        // 提交之前的更新操作
        commitUpdate();
        try {
            // 创建一个新的 JSON 对象，用于构建 POST 请求的内容
            JSONObject jsPost = new JSONObject();
            // 创建一个 JSON 数组，用于存储操作列表
            JSONArray actionList = new JSONArray();

            // action_list
            // 将节点标记为已删除
            node.setDeleted(true);
            // 获取节点的更新操作，并添加到操作列表中
            actionList.put(node.getUpdateAction(getActionId()));
            // 将操作列表添加到 POST 请求的 JSON 对象中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            // 设置客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送 POST 请求
            postRequest(jsPost);
            // 清空更新操作数组
            mUpdateArray = null;
        } catch (JSONException e) {
            // 记录 JSON 处理异常
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("delete node: handing jsonobject failed");
        }
    }


    /**
     * 获取所有任务列表
     *
     * @return 包含所有任务列表的 JSON 数组
     * @throws NetworkFailureException 网络请求失败时抛出该异常
     */
    public JSONArray getTaskLists() throws NetworkFailureException {
        // 检查是否已经登录，如果未登录则记录错误日志并抛出异常
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        try {
            // 创建一个 HttpGet 请求对象，使用当前的 GET 请求 URL
            HttpGet httpGet = new HttpGet(mGetUrl);
            // 初始化 HttpResponse 对象
            HttpResponse response = null;
            // 执行 HttpGet 请求并获取响应
            response = mHttpClient.execute(httpGet);

            // 获取任务列表
            // 从响应实体中获取响应内容
            String resString = getResponseContent(response.getEntity());
            // 定义 JSON 字符串的起始标记
            String jsBegin = "_setup(";
            // 定义 JSON 字符串的结束标记
            String jsEnd = ")}</script>";
            // 查找 JSON 字符串的起始位置
            int begin = resString.indexOf(jsBegin);
            // 查找 JSON 字符串的结束位置
            int end = resString.lastIndexOf(jsEnd);
            // 初始化 JSON 字符串变量
            String jsString = null;
            // 如果找到了起始和结束位置，并且起始位置在结束位置之前
            if (begin != -1 && end != -1 && begin < end) {
                // 提取 JSON 字符串
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            // 将 JSON 字符串解析为 JSONObject
            JSONObject js = new JSONObject(jsString);
            // 从 JSONObject 中获取包含任务列表的 JSON 数组并返回
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS);
        } catch (ClientProtocolException e) {
            // 记录 HTTP 协议异常日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出网络请求失败异常
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (IOException e) {
            // 记录输入输出异常日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出网络请求失败异常
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (JSONException e) {
            // 记录 JSON 处理异常日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("get task lists: handing jasonobject failed");
        }
    }

    /**
     * 根据任务列表的全局 ID 获取该任务列表中的所有任务
     *
     * @param listGid 任务列表的全局唯一标识符
     * @return 包含该任务列表中所有任务的 JSON 数组
     * @throws NetworkFailureException 当网络请求失败时抛出此异常
     */
    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        // 提交之前的更新操作，确保之前的操作已经完成
        commitUpdate();
        try {
            // 创建一个新的 JSON 对象，用于构建 POST 请求的内容
            JSONObject jsPost = new JSONObject();
            // 创建一个 JSON 数组，用于存储操作列表
            JSONArray actionList = new JSONArray();
            // 创建一个 JSON 对象，用于存储单个操作
            JSONObject action = new JSONObject();

            // action_list
            // 设置操作类型为获取所有任务
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL);
            // 设置操作 ID
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            // 设置要获取任务的任务列表的 ID
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid);
            // 设置是否获取已删除的任务，这里设置为 false 表示不获取
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false);
            // 将操作添加到操作列表中
            actionList.put(action);
            // 将操作列表添加到 POST 请求的 JSON 对象中
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // client_version
            // 设置客户端版本号
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送 POST 请求
            JSONObject jsResponse = postRequest(jsPost);
            // 从响应中提取包含任务的 JSON 数组并返回
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS);
        } catch (JSONException e) {
            // 记录 JSON 处理异常
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("get task list: handing jsonobject failed");
        }
    }

    public Account getSyncAccount() {
        return mAccount;
    }

    public void resetUpdateArray() {
        mUpdateArray = null;
    }
}
